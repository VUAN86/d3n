package de.ascendro.f4m.service.di.handler;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.handler.F4MMessageHandler;
import de.ascendro.f4m.service.handler.ParallelServiceJsonMessageHandler;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.session.SessionWrapperFactory;

public class JsonParallelServiceMessageHandlerProvider implements MessageHandlerProvider<String> {

	private final JsonMessageHandlerProvider jsonMessageHandlerProvider;
	private final Config config;
	private final SessionWrapperFactory sessionWrapperFactory;
	private final LoggingUtil loggingUtil;

	public JsonParallelServiceMessageHandlerProvider(JsonMessageHandlerProvider jsonMessageHandlerProvider,
			Config config, SessionWrapperFactory sessionWrapperFactory, LoggingUtil loggingUtil) {
		this.jsonMessageHandlerProvider = jsonMessageHandlerProvider;
		this.config = config;
		this.sessionWrapperFactory = sessionWrapperFactory;
		this.loggingUtil = loggingUtil;
	}

	@Override
	public F4MMessageHandler<String> get() {
		return new ParallelServiceJsonMessageHandler(jsonMessageHandlerProvider, config, loggingUtil, sessionWrapperFactory);
	}

}
