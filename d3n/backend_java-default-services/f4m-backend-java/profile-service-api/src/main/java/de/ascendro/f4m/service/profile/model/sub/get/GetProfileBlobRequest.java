package de.ascendro.f4m.service.profile.model.sub.get;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.UserIdentifier;

public class GetProfileBlobRequest implements JsonMessageContent, UserIdentifier {

	private String userId;

	private String name;

	public GetProfileBlobRequest() {
	}

	public GetProfileBlobRequest(String name) {
		this.name = name;
	}

	@Override
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
