package de.ascendro.f4m.service.analytics.module.sparc.advisory;

import javax.inject.Inject;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;

import org.apache.activemq.advisory.AdvisorySupport;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.activemq.command.CommandTypes;
import org.apache.activemq.command.DataStructure;
import org.slf4j.Logger;

import de.ascendro.f4m.service.analytics.AnalyticsScan;
import de.ascendro.f4m.service.analytics.activemq.router.AdvisoryMessageListener;
import de.ascendro.f4m.service.analytics.logging.InjectLogger;
import de.ascendro.f4m.service.analytics.notification.NotificationCommon;
import de.ascendro.f4m.service.usermessage.translation.Messages;

public class NoConsumerAdvisoryListener  implements AdvisoryMessageListener {
    @InjectLogger
    private static Logger LOGGER;

    protected final NotificationCommon notificationUtil;
    protected final AnalyticsScan analyticsScan;

    @Inject
    public NoConsumerAdvisoryListener(AnalyticsScan analyticsScan, NotificationCommon notificationUtil) {
        this.analyticsScan = analyticsScan;
        this.notificationUtil = notificationUtil;
    }

    @Override
    public void onMessage(Message message) {
        try {
            ActiveMQMessage activeMQMessage = (ActiveMQMessage) message;
            DataStructure dataStructure = activeMQMessage.getDataStructure();
            if (dataStructure != null) {

                switch (dataStructure.getDataStructureType()) {
                    case CommandTypes.ACTIVEMQ_MESSAGE:
                        ActiveMQTextMessage activeMQTextMessage = (ActiveMQTextMessage)dataStructure;
                        LOGGER.error("NoConsumerAdvisory error for topic {}", activeMQTextMessage.getDestination().getPhysicalName());

                        break;
                    default:
                        LOGGER.debug("Unknown data structure type. {}", activeMQMessage.getDestination());
                }

                StringBuilder messageBuilder = new StringBuilder();
                messageBuilder.append("Destination =  ")
                        .append(activeMQMessage.getDestination().getPhysicalName()).append("/n")
                        .append(activeMQMessage.getMemoryUsage());

                if (analyticsScan.isRunning()) {
                    analyticsScan.suspend();
                    notificationUtil.sendEmailToAdmin(Messages.AMQ_NO_CONSUMER_ERROR_TO_ADMIN_SUBJECT, null,
                            messageBuilder.toString(), null);
                }
            }
        } catch (Exception e) {
            LOGGER.error("NoConsumerAdvisory listener exception", e);
        }
    }

    @Override
    public String getAdvisoryTopic(Destination destination) throws JMSException {
        return AdvisorySupport.getNoConsumersAdvisoryTopic(destination).getPhysicalName();
    }
}
