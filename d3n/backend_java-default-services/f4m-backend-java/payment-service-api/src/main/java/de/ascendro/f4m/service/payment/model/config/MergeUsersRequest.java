package de.ascendro.f4m.service.payment.model.config;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class MergeUsersRequest implements JsonMessageContent {
	protected String tenantId;
	protected String fromProfileId;
	protected String toProfileId;

	public MergeUsersRequest() {
	}
	
	public MergeUsersRequest(String tenantId, String fromProfileId, String toProfileId) {
		this.setTenantId(tenantId);
		this.setFromProfileId(fromProfileId);
		this.setToProfileId(toProfileId);
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getFromProfileId() {
		return fromProfileId;
	}

	public void setFromProfileId(String fromProfileId) {
		this.fromProfileId = fromProfileId;
	}

	public String getToProfileId() {
		return toProfileId;
	}

	public void setToProfileId(String toProfileId) {
		this.toProfileId = toProfileId;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MergeUsersRequest [tenantId=");
		builder.append(tenantId);
		builder.append(", fromProfileId=");
		builder.append(fromProfileId);
		builder.append(", toProfileId=");
		builder.append(toProfileId);
		builder.append("]");
		return builder.toString();
	}
}
