package de.ascendro.f4m.service.di.server;

import javax.inject.Inject;

import de.ascendro.f4m.service.ServiceEndpoint;
import de.ascendro.f4m.service.annotation.ServerMessageHandler;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.di.handler.MessageHandlerProvider;
import de.ascendro.f4m.service.json.JsonServiceEndpoint;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.session.SessionWrapperFactory;
import de.ascendro.f4m.service.session.pool.SessionPool;
import de.ascendro.f4m.service.util.EventServiceClient;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;

public class JsonServiceServerEndpointProvider extends
		ServiceServerEndpointProvider<JsonMessage<? extends JsonMessageContent>, String, JsonMessageContent> {

	@Inject
	protected SessionPool sessionPool;

	@Inject
	protected LoggingUtil loggedMessageUtil;

	@Inject
	protected SessionWrapperFactory sessionWrapperFactory;

	@Inject
	protected ServiceRegistryClient serviceRegistryClient;
	
	@Inject
	protected EventServiceClient eventServiceClient;

	protected final Config config;

	protected final MessageHandlerProvider<String> messageHandlerProvider;

	@Inject
	public JsonServiceServerEndpointProvider(
			@ServerMessageHandler MessageHandlerProvider<String> messageHandlerProvider, Config config) {
		super(config.getProperty(F4MConfigImpl.JETTY_CONTEXT_PATH), JsonServiceEndpoint.class);
		this.config = config;
		this.messageHandlerProvider = messageHandlerProvider;
	}

	@Override
	protected ServiceEndpoint<JsonMessage<? extends JsonMessageContent>, String, JsonMessageContent> getServiceEndpoint() {
		return new JsonServiceEndpoint(messageHandlerProvider, sessionPool, loggedMessageUtil,
				sessionWrapperFactory, config, serviceRegistryClient, eventServiceClient);
	}

}
