package de.ascendro.f4m.service.integration.collector;

import de.ascendro.f4m.service.di.handler.DefaultJsonMessageHandlerProviderImpl;
import de.ascendro.f4m.service.json.handler.JsonMessageHandler;

public class ReceivedMessageSessionCollectorProvider extends DefaultJsonMessageHandlerProviderImpl {
	private final ReceivedMessageSessionCollector receivedMessageSessionCollector;

	public ReceivedMessageSessionCollectorProvider() {		
		receivedMessageSessionCollector = new ReceivedMessageSessionCollector();
	}

	@Override
	protected JsonMessageHandler createServiceMessageHandler() {
		return receivedMessageSessionCollector;
	}
}
