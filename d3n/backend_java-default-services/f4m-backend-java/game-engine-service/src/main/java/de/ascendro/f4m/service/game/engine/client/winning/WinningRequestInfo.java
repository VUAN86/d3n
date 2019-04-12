package de.ascendro.f4m.service.game.engine.client.winning;

import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.request.RequestInfoImpl;

public class WinningRequestInfo extends RequestInfoImpl {
	private final String gameInstanceId;
	private final String mgiId;
	private final GameType gameType;
	private final String freeWinningComponentId;
	private final String paidWinningComponentId;

	public WinningRequestInfo(String gameInstanceId, String mgiId, GameType gameType,
			String freeWinningComponentId, String paidWinningComponentId) {
		this.gameInstanceId = gameInstanceId;
		this.mgiId = mgiId;
		this.gameType = gameType;
		this.freeWinningComponentId = freeWinningComponentId;
		this.paidWinningComponentId = paidWinningComponentId;
	}
	
	public String getGameInstanceId() {
		return gameInstanceId;
	}

	public String getMgiId() {
		return mgiId;
	}
	
	public GameType getGameType() {
		return gameType;
	}

	public String getFreeWinningComponentId() {
		return freeWinningComponentId;
	}

	public String getPaidWinningComponentId() {
		return paidWinningComponentId;
	}

}
