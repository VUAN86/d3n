package de.ascendro.f4m.service.friend.config.request;

import de.ascendro.f4m.service.request.RequestInfoImpl;

public class InviteRequestInfoImpl extends RequestInfoImpl {

	private String invitationText;
	private String groupId;
	private String contactId;

	public InviteRequestInfoImpl(String invitationText, String groupId, String contactId) {
		this.invitationText = invitationText;
		this.groupId = groupId;
		this.contactId = contactId;
	}

	public String getContactId() {
		return contactId;
	}

	public String getInvitationText() {
		return invitationText;
	}

	public String getGroupId() {
		return groupId;
	}
	
}
