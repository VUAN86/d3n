package de.ascendro.f4m.service.event.activemq;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.jms.JMSException;
import javax.jms.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.event.model.publish.PublishMessageContent;

public class CachedMessageListener extends EventMessageListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(CachedMessageListener.class);

	private final List<Message> receivedMessage = new ArrayList<Message>();
	private final CountDownLatch countDownLatch;

	public CachedMessageListener(CountDownLatch countDownLatch) {
		super(null, null, null);
		this.countDownLatch = countDownLatch;
	}

	public CachedMessageListener() {
		super(null, null, null);
		this.countDownLatch = null;
	}

	@Override
	protected void onMessage(PublishMessageContent publishMessageContent) {
		LOGGER.error("Attempted to execute event publish message content handler");
	}

	@Override
	public void onMessage(Message message) {
		if (countDownLatch != null)
			countDownLatch.countDown();

		LOGGER.info("Received message: {}", message);

		receivedMessage.add(message);
		try {
			message.acknowledge();
		} catch (JMSException e) {
			LOGGER.error("Failed to acknowledge message", e);
		}
	}

	public List<Message> getReceivedMessage() {
		return receivedMessage;
	}
}
