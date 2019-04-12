package de.ascendro.f4m.service.game.engine.json;

import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO;

import java.io.IOException;

import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.singleplayer.SinglePlayerGameParameters;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.util.JsonLoader;

public class GameRegisterJsonBuilder {

	private final String gameId;
	private final String mgiId;
	private final SinglePlayerGameParameters singlePlayerGameConfig;

	private ClientInfo clientInfo = new ClientInfo();

	private GameRegisterJsonBuilder(String gameId, String mgiId, SinglePlayerGameParameters singlePlayerGameConfig) throws IOException {
		this.gameId = gameId;
		this.mgiId = mgiId;
		this.singlePlayerGameConfig = singlePlayerGameConfig;
		
		//clientInfo
		setClientInfo(ANONYMOUS_CLIENT_INFO);
	}
	
	private void setClientInfo(ClientInfo clientInfo) {
		this.clientInfo = ClientInfo.cloneOf(clientInfo);
	}

	public static GameRegisterJsonBuilder registerForQuiz24(String gameId, boolean trainingMode) throws IOException {
		return new GameRegisterJsonBuilder(gameId, null, new SinglePlayerGameParameters(trainingMode));
	}
	
	public static GameRegisterJsonBuilder registerForMultiplayerGame(String mgiId) throws IOException {
		return new GameRegisterJsonBuilder(null, mgiId, null);
	}
	
	public static GameRegisterJsonBuilder registerForQuiz24(String gameId, SinglePlayerGameParameters singlePlayerGameConfig) throws IOException {
		return new GameRegisterJsonBuilder(gameId, null, singlePlayerGameConfig);
	}
	
	public GameRegisterJsonBuilder withHandicap(double handicap){
		this.clientInfo.setHandicap(handicap);
		return this;
	}

	public GameRegisterJsonBuilder byUser(String userId, String... roles) {
		this.clientInfo.setUserId(userId);
		this.clientInfo.setRoles(roles);
		return this;
	}
	
	public GameRegisterJsonBuilder byUser(ClientInfo clientInfo) {
		setClientInfo(clientInfo);
		return this;
	}

	public Game buildGame(JsonUtil jsonUtil) throws IOException {
		return jsonUtil.fromJson(buildJson(jsonUtil), Game.class);
	}
	
	public String getMgiId() {
		return mgiId;
	}
	
	public String getGameId() {
		return gameId;
	}

	public boolean isTrainingMode() {
		return singlePlayerGameConfig != null && singlePlayerGameConfig.isTrainingMode();
	}
	
	public SinglePlayerGameParameters getSinglePlayerGameConfig() {
		return singlePlayerGameConfig;
	}
	
	public ClientInfo getClientInfo() {
		return clientInfo;
	}
	
	public String buildJson(JsonUtil jsonUtil) throws IOException {
		String gameStartJson = new JsonLoader(this).getPlainTextJsonFromResources("register.json", clientInfo);

		if (mgiId != null) {
			gameStartJson = gameStartJson.replace("<<mgiId>>", mgiId);
		} else {
			gameStartJson = gameStartJson.replace("\"mgiId\": \"<<mgiId>>\",", "");// remove mgiId property
		}
		
		if (gameId != null) {
			gameStartJson = gameStartJson.replace("<<gameId>>", gameId);
		} else {
			gameStartJson = gameStartJson.replace("\"gameId\": \"<<gameId>>\",", "");// remove gameId property
		}
		
		if(singlePlayerGameConfig != null){
			final StringBuilder singlePlayerGameConfigAsJson = new StringBuilder("{");
			if(singlePlayerGameConfig.getTrainingMode() != null){
				singlePlayerGameConfigAsJson.append("\"trainingMode\":").append(Boolean.toString(singlePlayerGameConfig.getTrainingMode()))
					.append(",");
			}
			if(singlePlayerGameConfig.getNumberOfQuestions() != null){
				singlePlayerGameConfigAsJson.append("\"numberOfQuestions\":").append(Integer.toString(singlePlayerGameConfig.getNumberOfQuestions()))
					.append(",");
			}
			if(singlePlayerGameConfig.getPoolIds() != null){
				singlePlayerGameConfigAsJson.append("\"poolIds\":").append(jsonUtil.toJson(singlePlayerGameConfig.getPoolIds()))
					.append(",");
			}
			
			singlePlayerGameConfigAsJson.append("}");
			gameStartJson = gameStartJson.replace("\"<<singlePlayerGameConfig>>\"", singlePlayerGameConfigAsJson.toString().replace(",}", "}"));
		}else{
			gameStartJson = gameStartJson.replace("\"<<singlePlayerGameConfig>>\"", "null");
		}
		return gameStartJson;
	}

}
