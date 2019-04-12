package de.ascendro.f4m.service.ping;

import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.di.handler.DefaultJsonMessageHandlerProviderImpl;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.handler.JsonMessageHandler;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.session.SessionWrapper;

public class TestMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestMessageHandlerProvider.class);
	
	//FIXME: actually this is pretty similar approach to ThrowingFunction in de.ascendro.f4m.service.integration.rule.MockServiceRule.getMessageHandler()
	//Create ThrowingBiFunction or convert ThrowingFunction -> ThrowingBiFunction? Or create CallInfo containing JsonMessage, SessionWrapper and ClientInfo?
	private BiFunction<RequestContext, SessionWrapper, JsonMessageContent> handler;
	private String serviceInterfaceName;
	
	public TestMessageHandlerProvider(String serviceInterfaceName) {
		this.serviceInterfaceName = serviceInterfaceName;
	}

	@Override
	protected JsonMessageHandler createServiceMessageHandler() {
		return new DefaultJsonMessageHandler() {
			@Override
			public JsonMessageContent onUserMessage(RequestContext context) throws F4MException {
				if (getHandler() != null) {
					return getHandler().apply(context, getSessionWrapper());
				} else {
					LOGGER.error("Incorrect test - handler instance not set for {}", serviceInterfaceName);
					throw new F4MFatalErrorException("Incorrect test - handler instance not set for " + serviceInterfaceName);
				}
			}
		};
	}

	public BiFunction<RequestContext, SessionWrapper, JsonMessageContent> getHandler() {
		return handler;
	}

	public void setHandler(
			BiFunction<RequestContext, SessionWrapper, JsonMessageContent> handler) {
		this.handler = handler;
	}
}