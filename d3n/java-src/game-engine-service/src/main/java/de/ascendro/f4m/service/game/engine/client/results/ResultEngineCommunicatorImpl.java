package de.ascendro.f4m.service.game.engine.client.results;

import javax.inject.Inject;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.game.engine.GameEngineMessageTypes;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.engine.model.end.EndGameRequest;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.result.engine.ResultEngineMessageTypes;
import de.ascendro.f4m.service.result.engine.model.calculate.CalculateMultiplayerResultsRequest;
import de.ascendro.f4m.service.result.engine.model.calculate.CalculateResultsRequest;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;

public class ResultEngineCommunicatorImpl implements ResultEngineCommunicator {
	private JsonMessageUtil jsonUtil;
	private JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;
	private ServiceRegistryClient serviceRegistryClient;
	

	@Inject
	public ResultEngineCommunicatorImpl(JsonMessageUtil jsonUtil,
			JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool, ServiceRegistryClient serviceRegistryClient) {
		this.jsonUtil = jsonUtil;
		this.jsonWebSocketClientSessionPool = jsonWebSocketClientSessionPool;
		this.serviceRegistryClient = serviceRegistryClient;
	}

	@Override
	public void requestCalculateResults(ClientInfo clientInfo, GameInstance gameInstance, SessionWrapper sourceSession) {
		final CalculateResultsRequest calculateResults = new CalculateResultsRequest(gameInstance.getJsonObject(),
				false);
		final JsonMessage<CalculateResultsRequest> calculateResultsMessage = jsonUtil.createNewMessage(
				ResultEngineMessageTypes.CALCULATE_RESULTS, calculateResults);

		final JsonMessage<EndGameRequest> sourceMessageAsEndGame = jsonUtil.createNewMessage(GameEngineMessageTypes.END_GAME);
		sourceMessageAsEndGame.setClientInfo(clientInfo);
		
		final Game game = gameInstance.getGame();
		final RequestInfo requestInfo = new ResultEngineRequestInfo(gameInstance.getMgiId(), game.getType(),
				gameInstance.getId(), game.getGameId(), game.getTitle());

		requestInfo.setSourceMessage(sourceMessageAsEndGame);
		requestInfo.setSourceSession(sourceSession);
		try {
			final ServiceConnectionInformation resultEngineConnInfo = serviceRegistryClient.getServiceConnectionInformation(ResultEngineMessageTypes.SERVICE_NAME);
			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(resultEngineConnInfo, calculateResultsMessage, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send result calculate request to result engine service", e);
		}
	}
	
	@Override
	public void requestCalculateMultiplayerResults(ClientInfo clientInfo, String mgiId,
			GameType gameType, SessionWrapper sourceSession) {
		final CalculateMultiplayerResultsRequest calculateMutliplayerResultsRequest = new CalculateMultiplayerResultsRequest();
		calculateMutliplayerResultsRequest.setMultiplayerGameInstanceId(mgiId);
		
		final JsonMessage<JsonMessageContent> sourceMessageAsEndGame = jsonUtil.createNewMessage(GameEngineMessageTypes.END_GAME);
		sourceMessageAsEndGame.setClientInfo(clientInfo);
		
		final JsonMessage<CalculateMultiplayerResultsRequest> calculateResultsMessage = jsonUtil.createNewMessage(
				ResultEngineMessageTypes.CALCULATE_MULTIPLAYER_RESULTS, calculateMutliplayerResultsRequest);
				
		final RequestInfo requestInfo = new ResultEngineRequestInfo(mgiId, gameType);
		requestInfo.setSourceMessage(sourceMessageAsEndGame);
		requestInfo.setSourceSession(sourceSession);

		try {
			final ServiceConnectionInformation resultEngineConnInfo = serviceRegistryClient.getServiceConnectionInformation(ResultEngineMessageTypes.SERVICE_NAME);
			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(resultEngineConnInfo, calculateResultsMessage, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send calculate game results request to result engine service", e);
		}
	}

}
