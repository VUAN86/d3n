package de.ascendro.f4m.service.profile.model.get.profile;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.UserIdentifier;

/**
 * Internal profile getter - request.
 */
public class GetProfileRequest implements JsonMessageContent, UserIdentifier {
	/**
	 * From other services only
	 */
	private String userId;
	private Boolean returnMergedUser;

	public GetProfileRequest() {
	}

	public GetProfileRequest(String userId) {
		this.userId = userId;
	}

	@Override
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public Boolean getReturnMergedUser() {
		return returnMergedUser;
	}

	public void setReturnMergedUser(Boolean returnMergedUser) {
		this.returnMergedUser = returnMergedUser;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GetProfileRequest [userId=");
		builder.append(userId);
		builder.append(", returnMergedUser=");
		builder.append(returnMergedUser);
		builder.append("]");
		return builder.toString();
	}
}
