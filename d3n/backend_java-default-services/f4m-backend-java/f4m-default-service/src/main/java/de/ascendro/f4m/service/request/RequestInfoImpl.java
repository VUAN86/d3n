package de.ascendro.f4m.service.request;

import de.ascendro.f4m.service.cache.CachedObject;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.session.SessionWrapper;

public class RequestInfoImpl extends CachedObject implements RequestInfo {
	private SessionWrapper sourceSession;
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
	
	public RequestInfoImpl(JsonMessage<?> sourceMessage, SessionWrapper sourceSession) {
		this.sourceMessage = sourceMessage;
		this.sourceSession = sourceSession;
	}

	@Override
	public Long getTimeToLive() {
		return timeToLive;
	}

	@Override
	public void setSourceSession(SessionWrapper sourceSession) {
		this.sourceSession = sourceSession;
	}

	@Override
	public SessionWrapper getSourceSession() {
		return sourceSession;
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
				"sourceSession=" + sourceSession +
				", sourceMessage=" + sourceMessage +
				", timeToLive=" + timeToLive +
				", lastAccessTime=" + lastAccessTime +
				"} ";
	}
}
