package de.ascendro.f4m.service.friend.model.api.contact;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class ContactInviteNewRequest implements JsonMessageContent {

	private ApiInvitee invitee;
	private String invitationText;
	private String groupId;

	public ContactInviteNewRequest() {
		// Initialize empty object
	}

	public ContactInviteNewRequest(String invitationText, String groupId, ApiInvitee invitee) {
		this.invitationText = invitationText;
		this.invitee = invitee;
		this.groupId = groupId;
	}

	public String getInvitationText() {
		return invitationText;
	}

	public String getGroupId() {
		return groupId;
	}
	
	public ApiInvitee getInvitees() {
		return invitee;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("invitee=").append(invitee);
		builder.append(", invitationText=").append(invitationText);
		builder.append(", groupId=").append(groupId);
		builder.append("]");
		return builder.toString();
	}

}
