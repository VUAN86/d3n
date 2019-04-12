package de.ascendro.f4m.service.event.model.resubscribe;

public class ResubscribeSubscriptionsRequest {
	private long subscription;
	private String topic;
	private boolean virtual;
	private String consumerName;

	public ResubscribeSubscriptionsRequest(long subscription) {
		this(subscription, null, null, false);
	}

	public ResubscribeSubscriptionsRequest(long subscription, String topic) {
		this(subscription, null, topic, false);
	}

	public ResubscribeSubscriptionsRequest(long subscription, String consumerName, String topic, boolean virtual) {
		setSubscription(subscription);
		setConsumerName(consumerName);
		setTopic(topic);
		setVirtual(virtual);
	}

	public long getSubscription() {
		return subscription;
	}

	public void setSubscription(long subscription) {
		this.subscription = subscription;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public boolean isVirtual() {
		return virtual;
	}

	public void setVirtual(boolean virtual) {
		this.virtual = virtual;
	}

	public String getConsumerName() {
		return consumerName;
	}

	public void setConsumerName(String consumerName) {
		this.consumerName = consumerName;
	}
	
}
