package de.ascendro.f4m.service.payment.model.internal;

public class GetAccountHistoryRequest extends GetUserAccountHistoryRequest {
	protected String profileId;

	public String getProfileId() {
		return profileId;
	}

	public void setProfileId(String profileId) {
		this.profileId = profileId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GetAccountHistoryRequest [currency=");
		builder.append(currency);
		builder.append(", offset=");
		builder.append(offset);
		builder.append(", limit=");
		builder.append(limit);
		builder.append(", startDate=");
		builder.append(startDate);
		builder.append(", endDate=");
		builder.append(endDate);
		builder.append(", profileId=");
		builder.append(profileId);
		builder.append("]");
		return builder.toString();
	}
}
