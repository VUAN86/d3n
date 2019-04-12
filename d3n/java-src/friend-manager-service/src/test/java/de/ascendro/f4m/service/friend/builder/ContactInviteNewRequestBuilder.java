package de.ascendro.f4m.service.friend.builder;

import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.friend.model.api.contact.ApiInvitee;
import de.ascendro.f4m.service.util.JsonLoader;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;

public class ContactInviteNewRequestBuilder {

	private ApiInvitee invitee;
	private String invitationText = "Join us!";
	private String groupId;

	private final JsonLoader jsonLoader = new JsonLoader(this);
	private final JsonUtil jsonUtil = new JsonUtil();

	public static ContactInviteNewRequestBuilder createContactInviteNewRequest() {
		return new ContactInviteNewRequestBuilder();
	}

	public String buildRequestJson() throws Exception {
		String result = jsonLoader.getPlainTextJsonFromResources("contactInviteNewRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO)
				.replace("\"<<invitee>>\"", jsonUtil.toJson(invitee))
				.replace("\"<<invitationText>>\"", jsonUtil.toJson(invitationText));
		if (groupId == null) {
			result = result.replace(",\"groupId\": \"<<groupId>>\"", "");
		} else {
			result = result.replace("<<groupId>>", groupId);
		}
		return result;
	}

	public ContactInviteNewRequestBuilder withInvitee(ApiInvitee invitee) {
		this.invitee = invitee;
		return this;
	}

	public ContactInviteNewRequestBuilder withGroupId(String groupId) {
		this.groupId = groupId;
		return this;
	}
	
	public ContactInviteNewRequestBuilder withInvitationText(String invitationText) {
		this.invitationText = invitationText;
		return this;
	}

}
