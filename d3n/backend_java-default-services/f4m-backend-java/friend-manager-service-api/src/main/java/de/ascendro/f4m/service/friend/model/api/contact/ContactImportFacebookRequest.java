package de.ascendro.f4m.service.friend.model.api.contact;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class ContactImportFacebookRequest implements JsonMessageContent {

	private String token;
	
	public ContactImportFacebookRequest() {
		// Initialize empty object
	}

	public ContactImportFacebookRequest(String token) {
		this.token = token;
	}
	
	public String getToken() {
		return token;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("token=").append(token);
		builder.append("]");
		return builder.toString();
	}

}
