package de.ascendro.f4m.service.friend.model.api.contact;

public class ApiPhonebookEntry {

	private String firstName;
	private String lastName;
	private String[] phoneNumbers;
	private String[] emails;

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String[] getPhoneNumbers() {
		return phoneNumbers;
	}

	public void setPhoneNumbers(String[] phoneNumbers) {
		this.phoneNumbers = phoneNumbers;
	}

	public String[] getEmails() {
		return emails;
	}

	public void setEmails(String[] emails) {
		this.emails = emails;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("firstName=").append(firstName);
		builder.append(", lastName=").append(lastName);
		builder.append(", phoneNumbers=").append(phoneNumbers);
		builder.append(", emails=").append(emails);
		builder.append("]");
		return builder.toString();
	}
    
}
