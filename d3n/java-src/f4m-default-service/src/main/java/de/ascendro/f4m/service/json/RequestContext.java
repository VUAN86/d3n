package de.ascendro.f4m.service.json;

import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.request.RequestInfo;

public class RequestContext {
	private JsonMessage<? extends JsonMessageContent> message;
	private RequestInfo originalRequestInfo;
	private boolean forwardErrorMessagesToOrigin;
	
	public RequestContext() {
		forwardErrorMessagesToOrigin = true;
	}

	public RequestContext(JsonMessage<? extends JsonMessageContent> message) {
		this();
		this.message = message;
	}

	@SuppressWarnings("unchecked")
	public <T extends JsonMessageContent> JsonMessage<T> getMessage() {
		if (message != null) {
			return (JsonMessage<T>) message;
		} else {
			return null;
		}
	}

	public void setMessage(JsonMessage<? extends JsonMessageContent> message) {
		this.message = message;
	}

	public ClientInfo getClientInfo() {
		return message.getClientInfo();
	}

	@SuppressWarnings("unchecked")
	public <T extends RequestInfo> T getOriginalRequestInfo() {
		if (originalRequestInfo != null) {
			return (T) originalRequestInfo;
		} else {
			return null;
		}
	}

	public void setOriginalRequestInfo(RequestInfo originalRequestInfo) {
		this.originalRequestInfo = originalRequestInfo;
	}

	public boolean isForwardErrorMessagesToOrigin() {
		return forwardErrorMessagesToOrigin;
	}

	public void setForwardErrorMessagesToOrigin(boolean forwardErrorMessagesToOrigin) {
		this.forwardErrorMessagesToOrigin = forwardErrorMessagesToOrigin;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RequestContext [message=");
		builder.append(message);
		builder.append(", originalRequestInfo=");
		builder.append(originalRequestInfo);
		builder.append(", forwardErrorMessagesToOrigin=");
		builder.append(forwardErrorMessagesToOrigin);
		builder.append("]");
		return builder.toString();
	}
}