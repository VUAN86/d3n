package de.ascendro.f4m.service.di.client;

import javax.inject.Inject;
import javax.inject.Provider;

import de.ascendro.f4m.service.ServiceEndpoint;
import de.ascendro.f4m.service.annotation.ClientMessageHandler;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.di.handler.MessageHandlerProvider;
import de.ascendro.f4m.service.json.JsonServiceEndpoint;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.session.SessionWrapperFactory;
import de.ascendro.f4m.service.session.pool.SessionPool;
import de.ascendro.f4m.service.util.EventServiceClient;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;

public class JsonServiceClientEndpointProvider implements
		Provider<ServiceEndpoint<JsonMessage<? extends JsonMessageContent>, String, JsonMessageContent>> {

	@Inject
	private SessionPool sessionStore;

	@Inject
	private LoggingUtil loggedMessageUtil;

	@Inject
	private SessionWrapperFactory sessionWrapperFactory;

	@Inject
	private Config config;

	@Inject
	private ServiceRegistryClient serviceRegistryClient;
	
	@Inject
	private EventServiceClient eventServiceClient;
	
	private final MessageHandlerProvider<String> jsonMessageHandlerProvider;

	@Inject
	public JsonServiceClientEndpointProvider(@ClientMessageHandler MessageHandlerProvider<String> jsonMessageHandlerProvider) {
		this.jsonMessageHandlerProvider = jsonMessageHandlerProvider;
	}

	@Override
	public ServiceEndpoint<JsonMessage<? extends JsonMessageContent>, String, JsonMessageContent> get() {
		return new JsonServiceEndpoint(jsonMessageHandlerProvider, sessionStore, loggedMessageUtil,
				sessionWrapperFactory, config, serviceRegistryClient, eventServiceClient);
	}

}
