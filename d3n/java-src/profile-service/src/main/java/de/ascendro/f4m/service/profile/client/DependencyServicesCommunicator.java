package de.ascendro.f4m.service.profile.client;

import javax.inject.Inject;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.service.auth.AuthMessageTypes;
import de.ascendro.f4m.service.auth.model.register.SetUserRoleRequest;
import de.ascendro.f4m.service.friend.FriendManagerMessageTypes;
import de.ascendro.f4m.service.friend.model.api.buddy.BuddyAddForUserRequest;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.model.config.InsertOrUpdateUserRequest;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.request.RequestInfoImpl;
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
	
	public void sendBuddyAddForUser(String userId, String friendId) {
		BuddyAddForUserRequest buddyAddForUserRequest = new BuddyAddForUserRequest(userId, false, friendId);
		JsonMessage<BuddyAddForUserRequest> message = jsonUtil
				.createNewMessage(FriendManagerMessageTypes.BUDDY_ADD_FOR_USER, buddyAddForUserRequest);
		final ServiceConnectionInformation friendManagerConnInfo = serviceRegistryClient.getServiceConnectionInformation(FriendManagerMessageTypes.SERVICE_NAME);
		jsonWebSocketClientSessionPool.sendAsyncMessage(friendManagerConnInfo, message);
	}

	public void synchronizeProfileToPaymentService(InsertOrUpdateUserRequest synchronizeRequest,
			PaymentUserRequestInfo requestInfo) {
		JsonMessage<InsertOrUpdateUserRequest> message = jsonUtil
				.createNewMessage(PaymentMessageTypes.INSERT_OR_UPDATE_USER, synchronizeRequest);
		final ServiceConnectionInformation connInfo = serviceRegistryClient
				.getServiceConnectionInformation(PaymentMessageTypes.SERVICE_NAME);
		jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(connInfo, message, requestInfo);
	}

	public void updateUserRoles(SetUserRoleRequest setUserRoleRequest, String appId, String tenantId) {
		JsonMessage<SetUserRoleRequest> message = jsonUtil.createNewMessage(AuthMessageTypes.SET_USER_ROLE,
				setUserRoleRequest);
		ClientInfo clientInfo = new ClientInfo();
		clientInfo.setAppId(appId);
		clientInfo.setTenantId(tenantId);
		message.setClientInfo(clientInfo);
		final ServiceConnectionInformation connInfo = serviceRegistryClient
				.getServiceConnectionInformation(AuthMessageTypes.SERVICE_NAME);
		final RequestInfo requestInfo = new RequestInfoImpl(message);
		jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(connInfo, message, requestInfo);
	}
}
