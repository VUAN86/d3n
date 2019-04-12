package de.ascendro.f4m.service.game.selection.builder;

import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.game.selection.model.dashboard.PlayedGameInfo;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.util.JsonLoader;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;

public class UpdatePlayedGameRequestBuilder {

	private String tenantId;
	private String userId;
	private String mgiId;
	private PlayedGameInfo playedGameInfo;

	private final JsonLoader jsonLoader = new JsonLoader(this);
	private final JsonUtil jsonUtil = new JsonUtil();

	private UpdatePlayedGameRequestBuilder(String tenantId) {
		this.tenantId = tenantId;
		this.playedGameInfo = new PlayedGameInfo();
	}

	public static UpdatePlayedGameRequestBuilder create(String tenantId) {
		return new UpdatePlayedGameRequestBuilder(tenantId);
	}

	public String buildRequestJson() throws Exception {
		String result = jsonLoader.getPlainTextJsonFromResources("updatePlayedGameRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO);
		result = replace(result, "tenantId", tenantId);
		result = replace(result, "userId", userId);
		result = replace(result, "mgiId", mgiId);
		result = replace(result, "playedGameInfo", playedGameInfo);
		return result;
	}

	private String replace(String result, String property, Object value) {
		if (value != null) {
			return result.replaceFirst("\"<<" + property + ">>\"", jsonUtil.toJson(value));
		} else {
			return result.replaceFirst("\"" + property + "\": \"<<" + property + ">>\",", "");
		}
	}

	public UpdatePlayedGameRequestBuilder withTenantId(String tenantId) {
		this.tenantId = tenantId;
		return this;
	}

	public UpdatePlayedGameRequestBuilder withUserId(String userId) {
		this.userId = userId;
		return this;
	}

	public UpdatePlayedGameRequestBuilder withMgiId(String mgiId) {
		this.mgiId = mgiId;
		return this;
	}

	public UpdatePlayedGameRequestBuilder withPlayedGameInfo(PlayedGameInfo playedGameInfo) {
		this.playedGameInfo = playedGameInfo;
		return this;
	}

	public UpdatePlayedGameRequestBuilder withPlayedGameInfo(String gameId, GameType type) {
		this.playedGameInfo.setGameId(gameId);
		this.playedGameInfo.setType(type);
		return this;
	}

}
