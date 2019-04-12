package de.ascendro.f4m.service.payment.callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.handler.JsonMessageHandler;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class JsonMessageHandlerCallback<T extends JsonMessageContent> implements Callback<T> {
	private static final Logger LOGGER = LoggerFactory.getLogger(JsonMessageHandlerCallback.class);
	
	private JsonMessageHandler handler;
	private RequestContext ctx;

	public JsonMessageHandlerCallback(JsonMessageHandler handler, RequestContext ctx) {
		this.handler = handler;
		this.ctx = ctx;
	}
	
	@Override
	public void completed(T response) {
		LOGGER.debug("Callback success with data {} in context {}", response, ctx);
		handler.onResponse(ctx, response);
	}

	@Override
	public void failed(Throwable throwable) {
		LOGGER.debug("Callback failed in context {}", ctx, throwable);
		String originalMessageEncoded = null; //currently originalMessageEncoded is used only for logging, so not mandatory
		handler.onFailure(originalMessageEncoded, ctx, throwable);
	}

}
