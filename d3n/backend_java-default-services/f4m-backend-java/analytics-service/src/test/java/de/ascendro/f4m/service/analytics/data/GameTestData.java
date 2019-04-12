package de.ascendro.f4m.service.analytics.data;

import java.io.IOException;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.analytics.AnalyticsDaoImpl;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.game.instance.CommonGameInstanceAerospikeDao;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.util.JsonLoader;
import de.ascendro.f4m.service.util.JsonTestUtil;


public class GameTestData {
	public static final String TOURNAMENT_INSTANCE_ID = "1";
	public static final String DUEL_INSTANCE_ID = "2";
	
    private JsonLoader jsonLoader;
    private Config config;
    private AnalyticsDaoImpl analyticsDaoImpl;

    public GameTestData(JsonLoader jsonLoader, AnalyticsDaoImpl analyticsDaoImpl, Config config) {
        this.jsonLoader = jsonLoader;
        this.config = config;
        this.analyticsDaoImpl = analyticsDaoImpl;
    }

    public void createGameInstanceDuel(String gameInstanceId) throws IOException {
    	String path = "gameInstanceDuel.json";
        createGameInstance(gameInstanceId, path);
    }

    public void createGameInstanceTournament(String gameInstanceId) throws IOException {
    	String path = "gameInstanceTournament.json";
        createGameInstance(gameInstanceId, path);
    }

	private void createGameInstance(String gameInstanceId, String path) throws IOException {
		String set = config.getProperty(AerospikeConfigImpl.AEROSPIKE_GAME_INSTANCE_SET);
		analyticsDaoImpl.createJson(set, "gameEngine:" + gameInstanceId, CommonGameInstanceAerospikeDao.BLOB_BIN_NAME,
                jsonLoader.getPlainTextJsonFromResources(path));
	}

    public GameInstance prepareTestGameInstance() throws IOException {
        return prepareTestGameInstance("gameInstanceTournament.json");
    }

    private GameInstance prepareTestGameInstance(String file) throws IOException {
        return new GameInstance(JsonTestUtil.getGson()
                .fromJson(jsonLoader.getPlainTextJsonFromResources(file), JsonObject.class));
    }
}
