package de.ascendro.f4m.service.game.selection.model.multiplayer;

import de.ascendro.f4m.service.json.model.user.ClientInfo;

public class MultiplayerUserGameInstance {

	private String gameInstanceId;
	private String userId;
	private String tenantId;
	private String appId;
	private String clientIp;
	private Double userHandicap;

	public MultiplayerUserGameInstance(String gameInstanceId, ClientInfo clientInfo) {
		this.gameInstanceId = gameInstanceId;
		this.userId = clientInfo.getUserId();
		this.tenantId = clientInfo.getTenantId();
		this.appId = clientInfo.getAppId();
		this.clientIp = clientInfo.getIp();
		this.userHandicap = clientInfo.getHandicap();
	}

	public String getGameInstanceId() {
		return gameInstanceId;
	}

	public void setGameInstanceId(String gameInstanceId) {
		this.gameInstanceId = gameInstanceId;
	}

	public String getUserId() {
		return userId;
	}

	public String getTenantId() {
		return tenantId;
	}

	public String getAppId() {
		return appId;
	}

	public String getClientIp() {
		return clientIp;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public Double getUserHandicap() {
		return userHandicap;
	}

	public void setUserHandicap(Double userHandicap) {
		this.userHandicap = userHandicap;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder("MultiplayerUserGameInstance [");
		builder.append("gameInstanceId=").append(gameInstanceId);
		builder.append(", userId=").append(userId);
		builder.append(", tenantId=").append(tenantId);
		builder.append(", appId=").append(appId);
		builder.append(", clientIp=").append(clientIp);
		builder.append(", userHandicap=").append(userHandicap);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((gameInstanceId == null) ? 0 : gameInstanceId.hashCode());
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
		result = prime * result + ((tenantId == null) ? 0 : tenantId.hashCode());
		result = prime * result + ((appId == null) ? 0 : appId.hashCode());
		result = prime * result + ((clientIp == null) ? 0 : clientIp.hashCode());
		result = prime * result + ((userHandicap == null) ? 0 : userHandicap.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MultiplayerUserGameInstance other = (MultiplayerUserGameInstance) obj;
		if (gameInstanceId == null) {
			if (other.gameInstanceId != null)
				return false;
		} else if (!gameInstanceId.equals(other.gameInstanceId))
			return false;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		if (tenantId == null) {
			if (other.tenantId != null)
				return false;
		} else if (!tenantId.equals(other.tenantId))
			return false;
		if (appId == null) {
			if (other.appId != null)
				return false;
		} else if (!appId.equals(other.appId))
			return false;
		if (clientIp == null) {
			if (other.clientIp != null)
				return false;
		} else if (!clientIp.equals(other.clientIp))
			return false;
		if (userHandicap == null) {
			if (other.userHandicap != null)
				return false;
		} else if (!userHandicap.equals(other.userHandicap))
			return false;
		return true;
	}

}
