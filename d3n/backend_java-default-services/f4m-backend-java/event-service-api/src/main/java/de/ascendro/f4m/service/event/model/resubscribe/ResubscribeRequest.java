package de.ascendro.f4m.service.event.model.resubscribe;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class ResubscribeRequest implements JsonMessageContent {

	private ResubscribeSubscriptionsRequest[] subscriptions;

	public ResubscribeRequest(ResubscribeSubscriptionsRequest... subscriptions) {
		setSubscriptions(subscriptions);
	}

	public void setSubscriptions(ResubscribeSubscriptionsRequest... subscriptions) {
		this.subscriptions = subscriptions;
	}

	public ResubscribeSubscriptionsRequest[] getSubscriptions() {
		return subscriptions;
	}

}
