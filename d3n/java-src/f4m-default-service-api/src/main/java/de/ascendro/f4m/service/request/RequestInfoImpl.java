package de.ascendro.f4m.service.request;

import de.ascendro.f4m.service.cache.CachedObject;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.MessageSource;
//import de.ascendro.f4m.service.session.SessionWrapper;

public class RequestInfoImpl extends CachedObject implements RequestInfo {
//	private SessionWrapper sourceSession;
	private MessageSource sourceMessageSource;
	private JsonMessage<? extends JsonMessageContent> sourceMessage;

	public RequestInfoImpl() {
		//default constructor
	}

	public RequestInfoImpl(long timeToLive) {
		super(timeToLive);
	}

	public RequestInfoImpl(JsonMessage<?> sourceMessage) {
		this.sourceMessage = sourceMessage;
	}

	// TODO: DELETE
	public RequestInfoImpl(JsonMessage<?> sourceMessage, Object sourceSession) {
		this.sourceMessage = sourceMessage;
//		this.sourceSession = (SessionWrapper) sourceSession;
	}

	public RequestInfoImpl(JsonMessage<?> sourceMessage, MessageSource sourceMessageSource) {
		this.sourceMessage = sourceMessage;
		this.sourceMessageSource = sourceMessageSource;
	}

	@Override
	public Long getTimeToLive() {
		return timeToLive;
	}

//	@Override
//	public void setSourceSession(SessionWrapper sourceSession) {
//		this.sourceSession = sourceSession;
//	}

	@Override
	public Object getSourceSession() {
		return null;
	}

	@Override
	public void setSourceMessageSource(MessageSource sourceMessageSource) {
		this.sourceMessageSource = sourceMessageSource;
	}

	@Override
	public MessageSource getSourceMessageSource() {
		return sourceMessageSource;
	}

	@Override
	public JsonMessage<? extends JsonMessageContent> getSourceMessage() {
		return sourceMessage;
	}

	@Override
	public void setSourceMessage(JsonMessage<? extends JsonMessageContent> sourceMessage) {
		this.sourceMessage = sourceMessage;
	}

	@Override
	public String toString() {
		return "RequestInfoImpl{" +
//				"sourceSession=" + sourceSession +
				", sourceMessageSource=" + sourceMessageSource +
				", sourceMessage=" + sourceMessage +
				", timeToLive=" + timeToLive +
				", lastAccessTime=" + lastAccessTime +
				"} " + super.toString();
	}
}
