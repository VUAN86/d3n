package de.ascendro.f4m.service.analytics.activemq.router;


import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

public interface AnalyticMessageListener extends MessageListener {
    @Override
	void onMessage(Message message);
    String getTopic();
    void initAdvisorySupport() throws JMSException;
}
