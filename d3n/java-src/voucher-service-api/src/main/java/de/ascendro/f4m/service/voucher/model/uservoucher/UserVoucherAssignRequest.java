package de.ascendro.f4m.service.voucher.model.uservoucher;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class UserVoucherAssignRequest implements JsonMessageContent {

	private String voucherId;
	private String userId;
	private String gameInstanceId;
	private String tenantId;
	private String appId;
	private String reason;

	public UserVoucherAssignRequest(String voucherId, String userId, String gameInstanceId, String tenantId, String appId, String reason) {
		this.voucherId = voucherId;
		this.userId = userId;
		this.gameInstanceId = gameInstanceId;
		this.tenantId = tenantId;
		this.appId = appId;
		this.reason = reason;
	}

	public String getVoucherId() {
		return voucherId;
	}

	public void setVoucherId(String voucherId) {
		this.voucherId = voucherId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	public String getGameInstanceId() {
		return gameInstanceId;
	}

	public void setGameInstanceId(String gameInstanceId) {
		this.gameInstanceId = gameInstanceId;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getReason() {
		return reason;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("UserVoucherAssignRequest{");
		sb.append("voucherId='").append(voucherId).append('\'');
		sb.append(", userId='").append(userId).append('\'');
		sb.append(", gameInstanceId='").append(gameInstanceId).append('\'');
		sb.append(", tenantId='").append(tenantId).append('\'');
		sb.append(", appId='").append(appId).append('\'');
		sb.append(", reason='").append(reason).append('\'');
		sb.append('}');
		return sb.toString();
	}
}
