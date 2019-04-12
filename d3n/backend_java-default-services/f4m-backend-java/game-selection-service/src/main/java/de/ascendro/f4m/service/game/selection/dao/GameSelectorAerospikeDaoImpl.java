package de.ascendro.f4m.service.game.selection.dao;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.game.GameAerospikeDaoImpl;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.util.ServiceUtil;

public class GameSelectorAerospikeDaoImpl extends GameAerospikeDaoImpl implements GameSelectorAerospikeDao {

	private final GameSelectorPrimaryKeyUtil gameSelectorPrimaryKeyUtil;
	private final ServiceUtil serviceUtil;

	@Inject
	public GameSelectorAerospikeDaoImpl(Config config, GameSelectorPrimaryKeyUtil gameSelectorPrimaryKeyUtil,
			AerospikeClientProvider aerospikeClientProvider, JsonUtil jsonUtil,
			ServiceUtil serviceUtil) {
		super(config, gameSelectorPrimaryKeyUtil, aerospikeClientProvider, jsonUtil);
		this.gameSelectorPrimaryKeyUtil = gameSelectorPrimaryKeyUtil;
		this.serviceUtil = serviceUtil;
	}

	@Override
	public JsonArray getGameList(String tenantId, String appId) {
		String key = gameSelectorPrimaryKeyUtil.createSearchKey(tenantId, appId);
		return getGameListByKey(key);
	}

	@Override
    public JsonArray getGameList(String tenantId, String appId, String searchField, String searchValue) {
        String key = gameSelectorPrimaryKeyUtil.createSearchKey(tenantId, appId, searchField, searchValue);
		return getGameListByKey(key);
	}

	@Override
    public JsonArray getGameList(String tenantId, String appId, Map<String, String> searchFields) {
		JsonArray gameList = new JsonArray();

		List<Set<JsonObject>> listOfGameSets = new ArrayList<>();
		for (Entry<String, String> entry : searchFields.entrySet()) {
			Set<JsonObject> set = getSetOfGameJsonObjects(tenantId, appId, entry.getKey(), entry.getValue());
			listOfGameSets.add(set);
		}

		Set<JsonObject> intersectionOfGames = serviceUtil.getIntersectionOfSets(listOfGameSets);
		intersectionOfGames.forEach(gameList::add);

		return gameList;
	}

	private Set<JsonObject> getSetOfGameJsonObjects(String tenantId, String appId, String searchField,
			String searchValue) {
		Set<JsonObject> gameSet = new HashSet<>();
		JsonArray gameList = getGameList(tenantId, appId, searchField, searchValue);

		for (JsonElement gameElement : gameList) {
			gameSet.add(gameElement.getAsJsonObject());
		}

		return gameSet;
	}

	private JsonArray getGameListByKey(String key) {
		String gameListAsString = readJson(getSet(), key, BLOB_BIN_NAME);
		return gameListAsString != null ? jsonUtil.fromJson(gameListAsString, JsonArray.class)
				: new JsonArray();
	}

}
