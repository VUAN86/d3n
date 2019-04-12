package de.ascendro.f4m.service.integration.collector;

import de.ascendro.f4m.service.di.handler.DefaultJsonMessageHandlerProviderImpl;
import de.ascendro.f4m.service.json.handler.JsonMessageHandler;

public class ReceivedMessageCollectorProvider extends DefaultJsonMessageHandlerProviderImpl {
	private ReceivedMessageCollector receivedMessageCollector;

	protected ReceivedMessageCollector createJsonMessageHandler() {
		return new ReceivedMessageCollector();
	}

	@Override
	protected JsonMessageHandler createServiceMessageHandler() {
		if (receivedMessageCollector == null) {
			receivedMessageCollector = createJsonMessageHandler();
		}
		return receivedMessageCollector;
	}
}
