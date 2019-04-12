package de.ascendro.f4m.service.json.handler;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.gateway.GatewayMessageTypes;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.UserIdentifier;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypes;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.registry.model.ServiceRegistryGetResponse;
import de.ascendro.f4m.service.registry.model.ServiceRegistryHeartbeatResponse;
import de.ascendro.f4m.service.registry.model.ServiceStatus;
import de.ascendro.f4m.service.session.pool.SessionStore;
import de.ascendro.f4m.service.util.register.HeartbeatMonitoringProvider;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class DefaultJsonMessageHandler extends JsonAuthenticationMessageHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultJsonMessageHandler.class);

	protected ServiceRegistryClient serviceRegistryClient;
	private HeartbeatMonitoringProvider serviceMonitoringStatus;

	@Override
	public JsonMessageContent onUserDefaultMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded) throws F4MException {
		final JsonMessageContent result;

		final ServiceRegistryMessageTypes serviceRegistryMessageType;
		final GatewayMessageTypes gatewayMessageTypes;

		if ((serviceRegistryMessageType = originalMessageDecoded.getType(ServiceRegistryMessageTypes.class)) != null) {
			result = onServiceRegistryMessage(serviceRegistryMessageType, originalMessageDecoded);
		} else if ((gatewayMessageTypes = originalMessageDecoded.getType(GatewayMessageTypes.class)) != null) {
			result = onGatewayServiceMessage(gatewayMessageTypes, originalMessageDecoded);
		} else {
			throw new F4MValidationFailedException("Unrecognized Service Message Type["
					+ originalMessageDecoded.getName() + "] is not supported");
		}

		return result;
	}

	protected JsonMessageContent onGatewayServiceMessage(GatewayMessageTypes gatewayMessageType,
			JsonMessage<? extends JsonMessageContent> originalMessageDecoded) throws F4MException {
		switch (gatewayMessageType) {

		case CLIENT_DISCONNECT:
			onClientDisconnect(originalMessageDecoded);
			break;
		case INSTANCE_DISCONNECT:
			onInstanceDisconnect(originalMessageDecoded);
			break;
		default:
			throw new F4MValidationFailedException("Gateway Registry Message Type["
					+ originalMessageDecoded.getName() + "] is not recognized");
		}

		return null;
	}

	protected void onClientDisconnect(JsonMessage<? extends JsonMessageContent> originalMessageDecoded)
			throws F4MException {
		if (originalMessageDecoded.getClientId() != null) {
			final SessionStore sessionStore = getSessionWrapper().getSessionStore();
			sessionStore.removeClient(originalMessageDecoded.getClientId());
		} else {
			throw new F4MFatalErrorException("Disconnected client id not specified");
		}
	}

	protected void onInstanceDisconnect(JsonMessage<? extends JsonMessageContent> originalMessageDecoded) {
		//TODO: implement action on Service instance disconnect, if needed
	}

	protected JsonMessageContent onServiceRegistryMessage(ServiceRegistryMessageTypes messageType,
			JsonMessage<? extends JsonMessageContent> originalMessageDecoded) throws F4MException {
		JsonMessageContent result = null;
		switch (messageType) {

		case HEARTBEAT:
			result = onHeartbeat(originalMessageDecoded);
			break;
		case GET_RESPONSE:
			onGetResponse((ServiceRegistryGetResponse) originalMessageDecoded.getContent());
			break;
		case REGISTER_RESPONSE:
		case UNREGISTER_RESPONSE:
			// No need to do anything with register response
			break;
		default:
			throw new F4MValidationFailedException("Service Registry Type[" + originalMessageDecoded.getName()
					+ "] is not recognized");
		}

		return result;
	}

	protected void onGetResponse(ServiceRegistryGetResponse getResponseConent) throws F4MException {
		if (getResponseConent != null && getResponseConent.getService() != null) {
			final ServiceConnectionInformation serviceInfo = getResponseConent.getService();
			if (serviceInfo.getUri() != null) {
				LOGGER.info("Received service connection information from service registry: {}", serviceInfo);
				serviceRegistryClient.addService(serviceInfo);
				if (EventMessageTypes.SERVICE_NAME.equalsIgnoreCase(serviceInfo.getServiceName())) {
					onEventServiceRegistered();
				}
			} else {				
				LOGGER.info("Received service with[{}] unknown location/uri", serviceInfo.getServiceName());
				if (serviceInfo.getServiceName() != null) {
					LOGGER.info("Request service with[{}] location/uri", serviceInfo.getServiceName());
					serviceRegistryClient.getServiceConnectionInformation(serviceInfo.getServiceName());
				} else {
					throw new F4MValidationFailedException(
							"Unexpected Service Registry message content - service name expected");
				}
			}
		} else {
			LOGGER.warn("No Service information not recieved");
		}
	}

	/** Can be overridden to provide some actions after event service is registered. */
	protected void onEventServiceRegistered() {
		
	}

	protected JsonMessageContent onHeartbeat(JsonMessage<? extends JsonMessageContent> heartbeatMessage) {
		getServiceMonitoringStatus().markHeartbeatResponseSent();
		return new ServiceRegistryHeartbeatResponse(ServiceStatus.GOOD);
	}

	public ServiceRegistryClient getServiceRegistryClient() {
		return serviceRegistryClient;
	}

	public void setServiceRegistryClient(ServiceRegistryClient serviceRegistryClient) {
		this.serviceRegistryClient = serviceRegistryClient;
	}

	public HeartbeatMonitoringProvider getServiceMonitoringStatus() {
		return serviceMonitoringStatus;
	}

	public void setServiceMonitoringStatus(HeartbeatMonitoringProvider serviceMonitoringStatus) {
		this.serviceMonitoringStatus = serviceMonitoringStatus;
	}

	protected boolean isMessageFromClient(JsonMessage<? extends JsonMessageContent> message) {
		return !StringUtils.isEmpty(message.getClientId());
	}

	protected boolean isAdmin(ClientInfo clientInfo) {
		LOGGER.debug("isAdmin clientInfo {} ", clientInfo);
		boolean isAdmin = false;
		if (clientInfo != null) {
			Set<String> roles = new HashSet<>(Arrays.asList(clientInfo.getRoles()));
			isAdmin = roles.contains(ClientInfo.TENANT_ROLE_PREFIX + clientInfo.getTenantId() + "_ADMIN");
			return isAdmin;
		}
		return true;
	}
	protected String getUserId(ClientInfo clientInfo, UserIdentifier userIdentifier) {
		final String userId;
		if (clientInfo != null) {
			userId = clientInfo.getUserId();
		} else {
			userId = userIdentifier.getUserId();
		}
		return userId;
	}
}
