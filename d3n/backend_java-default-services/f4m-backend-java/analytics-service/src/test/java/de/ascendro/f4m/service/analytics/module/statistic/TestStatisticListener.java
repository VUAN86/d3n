package de.ascendro.f4m.service.analytics.module.statistic;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.TextMessage;

import de.ascendro.f4m.server.analytics.EventContent;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.analytics.activemq.impl.ActiveMqBrokerManager;
import de.ascendro.f4m.service.analytics.listeners.TestListenerBase;
import de.ascendro.f4m.service.analytics.module.statistic.query.base.ITableUpdater;

public class TestStatisticListener extends TestListenerBase {
    public static CountDownLatch lock;


    public static void initLock(int count) {
        lock = new CountDownLatch(count);
    }

    @Override
    public CountDownLatch getLock() {
        return lock;
    }

	@Inject
    @SuppressWarnings("rawtypes")
    public TestStatisticListener(JsonUtil jsonUtil, Map<String, ITableUpdater> queryHandlers) {
        super(jsonUtil, queryHandlers, null);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void onListenerMessage(Message message) throws Exception {
        LOGGER.debug("Statistic Listener received message. Lock value: {}", lock.getCount());
        final TextMessage textMessage = (TextMessage) message;
        EventContent content = jsonUtil.fromJson(textMessage.getText(), EventContent.class);
        for (Map.Entry<String, ITableUpdater> updater : queryHandlers.entrySet()) {
            LOGGER.debug("Starting query handler: {}", updater.getKey());
            updater.getValue().processEvent(content);
            updater.getValue().triggerBatchExecute();
        }
        LOGGER.debug("Statistic Listener processed message. Lock value: {}", lock.getCount());
    }

    @Override
    public String getTopic() {
        return ActiveMqBrokerManager.STATISTIC_TOPIC;
    }
}
