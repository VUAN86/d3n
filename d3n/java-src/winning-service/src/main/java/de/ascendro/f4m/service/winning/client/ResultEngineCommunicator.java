package de.ascendro.f4m.service.winning.client;

import javax.inject.Inject;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.service.communicator.RabbitClientSender;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.request.RequestInfoImpl;
import de.ascendro.f4m.service.result.engine.ResultEngineMessageTypes;
import de.ascendro.f4m.service.result.engine.model.get.GetResultsRequest;
import de.ascendro.f4m.service.result.engine.model.store.StoreUserWinningComponentRequest;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;
import de.ascendro.f4m.service.winning.model.UserWinningComponent;
import de.ascendro.f4m.service.winning.model.WinningComponentType;
import de.ascendro.f4m.service.winning.model.component.UserWinningComponentAssignRequest;

public class ResultEngineCommunicator {

	private JsonMessageUtil jsonUtil;
//	private JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;
//	private ServiceRegistryClient serviceRegistryClient;

	@Inject
	public ResultEngineCommunicator(JsonMessageUtil jsonUtil
//			JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool,
//			ServiceRegistryClient serviceRegistryClient
	) {
		this.jsonUtil = jsonUtil;
//		this.jsonWebSocketClientSessionPool = jsonWebSocketClientSessionPool;
//		this.serviceRegistryClient = serviceRegistryClient;
	}

	public void requestGameInstanceResults(JsonMessage<? extends JsonMessageContent> sourceMessage, String gameInstanceId) {
		final RequestInfo requestInfo = new RequestInfoImpl(sourceMessage, sourceMessage.getMessageSource());

		final GetResultsRequest request = new GetResultsRequest(gameInstanceId);
		final JsonMessage<GetResultsRequest> message = jsonUtil.createNewMessage(ResultEngineMessageTypes.GET_RESULTS_INTERNAL, request);

		try {
//			final ServiceConnectionInformation resultEngineConnInfo = serviceRegistryClient
//					.getServiceConnectionInformation(ResultEngineMessageTypes.SERVICE_NAME);
			RabbitClientSender.sendAsyncMessageWithClientInfo(ResultEngineMessageTypes.SERVICE_NAME, message, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send getResultsRequest to result engine service", e);
		}
	}

	public void storeUserWinningComponentInResults(JsonMessage<UserWinningComponentAssignRequest> sourceMessage, 
			UserWinningComponent userWinningComponent) {
		final UserWinningComponentRequestInfo requestInfo = 
				new UserWinningComponentRequestInfo(sourceMessage, sourceMessage.getMessageSource(), userWinningComponent, null);

		StoreUserWinningComponentRequest request = new StoreUserWinningComponentRequest(
				sourceMessage.getContent().getGameInstanceId(),
				userWinningComponent == null ? null : userWinningComponent.getUserWinningComponentId(),
				sourceMessage.getContent().getType() == WinningComponentType.PAID);
		final JsonMessage<StoreUserWinningComponentRequest> message = jsonUtil.createNewMessage(
				ResultEngineMessageTypes.STORE_USER_WINNING_COMPONENT, request);

		try {
//			final ServiceConnectionInformation resultEngineConnInfo = serviceRegistryClient
//					.getServiceConnectionInformation(ResultEngineMessageTypes.SERVICE_NAME);
			RabbitClientSender.sendAsyncMessageWithClientInfo(ResultEngineMessageTypes.SERVICE_NAME, message, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send storeUserWinningComponent to result engine service", e);
		}
	}

}
