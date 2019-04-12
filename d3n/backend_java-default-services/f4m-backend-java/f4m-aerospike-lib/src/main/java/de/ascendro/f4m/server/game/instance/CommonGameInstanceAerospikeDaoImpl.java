package de.ascendro.f4m.server.game.instance;

import javax.inject.Inject;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeDaoImpl;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.game.util.GameEnginePrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.client.F4MEntryAlreadyExistsException;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.winning.model.WinningComponentType;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.List;

public class CommonGameInstanceAerospikeDaoImpl extends AerospikeDaoImpl<GameEnginePrimaryKeyUtil> implements CommonGameInstanceAerospikeDao {

	private final GameEnginePrimaryKeyUtil gameEnginePrimaryKeyUtil;
	private static final String MGI_ID_BIN_NAME = "mgiId";
	private static final String MGI_KEY_NAME = "mgiId";

	@Inject
	public CommonGameInstanceAerospikeDaoImpl(Config config, GameEnginePrimaryKeyUtil gameEnginePrimaryKeyUtil,
			JsonUtil jsonUtil, AerospikeClientProvider aerospikeClientProvider) {
		super(config, gameEnginePrimaryKeyUtil, jsonUtil, aerospikeClientProvider);
		this.gameEnginePrimaryKeyUtil = gameEnginePrimaryKeyUtil;
	}

	@Override
	public GameInstance getGameInstance(String gameInstanceId) {
		final String gameInstanceKey = gameEnginePrimaryKeyUtil.createPrimaryKey(gameInstanceId);
		final String gameInstanceAsString = readJson(getSet(), gameInstanceKey, BLOB_BIN_NAME);

		final GameInstance gameInstance;
		if (gameInstanceAsString != null) {
			gameInstance = getGameInstanceFromJson(gameInstanceAsString);
		} else {
			gameInstance = null;
		}
		return gameInstance;
	}

	public List<String> getGameInstancesList(String gameid){
		final String gameIdKey = GAME_KEY_PREFIX + KEY_ITEM_SEPARATOR + gameid;
		final String resultsJson = readJson(getSet(), gameIdKey, BLOB_BIN_NAME);
		List<String> gameInstanceList;
		if (StringUtils.isNotBlank(resultsJson)) {
			gameInstanceList = jsonUtil.fromJson(resultsJson, List.class);
		} else {
			gameInstanceList = new LinkedList();
		}
		return gameInstanceList;
	}

	@Override
	public List<String> getAdvertisementShown(String gameInstanceId){

		final String gameInstanceIdKey = INSTANCE_KEY_PREFIX + KEY_ITEM_SEPARATOR + gameInstanceId;
		final String resultsJson = readJson("advertisement", gameInstanceIdKey, ADS_BIN_NAME);
		List<String> advertisementBlobKeys;
		if (StringUtils.isNotBlank(resultsJson)) {
			advertisementBlobKeys = jsonUtil.fromJson(resultsJson, List.class);
		} else {
			advertisementBlobKeys = new LinkedList();
		}
		return advertisementBlobKeys;
	}

	@Override
	public Game getGameByInstanceId(String gameInstanceId) {
		final GameInstance gameInstance = getGameInstance(gameInstanceId);
		return gameInstance == null ? null : gameInstance.getGame();
	}

	private GameInstance getGameInstanceFromJson(final String gameInstanceAsString) {
		final JsonObject gameInstanceJsonElement = jsonUtil.fromJson(gameInstanceAsString, JsonObject.class);
		return new GameInstance(gameInstanceJsonElement);
	}

	@Override
	public void addUserWinningComponentIdToGameInstance(String gameInstanceId, String userWinningComponentId, WinningComponentType winningComponentType) {
		final String gameInstanceKey = gameEnginePrimaryKeyUtil.createPrimaryKey(gameInstanceId);
		updateJson(getSet(), gameInstanceKey, BLOB_BIN_NAME,
				(existingValue, policy) -> updateUserWinningComponentId(existingValue, userWinningComponentId, gameInstanceId, winningComponentType));
	}

	private String updateUserWinningComponentId(String existingValue, String userWinningComponentId,String gameInstanceId, WinningComponentType winningComponentType) {
		final String result;
		if (existingValue != null) {
			final GameInstance gameInstance = new GameInstance(jsonUtil.fromJson(existingValue, JsonObject.class));
			if (gameInstance.getUserWinningComponentId(winningComponentType.name()) == null) {
				gameInstance.setUserWinningComponentId(userWinningComponentId,winningComponentType.name());
				result = gameInstance.getAsString();
			} else {
				throw new F4MEntryAlreadyExistsException("User winning component already exists");
			}
		} else {
			throw new F4MEntryNotFoundException("Game instance not found (gameInstanceId=[" + gameInstanceId + "])");
		}
		return result;
	}

	public void createRecordInTheDatabaseOfPaymentOfAdditionalWinnings(String mgiId, String userId) {
		final String mgiKey = gameEnginePrimaryKeyUtil.createPrimaryKey(mgiId + ":user:" + userId) + MGI_KEY_NAME;
		createString(getSet(), mgiKey, MGI_ID_BIN_NAME, mgiId);
	}

	public boolean isRecordInTheDatabaseOfPaymentOfAdditionalWinnings(String mgiId, String userId) {
		final String mgiKey = gameEnginePrimaryKeyUtil.createPrimaryKey(mgiId + ":user:" + userId) + MGI_KEY_NAME;
		return readString(getSet(), mgiKey, mgiId) != null;
	}

	protected String getSet() {
		return config.getProperty(AerospikeConfigImpl.AEROSPIKE_GAME_INSTANCE_SET);
	}

}
