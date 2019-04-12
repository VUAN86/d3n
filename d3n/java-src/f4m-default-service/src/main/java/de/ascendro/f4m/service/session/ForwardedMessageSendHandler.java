package de.ascendro.f4m.service.session;

import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.request.RequestInfo;

public class ForwardedMessageSendHandler extends MessageSendHandler {

	private final RequestInfo requestInfo;

	public ForwardedMessageSendHandler(JsonMessageUtil jsonMessageUtil, SessionWrapper sessionWrapper,
			JsonMessage<? extends JsonMessageContent> message, RequestInfo requestInfo) {
		super(jsonMessageUtil, sessionWrapper, message);

		this.requestInfo = requestInfo;
	}

	@Override
	protected SessionWrapper getOriginalSession() {
		return null;
	}

	@Override
	protected JsonMessage<? extends JsonMessageContent> getOriginalMessage() {
		return requestInfo.getSourceMessage();
	}

}
