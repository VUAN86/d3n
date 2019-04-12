package de.ascendro.f4m.server.multiplayer.dao;

import static de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState.CALCULATED;
import static de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState.CANCELLED;
import static de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState.DECLINED;
import static de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState.DELETED;
import static de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState.EXPIRED;
import static de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState.INVITED;
import static de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState.PENDING;
import static de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState.REGISTERED;
import static de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState.STARTED;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.aerospike.client.*;
import com.aerospike.client.query.RecordSet;
import com.aerospike.client.query.Statement;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Operation;
import com.aerospike.client.Record;
import com.aerospike.client.Value;
import com.aerospike.client.cdt.MapOperation;
import com.aerospike.client.cdt.MapPolicy;
import com.aerospike.client.cdt.MapReturnType;
import com.google.gson.JsonObject;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeOperateDaoImpl;
import de.ascendro.f4m.server.config.GameConfigImpl;
import de.ascendro.f4m.server.game.GameUtil;
import de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.client.F4MEntryAlreadyExistsException;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.game.selection.exception.F4MGameInvitationNotValidException;
import de.ascendro.f4m.service.game.selection.exception.F4MGameParticipantCountExceededException;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CreatedBy;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.game.selection.model.multiplayer.Invitation;
import de.ascendro.f4m.service.game.selection.model.multiplayer.InvitedUser;
import de.ascendro.f4m.service.game.selection.model.multiplayer.MultiplayerUserGameInstance;
import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.OrderBy;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.F4MEnumUtils;

public class CommonMultiplayerGameInstanceDaoImpl extends AerospikeOperateDaoImpl<MultiplayerGameInstancePrimaryKeyUtil>
		implements CommonMultiplayerGameInstanceDao {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CommonMultiplayerGameInstanceDaoImpl.class);
	
	public static final String VALUE_BIN_NAME = "value";
    public static final String MGI_SET_NAME = "mgi";
	public static final String INDEX_BIN_NAME = "index";
	public static final String CAPACITY_BIN_NAME = "capacity";
	public static final String NOTIFICATION_BIN_NAME = "notification";
	public static final String ENTRY_COUNT_BIN_NAME = "entryCount";
	public static final String GAME_INSTANCE_ID_BIN_NAME = "gameInstanceId";

	public static final String CURRENT_MGI_ID_BIN_NAME = "currentMgiId";
	public static final String FINISHED_MGI_ID_BIN_NAME = "finishedMgiId";

	public static final String NOT_YET_CALCULATED_COUNTER_BIN = "notYetCalCnt";
	public static final String TOTAL_GAME_INSTANCE_COUNTER_BIN = "giCounter";
	public static final String DUEL_SET_NAME = "duel";
	public static final String MGI_DUEL_BIN_NAME = "mgiIdDuel";
	private static final String EXCEPTION_MSG_USER_NOT_INVITED = "User is not invited (mgiId=[%s], userId=[%s])";
	
	@Inject
	public CommonMultiplayerGameInstanceDaoImpl(Config config,
			MultiplayerGameInstancePrimaryKeyUtil multiplayerGameInstancePrimaryKeyUtil, JsonUtil jsonUtil,
			AerospikeClientProvider aerospikeClientProvider) {
		super(config, multiplayerGameInstancePrimaryKeyUtil, jsonUtil, aerospikeClientProvider);
	}

	@Override
	public String create(String userId, CustomGameConfig customGameConfig) {
		final String mgiId = primaryKeyUtil.generateId();
		customGameConfig.setId(mgiId);

		// Create config
		final String customGameConfigKey = primaryKeyUtil.createMetaKey(customGameConfig.getId());
		createJson(getSet(), customGameConfigKey, VALUE_BIN_NAME, jsonUtil.toJson(customGameConfig));

		// update capacity
		final String metaKey = primaryKeyUtil.createMetaKey(mgiId);
		createRecord(getIndexSet(), metaKey, getLongBin(CAPACITY_BIN_NAME, getRecordCapacity()),
				getLongBin(ENTRY_COUNT_BIN_NAME, 0L));
		
		// Add creator
		if (userId != null) {
			addUser(mgiId, userId, userId, INVITED);
		}

		return mgiId;
	}

	public void createDuel(String userId, CustomGameConfig customGameConfig, String mgiId) {
		final String metaKey = primaryKeyUtil.createMetaKey(mgiId);
		createRecord1(DUEL_SET_NAME, metaKey,
				getStringBin(MGI_DUEL_BIN_NAME, mgiId));
	}

	public void deleteDuel(String mgiId) {
		final String metaKey = primaryKeyUtil.createMetaKey(mgiId);
		final Key k = new Key(getNamespace(),  DUEL_SET_NAME, metaKey);
		deleteSilently(k);
	}

	public void updateDuel(String userId, CustomGameConfig customGameConfig, String mgiId, long decrement) {
		final String metaKey = primaryKeyUtil.createMetaKey(mgiId);
		createRecord1(getIndexSet(), metaKey, getLongBin(CAPACITY_BIN_NAME, getRecordCapacity()),
				getLongBin(ENTRY_COUNT_BIN_NAME, decrement));
	}

	@Override
	public void addUser(String mgiId, String userId, String inviterId, MultiplayerGameInstanceState state) {
		if (getIndexKeyToInstanceRecord(mgiId, userId) == null) {
			long recordNumber = updateEntryCountAndGetAvailableRecordNumber(mgiId);

			// add user
			String instanceKey = primaryKeyUtil.createInstanceRecordKey(mgiId, recordNumber);
			createOrUpdateMapValueByKey(getSet(), instanceKey, INVITED.getBinName(), userId,
					(v, wp) -> state.name());

			// save index to record
			String indexKey = primaryKeyUtil.createIndexKeyToInstanceRecord(mgiId, userId);
			createString(getIndexSet(), indexKey, INDEX_BIN_NAME, instanceKey);

			// add instance to list of all invitations of user
			addInstanceToInvitationList(userId, mgiId, inviterId, state);
		} else {
			throw new F4MEntryAlreadyExistsException(String.format("User is already added (mgiId=[%s], userId=[%s])", mgiId, userId));
		}
	}
	
	private long updateEntryCountAndGetAvailableRecordNumber(String mgiId) {
		String key = primaryKeyUtil.createMetaKey(mgiId);

		Record readRecord = operate(getIndexSet(), key, new Operation[] { Operation.get(ENTRY_COUNT_BIN_NAME) },
				(readResult, wp) -> {
					if (readResult != null && readResult.getValue(ENTRY_COUNT_BIN_NAME) != null) {
						return Arrays.asList(Operation.add(getLongBin(ENTRY_COUNT_BIN_NAME, 1L)), Operation.get(CAPACITY_BIN_NAME));
					} else {
						throw new F4MEntryNotFoundException(String.format("Multiplayer Game Instance index record not found (mgiId=[%s])", mgiId));
					}
				});

		// XXX: There is possibility that readRecord is not up to date: concurrency problem

		if (readRecord.getLong(CAPACITY_BIN_NAME) <= 0) {
			throw new F4MFatalErrorException(String.format("Invalid capacity of Multiplayer Game Instance index record (mgiId=[%s])", mgiId));
		}

		return (readRecord.getLong(ENTRY_COUNT_BIN_NAME) - 1) / readRecord.getLong(CAPACITY_BIN_NAME) + 1;
	}
	
	@Override
	public List<String> activateInvitations(String mgiId) {
		List<String> pendingUsers = getAllUsersByState(mgiId, PENDING);
		List<String> invitedUsers = new ArrayList<>(pendingUsers.size());
		for (String userId : pendingUsers) {
			boolean isActivated = activateUserAtInstanceRecord(mgiId, userId);
			if (isActivated) {
				changeInvitationStatus(userId, mgiId, PENDING, INVITED);
				invitedUsers.add(userId);
			}
		}
		return invitedUsers;
	}

	private boolean activateUserAtInstanceRecord(String mgiId, String userId) {
		AtomicBoolean result = new AtomicBoolean(false);
		String indexKeyToInstanceRecord = getIndexKeyToInstanceRecord(mgiId, userId);
		Operation[] readOperations = createGetOperation(userId, MapReturnType.VALUE, INVITED);
		operate(getSet(), indexKeyToInstanceRecord, readOperations, (readResult, wp) -> {
			if (readResult != null && StringUtils.equals(readResult.getString(INVITED.getBinName()), PENDING.name())) {
				Operation changeState = MapOperation.put(MapPolicy.Default, INVITED.getBinName(), Value.get(userId), Value.get(INVITED.name()));
				result.set(true);
				return Arrays.asList(changeState);
			} else {
				LOGGER.error("User in state PENDING not found (mgiId=[{}]; userId=[{}])", mgiId, userId);
				return Collections.emptyList();
			}
		});
		return result.get();
	}

	@Override
	public CustomGameConfig getConfig(String mgiId) throws F4MEntryNotFoundException{
		final String key = primaryKeyUtil.createMetaKey(mgiId);
		final String customGameConfigAsString = readJson(getSet(), key, VALUE_BIN_NAME);
		if(customGameConfigAsString != null){
			return jsonUtil.fromJson(customGameConfigAsString, CustomGameConfig.class);
		}else{
			throw new F4MEntryNotFoundException(String.format("Custom game config not found by mgiId [%s]", mgiId));
		}
	}

    @Override
    public CustomGameConfig getConfigForFinish(String mgiId) throws F4MEntryNotFoundException{
        final String key = primaryKeyUtil.createMetaMgiKey(mgiId);
        final String customGameConfigAsString = readJson(MGI_SET_NAME, key, VALUE_BIN_NAME);
        if(customGameConfigAsString != null){
            return jsonUtil.fromJson(customGameConfigAsString, CustomGameConfig.class);
        }else{
            throw new F4MEntryNotFoundException(String.format("Custom game config not found by mgiId [%s]", mgiId));
        }
    }

	@Override
	public void joinGame(String mgiId, String gameInstanceId, String userId, Double userHandicap) {
		final String indexKeyToInstanceRecord = getIndexKeyToInstanceRecord(mgiId, userId);

		final Operation[] readOperations = createGetOperation(gameInstanceId, MapReturnType.VALUE, REGISTERED);
		operate(getSet(), indexKeyToInstanceRecord, readOperations, (readResult, wp) -> {
			if (readResult == null || readResult.getValue(REGISTERED.getBinName()) == null) {
				throw new F4MEntryNotFoundException(String.format("User is not registered (mgiId=[%s], userId=[%s])", mgiId, userId));
			}  else {
				final String multiplayerUserGameInstanceAsString = readResult.getString(REGISTERED.getBinName());
				final MultiplayerUserGameInstance multiplayerUserGameInstance = jsonUtil.fromJson(multiplayerUserGameInstanceAsString, MultiplayerUserGameInstance.class);
				multiplayerUserGameInstance.setUserHandicap(userHandicap);
				return moveState(REGISTERED, gameInstanceId, STARTED, gameInstanceId, multiplayerUserGameInstance, 
						mgiId, userId);
			}
		});		
	}
	
	@Override
	public MultiplayerGameInstanceState cancelGameInstance(String mgiId, String gameInstanceId, String userId) {
		MultiplayerGameInstanceState[] fromStates = { REGISTERED, STARTED };
		return changeGameInstanceState(mgiId, gameInstanceId, userId, fromStates, CANCELLED);
	}
	
	@Override
	public void markGameInstanceAsCalculated(String mgiId, String gameInstanceId, String userId) {
		MultiplayerGameInstanceState[] fromStates = { STARTED, CANCELLED };
		changeGameInstanceState(mgiId, gameInstanceId, userId, fromStates, CALCULATED);	
	}
	
	private MultiplayerGameInstanceState changeGameInstanceState(String mgiId, String gameInstanceId, String userId,
			MultiplayerGameInstanceState[] fromStates, MultiplayerGameInstanceState toState) {
		String indexKeyToInstanceRecord = getIndexKeyToInstanceRecord(mgiId, userId);
		MutableObject<MultiplayerGameInstanceState> fromState = new MutableObject<>();

		Operation[] readOperations = createGetOperation(gameInstanceId, MapReturnType.VALUE, fromStates);
		operate(getSet(), indexKeyToInstanceRecord, readOperations, (readResult, wp) -> {
			fromState.setValue(getGameInstanceState(readResult, fromStates));

			// move
			String multiplayerUserGameInstanceAsString = readResult.getString(fromState.getValue().getBinName());
			MultiplayerUserGameInstance multiplayerUserGameInstance = jsonUtil
					.fromJson(multiplayerUserGameInstanceAsString, MultiplayerUserGameInstance.class);
			return moveState(fromState.getValue(), gameInstanceId, toState, gameInstanceId, multiplayerUserGameInstance,
					mgiId, userId);
		});
		return fromState.getValue();
	}
	
	private MultiplayerGameInstanceState getGameInstanceState(Record record, MultiplayerGameInstanceState... possibleStates) {
		MultiplayerGameInstanceState state;
		if (record != null) {
			Optional<MultiplayerGameInstanceState> gameInstanceState = Stream.of(possibleStates)
					.filter(s -> record.getValue(s.getBinName()) != null)
					.findFirst();
			if (gameInstanceState.isPresent()) {
				state = gameInstanceState.get();
			} else {
				throw new F4MEntryNotFoundException(
						String.format("Game instance not found (states=[%s])", Arrays.toString(possibleStates)));
			}
		} else {
			throw new F4MEntryNotFoundException(
					String.format("Game instance not found (states=[%s])", Arrays.toString(possibleStates)));
		}
		return state;
	}
	
	@Override
	public boolean hasNoRemainingGameInstances(String mgiId) {
		final String gameInstanceCounterKey = primaryKeyUtil.createGameInstanceCounterKey(mgiId);
		final Record counterRecord = readRecord(getSet(), gameInstanceCounterKey);
		
		boolean allGameInstancesCalculatedOrCancelled = false;
		final Long totalGameInstances = (Long) counterRecord.getValue(TOTAL_GAME_INSTANCE_COUNTER_BIN);
		if (totalGameInstances != null && totalGameInstances > 0) {
			final Long notYetCalculatedInstances = (Long) counterRecord.getValue(NOT_YET_CALCULATED_COUNTER_BIN);

			if (notYetCalculatedInstances != null && notYetCalculatedInstances == 0) {
				allGameInstancesCalculatedOrCancelled = true;
			}
		}
		
		return allGameInstancesCalculatedOrCancelled;
	}
	
	@Override
	public int getGameInstancesCount(String mgiId) {
		final String gameInstanceCounterKey = primaryKeyUtil.createGameInstanceCounterKey(mgiId);
		final Long gameInstanceCount = readLong(getSet(), gameInstanceCounterKey, TOTAL_GAME_INSTANCE_COUNTER_BIN);
		return Optional.ofNullable(gameInstanceCount).orElse(0L).intValue();
	}
	
	@Override
	public int getNotYetCalculatedGameInstancesCount(String mgiId) {
		final String gameInstanceCounterKey = primaryKeyUtil.createGameInstanceCounterKey(mgiId);
		final Long notYetCalculatedGameInstanceCount = readLong(getSet(), gameInstanceCounterKey, NOT_YET_CALCULATED_COUNTER_BIN);
		return Optional.ofNullable(notYetCalculatedGameInstanceCount).orElse(0L).intValue();
	}
	
	@Override
	public boolean isMultiplayerInstanceAvailable(String mgiId) {
		final String metaKey = primaryKeyUtil.createMetaKey(mgiId);
		return exists(getIndexSet(), metaKey);
	}
	
	@Override
	public void addToNotYetCalculatedCounter(String mgiId, int amount){
		final String gameInstanceCounterKey = primaryKeyUtil.createGameInstanceCounterKey(mgiId);
		add(getSet(), gameInstanceCounterKey, NOT_YET_CALCULATED_COUNTER_BIN, amount);
	}
	
	@Override
	public void addToGameInstancesCounter(String mgiId, int amount){
		final String gameInstanceCounterKey = primaryKeyUtil.createGameInstanceCounterKey(mgiId);
		add(getSet(), gameInstanceCounterKey, TOTAL_GAME_INSTANCE_COUNTER_BIN, amount);
	}

	@Override
	public void registerForGame(String mgiId, ClientInfo clientInfo, String gameInstanceId) {
		final String indexKeyToInstanceRecord = getIndexKeyToInstanceRecord(mgiId, clientInfo.getUserId());

		final Operation[] readOperations = createGetOperation(clientInfo.getUserId(), MapReturnType.VALUE, INVITED);
		operate(getSet(), indexKeyToInstanceRecord, readOperations, (readResult, wp) -> {
			if (!hasValidInvitationState(readResult, INVITED)) {
				final String currentStateAsString = readResult.getString(INVITED.getBinName());
				throw new F4MGameInvitationNotValidException(
						String.format("User is not invited anymore (mgiId=[%s], userId=[%s], currentState=[%s])", mgiId, clientInfo.getUserId(), currentStateAsString));
			} else if (!hasValidParticipantCount(mgiId)) {
				throw new F4MGameParticipantCountExceededException(
						String.format("Max registered user count has been reached (mgiId=[%s], userId=[%s])", mgiId, clientInfo.getUserId()));
			} else {
				final MultiplayerUserGameInstance instance = new MultiplayerUserGameInstance(gameInstanceId, clientInfo);
				final Operation changeState = MapOperation.put(MapPolicy.Default, INVITED.getBinName(), Value.get(clientInfo.getUserId()), Value.get(REGISTERED.name()));
				final Operation putToStateOperation = MapOperation.put(MapPolicy.Default, REGISTERED.getBinName(), Value.get(gameInstanceId), Value.get(jsonUtil.toJson(instance)));
				return Arrays.asList(changeState, putToStateOperation);
			}
		});
		changeInvitationStatus(clientInfo.getUserId(), mgiId, INVITED, REGISTERED);
		saveReferenceToGameInstance(mgiId, clientInfo.getUserId(), gameInstanceId);
	}
	
	private void changeInvitationStatus(String userId, String mgiId, MultiplayerGameInstanceState oldStatus,
			MultiplayerGameInstanceState newStatus) {
		CustomGameConfig gameConfig = getConfig(mgiId);
		String tenantId = gameConfig.getTenantId();
		String appId = gameConfig.getAppId();
		String invitationListKey = primaryKeyUtil.createInvitationListKey(tenantId, appId, userId);

		Operation[] readOperations = createGetOperation(mgiId, MapReturnType.VALUE, oldStatus);
		operate(getSet(), invitationListKey, readOperations, (readResult, wp) -> {
			if (readResult != null) {
				Value instanceIdAsValue = Value.get(mgiId);
				Value inviterIdAsValue = Value.get(readResult.getString(oldStatus.getBinName()));
				Operation putOperation = MapOperation.put(MapPolicy.Default, newStatus.getBinName(), instanceIdAsValue, inviterIdAsValue);
				Operation removeOperation = MapOperation.removeByKey(oldStatus.getBinName(), instanceIdAsValue, MapReturnType.NONE);
				return Arrays.asList(putOperation, removeOperation);
			} else {
				throw new F4MEntryNotFoundException(String.format("Invitation not found (mgiId=[%s], userId=[%s], oldStatus=[%s])", mgiId, userId, oldStatus));
			}
		});
	}
	
	private void saveReferenceToGameInstance(String mgiId, String userId, String gameInstanceId) {
		String key = primaryKeyUtil.createGameInstanceIdKey(mgiId, userId);
		createOrUpdateString(getSet(), key, GAME_INSTANCE_ID_BIN_NAME, (r, wp) -> gameInstanceId);
	}

	private List<Operation> moveState(MultiplayerGameInstanceState fromState, String fromKey,
			MultiplayerGameInstanceState toState, String toKey, MultiplayerUserGameInstance toInstance,
			String mgiId, String userId) {
		final Value fromKeyAsValue = Value.get(fromKey);
		
		final Value toKeyAsValue = Value.get(toKey);
		final Value toInstanceAsValue = Value.get(jsonUtil.toJson(toInstance));
		
		final Operation removeFromStateOperation = MapOperation.removeByKey(fromState.getBinName(), fromKeyAsValue, MapReturnType.NONE);
		final Operation putToStateOperation = MapOperation.put(MapPolicy.Default, toState.getBinName(), toKeyAsValue, toInstanceAsValue);
		final Operation changeStatusOperation = MapOperation.put(MapPolicy.Default, INVITED.getBinName(), Value.get(userId), Value.get(toState.name()));
		
		changeInvitationStatus(userId, mgiId, fromState, toState);
		
		return Arrays.asList(removeFromStateOperation, putToStateOperation, changeStatusOperation);
	}
	
	private void addInstanceToInvitationList(String userId, String mgiId, String inviterId,
			MultiplayerGameInstanceState status) {
		String key = getPK(mgiId, userId);
		createOrUpdateMapValueByKey(getSet(), key, status.getBinName(), mgiId, (readResult, wp) -> {
			if (readResult == null) {
				return inviterId;
			} else {
				throw new F4MEntryAlreadyExistsException(String.format("Invitation already exists (key=[%s], mgiId=[%s])", key, mgiId));
			}
		});
	}

	@Override
	public boolean hasValidParticipantCount(String mgiId) {
		Integer maxNumberOfParticipants = getConfig(mgiId).getMaxNumberOfParticipants();
		return maxNumberOfParticipants == null || getGameInstancesCount(mgiId) < maxNumberOfParticipants;
	}
	
	private Operation[] createGetOperation(String key, MapReturnType mapReturnType, MultiplayerGameInstanceState...states) {
		return Arrays.stream(states)
				.map(s -> MapOperation.getByKey(s.getBinName(), Value.get(key), mapReturnType))
				.toArray(size -> new Operation[size]);
	}
	
	@Override
	public void declineInvitation(String userId, String mgiId) {
		changeNotAcceptedInvitation(mgiId, userId, DECLINED);
	}
	
	@Override
	public void rejectInvitation(String userId, String mgiId) {
		changeNotAcceptedInvitation(mgiId, userId, EXPIRED);
	}

	@Override
	public String getInviterId(String userId, String mgiId, MultiplayerGameInstanceState status) {
		String key = getPK(mgiId, userId);
		return getByKeyFromMap(getSet(), key, status.getBinName(), mgiId);
	}
	
	@Override
	public String getInviterId(String userId, String mgiId) {
		return getInviterId(userId, mgiId, getUserState(mgiId, userId));
	}
	
	@Override
	public void deleteNotAcceptedInvitations(String mgiId) {
		getAllUsersByState(mgiId, INVITED)
			.forEach(userId -> changeNotAcceptedInvitation(mgiId, userId, DELETED));
	}
	
	@Override
	public void markAsExpired(String mgiId) {
		getAllUsersByState(mgiId, INVITED)
			.forEach(userId -> changeNotAcceptedInvitation(mgiId, userId, EXPIRED));
	}

	private void changeNotAcceptedInvitation(String mgiId, String userId, MultiplayerGameInstanceState newStatus) {
		String key = getIndexKeyToInstanceRecord(mgiId, userId);
		
		Operation[] readOperations = createGetOperation(userId, MapReturnType.VALUE, INVITED, DELETED);
		operate(getSet(), key, readOperations, (readResult, wp) -> {
			if (hasValidInvitationState(readResult, INVITED, DELETED)) {
				MultiplayerGameInstanceState oldStatus = F4MEnumUtils.getEnum(MultiplayerGameInstanceState.class, readResult.getString(INVITED.getBinName()));
				changeInvitationStatus(userId, mgiId, oldStatus, newStatus);
				return Arrays.asList(MapOperation.put(MapPolicy.Default, INVITED.getBinName(), Value.get(userId), Value.get(newStatus.name())));
			} else {
				throw new F4MGameInvitationNotValidException(String.format(EXCEPTION_MSG_USER_NOT_INVITED, mgiId, userId));
			}
		});
	}
	
	@Override
	public Map<String, String> getAllUsersOfMgi(String mgiId) {
		return LongStream.range(1, getTotalRecordCount(mgiId) + 1)
			.mapToObj(i -> getInvitedUsersOfRecord(mgiId, i))
			.collect(HashMap::new, Map::putAll, Map::putAll);
	}

	private Map<String, String> getInvitedUsersOfRecord(String mgiId, long recordNumber) {
		String instanceKey = primaryKeyUtil.createInstanceRecordKey(mgiId, recordNumber);
		return getAllMap(getSet(), instanceKey, INVITED.getBinName());
	}

	private long getTotalRecordCount(String mgiId) {
		long result;
		String key = primaryKeyUtil.createMetaKey(mgiId);
		Long capacity = readLong(getIndexSet(), key, CAPACITY_BIN_NAME);
		Long entryCount = readLong(getIndexSet(), key, ENTRY_COUNT_BIN_NAME);
		if (capacity != null && capacity > 0 && entryCount != null && entryCount > 0) {
			result = (entryCount - 1) / capacity + 1;
		} else {
			result = 0;
		}
		return result;
	}
	
	private List<String> getAllUsersByState(String mgiId, MultiplayerGameInstanceState state) {
		return getAllUsersOfMgi(mgiId).entrySet().stream()
				.filter(entry -> state.name().equals(entry.getValue()))
				.map(entry -> entry.getKey())
				.collect(Collectors.toList());
	}
	
	@Override
	public List<Invitation> getInvitationList(String tenantId, String appId, String userId,
			List<MultiplayerGameInstanceState> states, CreatedBy createdBy, FilterCriteria filterCriteria) {
		invalidateExpiredInvitations(tenantId, appId, userId);
		String key = primaryKeyUtil.createInvitationListKey(tenantId, appId, userId);
		
		List<JsonObject> invitationJsonObjects = new ArrayList<>();
		states.forEach(state -> {
			List<JsonObject> invitationJsonObjectsByState = getInvitationJsonObjectsByState(key, state);
			invitationJsonObjects.addAll(invitationJsonObjectsByState);
		});
		
		List<JsonObject> filteredInvitationJsonObjects = filterJsonObjects(invitationJsonObjects, filterCriteria);
		List<Invitation> filteredInvitations = filteredInvitationJsonObjects.stream()
				.map(i -> jsonUtil.fromJson(i, Invitation.class))
				.collect(Collectors.toList());
		return filterInvitationsByCreator(filteredInvitations, userId, createdBy);
	}
	
	private List<JsonObject> getInvitationJsonObjectsByState(String invitationListKey, MultiplayerGameInstanceState state) {
		Map<String, String> invitationsMap = getAllMap(getSet(), invitationListKey, state.getBinName());
		List<JsonObject> list = new ArrayList<>();
		for (Entry<String, String> entry :  invitationsMap.entrySet()) {
			String mgiId = entry.getKey();
			String userId = entry.getValue();
			try {
				JsonObject jsonObj = getInvitationJsonObject(mgiId, userId, state);
				list.add(jsonObj);
			} catch (F4MEntryNotFoundException e) {
				LOGGER.error("Error while processing invitation for (mgiId=[{}] and userId=[{}]); skipping this item.", mgiId, userId, e);
			}
		}
		return list;
	}
	
	private List<Invitation> filterInvitationsByCreator(List<Invitation> invitations, String userId, CreatedBy createdBy) {
		List<Invitation> invitationsByCreator;
		if (createdBy == CreatedBy.MYSELF) {
			Predicate<Invitation> createdByMyself = i -> userId.equals(i.getCreator().getUserId());
			invitationsByCreator = filterInvitations(invitations, createdByMyself);
		} else if (createdBy == CreatedBy.OTHERS) {
			Predicate<Invitation> createdByOthers = i -> !userId.equals(i.getCreator().getUserId());
			invitationsByCreator = filterInvitations(invitations, createdByOthers);
		} else {
			invitationsByCreator = invitations;
		}
		return invitationsByCreator;
	}
	
	private List<Invitation> filterInvitations(List<Invitation> invitations, Predicate<? super Invitation> predicate) {
		return invitations.stream()
				.filter(predicate)
				.collect(Collectors.toList());
	}
	
	@Override
	public Set<String> getGamesInvitedTo(String tenantId, String appId, String userId) {
		invalidateExpiredInvitations(tenantId, appId, userId);
		final String key = primaryKeyUtil.createInvitationListKey(tenantId, appId, userId);
		final Map<String, String> invitationsMap = getAllMap(getSet(), key, INVITED.getBinName());
		return invitationsMap.entrySet().stream()
				.map(e -> getConfig(e.getKey()).getGameId())
				.collect(Collectors.toSet());
	}
	
	private void invalidateExpiredInvitations(String tenantId, String appId, String userId) {
		String key = primaryKeyUtil.createInvitationListKey(tenantId, appId, userId);
		Map<String, String> invitationsMap = getAllMap(getSet(), key, INVITED.getBinName());
		invitationsMap.keySet().stream().forEach(mgiId -> {
			if (GameUtil.isGameInvitationExpired(getConfig(mgiId))) {
				markAsExpired(mgiId);
			}
		});
	}

	private JsonObject getInvitationJsonObject(String mgiId, String inviterId, MultiplayerGameInstanceState status) {
		final Invitation invitation = new Invitation(getConfig(mgiId), inviterId, status.name());
        return (JsonObject) jsonUtil.toJsonElement(invitation);
	}
	
	@Override
	public List<InvitedUser> getInvitedList(String mgiId, int limit, long offset, List<OrderBy> orderBy,
			Map<String, String> searchBy) {
		List<JsonObject> allUsers = getAllUsersOfMgi(mgiId).entrySet().stream()
				.map(e -> getInviterUserJsonObject(e.getKey(), e.getValue()))
				.collect(Collectors.toList());
		
		List<JsonObject> filteredUsers = JsonUtil.filter(allUsers, searchBy);
		
		JsonUtil.sort(filteredUsers, orderBy);
		
		return filteredUsers.stream()
				.skip(offset < 0 ? 0 : offset)
				.limit(limit < 0 ? 0 : limit)
				.map(u -> jsonUtil.fromJson(u.toString(), InvitedUser.class))
				.collect(Collectors.toList());
	}

	@Override
	public List<MultiplayerUserGameInstance> getGameInstances(String mgiId){
		return getGameInstances(mgiId, REGISTERED, STARTED, CALCULATED, CANCELLED);
	}

	@Override
	public List<MultiplayerUserGameInstance> getGameInstances(String mgiId, final MultiplayerGameInstanceState... states) {
		if (!ArrayUtils.contains(states, INVITED) && !ArrayUtils.contains(states, DECLINED)
				&& !ArrayUtils.contains(states, DELETED)) {
			return LongStream.range(1, getTotalRecordCount(mgiId) + 1)
					.mapToObj(i -> getGameInstancesOfRecord(mgiId, i, states))
					.flatMap(List::stream)
					.collect(Collectors.toList());
		}else{
			throw new IllegalArgumentException(String.format("Contains invalid state to be selected INVITED[%b], DECLINED[%b], DELETED[%b]",
					ArrayUtils.contains(states, INVITED), ArrayUtils.contains(states, DECLINED), ArrayUtils.contains(states, DELETED)));
		}
	}
	
	@Override
	public String getGameInstanceId(String mgiId, String userId) {
		String key = primaryKeyUtil.createGameInstanceIdKey(mgiId, userId);
		return readString(getSet(), key, GAME_INSTANCE_ID_BIN_NAME);
	}
	
	@Override
	public MultiplayerGameInstanceState getUserState(String mgiId, String userId) {
		final String indexKey = getIndexKeyToInstanceRecord(mgiId, userId);
		MultiplayerGameInstanceState state = null;
		if (indexKey != null) {
			final String stateAsString = getByKeyFromMap(getSet(), indexKey, INVITED.getBinName(), userId);
			state = F4MEnumUtils.getEnum(MultiplayerGameInstanceState.class, stateAsString);
		}
		return state;
	}
	
	@Override
	public void mapTournament(String gameId, String mgiId) {
		String key = primaryKeyUtil.createTournamentMappingKey(gameId);
		try {
			createString(getSet(), key, CURRENT_MGI_ID_BIN_NAME, mgiId);
		} catch (AerospikeException aEx) {
			operate(getSet(), key, new Operation[] { Operation.get(CURRENT_MGI_ID_BIN_NAME) }, (r, wp) -> {
				final String currentMgiId = r.getString(CURRENT_MGI_ID_BIN_NAME);
				final Operation moveCurrentMgiToFinished = MapOperation.put(MapPolicy.Default, FINISHED_MGI_ID_BIN_NAME,
						Value.get(currentMgiId), Value.get(DateTimeUtil.getUTCTimestamp()));
				final Operation setNewMgiAsCurrent = Operation.put(getStringBin(CURRENT_MGI_ID_BIN_NAME, mgiId));
				return Arrays.asList(moveCurrentMgiToFinished, setNewMgiAsCurrent);
			});
		}
	}

	public void moveTournamentToFinished(String gameId, String mgiId) {
		LOGGER.debug("moveTournamentToFinished gameId {} , mgiId {} ", gameId, mgiId);
		String key = primaryKeyUtil.createTournamentMappingMgiKey(gameId);
        operate(MGI_SET_NAME, key, new Operation[] { Operation.get(CURRENT_MGI_ID_BIN_NAME) }, (r, wp) -> {
            final String currentMgiId = r.getString(CURRENT_MGI_ID_BIN_NAME);

            final Operation moveCurrentMgiToFinished = MapOperation.put(MapPolicy.Default, FINISHED_MGI_ID_BIN_NAME,
                    Value.get(currentMgiId), Value.get(DateTimeUtil.getUTCTimestamp()));
            final Operation setNewMgiAsCurrent = Operation.put(getStringBin(CURRENT_MGI_ID_BIN_NAME, null));
            return Arrays.asList(moveCurrentMgiToFinished, setNewMgiAsCurrent);
        });
    }

	@Override
	public String getTournamentInstanceId(String gameId) {
		String key = primaryKeyUtil.createTournamentMappingKey(gameId);
		return readString(getSet(), key, CURRENT_MGI_ID_BIN_NAME);
	}
	
	@Override
	public void markTournamentAsEnded(String mgiId) {
		final String customGameConfigKey = primaryKeyUtil.createMetaKey(mgiId);
		updateJson(getSet(), customGameConfigKey, VALUE_BIN_NAME, (json, wp) -> {
			final CustomGameConfig customGameConfig = jsonUtil.fromJson(json, CustomGameConfig.class);
			customGameConfig.setEndDateTime(DateTimeUtil.getCurrentDateTime());
			return jsonUtil.toJson(customGameConfig);
		});
	}

	public List<String> getExtendedMgiDuel() {
		final Statement statement = new Statement();
		statement.setNamespace(getNamespace());
		statement.setSetName(DUEL_SET_NAME);
		statement.setBinNames(MGI_DUEL_BIN_NAME);

		List<String> mgiIds = new ArrayList<>();

		try (final RecordSet resultSet = getAerospikeClient().query(null, statement)) {
			while (resultSet.next()) {
				String mgiId = resultSet.getRecord().getString("mgiIdDuel");
				CustomGameConfig customGameConfig = getConfig(mgiId);
				ZonedDateTime now = DateTimeUtil.getCurrentDateTime();
				if (now.isAfter(customGameConfig.getExpiryDateTime())) {
					mgiIds.add(mgiId);
				}
			}
		}
		return mgiIds;
	}

	public List<String> getExtendedMgiTournament() {
		final Statement statement = new Statement();
		statement.setNamespace(getNamespace());
		statement.setSetName(getSet());
		statement.setBinNames(CURRENT_MGI_ID_BIN_NAME);

		List<String> mgiIds = new ArrayList<>();

		try(final RecordSet resultSet = getAerospikeClient().query(null, statement)){
			while(resultSet.next()){
                String mgiId = resultSet.getRecord().getString(CURRENT_MGI_ID_BIN_NAME);
                CustomGameConfig customGameConfig = getConfig(mgiId);
				ZonedDateTime now = DateTimeUtil.getCurrentDateTime();

				if (now.isAfter(customGameConfig.getEndDateTime()) && customGameConfig.getGameType() == GameType.TOURNAMENT) {
					mgiIds.add(mgiId);
				}
			}
		}

		return mgiIds;
	}

	@Override
	public void markGameAsExpired(String mgiId) {
		final String customGameConfigKey = primaryKeyUtil.createMetaKey(mgiId);
		updateJson(getSet(), customGameConfigKey, VALUE_BIN_NAME, (json, wp) -> {
			final CustomGameConfig customGameConfig = jsonUtil.fromJson(json, CustomGameConfig.class);
			customGameConfig.setExpiryDateTime(DateTimeUtil.getCurrentDateTime());
			return jsonUtil.toJson(customGameConfig);
		});
	}
	
	private List<MultiplayerUserGameInstance> getGameInstancesOfRecord(String mgiId, long recordNumber, 
			MultiplayerGameInstanceState... states) {
		String instanceKey = primaryKeyUtil.createInstanceRecordKey(mgiId, recordNumber);
		final String[] binNames = Stream.of(states).map(MultiplayerGameInstanceState::getBinName).toArray(String[]::new);
		return (this.<String, String>getAllMap(getSet(), instanceKey, binNames)).values().stream()
				.map(multiplayerUserGameInstanceAsString -> jsonUtil.fromJson(multiplayerUserGameInstanceAsString, MultiplayerUserGameInstance.class))
				.collect(Collectors.toList());
	}


	private JsonObject getInviterUserJsonObject(String userId, String status) {
		InvitedUser invitedUser = new InvitedUser(userId, status);
		return jsonUtil.fromJson(jsonUtil.toJson(invitedUser), JsonObject.class);
	}

	protected String getSet() {
		return config.getProperty(GameConfigImpl.AEROSPIKE_MULTIPLAYER_SET);
	}

	protected String getIndexSet() {
		return config.getProperty(GameConfigImpl.AEROSPIKE_MULTIPLAYER_INDEX_SET);
	}

	private Long getRecordCapacity() {
		Long capacity = config.getPropertyAsLong(GameConfigImpl.AEROSPIKE_MULTIPLAYER_RECORD_CAPACITY);
		return capacity == null ? GameConfigImpl.AEROSPIKE_MULTIPLAYER_RECORD_CAPACITY_DEFAULT : capacity;
	}
	
	private boolean hasValidInvitationState(Record readResult, MultiplayerGameInstanceState... states) {
		Predicate<MultiplayerGameInstanceState> readResultHasValidState = s -> StringUtils.equals(s.name(), readResult.getString(INVITED.getBinName()));
		return readResult != null && Stream.of(states).anyMatch(readResultHasValidState);
	}
	
	/**
	 * 
	 * @param mgiId
	 * @param userId
	 * @return index key to MGI record in form of mgi:[mgi_id]:[record_number]
	 */
	protected String getIndexKeyToInstanceRecord(String mgiId, String userId) {
		String indexKey = primaryKeyUtil.createIndexKeyToInstanceRecord(mgiId, userId);
		return readString(getIndexSet(), indexKey, INDEX_BIN_NAME);
	}
	
	@Override
	public boolean hasAnyCalculated(String mgiId) {
		final long totalRecordCount = getTotalRecordCount(mgiId);
		
		boolean hasAnyCalculated = false;
		for (int recordIndex = 1; recordIndex <= totalRecordCount; recordIndex++) {
			final String instanceKey = primaryKeyUtil.createInstanceRecordKey(mgiId, recordIndex);
			final Integer calculatedCount = getMapSize(getSet(), instanceKey, CALCULATED.getBinName());
			if (calculatedCount != null && calculatedCount > 0) {
				hasAnyCalculated = true;
				break;
			}
		}
		
		return hasAnyCalculated;
	}

	@Override
	public void setInvitationExpirationNotificationId(String mgiId, String userId, String notificationId) {
		String key = getPK(mgiId, userId);
		createOrUpdateMapValueByKey(getSet(), key, NOTIFICATION_BIN_NAME, mgiId, (readResult, wp) -> {
			if (readResult == null) {
				return notificationId;
			} else {
				throw new F4MEntryAlreadyExistsException(String.format("Notification already exists/stored (key=[%s], mgiId=[%s])", key, mgiId));
			}
		});
	}

	@Override
	public void resetInvitationExpirationNotificationId(String mgiId, String userId) {
		String key = getPK(mgiId, userId);
		deleteByKeyFromMapSilently(getSet(), key, NOTIFICATION_BIN_NAME, mgiId);
	}

	@Override
	public String getInvitationExpirationNotificationId(String mgiId, String userId) {
		String key = getPK(mgiId, userId);
		return getByKeyFromMap(getSet(), key, NOTIFICATION_BIN_NAME, mgiId);
	}

	private String getPK(String mgiId, String userId) {
		CustomGameConfig gameConfig = getConfig(mgiId);
		String key = primaryKeyUtil.createInvitationListKey(gameConfig.getTenantId(), gameConfig.getAppId(), userId);
		return key;
	}

    public long getTotalRecordCountDuel(String mgiId) {
        long result;
        String key = primaryKeyUtil.createMetaKey(mgiId);
        Long capacity = readLong(getIndexSet(), key, CAPACITY_BIN_NAME);
        Long entryCount = readLong(getIndexSet(), key, ENTRY_COUNT_BIN_NAME);
        if (capacity != null && capacity > 0 && entryCount != null && entryCount > 0) {
            result = entryCount - 1;
        } else {
            result = 0;
        }
        return result;
    }
}
