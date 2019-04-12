package de.ascendro.f4m.service.analytics.activemq.router;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

public interface AdvisoryMessageListener extends MessageListener {
    @Override
    void onMessage(Message message);
    String getAdvisoryTopic(Destination destination) throws JMSException;
}
