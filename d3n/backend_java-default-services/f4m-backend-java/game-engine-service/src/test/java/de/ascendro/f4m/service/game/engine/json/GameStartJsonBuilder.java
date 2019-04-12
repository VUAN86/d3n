package de.ascendro.f4m.service.game.engine.json;

import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO;

import java.io.IOException;

import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.util.JsonLoader;

public class GameStartJsonBuilder {
	
	private String gameInstanceId;

	private ClientInfo clientInfo = new ClientInfo();

	private GameStartJsonBuilder(String gameInstanceId) throws IOException {
		this.gameInstanceId = gameInstanceId;
		setClientInfo(ANONYMOUS_CLIENT_INFO);
	}
	
	private void setClientInfo(ClientInfo clientInfo) {
		this.clientInfo = ClientInfo.cloneOf(clientInfo);
	}

	public static GameStartJsonBuilder startGame(String gameInstanceId) throws IOException{
		return new GameStartJsonBuilder(gameInstanceId);
	}

	public GameStartJsonBuilder withUserLanguage(String userLanguage) {
		this.clientInfo.setLanguage(userLanguage);
		return this;
	}

	public GameStartJsonBuilder byUser(String userId, String... roles) {
		this.clientInfo.setUserId(userId);
		this.clientInfo.setRoles(roles);
		return this;
	}
	
	public GameStartJsonBuilder byUser(ClientInfo clientInfo) {
		setClientInfo(clientInfo);
		return this;
	}

	public Game buildGame(JsonUtil jsonUtil) throws IOException {
		return jsonUtil.fromJson(buildJson(jsonUtil), Game.class);
	}
	
	public ClientInfo getClientInfo() {
		return clientInfo;
	}

	public String buildJson(JsonUtil jsonUtil) throws IOException {
		return new JsonLoader(this).getPlainTextJsonFromResources("startGame.json", clientInfo)
			.replace("<<userLanguage>>", clientInfo.getLanguage())				
			.replace("<<gameInstanceId>>", gameInstanceId);
	}

}
