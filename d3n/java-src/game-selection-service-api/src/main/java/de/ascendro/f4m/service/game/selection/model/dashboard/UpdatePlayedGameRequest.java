package de.ascendro.f4m.service.game.selection.model.dashboard;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class UpdatePlayedGameRequest implements JsonMessageContent {

	private String tenantId;
	private String userId;
	private String mgiId;
	private PlayedGameInfo playedGameInfo;

	public UpdatePlayedGameRequest(String tenantId, String userId, PlayedGameInfo playedGameInfo) {
		this.tenantId = tenantId;
		this.userId = userId;
		this.playedGameInfo = playedGameInfo;
	}

	public UpdatePlayedGameRequest(String tenantId, String mgiId) {
		this.tenantId = tenantId;
		this.mgiId = mgiId;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getMgiId() {
		return mgiId;
	}

	public void setMgiId(String mgiId) {
		this.mgiId = mgiId;
	}

	public PlayedGameInfo getPlayedGameInfo() {
		return playedGameInfo;
	}

	public void setPlayedGameInfo(PlayedGameInfo playedGameInfo) {
		this.playedGameInfo = playedGameInfo;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UpdatePlayedGameRequest [tenantId=");
		builder.append(tenantId);
		builder.append(", userId=");
		builder.append(userId);
		builder.append(", mgiId=");
		builder.append(mgiId);
		builder.append(", playedGameInfo=");
		builder.append(playedGameInfo);
		builder.append("]");
		return builder.toString();
	}

}
