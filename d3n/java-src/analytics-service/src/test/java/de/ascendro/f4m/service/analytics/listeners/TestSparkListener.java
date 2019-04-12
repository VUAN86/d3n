package de.ascendro.f4m.service.analytics.listeners;

import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;
import javax.jms.Message;

import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.analytics.activemq.impl.ActiveMqBrokerManager;

public class TestSparkListener extends TestListenerBase {
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
	public TestSparkListener(JsonUtil jsonUtil) {
		super(jsonUtil, null, null);
	}

	@Override
	public String getTopic() {
		return ActiveMqBrokerManager.SPARK_TOPIC;
	}
}
