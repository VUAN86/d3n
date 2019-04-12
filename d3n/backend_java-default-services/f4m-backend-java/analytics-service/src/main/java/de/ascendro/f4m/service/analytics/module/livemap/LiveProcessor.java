package de.ascendro.f4m.service.analytics.module.livemap;

import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;

import org.slf4j.Logger;

import de.ascendro.f4m.service.analytics.activemq.impl.ActiveMqBrokerManager;
import de.ascendro.f4m.service.analytics.activemq.router.AnalyticMessageListener;
import de.ascendro.f4m.service.analytics.logging.InjectLogger;
import de.ascendro.f4m.service.analytics.module.BaseModule;
import de.ascendro.f4m.service.analytics.notification.NotificationCommon;

public class LiveProcessor extends BaseModule implements AnalyticMessageListener
{
    @InjectLogger
    private static Logger LOGGER;

    @Inject
    public LiveProcessor(NotificationCommon notificationUtil) {
        super(notificationUtil);
    }

    @Override
    public void onModuleMessage(Message message) {
        //:TODO this need to be implemented when the Live Map sepecs are set
//        final TextMessage textMessage = (TextMessage) message;
//        try {
//            LOGGER.info(this.getClass().getSimpleName() + ": "  + textMessage.getText());
//        } catch (Exception e) {
//            LOGGER.error("Error on message type", e);
//        }
    }

    @Override
    public void initAdvisorySupport() throws JMSException {
        //No advisory
    }

    @Override
    public String getTopic() {
        return ActiveMqBrokerManager.LIVE_MAP_TOPIC;
    }
}
