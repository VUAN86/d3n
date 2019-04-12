package de.ascendro.f4m.service.analytics.module.statistic;


import java.util.Map;

import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

import org.slf4j.Logger;

import de.ascendro.f4m.server.analytics.EventContent;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.analytics.activemq.impl.ActiveMqBrokerManager;
import de.ascendro.f4m.service.analytics.activemq.router.AnalyticMessageListener;
import de.ascendro.f4m.service.analytics.logging.InjectLogger;
import de.ascendro.f4m.service.analytics.module.BaseModule;
import de.ascendro.f4m.service.analytics.module.statistic.query.base.ITableUpdater;
import de.ascendro.f4m.service.analytics.notification.NotificationCommon;

public class StatisticMessageListener extends BaseModule implements AnalyticMessageListener
{

    @InjectLogger
    private static Logger LOGGER;

    protected final JsonUtil jsonUtil;
    @SuppressWarnings("rawtypes")
    protected final Map<String, ITableUpdater> queryHandlers;


    @Inject
    @SuppressWarnings("rawtypes")
    public StatisticMessageListener(JsonUtil jsonUtil, Map<String, ITableUpdater> queryHandlers, NotificationCommon notificationUtil) {
        super(notificationUtil);
        this.jsonUtil = jsonUtil;
        this.queryHandlers = queryHandlers;
    }

    @Override
    public void initAdvisorySupport() throws JMSException {
        //No advisory
    }

    @Override
    public void onModuleMessage(Message message) {
        final TextMessage textMessage = (TextMessage) message;
        try {
            process(textMessage);
        } catch (Exception e) {
            LOGGER.error("Error on message type", e);
        }
    }

    protected void process(TextMessage textMessage) throws JMSException {
        LOGGER.debug("Message received: {} ", textMessage.getText());
        EventContent content = jsonUtil.fromJson(textMessage.getText(), EventContent.class);
        queryHandlers.entrySet().parallelStream().forEach(h->h.getValue().handleEvent(content));
    }

    @Override
    public String getTopic() {
        return ActiveMqBrokerManager.STATISTIC_TOPIC;
    }


}
