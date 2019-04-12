package de.ascendro.f4m.service.game.engine.client.winning;

import javax.inject.Inject;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.game.engine.GameEngineMessageTypes;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;
import de.ascendro.f4m.service.winning.WinningMessageTypes;
import de.ascendro.f4m.service.winning.model.WinningComponentType;
import de.ascendro.f4m.service.winning.model.component.UserWinningComponentAssignRequest;

public class WinningCommunicator {
	private JsonMessageUtil jsonUtil;
	private JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;
	private ServiceRegistryClient serviceRegistryClient;

	@Inject
	public WinningCommunicator(JsonMessageUtil jsonUtil,
			JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool, ServiceRegistryClient serviceRegistryClient) {
		this.jsonUtil = jsonUtil;
		this.jsonWebSocketClientSessionPool = jsonWebSocketClientSessionPool;
		this.serviceRegistryClient = serviceRegistryClient;
	}

	/**
	 * Request to assign winning component
	 * @param clientInfo - player client info
	 * @param gameInstance - game instance to calculate results for
	 * @param sourceSession
	 */
//	Not used
//	public void requestAssignWinningComponent(ClientInfo clientInfo, String gameInstanceId, String mgiId, GameType gameType,
//			String freeWinningComponentId, String paidWinningComponentId, WinningComponentType type, SessionWrapper sourceSession) {
//		final UserWinningComponentAssignRequest assignRequest = new UserWinningComponentAssignRequest(gameInstanceId, type);
//		final JsonMessage<UserWinningComponentAssignRequest> userWinningComponentAssignRequest = jsonUtil.createNewMessage(
//				WinningMessageTypes.USER_WINNING_COMPONENT_ASSIGN, assignRequest);
//
//		final JsonMessage<JsonMessageContent> sourceMessageAsEndGame = jsonUtil.createNewMessage(GameEngineMessageTypes.END_GAME);
//		sourceMessageAsEndGame.setClientInfo(clientInfo);
//
//		final RequestInfo requestInfo = new WinningRequestInfo(gameInstanceId, mgiId, gameType, freeWinningComponentId,
//				paidWinningComponentId);
//		requestInfo.setSourceMessage(sourceMessageAsEndGame);
//		requestInfo.setSourceSession(sourceSession);
//		try {
//			final ServiceConnectionInformation resultEngineConnInfo = serviceRegistryClient.getServiceConnectionInformation(WinningMessageTypes.SERVICE_NAME);
//			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(resultEngineConnInfo, userWinningComponentAssignRequest, requestInfo);
//		} catch (F4MValidationFailedException | F4MIOException e) {
//			throw new F4MFatalErrorException("Unable to send assign winning component request to winning service", e);
//		}
//	}
	
}
