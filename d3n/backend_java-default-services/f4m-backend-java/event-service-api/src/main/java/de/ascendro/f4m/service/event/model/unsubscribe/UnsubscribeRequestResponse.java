package de.ascendro.f4m.service.event.model.unsubscribe;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class UnsubscribeRequestResponse implements JsonMessageContent {
	private long subscription;

	public UnsubscribeRequestResponse() {
	}

	public UnsubscribeRequestResponse(long subscription) {
		this.subscription = subscription;
	}

	public long getSubscription() {
		return subscription;
	}

	public void setSubscription(long subscription) {
		this.subscription = subscription;
	}

}
