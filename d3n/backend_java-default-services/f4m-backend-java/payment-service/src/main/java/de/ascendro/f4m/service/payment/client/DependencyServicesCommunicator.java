package de.ascendro.f4m.service.payment.client;

import javax.inject.Inject;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.service.auth.AuthMessageTypes;
import de.ascendro.f4m.service.auth.model.register.SetUserRoleRequest;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.profile.ProfileMessageTypes;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.update.UpdateProfileRequest;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.request.RequestInfoImpl;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.usermessage.model.SendWebsocketMessageRequest;
import de.ascendro.f4m.service.usermessage.notification.MobilePushJsonNotification;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;

public class DependencyServicesCommunicator {

	private ServiceRegistryClient serviceRegistryClient;
	private JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;
	private JsonMessageUtil jsonUtil;

	@Inject
	public DependencyServicesCommunicator(ServiceRegistryClient serviceRegistryClient,
			JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool, JsonMessageUtil jsonUtil) {
		this.serviceRegistryClient = serviceRegistryClient;
		this.jsonWebSocketClientSessionPool = jsonWebSocketClientSessionPool;
		this.jsonUtil = jsonUtil;
	}

	public void requestUpdateProfileIdentity(String profileId, Profile profile) {
		final UpdateProfileRequest request = new UpdateProfileRequest(profileId);
		request.setProfile(profile.getJsonObject());
		request.setService("paymentService");
		final JsonMessage<UpdateProfileRequest> message = jsonUtil.createNewMessage(ProfileMessageTypes.UPDATE_PROFILE,
				request);
		final RequestInfo requestInfo = new RequestInfoImpl(message);
		final ServiceConnectionInformation connInfo = serviceRegistryClient
				.getServiceConnectionInformation(ProfileMessageTypes.SERVICE_NAME);
		jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(connInfo, message, requestInfo);
	}
	
	public void sendPushAndDirectMessages(String profileId, String appId, String messageText, String[] parameters,
			MobilePushJsonNotification payload) {
		sendWebsocketToUser(profileId, appId, messageText, parameters, payload);
	}

	private void sendWebsocketToUser(String profileId, String appId, String messageText, String[] parameters,
			MobilePushJsonNotification payload) {
		SendWebsocketMessageRequest requestContent = new SendWebsocketMessageRequest(true);
		requestContent.setUserId(profileId);
		requestContent.setType(payload.getType());
		requestContent.setPayload(jsonUtil.toJsonElement(payload));
		requestContent.setLanguageAuto();
		requestContent.setMessage(messageText);
		requestContent.setParameters(parameters);
		JsonMessage<SendWebsocketMessageRequest> message = jsonUtil
				.createNewMessage(UserMessageMessageTypes.SEND_WEBSOCKET_MESSAGE, requestContent);
		ClientInfo clientInfo = new ClientInfo();
		clientInfo.setAppId(appId);
		clientInfo.setUserId(profileId);
		message.setClientInfo(clientInfo);
		sendToUserMessage(message);
	}

	private void sendToUserMessage(JsonMessage<?> message) {
		try {
			ServiceConnectionInformation userMessageConnInfo = serviceRegistryClient
					.getServiceConnectionInformation(UserMessageMessageTypes.SERVICE_NAME);
			jsonWebSocketClientSessionPool.sendAsyncMessage(userMessageConnInfo, message);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send push loadOrWithdrawWithoutCoverage to User Message Service", e);
		}
	}

	public void updateUserRoles(SetUserRoleRequest setUserRoleRequest) {
		JsonMessage<SetUserRoleRequest> message = jsonUtil.createNewMessage(AuthMessageTypes.SET_USER_ROLE,
				setUserRoleRequest);
		final ServiceConnectionInformation connInfo = serviceRegistryClient
				.getServiceConnectionInformation(AuthMessageTypes.SERVICE_NAME);
		final RequestInfo requestInfo = new RequestInfoImpl(message);
		jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(connInfo, message, requestInfo);
	}
}
