package de.ascendro.f4m.service.game.selection.model.dashboard;

import de.ascendro.f4m.service.profile.model.api.ApiProfileBasicInfo;

public class PlayedDuelInfo {

	private String opponentUserId;
	private ApiProfileBasicInfo opponentInfo;
	private String gameCreatorId;

	public PlayedDuelInfo(String gameCreatorId) {
		this.setGameCreatorId(gameCreatorId);
		// initializing empty object
	}

	public PlayedDuelInfo(String gameCreatorId, String opponentUserId) {
		this(gameCreatorId);
		this.opponentUserId = opponentUserId;
	}

	public String getOpponentUserId() {
		return opponentUserId;
	}

	public void setOpponentUserId(String opponentUserId) {
		this.opponentUserId = opponentUserId;
	}

	public ApiProfileBasicInfo getOpponentInfo() {
		return opponentInfo;
	}

	public void setOpponentInfo(ApiProfileBasicInfo opponentInfo) {
		this.opponentInfo = opponentInfo;
	}

	public String getGameCreatorId() {
		return gameCreatorId;
	}

	public void setGameCreatorId(String gameCreatorId) {
		this.gameCreatorId = gameCreatorId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PlayedDuelInfo [opponentUserId=");
		builder.append(opponentUserId);
		builder.append(", opponentInfo=");
		builder.append(opponentInfo);
		builder.append("]");
		return builder.toString();
	}

}
