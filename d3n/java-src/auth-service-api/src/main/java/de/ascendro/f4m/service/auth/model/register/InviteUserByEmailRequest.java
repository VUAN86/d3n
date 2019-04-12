package de.ascendro.f4m.service.auth.model.register;

import java.util.Arrays;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class InviteUserByEmailRequest implements JsonMessageContent {

	private String[] emails;
	private String invitationPerson;
	private String invitationText;

	public InviteUserByEmailRequest(String invitationText, String invitationPerson, String... emails) {
		this.invitationText = invitationText;
		this.invitationPerson = invitationPerson;
		this.emails = emails;
	}

	public String[] getEmails() {
		return emails;
	}

	public void setEmails(String... emails) {
		this.emails = emails;
	}

	public String getInvitationPerson() {
		return invitationPerson;
	}

	public void setInvitationPerson(String invitationPerson) {
		this.invitationPerson = invitationPerson;
	}

	public String getInvitationText() {
		return invitationText;
	}

	public void setInvitationText(String invitationText) {
		this.invitationText = invitationText;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("InviteUserByEmailRequest [emails=");
		builder.append(Arrays.toString(emails));
		builder.append(", invitationPerson=");
		builder.append(invitationPerson);
		builder.append(", invitationText=");
		builder.append(invitationText);
		builder.append("]");
		return builder.toString();
	}

}
