package de.ascendro.f4m.service.event.activemq;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.event.config.EventConfig;
import de.ascendro.f4m.service.event.model.publish.PublishMessageContent;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.logging.LoggingUtil;

/**
 * Common ActiveMQ event message listener as JSON message with PublishMessageContent
 */
public abstract class EventMessageListener implements MessageListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(EventMessageListener.class);

	protected final JsonMessageUtil jsonUtil;
	protected final Config config;
	private final LoggingUtil loggingUtil;


	public EventMessageListener(JsonMessageUtil jsonUtil, Config config, LoggingUtil loggingUtil) {
		this.jsonUtil = jsonUtil;
		this.config = config;
		this.loggingUtil = loggingUtil;
	}

	protected abstract void onMessage(PublishMessageContent publishMessageContent);

	@Override
	public void onMessage(Message message) {
		final TextMessage textMessage = (TextMessage) message;

		try {
			loggingUtil.saveBasicInformationInThreadContext();
			final String text = textMessage.getText();
			final JsonMessage<? extends JsonMessageContent> publishMessage = jsonUtil.fromJson(text);
			jsonUtil.validate(text);
			if (publishMessage != null && publishMessage.getContent() != null
					&& publishMessage.getContent() instanceof PublishMessageContent) {
				final PublishMessageContent publishMessageContent = (PublishMessageContent) publishMessage.getContent();

				final String topic = toExternalTopicPattern(publishMessageContent.getTopic());
				publishMessageContent.setTopic(topic);

				onMessage(publishMessageContent);
			} else {
				LOGGER.error("Received incorrect type of publish message: " + publishMessage);
			}
		} catch (Exception e) {
			LOGGER.error("Failed to process received event as PublishMessage", e);
		}
	}

	protected String toExternalTopicPattern(String originalTopic) {
		if (originalTopic != null) {
			originalTopic = originalTopic.replaceAll(EmbeddedActiveMQ.DEFAULT_WILDCARD_SEPARATOR,
					config.getProperty(EventConfig.WILDCARD_SEPARATOR));
		}
		return originalTopic;
	}

}
