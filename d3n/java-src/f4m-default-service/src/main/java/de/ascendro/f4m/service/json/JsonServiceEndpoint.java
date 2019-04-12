package de.ascendro.f4m.service.json;

import de.ascendro.f4m.service.ServiceEndpoint;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.di.handler.MessageHandlerProvider;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.session.SessionWrapperFactory;
import de.ascendro.f4m.service.session.pool.SessionPool;
import de.ascendro.f4m.service.util.EventServiceClient;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;

public class JsonServiceEndpoint
		extends ServiceEndpoint<JsonMessage<? extends JsonMessageContent>, String, JsonMessageContent> {

	public JsonServiceEndpoint(MessageHandlerProvider<String> jsonMessageHandlerProvider, SessionPool sessionPool,
			LoggingUtil loggedMessageUtil, SessionWrapperFactory sessionWrapperFactory, Config config,
			ServiceRegistryClient serviceRegistryClient, EventServiceClient eventServiceClient) {
		super(jsonMessageHandlerProvider, sessionPool, loggedMessageUtil, sessionWrapperFactory, config,
				serviceRegistryClient, eventServiceClient);
	}
}
