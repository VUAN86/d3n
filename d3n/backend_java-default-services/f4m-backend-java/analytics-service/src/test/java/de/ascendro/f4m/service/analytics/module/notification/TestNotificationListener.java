package de.ascendro.f4m.service.analytics.module.notification;

import java.util.Set;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;
import javax.jms.Message;

import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.analytics.activemq.impl.ActiveMqBrokerManager;
import de.ascendro.f4m.service.analytics.listeners.TestListenerBase;
import de.ascendro.f4m.service.analytics.module.notification.processor.base.INotificationProcessor;


public class TestNotificationListener extends TestListenerBase {
	public static CountDownLatch lock;

	public static void initLock(int count) {
		lock = new CountDownLatch(count);
	}

	@Override
	public void onListenerMessage(Message message) {
		//Nothing to do for this listener
	}

	@Override
	public CountDownLatch getLock() {
		return lock;
	}

	@Inject
    public TestNotificationListener(JsonUtil jsonUtil, Set<INotificationProcessor> notificationHandlers) {
		super(jsonUtil, null, notificationHandlers);
    }

    @Override
    public String getTopic() {
        return ActiveMqBrokerManager.NOTIFICATION_TOPIC;
    }
}
