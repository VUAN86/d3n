package de.ascendro.f4m.service.payment.model.internal;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class GetJackpotRequest implements JsonMessageContent {

	private String tenantId;
	private String multiplayerGameInstanceId;

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getMultiplayerGameInstanceId() {
		return multiplayerGameInstanceId;
	}

	public void setMultiplayerGameInstanceId(String multiplayerGameInstanceId) {
		this.multiplayerGameInstanceId = multiplayerGameInstanceId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GetJackpotRequest [tenantId=");
		builder.append(tenantId);
		builder.append(", multiplayerGameInstanceId=");
		builder.append(multiplayerGameInstanceId);
		builder.append("]");
		return builder.toString();
	}
}
