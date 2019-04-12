package de.ascendro.f4m.service.friend.model.api.contact;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class ContactRemoveInvitationRequest implements JsonMessageContent {

	private String[] contactIds;
	
	public ContactRemoveInvitationRequest() {
		// Initialize empty object
	}

	public ContactRemoveInvitationRequest(String... contactIds) {
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
