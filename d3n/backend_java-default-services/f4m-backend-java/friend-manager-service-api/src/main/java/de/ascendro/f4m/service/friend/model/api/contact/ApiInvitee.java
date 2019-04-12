package de.ascendro.f4m.service.friend.model.api.contact;

public class ApiInvitee {

    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String email;
    
	public ApiInvitee(String firstName, String lastName, String phoneNumber, String email) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.phoneNumber = phoneNumber;
		this.email = email;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public String getEmail() {
		return email;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("firstName=").append(firstName);
		builder.append(", lastName=").append(lastName);
		builder.append(", phoneNumber=").append(phoneNumber);
		builder.append(", email=").append(email);
		builder.append("]");
		return builder.toString();
	}
    
}
