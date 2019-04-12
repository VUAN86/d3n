package de.ascendro.f4m.service.event.model.resubscribe;

public class ResubscribeSubscriptionsResponse {
	private long subscription;
	private long resubscription;
	private String topic;

	public ResubscribeSubscriptionsResponse(ResubscribeSubscriptionsRequest resubscribeSubscriptionsRequest) {
		this.subscription = resubscribeSubscriptionsRequest.getSubscription();
		this.topic = resubscribeSubscriptionsRequest.getTopic();
	}

	public ResubscribeSubscriptionsResponse(long subscription, long resubscription) {
		this.subscription = subscription;
		this.resubscription = resubscription;
	}

	public long getSubscription() {
		return subscription;
	}

	public void setSubscription(long subscription) {
		this.subscription = subscription;
	}

	public long getResubscription() {
		return resubscription;
	}

	public void setResubscription(long resubscription) {
		this.resubscription = resubscription;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

}
