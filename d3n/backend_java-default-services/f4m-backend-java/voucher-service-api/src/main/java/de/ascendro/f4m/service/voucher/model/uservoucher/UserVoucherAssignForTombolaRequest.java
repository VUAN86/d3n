package de.ascendro.f4m.service.voucher.model.uservoucher;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class UserVoucherAssignForTombolaRequest implements JsonMessageContent {

	private String voucherId;
	private String userId;
	private String tombolaId;
	private String tombolaName;
	private String tenantId;
	private String appId;

	public UserVoucherAssignForTombolaRequest(String voucherId, String userId, String tombolaId, String tombolaName, String tenantId, String appId) {
		this.voucherId = voucherId;
		this.userId = userId;
		this.tombolaId = tombolaId;
		this.tombolaName = tombolaName;
		this.tenantId = tenantId;
		this.appId = appId;
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

	public String getTombolaId() {
		return tombolaId;
	}

	public void setTombolaId(String tombolaId) {
		this.tombolaId = tombolaId;
	}

	public String getTombolaName() {
		return tombolaName;
	}

	public void setTombolaName(String tombolaName) {
		this.tombolaName = tombolaName;
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

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("UserVoucherAssignForTombolaRequest{");
		sb.append("voucherId='").append(voucherId).append('\'');
		sb.append(", userId='").append(userId).append('\'');
		sb.append(", tombolaId='").append(tombolaId).append('\'');
		sb.append(", tombolaName='").append(tombolaName).append('\'');
		sb.append(", tenantId='").append(tenantId).append('\'');
		sb.append(", appId='").append(appId).append('\'');
		sb.append('}');
		return sb.toString();
	}
}
