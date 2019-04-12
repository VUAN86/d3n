package de.ascendro.f4m.service.analytics.listeners;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;

import org.slf4j.Logger;

import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.analytics.activemq.router.AnalyticMessageListener;
import de.ascendro.f4m.service.analytics.logging.InjectLogger;
import de.ascendro.f4m.service.analytics.module.notification.processor.base.INotificationProcessor;
import de.ascendro.f4m.service.analytics.module.statistic.query.base.ITableUpdater;

public abstract class TestListenerBase implements AnalyticMessageListener {

	@InjectLogger
	protected Logger LOGGER;

	protected final JsonUtil jsonUtil;
	@SuppressWarnings("rawtypes")
	protected final Map<String, ITableUpdater> queryHandlers;
	protected final Set<INotificationProcessor> notificationHandlers;


	@Inject
	@SuppressWarnings("rawtypes")
	public TestListenerBase(JsonUtil jsonUtil, Map<String, ITableUpdater> queryHandlers, Set<INotificationProcessor> notificationHandlers) {
		this.jsonUtil = jsonUtil;
		this.queryHandlers = queryHandlers;
		this.notificationHandlers = notificationHandlers;
	}

	@Override
	public void initAdvisorySupport() throws JMSException {
		//No advisory
	}

	@Override
	public void onMessage(Message message) {
		LOGGER.debug("Listener {} on topic {} received message {}",
				this.getClass().getSimpleName(), getTopic(), message);
		try {
			CountDownLatch lock = getLock();
			onListenerMessage(message);
			if (lock != null) {
				if (lock.getCount() <= 0) {
					LOGGER.warn("Listener {} on {} received unexpected message {}",
							this.getClass().getSimpleName(), getTopic(), message);
				}
				lock.countDown();
				LOGGER.info("Messages to be received {} on topic {} for listener {}",
						lock.getCount(), getTopic(), this.getClass().getSimpleName());
			}
		} catch (Exception e) {
			LOGGER.error("Error on message type", e);
		}
	}

	public abstract void onListenerMessage(Message message) throws Exception;

	public abstract CountDownLatch getLock();
}
