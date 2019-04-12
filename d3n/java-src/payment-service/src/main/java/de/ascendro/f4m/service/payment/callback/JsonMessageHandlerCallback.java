package de.ascendro.f4m.service.payment.callback;

import de.ascendro.f4m.service.json.handler.JsonMessageMQHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.handler.JsonMessageHandler;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class JsonMessageHandlerCallback<T extends JsonMessageContent> implements Callback<T> {
	private static final Logger LOGGER = LoggerFactory.getLogger(JsonMessageHandlerCallback.class);
	
	private JsonMessageMQHandler handler;
	private RequestContext ctx;

	public JsonMessageHandlerCallback(JsonMessageMQHandler handler, RequestContext ctx) {
		this.handler = handler;
		this.ctx = ctx;
	}
	
	@Override
	public void completed(T response) {
		LOGGER.debug("Callback success with data {} in context {}", response, ctx);
		LOGGER.error("Its \"completed\" newer happend???");
		System.out.println("Its \"completed\" newer happend???");
//		handler.onResponse(ctx, response, messageSource);
	}

	@Override
	public void failed(Throwable throwable) {

		System.out.println("Its \"failed\" newer happend???");
		LOGGER.debug("Callback failed in context {}", ctx, throwable);
		LOGGER.error("Its \"failed\" newer happend???");
		String originalMessageEncoded = null; //currently originalMessageEncoded is used only for logging, so not mandatory
//		handler.onFailure(originalMessageEncoded, ctx, messageSource, throwable);
	}

}
