package de.ascendro.f4m.service.event.model.subscribe;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class SubscribeResponse implements JsonMessageContent {

	private long subscription;
	private String topic;
	private String consumerName;
	private boolean virtual;

	public SubscribeResponse() {
	}

	public SubscribeResponse(long subscriptionId, boolean virtual, String consumerName, String topic) {
		setSubscription(subscriptionId);
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
