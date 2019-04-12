package de.ascendro.f4m.service.event.pool;

import java.time.ZonedDateTime;

import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.session.SessionWrapper;

/**
 * Event Pool for front management of events
 */
public interface EventPool {
	long subscribe(String consumerName, String topicName, boolean virtual, SessionWrapper sessionWrapper) throws F4MException;

	void unsubscribe(long subscription, SessionWrapper sessionWrapper) throws F4MException;

	void publish(String topic, boolean virtual, String content, ZonedDateTime publishDate) throws F4MException;

	long resubscribe(long subscription, String consumerName, String topic, boolean virtual, SessionWrapper sessionWrapper) throws F4MException;
}
