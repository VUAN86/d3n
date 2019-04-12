package de.ascendro.f4m.service.voucher.request;

import de.ascendro.f4m.service.request.RequestInfoImpl;

public class ProfileProcessingInfo extends RequestInfoImpl {
	private ProfileRequestStatus profileRequestStatus = null;
	private String userId;

	private String relatedFieldId;

	public ProfileProcessingInfo(ProfileRequestStatus profileRequestStatus) {
		this.profileRequestStatus = profileRequestStatus;
	}

	public ProfileRequestStatus getProfileRequestStatus() {
		return profileRequestStatus;
	}

	public void setProfileRequestStatus(ProfileRequestStatus profileRequestStatus) {
		this.profileRequestStatus = profileRequestStatus;
	}

	public void setRelatedFieldId(String relatedFieldId) {
		this.relatedFieldId = relatedFieldId;
	}

	public String getRelatedFieldId() {
		return relatedFieldId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getUserId() {
		return userId;
	}
}
