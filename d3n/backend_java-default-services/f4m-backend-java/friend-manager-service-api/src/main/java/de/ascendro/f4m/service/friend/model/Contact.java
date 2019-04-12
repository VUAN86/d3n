package de.ascendro.f4m.service.friend.model;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import de.ascendro.f4m.service.friend.model.api.contact.ApiInvitee;
import de.ascendro.f4m.service.friend.model.api.contact.ApiPhonebookEntry;
import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

public class Contact extends JsonObjectWrapper {

    public static final String PROPERTY_OWNER_ID = "contactOwnerId";
    public static final String PROPERTY_CONTACT_ID = "contactId";
    public static final String PROPERTY_FIRST_NAME = "contactFirstName";
    public static final String PROPERTY_LAST_NAME = "contactLastName";
    public static final String PROPERTY_PHONE_NUMBERS = "contactPhoneNumbers";
    public static final String PROPERTY_EMAILS = "contactEmails";
    public static final String PROPERTY_USER_ID = "contactUserId";
    public static final String PROPERTY_TENANT_IDS = "contactTenantIds";

    public static final String PROPERTY_SENT_INVITATION_TEXTS = "sentInvitationTexts";
    public static final String PROPERTY_APPLICATION_ID = "applicationId";
    public static final String PROPERTY_INVITATION_TEXT = "invitationText";
    public static final String PROPERTY_GROUP_ID = "groupId";

    public Contact() {
    	super();
    }

    public Contact(JsonObject contact) {
    	super(contact);
    }
    
    public Contact(String ownerId, ApiInvitee invitee) {
    	setOwnerId(ownerId);
        setFirstName(invitee.getFirstName());
        setLastName(invitee.getLastName());
        setPhoneNumbers(invitee.getPhoneNumber());
        setEmails(invitee.getEmail());
    }

    public Contact(String ownerId, String tenantId, ApiPhonebookEntry entry) {
    	setOwnerId(ownerId);
    	setTenantIds(tenantId);
    	setFirstName(entry.getFirstName());
    	setLastName(entry.getLastName());
    	setPhoneNumbers(removeBlankEntries(entry.getPhoneNumbers()));
    	setEmails(removeBlankEntries(entry.getEmails()));
    }
    
    private String[] removeBlankEntries(String[] array){
		final String[] arrayWithoutBlanks;
    	if (array != null) {
    		arrayWithoutBlanks = Arrays.stream(array)
    			.filter(StringUtils::isNotBlank)
    			.toArray(String[]::new);
		} else {
			arrayWithoutBlanks = null;
		}
    	return arrayWithoutBlanks;
    }
    
	public void merge(String tenantId, ApiPhonebookEntry item) {
		// Overwrite firstname/lastname
		setFirstName(item.getFirstName());
		setLastName(item.getLastName());
		// Add tenant, if not yet present
		String[] tenantIds = getTenantIds();
		if (tenantIds == null || ! Arrays.asList(tenantIds).contains(tenantId)) {
			addElementToArray(PROPERTY_TENANT_IDS, new JsonPrimitive(tenantId));
		}
		// Add new phones/emails
		mergeArraysUnion(item.getPhoneNumbers(), PROPERTY_PHONE_NUMBERS);
		mergeArraysUnion(item.getEmails(), PROPERTY_EMAILS);
	}

	private void mergeArraysUnion(String[] source, String arrayPropertyName) {
		if (ArrayUtils.isEmpty(source)) {
			return;
		}
		Map<String, String> newEntries = Arrays.stream(source).collect(Collectors.toMap(this::normalize, s -> s));
		JsonArray existing = getArray(arrayPropertyName);
		if (existing == null) {
			existing = new JsonArray();
			setProperty(arrayPropertyName, existing);
		} else {
			existing.forEach(e -> newEntries.remove(normalize(e.getAsString())));
		}
		newEntries.values().forEach(existing::add);
	}

	private String normalize(String s) {
		return StringUtils.lowerCase(StringUtils.trim(s));
	}

	public String getOwnerId() {
    	return getPropertyAsString(PROPERTY_OWNER_ID);
    }
    
    public void setOwnerId(String ownerId) {
    	setProperty(PROPERTY_OWNER_ID, ownerId);
    }
    
    public String getContactId() {
        return getPropertyAsString(PROPERTY_CONTACT_ID);
    }

    public void setContactId(String contactId) {
        setProperty(PROPERTY_CONTACT_ID, contactId);
    }

    public String getFirstName() {
        return getPropertyAsString(PROPERTY_FIRST_NAME);
    }

    public void setFirstName(String firstName) {
        setProperty(PROPERTY_FIRST_NAME, firstName);
    }

    public String getLastName() {
        return getPropertyAsString(PROPERTY_LAST_NAME);
    }

    public void setLastName(String lastName) {
        setProperty(PROPERTY_LAST_NAME, lastName);
    }

    public String[] getPhoneNumbers() {
        String[] phoneNumbers = getPropertyAsStringArray(PROPERTY_PHONE_NUMBERS);
        return phoneNumbers == null ? ArrayUtils.EMPTY_STRING_ARRAY : phoneNumbers;
    }

    public void setPhoneNumbers(String... phoneNumbers) {
        setArray(PROPERTY_PHONE_NUMBERS, phoneNumbers);
    }

    public String[] getEmails() {
        String[] emails = getPropertyAsStringArray(PROPERTY_EMAILS);
        return emails == null ? ArrayUtils.EMPTY_STRING_ARRAY : emails;
    }

    public void setEmails(String... emails) {
        setArray(PROPERTY_EMAILS, emails);
    }

    /** Get sent invitation text and group for given appId. */
    public Pair<String, String> getSentInvitationTextAndGroup(String appId) {
    	JsonArray sentInvitationTexts = getArray(PROPERTY_SENT_INVITATION_TEXTS);
    	if (sentInvitationTexts != null) {
    		for (JsonElement sentInvitationText : sentInvitationTexts) {
    			if (isSentInvitationForAppId(sentInvitationText, appId)) {
    				JsonObject invitation = sentInvitationText.getAsJsonObject();
    				JsonElement groupId = invitation.get(PROPERTY_GROUP_ID);
    		    	return Pair.of(invitation.get(PROPERTY_INVITATION_TEXT).getAsString(),
    		    			groupId == null || groupId.isJsonNull() ? null : groupId.getAsString());
    			}
    		}
    	}
    	return Pair.of(null,  null);
    }
    
    public void setSentInvitationTextAndGroup(String appId, String sentInvitationText, String groupId) {
    	JsonArray sentInvitationTexts = getArray(PROPERTY_SENT_INVITATION_TEXTS);
    	if (sentInvitationTexts == null) {
    		sentInvitationTexts = new JsonArray();
    		setProperty(PROPERTY_SENT_INVITATION_TEXTS, sentInvitationTexts);
    	}
    	for (Iterator<JsonElement> it = sentInvitationTexts.iterator() ; it.hasNext() ; ) {
    		JsonElement text = it.next();
    		if (isSentInvitationForAppId(text, appId)) {
    			if (sentInvitationText == null) {
    				it.remove();
    			} else {
    				JsonObject invitation = text.getAsJsonObject();
    				invitation.addProperty(PROPERTY_INVITATION_TEXT, sentInvitationText);
    				invitation.addProperty(PROPERTY_GROUP_ID, groupId);
    			}
    			return;
    		}
    	}
    	if (sentInvitationText != null) {
    		JsonObject text = new JsonObject();
    		text.addProperty(PROPERTY_APPLICATION_ID, appId);
    		text.addProperty(PROPERTY_INVITATION_TEXT, sentInvitationText);
    		text.addProperty(PROPERTY_GROUP_ID, groupId);
    		sentInvitationTexts.add(text);
    	}
    }
    
    private boolean isSentInvitationForAppId(JsonElement sentInvitationText, String appId) {
    	return sentInvitationText == null ? false : sentInvitationText.getAsJsonObject().get(PROPERTY_APPLICATION_ID).getAsString().equals(appId);
    }
    
    public String getUserId() {
    	return getPropertyAsString(PROPERTY_USER_ID);
    }
    
    public void setUserId(String userId) {
    	setProperty(PROPERTY_USER_ID, userId);
    }

	public String[] getTenantIds() {
		return getPropertyAsStringArray(PROPERTY_TENANT_IDS);
	}

	public void setTenantIds(String... tenants) {
		setArray(PROPERTY_TENANT_IDS, tenants);
	}

}
