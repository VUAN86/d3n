package de.ascendro.f4m.service.friend.client;

import javax.inject.Inject;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.service.auth.AuthMessageTypes;
import de.ascendro.f4m.service.auth.model.register.InviteUserByEmailRequest;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.friend.config.request.InviteRequestInfoImpl;
import de.ascendro.f4m.service.friend.model.Contact;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.profile.model.api.ApiProfileBasicInfo;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;

public class AuthServiceCommunicator {

	private final ServiceRegistryClient serviceRegistryClient;
	private final JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;
	private final JsonMessageUtil jsonUtil;
	private final CommonProfileAerospikeDao profileDao;

	@Inject
	public AuthServiceCommunicator(ServiceRegistryClient serviceRegistryClient,
			JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool, JsonMessageUtil jsonUtil,
			CommonProfileAerospikeDao profileDao) {
		this.serviceRegistryClient = serviceRegistryClient;
		this.jsonWebSocketClientSessionPool = jsonWebSocketClientSessionPool;
		this.jsonUtil = jsonUtil;
		this.profileDao = profileDao;
	}

	public void requestInviteUserByEmail(Contact contact, String invitationText, String groupId, 
			JsonMessage<?> sourceMessage, SessionWrapper sourceSession) {
		ApiProfileBasicInfo inviter = profileDao.getProfileBasicInfo(sourceMessage.getUserId());
		InviteUserByEmailRequest request = new InviteUserByEmailRequest(invitationText, inviter.getName(), contact.getEmails());
		JsonMessage<InviteUserByEmailRequest> message = jsonUtil.createNewMessage(AuthMessageTypes.INVITE_USER_BY_EMAIL,
				request);

		InviteRequestInfoImpl requestInfo = new InviteRequestInfoImpl(invitationText, groupId, contact.getContactId());
		requestInfo.setSourceMessage(sourceMessage);
		requestInfo.setSourceSession(sourceSession);

		try {
			ServiceConnectionInformation authServiceConnInfo = serviceRegistryClient
					.getServiceConnectionInformation(AuthMessageTypes.SERVICE_NAME);
			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(authServiceConnInfo, message, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send InviteUserByEmailRequest to Auth Service", e);
		}
	}

}
