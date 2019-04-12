package de.ascendro.f4m.service.event.client;

import java.net.URI;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.service.config.F4MConfig;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.event.activemq.BrokerNetworkActiveMQ;
import de.ascendro.f4m.service.event.config.EventConfig;
import de.ascendro.f4m.service.event.model.info.InfoRequest;
import de.ascendro.f4m.service.event.model.info.InfoResponse;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.registry.EventServiceStore;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypes;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.registry.model.ServiceRegistryListResponse;

/**
 * Web Socket Client response message handler for Event Service.
 */
public class EventServiceClientMessageHandler extends DefaultJsonMessageHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(EventServiceClientMessageHandler.class);

	private final EventServiceStore eventServiceStore;
	private final JsonWebSocketClientSessionPool webSocketClient;
	private final BrokerNetworkActiveMQ brokerNetworkActiveMq;

	public EventServiceClientMessageHandler(EventServiceStore eventServiceStore,
			BrokerNetworkActiveMQ brokerNetworkActiveMq, JsonWebSocketClientSessionPool webSocketClient) {

		this.eventServiceStore = eventServiceStore;
		this.brokerNetworkActiveMq = brokerNetworkActiveMq;
		this.webSocketClient = webSocketClient;
	}

	@Override
	public JsonMessageContent onUserMessage(JsonMessage<? extends JsonMessageContent> message)
			throws F4MException {
		final ServiceRegistryMessageTypes serviceRegistryType = message.getType(ServiceRegistryMessageTypes.class);

		if (serviceRegistryType != null) {
			onServiceRegistryMessage(serviceRegistryType, message);
		} else {
			final EventMessageTypes eventMessageType = message.getType(EventMessageTypes.class);
			if (eventMessageType != null) {
				onEventServiceMessage(eventMessageType, message);
			} else {
				throw new F4MValidationFailedException("Unsupported message type by name [" + message.getName() + "]");
			}
		}

		return null;
	}

	/**
	 * Handles Service Registry messages
	 * 
	 * @param serviceRegistryType
	 *            - Service Regsitry message type
	 * @param message
	 *            - Message received
	 * @throws F4MException
	 *             - Failed to process message or messag
	 * @throws UnsupportedOperationException
	 *             -
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected JsonMessageContent onServiceRegistryMessage(ServiceRegistryMessageTypes serviceRegistryType,
			JsonMessage<? extends JsonMessageContent> message) throws F4MException, UnsupportedOperationException {
		JsonMessageContent result = null;

		if (serviceRegistryType == ServiceRegistryMessageTypes.LIST_RESPONSE) {
			onListResponse((JsonMessage<ServiceRegistryListResponse>) message);
		} else {
			result = super.onServiceRegistryMessage(serviceRegistryType, message);
		}

		return result;
	}

	/**
	 * Handles Event Service messages
	 * 
	 * @param eventMessageType
	 *            - Event Service message type
	 * @param message
	 *            - Message received
	 */
	@SuppressWarnings("unchecked")
	protected void onEventServiceMessage(EventMessageTypes eventMessageType,
			JsonMessage<? extends JsonMessageContent> message) {
		LOGGER.debug("onEventServiceMessage type {}    ----    message {} ", eventMessageType, message);
		switch (eventMessageType) {
		case INFO_RESPONSE:
			onInfoResponse((JsonMessage<InfoResponse>) message);
			break;
		default:
			throw new UnsupportedOperationException("Unsupported Service Registry message type by name ["
					+ message.getName() + "]");
		}
	}

	/**
	 * Handles Event Service infoResponse Message
	 * 
	 * @param message
	 *            - InfoResponse JSON message
	 */
	protected void onInfoResponse(JsonMessage<InfoResponse> message) {
		final URI externalEventServiceURI = getSessionWrapper().getLocalClientSessionURI();
		final InfoResponse infoResponse = message.getContent();
		try {
			final String activeMqExternalProtocol = config
					.getProperty(EventConfig.ACTIVE_MQ_EXTERNAL_COMMUNICATION_PROTOCOL);
			final String activeMqExternalHost = config.getProperty(EventConfig.ACTIVE_MQ_EXTERNAL_COMMUNICATION_HOST);
			final URI activeMqURI = new URI(activeMqExternalProtocol, externalEventServiceURI.getUserInfo(),
					activeMqExternalHost, infoResponse.getJmsPort(), null, null, null);

			brokerNetworkActiveMq.addBrokerIntoNetwork(activeMqURI);
			eventServiceStore.addEventServiceInfo(externalEventServiceURI.toString(), infoResponse);
		} catch (Exception e) {
			LOGGER.error("Failed to add ActiveQM Broker to Network, External Event Service URI ["
					+ externalEventServiceURI + "] and JMS-PORT[" + infoResponse.getJmsPort() + "]", e);
		}
	}

	/**
	 * Handles Service Registry listResponse message
	 * 
	 * @param message
	 *            - ServiceRegistryListResponse JSON message
	 * @throws F4MException
	 *             - failed to process message
	 */
	protected void onListResponse(JsonMessage<ServiceRegistryListResponse> message) throws F4MException {
		final ServiceRegistryListResponse eventServiceListResponse = message.getContent();

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Received message with ServiceRegistryListResponse[{}] content", message.getContent());
			LOGGER.debug("Current service info[host={}, port={}]", config.getProperty(F4MConfig.SERVICE_HOST),
					config.getProperty(F4MConfigImpl.JETTY_SSL_PORT));
		}

		if (CollectionUtils.isEmpty(eventServiceListResponse.getServices())) {
			eventServiceStore.setAllKnownEventServicesAreConnected(true);
		}
		for (ServiceConnectionInformation serviceConnectionInfo : eventServiceListResponse.getServices()) {
			try {
				if (!isMe(serviceConnectionInfo)) {
					LOGGER.debug("Adding EventService info[{}] into registry to build network", serviceConnectionInfo);
					
					final JsonMessage<InfoRequest> infoRequestMessage = jsonMessageUtil.createNewMessage(
							EventMessageTypes.INFO, new InfoRequest());
					eventServiceStore.addService(serviceConnectionInfo.getServiceName(), serviceConnectionInfo.getUri(), 
							serviceConnectionInfo.getServiceNamespaces());
					webSocketClient.sendAsyncMessage(serviceConnectionInfo, infoRequestMessage);
				}else{
					LOGGER.debug("Skipping serviceConnectionInfo[{}] as it's myself", serviceConnectionInfo);
				}
			} catch (F4MIOException e) {
				LOGGER.error(
						"Cannot request info of Event Service at location[" + serviceConnectionInfo.getUri() + "]", e);
			}
		}
	}

	/**
	 * Determines if received Service connection info is referencing to myself
	 * 
	 * @param serviceConnectionInfo
	 *            - ServiceConnectionInformation with not null URI
	 * @return true if service connection URI references to myself, false otherwise
	 */
	protected boolean isMe(ServiceConnectionInformation serviceConnectionInfo) {
		final int jettySslPort = config.getPropertyAsInteger(F4MConfigImpl.JETTY_SSL_PORT);
		final String serviceHost = config.getProperty(F4MConfigImpl.SERVICE_HOST);

		final URI serviceURI = URI.create(serviceConnectionInfo.getUri());
		return serviceURI.getHost().equalsIgnoreCase(serviceHost) && serviceURI.getPort() == jettySslPort;
	}

	/**
	 * No authentication is performed
	 */
	@Override
	public ClientInfo onAuthentication(RequestContext context) {
		return null;
	}

}
