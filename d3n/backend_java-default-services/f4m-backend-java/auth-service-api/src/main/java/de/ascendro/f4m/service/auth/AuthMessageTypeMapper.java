package de.ascendro.f4m.service.auth;

import com.google.gson.reflect.TypeToken;

import de.ascendro.f4m.service.auth.model.register.InviteUserByEmailRequest;
import de.ascendro.f4m.service.auth.model.register.InviteUserByEmailResponse;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMapImpl;

public class AuthMessageTypeMapper extends JsonMessageTypeMapImpl {
	private static final long serialVersionUID = 1782640613462657356L;

	public AuthMessageTypeMapper() {
		init();
	}

	protected void init() {
		///getPublicKey
		this.register(AuthMessageTypes.GET_PUBLIC_KEY, new TypeToken<EmptyJsonMessageContent>() {
		}.getType());
		this.register(AuthMessageTypes.INVITE_USER_BY_EMAIL, new TypeToken<InviteUserByEmailRequest>() {
		}.getType());
		this.register(AuthMessageTypes.INVITE_USER_BY_EMAIL_RESPONSE, new TypeToken<InviteUserByEmailResponse>() {
		}.getType());
	}
}
