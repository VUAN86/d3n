package de.ascendro.f4m.service.analytics.module.sparc.advisory;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;

import org.apache.activemq.advisory.AdvisorySupport;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.CommandTypes;
import org.apache.activemq.command.ConsumerId;
import org.apache.activemq.command.ConsumerInfo;
import org.apache.activemq.command.DataStructure;
import org.apache.activemq.command.RemoveInfo;
import org.slf4j.Logger;

import de.ascendro.f4m.service.analytics.activemq.router.AdvisoryMessageListener;
import de.ascendro.f4m.service.analytics.logging.InjectLogger;


public class ConsumerAdvisoryListener implements AdvisoryMessageListener {
    @InjectLogger
    private static Logger LOGGER;

    @Override
    public void onMessage(Message message) {
        ActiveMQMessage activeMQMessage = (ActiveMQMessage) message;
        DataStructure dataStructure = activeMQMessage.getDataStructure();
        if (dataStructure != null) {
            switch (dataStructure.getDataStructureType()) {
                case CommandTypes.CONSUMER_INFO:
                    ConsumerInfo consumerInfo = (ConsumerInfo) dataStructure;
                    LOGGER.debug("Consumer '" + consumerInfo.getConsumerId()
                            + "' subscribed to '" + consumerInfo.getDestination()
                            + "'");
                    break;
                case CommandTypes.REMOVE_INFO:
                    RemoveInfo removeInfo = (RemoveInfo) dataStructure;
                    ConsumerId consumerId = (ConsumerId) removeInfo.getObjectId();
                    LOGGER.debug("Consumer '" + consumerId + "' unsubscribed");
                    break;
                default:
                    LOGGER.debug("Unknown data structure type");
            }
        } else {
            LOGGER.debug("No data structure provided");
        }
    }

    @Override
    public String getAdvisoryTopic(Destination destination) throws JMSException {
        return AdvisorySupport.getConsumerAdvisoryTopic(destination).getPhysicalName();
    }
}
