package de.ascendro.f4m.service.friend.model.api.contact;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class ContactInviteRequest implements JsonMessageContent {

	private String[] contactIds;
	private String invitationText;
	private String groupId;
	
	public ContactInviteRequest() {
		// Initialize empty object
	}

	public ContactInviteRequest(String invitationText, String groupId, String... contactIds) {
		this.invitationText = invitationText;
		this.contactIds = contactIds;
		this.groupId = groupId;
	}
	
	public String getInvitationText() {
		return invitationText;
	}
	
	public String getGroupId() {
		return groupId;
	}
	
	public String[] getContactIds() {
		return contactIds;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("contactIds=").append(contactIds);
		builder.append(", invitationText=").append(invitationText);
		builder.append(", groupId=").append(groupId);
		builder.append("]");
		return builder.toString();
	}

}
