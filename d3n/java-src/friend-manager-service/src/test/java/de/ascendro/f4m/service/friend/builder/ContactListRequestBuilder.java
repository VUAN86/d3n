package de.ascendro.f4m.service.friend.builder;

import de.ascendro.f4m.service.friend.model.api.contact.ContactListRequest;
import de.ascendro.f4m.service.util.JsonLoader;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;

public class ContactListRequestBuilder {
	
	private Boolean hasApp = false;
	private Boolean hasProfile;
	private Boolean hasInvitation;
	private Boolean includeProfileInfo;
	private Boolean includeBuddyInfo;
	private int limit = ContactListRequest.MAX_LIST_LIMIT;
	private long offset = 0L;
	
	private final JsonLoader jsonLoader = new JsonLoader(this);
	
	public static ContactListRequestBuilder createContactListRequest() {
		return new ContactListRequestBuilder();
	}
	
	public String buildRequestJson() throws Exception {
		String result = jsonLoader.getPlainTextJsonFromResources("contactListRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO)
				.replace("\"<<limit>>\"", Integer.toString(limit))
				.replace("\"<<offset>>\"", Long.toString(offset));
		result = replace(result, "hasApp", hasApp);
		result = replace(result, "hasProfile", hasProfile);
		result = replace(result, "hasInvitation", hasInvitation);
		result = replace(result, "includeProfileInfo", includeProfileInfo);
		result = replace(result, "includeBuddyInfo", includeBuddyInfo);
		return result;
	}

	private String replace(String result, String property, Boolean flag) {
		if (flag == null) {
			return result.replace("\"" + property + "\": \"<<" + property + ">>\",", "");
		} else {
			return result.replace("\"<<" + property + ">>\"", Boolean.toString(flag));
		}
	}

	public ContactListRequestBuilder withHasApp() {
		this.hasApp = true;
		return this;
	}

	public ContactListRequestBuilder withHasProfile() {
		this.hasProfile = true;
		return this;
	}

	public ContactListRequestBuilder withHasInvitation() {
		this.hasInvitation = true;
		return this;
	}

	public ContactListRequestBuilder withIncludeProfileInfo() {
		this.includeProfileInfo = true;
		return this;
	}

	public ContactListRequestBuilder withIncludeBuddyInfo() {
		this.includeBuddyInfo = true;
		return this;
	}

	public ContactListRequestBuilder withLimit(int limit) {
		this.limit = limit;
		return this;
	}

	public ContactListRequestBuilder withOffset(long offset) {
		this.offset = offset;
		return this;
	}

}
