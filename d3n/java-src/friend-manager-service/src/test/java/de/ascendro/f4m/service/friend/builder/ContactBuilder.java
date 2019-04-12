package de.ascendro.f4m.service.friend.builder;

import java.util.Arrays;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.friend.model.Contact;
import de.ascendro.f4m.service.util.JsonLoader;

public class ContactBuilder {
	
	private String ownerId;
	private String contactId;
    private String firstName;
    private String lastName;
    private String[] phoneNumbers;
    private String[] emails;
    private JsonArray sentInvitationTexts;
    private String userId;
    private String[] tenantIds;
    
    private final JsonUtil jsonUtil = new JsonUtil();
    
	public ContactBuilder(String ownerId, String contactId, String tenantId) {
		this.ownerId = ownerId;
		this.contactId = contactId;
		this.tenantIds = new String[] { tenantId };
	}

	public static ContactBuilder createContact(String ownerId, String contactId, String tenantId) {
    	return new ContactBuilder(ownerId, contactId, tenantId);
    }
    
    public Contact buildContact() throws Exception {
    	String contactJson = JsonLoader.getTextFromResources("contact.json", this.getClass())
    			.replace("\"<<ownerId>>\"", jsonUtil.toJson(ownerId))
    			.replace("\"<<contactId>>\"", jsonUtil.toJson(contactId))
    			.replace("\"<<firstName>>\"", jsonUtil.toJson(firstName))
    			.replace("\"<<lastName>>\"", jsonUtil.toJson(lastName))
    			.replace("\"<<phoneNumbers>>\"", jsonUtil.toJson(phoneNumbers))
    			.replace("\"<<emails>>\"", jsonUtil.toJson(emails))
    			.replace("\"<<sentInvitationTexts>>\"", jsonUtil.toJson(sentInvitationTexts))
    			.replace("\"<<userId>>\"", jsonUtil.toJson(userId))
    			.replace("\"<<tenantIds>>\"", jsonUtil.toJson(tenantIds));
    	return new Contact(jsonUtil.fromJson(contactJson, JsonObject.class));
	}

	public ContactBuilder withContactId(String contactId) {
		this.contactId = contactId;
		return this;
	}

	public ContactBuilder withFirstName(String firstName) {
		this.firstName = firstName;
		return this;
	}

	public ContactBuilder withLastName(String lastName) {
		this.lastName = lastName;
		return this;
	}

	public ContactBuilder withPhoneNumbers(String... phoneNumbers) {
		this.phoneNumbers = phoneNumbers;
		return this;
	}

	public ContactBuilder withEmails(String... emails) {
		this.emails = emails;
		return this;
	}

	public ContactBuilder withSentInvitationText(String sentInvitationText, String... applicationIds) {
		sentInvitationTexts = new JsonArray();
		Arrays.stream(applicationIds).forEach(appId -> {
			JsonObject text = new JsonObject();
			text.addProperty(Contact.PROPERTY_APPLICATION_ID, appId);
			text.addProperty(Contact.PROPERTY_INVITATION_TEXT, sentInvitationText);
			sentInvitationTexts.add(text);
		});
		return this;
	}

	public ContactBuilder withUserId(String userId) {
		this.userId = userId;
		return this;
	}

}
