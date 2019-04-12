package de.ascendro.f4m.service.game.engine.model.end;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class EndGameRequest implements JsonMessageContent {

	private String gameInstanceId;
	private GameEndStatus status;
	private String paidWinningComponentId;
	private String freeWinningComponentId;
	
	public EndGameRequest(String gameInstanceId, GameEndStatus status, String paidWinningComponentId, 
			String freeWinningComponentId) {
		this.gameInstanceId = gameInstanceId;
		this.status = status;
		this.paidWinningComponentId = paidWinningComponentId;
		this.freeWinningComponentId = freeWinningComponentId;
	}

	public String getGameInstanceId() {
		return gameInstanceId;
	}

	public GameEndStatus getStatus() {
		return status;
	}

	public String getPaidWinningComponentId() {
		return paidWinningComponentId;
	}
	
	public String getFreeWinningComponentId() {
		return freeWinningComponentId;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("EndGameRequest [gameInstanceId=").append(gameInstanceId);
		builder.append(", status=").append(status);
		builder.append(", paidWinningComponentId=").append(paidWinningComponentId);
		builder.append(", freeWinningComponentId=");
		builder.append(freeWinningComponentId).append("]");
		return builder.toString();
	}
	
}
