package de.ascendro.f4m.service.game.selection.client.communicator;

import javax.inject.Inject;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.game.selection.model.dashboard.GetDashboardResponse;
import de.ascendro.f4m.service.game.selection.request.ResultEngineRequestInfo;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.result.engine.ResultEngineMessageTypes;
import de.ascendro.f4m.service.result.engine.model.get.GetMultiplayerResultsRequest;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;

public class ResultEngineCommunicator {

	private final ServiceRegistryClient serviceRegistryClient;
	private final JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;
	private final JsonMessageUtil jsonMessageUtil;

	@Inject
	public ResultEngineCommunicator(ServiceRegistryClient serviceRegistryClient,
			JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool, JsonMessageUtil jsonMessageUtil) {
		this.serviceRegistryClient = serviceRegistryClient;
		this.jsonWebSocketClientSessionPool = jsonWebSocketClientSessionPool;
		this.jsonMessageUtil = jsonMessageUtil;
	}

	public void requestGetMultiplayerResults(JsonMessage<? extends JsonMessageContent> sourceMessage,
			SessionWrapper sourceSession, GetDashboardResponse dashboard, String gameInstanceId) {
		JsonMessage<GetMultiplayerResultsRequest> message = jsonMessageUtil.createNewMessage(
				ResultEngineMessageTypes.GET_MULTIPLAYER_RESULTS, new GetMultiplayerResultsRequest(gameInstanceId));
		RequestInfo requestInfo = new ResultEngineRequestInfo(sourceMessage, sourceSession, dashboard);
		try {
			ServiceConnectionInformation resultEngineConnInfo = serviceRegistryClient
					.getServiceConnectionInformation(ResultEngineMessageTypes.SERVICE_NAME);
			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(resultEngineConnInfo, message, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send registerRequest to game engine service", e);
		}
	}

}
