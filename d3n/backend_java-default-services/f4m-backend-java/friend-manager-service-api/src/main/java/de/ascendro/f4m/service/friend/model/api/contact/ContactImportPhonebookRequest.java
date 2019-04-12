package de.ascendro.f4m.service.friend.model.api.contact;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class ContactImportPhonebookRequest implements JsonMessageContent {

	private ApiPhonebookEntry[] contacts;
	
	public ContactImportPhonebookRequest() {
		// Initialize empty object
	}

	public ContactImportPhonebookRequest(ApiPhonebookEntry... contacts) {
		this.contacts = contacts;
	}
	
	public ApiPhonebookEntry[] getContacts() {
		return contacts;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("contacts=").append(contacts);
		builder.append("]");
		return builder.toString();
	}

}
