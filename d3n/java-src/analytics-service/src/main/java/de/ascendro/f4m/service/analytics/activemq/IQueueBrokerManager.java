package de.ascendro.f4m.service.analytics.activemq;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Session;

import org.apache.activemq.ActiveMQTopicSubscriber;

import de.ascendro.f4m.service.analytics.activemq.router.AdvisoryMessageListener;
import de.ascendro.f4m.service.analytics.activemq.router.AnalyticMessageListener;

public interface IQueueBrokerManager {
    boolean start() throws Exception;
    void stop();
    void stopSubscribers();
    boolean isStarted();
    void initConsumers() throws JMSException;
    void sendTopic(String topicName, String content) throws JMSException;
    MapMessage gatherAMQStatistics() throws JMSException;
    void startAdvisory(String topicName, AdvisoryMessageListener messageListner) throws JMSException;
    void disposeConnection() throws JMSException;
    String toInternalTopicPattern(String externalTopic);
    String toExternalTopicPattern(String internalTopic);
    Session getSession() throws JMSException;
    ActiveMQTopicSubscriber createTopicSubscriber(AnalyticMessageListener messageListner,
                                                  String subscription) throws JMSException;
}
