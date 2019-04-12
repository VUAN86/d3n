package de.ascendro.f4m.service.event.model.resubscribe;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class ResubscribeResponse implements JsonMessageContent {

	private ResubscribeSubscriptionsResponse[] subscriptions;

	public ResubscribeResponse(ResubscribeSubscriptionsResponse... subscriptions) {
		this.subscriptions = subscriptions;
	}

	public ResubscribeSubscriptionsResponse[] getSubscriptions() {
		return subscriptions;
	}

	public void setSubscriptions(ResubscribeSubscriptionsResponse... subscriptions) {
		this.subscriptions = subscriptions;
	}

}
