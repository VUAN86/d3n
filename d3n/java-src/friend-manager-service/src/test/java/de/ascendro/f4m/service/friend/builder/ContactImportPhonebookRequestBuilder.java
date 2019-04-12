package de.ascendro.f4m.service.friend.builder;

import java.util.Arrays;

import com.google.gson.JsonArray;

import de.ascendro.f4m.service.util.JsonLoader;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;

public class ContactImportPhonebookRequestBuilder {
	
	private String firstName = "toBeReplaced";
	private String lastName = "toBeReplaced";
	private JsonArray phoneNumbers = new JsonArray();
	private JsonArray emails = new JsonArray();
	
	private final JsonLoader jsonLoader = new JsonLoader(this);
	
	public static ContactImportPhonebookRequestBuilder createImportPhonebookRequest() {
		return new ContactImportPhonebookRequestBuilder();
	}
	
	public String buildRequestJson() throws Exception {
		String result = jsonLoader.getPlainTextJsonFromResources("contactImportPhonebookRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO)
				.replace("<<firstName>>", firstName)
				.replace("<<lastName>>", lastName)
				.replace("<<phoneNumbers>>", phoneNumbers.toString())
				.replace("<<emails>>", emails.toString());
		return result;
	}

	public ContactImportPhonebookRequestBuilder withFirstName(String firstName) {
		this.firstName = firstName;
		return this;
	}

	public ContactImportPhonebookRequestBuilder withLastName(String lastName) {
		this.lastName = lastName;
		return this;
	}

	public ContactImportPhonebookRequestBuilder withPhoneNumbers(String... phoneNumbers) {
		Arrays.stream(phoneNumbers).forEach(this.phoneNumbers::add);
		return this;
	}

	public ContactImportPhonebookRequestBuilder withEmails(String... emails) {
		Arrays.stream(emails).forEach(this.emails::add);
		return this;
	}

}
