package de.ascendro.f4m.server.multiplayer.move.dao;

import static de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState.INVITED;
import static de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDaoImpl.GAME_INSTANCE_ID_BIN_NAME;
import static de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDaoImpl.INDEX_BIN_NAME;
import static de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDaoImpl.VALUE_BIN_NAME;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aerospike.client.Operation;
import com.aerospike.client.Value;
import com.aerospike.client.cdt.MapOperation;
import com.aerospike.client.cdt.MapPolicy;
import com.aerospike.client.cdt.MapReturnType;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeOperateDaoImpl;
import de.ascendro.f4m.server.config.GameConfigImpl;
import de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDao;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDaoImpl;
import de.ascendro.f4m.server.multiplayer.dao.MultiplayerGameInstancePrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.game.selection.model.multiplayer.MultiplayerUserGameInstance;
import de.ascendro.f4m.service.util.F4MEnumUtils;

public class MoveMultiplayerGameInstanceDaoImpl extends AerospikeOperateDaoImpl<MultiplayerGameInstancePrimaryKeyUtil>
		implements MoveMultiplayerGameInstanceDao {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MoveMultiplayerGameInstanceDaoImpl.class);
	
	private final CommonMultiplayerGameInstanceDao mgiDao;

	@Inject
	public MoveMultiplayerGameInstanceDaoImpl(Config config, MultiplayerGameInstancePrimaryKeyUtil primaryKeyUtil,
			JsonUtil jsonUtil, AerospikeClientProvider aerospikeClientProvider,
			CommonMultiplayerGameInstanceDao mgiDao) {
		super(config, primaryKeyUtil, jsonUtil, aerospikeClientProvider);
		this.mgiDao = mgiDao;
	}

	@Override
	public void moveUserMgiData(String tenantId, String appId, String sourceUserId, String targetUserId) {
		String sourceKey = primaryKeyUtil.createInvitationListKey(tenantId, appId, sourceUserId);
		Stream.of(MultiplayerGameInstanceState.values()).forEach(state -> {
			Map<String, String> invitations = getAllMap(getSet(), sourceKey, state.getBinName());
			Map<String, String> notifications = getAllMap(getSet(), sourceKey, CommonMultiplayerGameInstanceDaoImpl.NOTIFICATION_BIN_NAME);
			if (MapUtils.isNotEmpty(invitations)) {
				moveReferencesToMgi(tenantId, appId, invitations, sourceUserId, targetUserId);
				moveUserInvitations(tenantId, appId, targetUserId, invitations, state);
				moveUserScheduledNotifications(tenantId, appId, targetUserId, notifications);
			}
		});
		deleteSilently(getSet(), sourceKey);
	}

	protected void moveReferencesToMgi(String tenantId, String appId, Map<String, String> invitations, String sourceUserId, String targetUserId) {
		invitations.entrySet().forEach(invitation -> {
			String mgiId = invitation.getKey();

			String sourceKey = primaryKeyUtil.createIndexKeyToInstanceRecord(mgiId, sourceUserId);
			String mgiKey = readString(getIndexSet(), sourceKey, INDEX_BIN_NAME);
			if (StringUtils.isNotBlank(mgiKey)) {
				changeInviter(tenantId, appId, mgiId, sourceUserId, targetUserId);
				changeGameCreatorId(mgiId, sourceUserId, targetUserId);
				changeUserDataInMgi(tenantId, mgiId, mgiKey, sourceUserId, targetUserId);

				// move reference to current MGI
				String targetKey = primaryKeyUtil.createIndexKeyToInstanceRecord(mgiId, targetUserId);
				createOrUpdateString(getIndexSet(), targetKey, INDEX_BIN_NAME, (r, wp) -> mgiKey);
				deleteSilently(getIndexSet(), sourceKey);
			} else {
				LOGGER.warn(
						"Failed to move references to MGI, reference not found; mgiId [{}]; sourceUserId [{}]; targetUserId [{}]",
						mgiId, sourceUserId, targetUserId);
			}
		});
	}
	
	private void moveUserInvitations(String tenantId, String appId, String targetUserId, Map<String, String> invitations, MultiplayerGameInstanceState state) {
		String targetKey = primaryKeyUtil.createInvitationListKey(tenantId, appId, targetUserId);
		super.<String, String> createOrUpdateMap(getSet(), targetKey, state.getBinName(), (existingValue, wp) -> {
			if (existingValue != null) {
				invitations.putAll(existingValue);
			}
			return invitations;
		});
	}

	private void moveUserScheduledNotifications(String tenantId, String appId, String targetUserId,
			Map<String, String> notifications) {
		String targetKey = primaryKeyUtil.createInvitationListKey(tenantId, appId, targetUserId);
		super.<String, String> createOrUpdateMap(getSet(), targetKey, CommonMultiplayerGameInstanceDaoImpl.NOTIFICATION_BIN_NAME, (existingValue, wp) -> {
			if (existingValue != null) {
				notifications.putAll(existingValue);
			}
			return notifications;
		});
	}

	protected void changeUserDataInMgi(String tenantId, String mgiId, String mgiKey, String sourceUserId, String targetUserId) {
		// move state record in "invited" bin
		moveMapRecord(getSet(), mgiKey, INVITED.getBinName(), sourceUserId, targetUserId);
		
		// change userId in MultiplayerUserGameInstance
		String state = getByKeyFromMap(getSet(), mgiKey, INVITED.getBinName(), targetUserId);
		changeUserInUserGameInstance(mgiId, mgiKey, state, sourceUserId, targetUserId);
		
		moveReferenceToGameInstanceId(mgiId, sourceUserId, targetUserId);
	}

	private void changeUserInUserGameInstance(String mgiId, String mgiKey, String state, String sourceUserId, String targetUserId) {
		MultiplayerGameInstanceState stateEnum = F4MEnumUtils.getEnum(MultiplayerGameInstanceState.class, state);
		if (stateEnum != null && stateEnum != INVITED) {
			String gameInstanceId = mgiDao.getGameInstanceId(mgiId, sourceUserId);
			Operation read = MapOperation.getByKey(stateEnum.getBinName(), Value.get(gameInstanceId), MapReturnType.VALUE);
			operate(getSet(), mgiKey, new Operation[] { read }, (readResult, wp) -> {
				if (readResult != null && readResult.getString(stateEnum.getBinName()) != null) {
					String jsonString = readResult.getString(stateEnum.getBinName());
					MultiplayerUserGameInstance userGameInstance = jsonUtil.fromJson(jsonString, MultiplayerUserGameInstance.class);
					userGameInstance.setUserId(targetUserId);
					Operation put = MapOperation.put(MapPolicy.Default, stateEnum.getBinName(), Value.get(gameInstanceId), Value.get(jsonUtil.toJson(userGameInstance)));
					return Arrays.asList(put);
				} else {
					LOGGER.warn(
							"Failed to change userId in User Game Instance, entry not found; mgiId [{}]; gameInstanceId [{}]; sourceUserId [{}]; targetUserId [{}]",
							mgiId, gameInstanceId, sourceUserId, targetUserId);
					return Collections.emptyList();
				}
			});
		} else if (stateEnum == null) {
			LOGGER.warn(
					"Failed to change userId in User Game Instance, invalid state; mgiId [{}]; state [{}]; sourceUserId [{}]; targetUserId [{}]",
					mgiId, state, sourceUserId, targetUserId);
		}
	}
	
	private void moveReferenceToGameInstanceId(String mgiId, String sourceUserId, String targetUserId) {
		String sourceKey = primaryKeyUtil.createGameInstanceIdKey(mgiId, sourceUserId);
		String gameInstanceId = readString(getSet(), sourceKey, GAME_INSTANCE_ID_BIN_NAME);
		if (gameInstanceId != null) {
			String targetKey = primaryKeyUtil.createGameInstanceIdKey(mgiId, targetUserId);
			createOrUpdateString(getSet(), targetKey, GAME_INSTANCE_ID_BIN_NAME, (r, wp) -> gameInstanceId);
			deleteSilently(getSet(), sourceKey);
		} else {
			LOGGER.warn(
					"Failed to move reference to game instance ID, record not found; mgiId [{}]; sourceUserId [{}]; targetUserId [{}]",
					mgiId, sourceUserId, targetUserId);
		}
	}
	
	protected void changeInviter(String tenantId, String appId, String mgiId, String sourceUserId, String targetUserId) {
		Map<String, String> allUsersOfMgi = mgiDao.getAllUsersOfMgi(mgiId);
		allUsersOfMgi.entrySet().forEach(entry -> {
			String inviteeId = entry.getKey();
			String state = entry.getValue();
			MultiplayerGameInstanceState stateEnum = F4MEnumUtils.getEnum(MultiplayerGameInstanceState.class, state);
			if (stateEnum != null) {
				String key = primaryKeyUtil.createInvitationListKey(tenantId, appId, inviteeId);
				changeMapValue(getSet(), key, stateEnum.getBinName(), mgiId, sourceUserId, targetUserId);
			} else {
				LOGGER.warn(
						"Failed to change inviterId in list of invitations, invalid state; state [{}]; sourceUserId [{}]; targetUserId [{}]",
						state, sourceUserId, targetUserId);
			}
		});
	}
	
	protected void changeGameCreatorId(String mgiId, String sourceUserId, String targetUserId) {
		String key = primaryKeyUtil.createMetaKey(mgiId);
		String customGameConfigJson = readJson(getSet(), key, VALUE_BIN_NAME);
		CustomGameConfig customGameConfig = jsonUtil.fromJson(customGameConfigJson, CustomGameConfig.class);
		if (StringUtils.equals(customGameConfig.getGameCreatorId(), sourceUserId)) {
			customGameConfig.setGameCreatorId(targetUserId);
			updateJson(getSet(), key, VALUE_BIN_NAME, (json, wp) -> jsonUtil.toJson(customGameConfig));
		}
	}
	
	/** Change 'key' of map value from 'sourceMapKey' to 'targetMapKey' */
	private void moveMapRecord(String set, String key, String bin, String sourceMapKey, String targetMapKey) {
		Operation read = MapOperation.getByKey(bin, Value.get(sourceMapKey), MapReturnType.VALUE);
		operate(set, key, new Operation[] { read }, (readResult, wp) -> {
			if (readResult != null && readResult.getValue(bin) != null) {
				Object value = readResult.getValue(bin);
				Operation put = MapOperation.put(MapPolicy.Default, bin, Value.get(targetMapKey), Value.get(value));
				Operation remove = MapOperation.removeByKey(bin, Value.get(sourceMapKey), MapReturnType.NONE);
				return Arrays.asList(put, remove);
			} else {
				LOGGER.warn(
						"Failed to move map record, record not found; key [{}]; bin [{}]; sourceMapKey [{}]; targetMapKey [{}]",
						key, bin, sourceMapKey, targetMapKey);
				return Collections.emptyList();
			}
		});
	}
	
	/** Change map 'value' to 'newValue' if it is equal to 'oldValue' */
	private void changeMapValue(String set, String key, String bin, String mapKey, Object oldValue, Object newValue) {
		Operation read = MapOperation.getByKey(bin, Value.get(mapKey), MapReturnType.VALUE);
		operate(set, key, new Operation[] { read }, (readResult, wp) -> {
			List<Operation> operations;
			if (readResult != null && readResult.getValue(bin) != null) {
				Object value = readResult.getValue(bin);
				if (Objects.equals(value, oldValue)) {
					Operation put = MapOperation.put(MapPolicy.Default, bin, Value.get(mapKey), Value.get(newValue));
					operations = Arrays.asList(put);
				} else {
					operations = Collections.emptyList();
				}
			} else {
				LOGGER.warn(
						"Failed to change map value, record not found; key [{}]; bin [{}]; oldValue [{}]; newValue [{}]",
						key, bin, oldValue, newValue);
				operations = Collections.emptyList();
			}
			return operations;
		});
	}

	private String getSet() {
		return config.getProperty(GameConfigImpl.AEROSPIKE_MULTIPLAYER_SET);
	}
	
	protected String getIndexSet() {
		return config.getProperty(GameConfigImpl.AEROSPIKE_MULTIPLAYER_INDEX_SET);
	}

}
