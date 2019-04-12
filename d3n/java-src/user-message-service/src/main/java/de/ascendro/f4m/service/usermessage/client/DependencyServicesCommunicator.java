package de.ascendro.f4m.service.usermessage.client;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.server.session.GlobalClientSessionDao;
import de.ascendro.f4m.server.session.GlobalClientSessionInfo;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.gateway.GatewayMessageTypes;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.request.RequestInfoImpl;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.usermessage.direct.MessageInfoUtil;
import de.ascendro.f4m.service.usermessage.model.NewWebsocketMessageRequest;
import de.ascendro.f4m.service.usermessage.model.SendWebsocketMessageRequest;

/**
 * Class for sending user information requests to Profile Service
 */
public class DependencyServicesCommunicator {
	private static final Logger LOGGER = LoggerFactory.getLogger(DependencyServicesCommunicator.class);
	
	private JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;
	private JsonMessageUtil jsonUtil;
	private GlobalClientSessionDao globalClientSessionDao;

	@Inject
	public DependencyServicesCommunicator(JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool, JsonMessageUtil jsonUtil,
			GlobalClientSessionDao globalClientSessionDao) {
		this.jsonWebSocketClientSessionPool = jsonWebSocketClientSessionPool;
		this.jsonUtil = jsonUtil;
		this.globalClientSessionDao = globalClientSessionDao;
	}

	public void initiateDirectMessageSendingToUserViaGateway(SendWebsocketMessageRequest websocketRequest,
			String messageId, SessionWrapper sessionWrapper) {
		NewWebsocketMessageRequest request = new NewWebsocketMessageRequest();
		request = MessageInfoUtil.copyMessageInfo(websocketRequest, request);
		request.setMessageId(messageId);
		request.setUserId(websocketRequest.getUserId());
		JsonMessage<NewWebsocketMessageRequest> requestJsonMessage = jsonUtil.createNewMessage(
				UserMessageMessageTypes.NEW_WEBSOCKET_MESSAGE, request);
		RequestInfo requestInfo = new RequestInfoImpl();
		requestInfo.setSourceMessage(requestJsonMessage);
		requestInfo.setSourceSession(sessionWrapper);
		try {
			GlobalClientSessionInfo session = globalClientSessionDao.getGlobalClientSessionInfoByUserId(websocketRequest.getUserId());
			if (session != null) {
				requestJsonMessage.setClientId(session.getClientId());
				final ServiceConnectionInformation gatewayConnInfo = getUserGatewayConnInfo(session);
				jsonWebSocketClientSessionPool.sendAsyncMessageNoClientInfo(gatewayConnInfo, requestJsonMessage, requestInfo);
			} else {
				LOGGER.debug(
						"Cannot send direct message, no active session for user {}, message is stored for later retrieval",
						websocketRequest.getUserId());
			}
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send getProfile request to profile service", e);
		}
	}

	private ServiceConnectionInformation getUserGatewayConnInfo(GlobalClientSessionInfo session) {
		String url = normalizeWebsocketURL(session.getGatewayURL());
		return new ServiceConnectionInformation(GatewayMessageTypes.SERVICE_NAME, url, GatewayMessageTypes.SERVICE_NAME);
	}
	
	protected String normalizeWebsocketURL(String originalUrl) {
		String uriAsString;
		if (originalUrl.contains(":/")) {
			uriAsString = originalUrl; //assume that scheme is given
		} else {
			uriAsString = "wss://" + originalUrl; //add default scheme wss
		}
		return uriAsString;
	}
}
