package de.ascendro.f4m.service.analytics.module.notification;

import java.util.Set;

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
import de.ascendro.f4m.service.analytics.module.notification.processor.base.INotificationProcessor;
import de.ascendro.f4m.service.analytics.notification.NotificationCommon;

public class NotificationMessageListener extends BaseModule implements AnalyticMessageListener
{

    @InjectLogger
    private static Logger LOGGER;

    protected final JsonUtil jsonUtil;
    protected final Set<INotificationProcessor> notificationHandlers;

    @Inject
    public NotificationMessageListener(JsonUtil jsonUtil, Set<INotificationProcessor> notificationHandlers,
                                       NotificationCommon notificationUtil) {
        super(notificationUtil);
        this.jsonUtil = jsonUtil;
        this.notificationHandlers = notificationHandlers;
    }

    @Override
    public void initAdvisorySupport() throws JMSException {
        //No advisory
    }

    @Override
    public void onModuleMessage(Message message) {
        final TextMessage textMessage = (TextMessage) message;
        try {
            LOGGER.debug("Message received: {} ", textMessage.getText());
            EventContent content = jsonUtil.fromJson(textMessage.getText(), EventContent.class);
            notificationHandlers.forEach(h->h.handleEvent(content));
        } catch (Exception e) {
            LOGGER.error("Error on message type", e);
        }
    }

    @Override
    public String getTopic() {
        return ActiveMqBrokerManager.NOTIFICATION_TOPIC;
    }
}

