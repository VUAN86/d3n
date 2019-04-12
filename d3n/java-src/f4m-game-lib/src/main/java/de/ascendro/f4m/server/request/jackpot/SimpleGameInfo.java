package de.ascendro.f4m.server.request.jackpot;

import de.ascendro.f4m.service.game.selection.model.game.Jackpot;

public class SimpleGameInfo {

	private String gameId;
	private String multiplayerGameInstanceId;
	private boolean isGameFree;
	private Jackpot jackpot;

	public SimpleGameInfo(String gameId, String multiplayerGameInstanceId, boolean isGameFree) {
		this.gameId = gameId;
		this.multiplayerGameInstanceId = multiplayerGameInstanceId;
		this.isGameFree = isGameFree;
	}

	public String getGameId() {
		return gameId;
	}

	public void setGameId(String gameId) {
		this.gameId = gameId;
	}

	public String getMultiplayerGameInstanceId() {
		return multiplayerGameInstanceId;
	}

	public void setMultiplayerGameInstanceId(String multiplayerGameInstanceId) {
		this.multiplayerGameInstanceId = multiplayerGameInstanceId;
	}

	public boolean isGameFree() {
		return isGameFree;
	}

	public void setGameFree(boolean isGameFree) {
		this.isGameFree = isGameFree;
	}

	public Jackpot getJackpot() {
		return jackpot;
	}

	public void setJackpot(Jackpot jackpot) {
		this.jackpot = jackpot;
	}

}
