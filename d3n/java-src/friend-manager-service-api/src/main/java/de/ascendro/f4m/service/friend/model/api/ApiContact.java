package de.ascendro.f4m.service.friend.model.api;

import de.ascendro.f4m.service.friend.model.Contact;

public class ApiContact {

    private String contactId;
    private String firstName;
    private String lastName;
    private String[] phoneNumbers;
    private String[] emails;
    private String sentInvitationText;
    private String userId;
    
	public ApiContact(String appId, Contact contact) {
		contactId = contact.getContactId();
		firstName = contact.getFirstName();
		lastName = contact.getLastName();
		phoneNumbers = contact.getPhoneNumbers();
		emails = contact.getEmails();
		sentInvitationText = contact.getSentInvitationTextAndGroup(appId).getLeft();
		userId = contact.getUserId();
	}

	public String getContactId() {
		return contactId;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String[] getPhoneNumbers() {
		return phoneNumbers;
	}

	public String[] getEmails() {
		return emails;
	}

	public String getSentInvitationText() {
		return sentInvitationText;
	}

	public String getUserId() {
		return userId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("contactId=").append(contactId);
		builder.append(", firstName=").append(firstName);
		builder.append(", lastName=").append(lastName);
		builder.append(", phoneNumbers=").append(phoneNumbers);
		builder.append(", emails=").append(emails);
		builder.append(", sentInvitationText=").append(sentInvitationText);
		builder.append(", userId=").append(userId);
		builder.append("]");
		return builder.toString();
	}
    
}
