package de.ascendro.f4m.service.game.engine.client.results;

import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.request.RequestInfoImpl;

public class ResultEngineRequestInfo extends RequestInfoImpl {
	private final String mgiId;
	private final GameType gameType;
	private final String gameInstanceId;
	private final String gameId;
	private final String gameTitle;

	public ResultEngineRequestInfo(String mgiId, GameType gameType, String gameInstanceId, String gameId,
			String gameTitle) {
		this.mgiId = mgiId;
		this.gameType = gameType;
		this.gameInstanceId = gameInstanceId;
		this.gameId = gameId;
		this.gameTitle = gameTitle;
	}

	public ResultEngineRequestInfo(String mgiId, GameType gameType) {
		this(mgiId, gameType, null, null, null);
	}

	public String getMgiId() {
		return mgiId;
	}

	public GameType getGameType() {
		return gameType;
	}

	public String getGameInstanceId() {
		return gameInstanceId;
	}

	public String getGameId() {
		return gameId;
	}

	public String getGameTitle() {
		return gameTitle;
	}

}
