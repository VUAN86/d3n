package de.ascendro.f4m.service.friend.model.api.contact;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class ContactInviteResponse implements JsonMessageContent {

	private String[] contactIds;
	
	public ContactInviteResponse() {
		// Initialize empty object
	}

	public ContactInviteResponse(String... contactIds) {
		this.contactIds = contactIds;
	}
	
	public String[] getContactIds() {
		return contactIds;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("contactIds=").append(contactIds);
		builder.append("]");
		return builder.toString();
	}

}
