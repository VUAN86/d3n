package de.ascendro.f4m.service.friend.builder;

import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.util.JsonLoader;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;

public class ContactInviteOrResendRequestBuilder {

	private String request;
	private String[] contactIds = {};
	private String invitationText;

	private final JsonLoader jsonLoader = new JsonLoader(this);
	private final JsonUtil jsonUtil = new JsonUtil();

	public ContactInviteOrResendRequestBuilder(String request) {
		this.request = request;
	}

	public static ContactInviteOrResendRequestBuilder createContactInviteOrResendRequest(String request) {
		return new ContactInviteOrResendRequestBuilder(request);
	}

	public String buildRequestJson() throws Exception {
		String result = jsonLoader.getPlainTextJsonFromResources("contactInviteOrResendRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO)
				.replaceFirst("<<request>>", request)
				.replaceFirst("\"<<contactIds>>\"", jsonUtil.toJson(contactIds));
		if (invitationText != null) {
			result = result.replaceFirst("\"<<invitationText>>\"", jsonUtil.toJson(invitationText));
		} else {
			result = result.replaceFirst("\"invitationText\": \"<<invitationText>>\",", "");
		}
		return result;
	}

	public ContactInviteOrResendRequestBuilder withContactIds(String... contactIds) {
		this.contactIds = contactIds;
		return this;
	}

	public ContactInviteOrResendRequestBuilder withInvitationText(String invitationText) {
		this.invitationText = invitationText;
		return this;
	}

}
