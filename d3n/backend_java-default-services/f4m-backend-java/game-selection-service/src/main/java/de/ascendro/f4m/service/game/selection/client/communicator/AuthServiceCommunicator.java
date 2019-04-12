package de.ascendro.f4m.service.game.selection.client.communicator;

import javax.inject.Inject;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.service.auth.AuthMessageTypes;
import de.ascendro.f4m.service.auth.model.register.InviteUserByEmailRequest;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.game.selection.request.InviteRequestInfoImpl;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.profile.model.api.ApiProfileBasicInfo;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;

public class AuthServiceCommunicator {

	private final ServiceRegistryClient serviceRegistryClient;
	private final JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;
	private final JsonMessageUtil jsonMessageUtil;
	private final CommonProfileAerospikeDao profileDao;

	@Inject
	public AuthServiceCommunicator(ServiceRegistryClient serviceRegistryClient,
			JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool, JsonMessageUtil jsonMessageUtil,
			CommonProfileAerospikeDao profileDao) {
		this.serviceRegistryClient = serviceRegistryClient;
		this.jsonWebSocketClientSessionPool = jsonWebSocketClientSessionPool;
		this.jsonMessageUtil = jsonMessageUtil;
		this.profileDao = profileDao;
	}

	public void requestInviteUserByEmail(JsonMessage<? extends JsonMessageContent> sourceMessage,
			SessionWrapper sourceSession, String email, String mgiId, String gameInstanceId) {
		InviteRequestInfoImpl requestInfo = new InviteRequestInfoImpl(sourceMessage, sourceSession, mgiId);
		requestInfo.setGameInstanceId(gameInstanceId);
		
		ApiProfileBasicInfo inviter = profileDao.getProfileBasicInfo(sourceMessage.getUserId());
		InviteUserByEmailRequest request = new InviteUserByEmailRequest(null, inviter.getName(), email);
		JsonMessage<InviteUserByEmailRequest> message = jsonMessageUtil.createNewMessage(AuthMessageTypes.INVITE_USER_BY_EMAIL,
				request);

		try {
			ServiceConnectionInformation authServiceConnInfo = serviceRegistryClient
					.getServiceConnectionInformation(AuthMessageTypes.SERVICE_NAME);
			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(authServiceConnInfo, message, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send InviteUserByEmailRequest to Auth Service", e);
		}
	}

}
