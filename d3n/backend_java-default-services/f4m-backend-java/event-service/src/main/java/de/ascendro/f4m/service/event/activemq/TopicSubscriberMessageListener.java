package de.ascendro.f4m.service.event.activemq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.event.model.NotifySubscriberMessageContent;
import de.ascendro.f4m.service.event.model.publish.PublishMessageContent;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.session.SessionWrapper;

public class TopicSubscriberMessageListener extends EventMessageListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(TopicSubscriberMessageListener.class);

	private final SessionWrapper sessionWrapper;
	private final long subscription;
	private final String topic;

	public TopicSubscriberMessageListener(long subscription, String topic, SessionWrapper sessionWrapper,
			Config config, JsonMessageUtil jsonUtil, LoggingUtil loggingUtil) {
		super(jsonUtil, config, loggingUtil);
		this.subscription = subscription;
		this.topic = topic;

		this.sessionWrapper = sessionWrapper;
	}

	@Override
	protected void onMessage(PublishMessageContent publishContent) {
		if (sessionWrapper.getSession().isOpen()) {
			notifySubscribers(publishContent);
		} else {
			LOGGER.error("Topic[{}] subscriber[{}] consists of closed Session", topic, subscription);
		}
	}

	protected void notifySubscribers(PublishMessageContent publishContent) {
		final JsonMessage<NotifySubscriberMessageContent> notifyMessage = jsonUtil
				.<NotifySubscriberMessageContent> createNewMessage(EventMessageTypes.NOTIFY_SUBSCRIBER);

		final NotifySubscriberMessageContent notifySubscriberContent = new NotifySubscriberMessageContent(subscription,
				topic);

		notifySubscriberContent.setNotificationContent(publishContent.getNotificationContent());

		notifyMessage.setContent(notifySubscriberContent);
		sessionWrapper.sendAsynMessage(notifyMessage);
	}
}
