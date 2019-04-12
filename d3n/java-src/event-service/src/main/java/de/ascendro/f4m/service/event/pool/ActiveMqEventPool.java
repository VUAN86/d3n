package de.ascendro.f4m.service.event.pool;

import java.time.ZonedDateTime;

import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;

import org.apache.activemq.transport.TransportDisposedIOException;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.event.activemq.EmbeddedActiveMQ;
import de.ascendro.f4m.service.event.activemq.EventMessageListener;
import de.ascendro.f4m.service.event.activemq.TopicSubscriberMessageListener;
import de.ascendro.f4m.service.event.session.EventSessionStore;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.exception.F4MShuttingDownException;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.ServiceUtil;

/**
 * Management of events via ActiveMQ broker network (single locally embedded ActiveMQ).
 */
public class ActiveMqEventPool implements EventPool {
	
	private final EmbeddedActiveMQ activeMq;
	private final JsonMessageUtil jsonUtil;
	private final ServiceUtil serviceUtil;
	private final Config config;
	private LoggingUtil loggingUtil;

	@Inject
	public ActiveMqEventPool(EmbeddedActiveMQ activeMq, JsonMessageUtil jsonUtil, ServiceUtil serviceUtil,
			Config config, LoggingUtil loggingUtil) {
		this.activeMq = activeMq;
		this.jsonUtil = jsonUtil;
		this.serviceUtil = serviceUtil;
		this.config = config;
		this.loggingUtil = loggingUtil;
	}

	@Override
	public long subscribe(String consumerName, String topicName, boolean virtual, SessionWrapper sessionWrapper) throws F4MException {
		return subscribe(consumerName, topicName, serviceUtil.generateId(), virtual, sessionWrapper);
	}

	protected void addSubscription(SessionWrapper sessionWrapper, long subscription, MessageConsumer consumer) {
		final EventSessionStore eventSessionStore = sessionWrapper.getSessionStore();
		eventSessionStore.addSubscription(subscription, consumer);
	}

	@Override
	public void unsubscribe(long subscription, SessionWrapper sessionWrapper) throws F4MException {
		final EventSessionStore eventSessionStore = sessionWrapper.getSessionStore();
		if (eventSessionStore.hasSubscription(subscription)
				&& eventSessionStore.removeSubscription(subscription)) {
			try {
				activeMq.unsubscribeForTopic(subscription);
			} catch (JMSException e) {
				if (e.getCause() instanceof TransportDisposedIOException) {
					throw new F4MShuttingDownException(e);
				} else {
					throw new F4MFatalErrorException(e.getMessage(), e);
				}
			}
		}
	}

	@Override
	public void publish(String topicName, boolean virtual, String content, ZonedDateTime publishDate) {
		try {
			Long delay = null;
			if (publishDate != null) {
				delay = publishDate.toInstant().toEpochMilli() - DateTimeUtil.getUTCTimestamp();
			}
			activeMq.sendTopic(topicName, virtual, content, delay);
		} catch (JMSException e) {
			if (e.getCause() instanceof TransportDisposedIOException) {
				throw new F4MShuttingDownException(e);
			} else {
				throw new F4MFatalErrorException(e.getMessage(), e);
			}
		}
	}

	@Override
	public long resubscribe(long subscription, String consumerName, String topic, boolean virtual, SessionWrapper sessionWrapper)
			throws F4MException {
		final EventSessionStore store = sessionWrapper.getSessionStore();
		return store.hasSubscription(subscription) 
				? subscribe(consumerName, topic, null, virtual, sessionWrapper)
				: subscribe(consumerName, topic, subscription, virtual, sessionWrapper);
	}

	private long subscribe(String consumerName, String topicName, Long subscription, boolean virtual, SessionWrapper sessionWrapper)
			throws F4MException {
		long resubscription = subscription != null ? subscription : serviceUtil.generateId();
		try {
			final EventMessageListener messagListener = new TopicSubscriberMessageListener(resubscription, topicName,
					sessionWrapper, config, jsonUtil, loggingUtil);
			final MessageConsumer consumer = virtual 
					? activeMq.createVirtualTopicSubscriber(consumerName, topicName, messagListener, resubscription)
					: activeMq.createTopicSubscriber(topicName, messagListener, resubscription);

			addSubscription(sessionWrapper, resubscription, consumer);
		} catch (JMSException e) {
			if (e.getCause() instanceof TransportDisposedIOException) {
				throw new F4MShuttingDownException(e);
			} else {
				throw new F4MFatalErrorException(e.getMessage(), e);
			}
		}

		return resubscription;
	}

}
