package de.ascendro.f4m.service.event.model.subscribe;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class SubscribeRequest implements JsonMessageContent {
	private String topic;
	private String consumerName;
	
	/** 
	 * Determines if the request is to subscribe to virtual topic or normal. 
	 * For more information see http://activemq.apache.org/virtual-destinations.html. 
	 */
	private boolean virtual;

	public SubscribeRequest() {
	}

	public SubscribeRequest(String topic) {
		this(null, topic, false);
	}

	public SubscribeRequest(String consumerName, String topic, boolean virtual) {
		setConsumerName(consumerName);
		setTopic(topic);
		setVirtual(virtual);
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

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SubscribeRequest [topic=");
		builder.append(topic);
		builder.append(", consumerName=");
		builder.append(consumerName);
		builder.append(", virtual=");
		builder.append(virtual);
		builder.append("]");
		return builder.toString();
	}

}
