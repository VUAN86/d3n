package de.ascendro.f4m.client.json;

import de.ascendro.f4m.client.WebSocketClientSessionPool;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.request.RequestInfo;

public interface JsonWebSocketClientSessionPool extends WebSocketClientSessionPool {
	void sendAsyncMessage(ServiceConnectionInformation serviceConnectionInformation,
			JsonMessage<? extends JsonMessageContent> message);

	void sendAsyncMessageWithClientInfo(ServiceConnectionInformation serviceConnectionInformation,
			JsonMessage<? extends JsonMessageContent> message, RequestInfo requestInfo);

	void sendAsyncMessageNoClientInfo(ServiceConnectionInformation serviceConnectionInformation,
			JsonMessage<? extends JsonMessageContent> message, RequestInfo requestInfo);
}
