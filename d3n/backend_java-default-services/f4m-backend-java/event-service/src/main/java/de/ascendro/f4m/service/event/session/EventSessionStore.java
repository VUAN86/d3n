package de.ascendro.f4m.service.event.session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.TopicSubscriber;

import org.apache.activemq.transport.TransportDisposedIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.session.pool.SessionStoreImpl;

/**
 * WebSocket session store for Event Service. Stores all event subscriptions received within the session. On destroy all
 * subscriptions are closed.
 */
public class EventSessionStore extends SessionStoreImpl {
	private static final Logger LOGGER = LoggerFactory.getLogger(EventSessionStore.class);

	private final Map<Long, MessageConsumer> consumers = new ConcurrentHashMap<>();

	public EventSessionStore(Config config, LoggingUtil loggingUtil, SessionWrapper sessionWrapper) {
		super(config, loggingUtil, sessionWrapper);
	}

	public MessageConsumer getSubscription(long id) {
		return consumers.get(id);
	}

	public void addSubscription(long subscriptionId, MessageConsumer consumer) {
		consumers.put(subscriptionId, consumer);
	}

	public boolean hasSubscription(long subscription) {
		return consumers.containsKey(subscription);
	}

	/** 
	 * Remove subscription.
	 * @return <code>true</code> if subscription was durable
	 */
	public boolean removeSubscription(long id) {
		final MessageConsumer consumer = consumers.remove(id);
		if (consumer != null) {
			try {
				consumer.close();
				return consumer instanceof TopicSubscriber;
			} catch (JMSException jmsEx) {
				if (! (jmsEx.getCause() instanceof TransportDisposedIOException)) {
					LOGGER.error("Cannot close topic subscriber", jmsEx);
				}
			}
		}
		return false;
	}

	@Override
	public void destroy() {
		consumers.entrySet().forEach(e -> removeSubscription(e.getKey()));
	}
}
