package de.ascendro.f4m.service.event.util;

import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;

import org.apache.activemq.ActiveMQTopicSubscriber;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.event.activemq.BrokerNetworkActiveMQ;
import de.ascendro.f4m.service.event.activemq.EmbeddedActiveMQ;
import de.ascendro.f4m.service.event.subscriptions.ServiceRegisterListener;
import de.ascendro.f4m.service.event.subscriptions.ServiceUnregisterListener;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;

/**
 * Event Service discovery utility for Event Service connection into single network. Subscribe/Unsubcribe for Service
 * Register register/unregister events.
 */
public class EventServiceDiscoveryUtil {
	private final EmbeddedActiveMQ brokerNetworkActiveMQ;
	private final ServiceRegistryClient serviceRegistryClient;

	private final ServiceRegisterListener serviceRegisterListener;
	private MessageConsumer serviceRegisterConsumer;

	private final ServiceUnregisterListener serviceUnregisterListener;
	private ActiveMQTopicSubscriber serviceUnregisterSubscriber;

	@Inject
	public EventServiceDiscoveryUtil(BrokerNetworkActiveMQ brokerNetworkActiveMQ,
			ServiceRegistryClient serviceRegistryClient, Config config, JsonMessageUtil jsonUtil,
			LoggingUtil loggingUtil) {
		this.brokerNetworkActiveMQ = brokerNetworkActiveMQ;
		this.serviceRegistryClient = serviceRegistryClient;

		serviceUnregisterListener = new ServiceUnregisterListener(brokerNetworkActiveMQ, config, jsonUtil, loggingUtil);
		serviceRegisterListener = new ServiceRegisterListener(brokerNetworkActiveMQ, config, jsonUtil, loggingUtil);
	}

	public void subscribeForRegister() throws JMSException {
		if (serviceRegisterConsumer != null) {
			final String eventServiceRegisterTopicName = serviceRegistryClient
					.getServiceRegisterTopicName(EventMessageTypes.SERVICE_NAME);
			serviceRegisterConsumer = brokerNetworkActiveMQ.createTopicSubscriber(eventServiceRegisterTopicName,
					serviceRegisterListener, ServiceRegisterListener.SUBSCRIPTION_ID);
		}
	}

	public void unsubscribeForRegister() throws JMSException {
		if (serviceRegisterConsumer != null) {
			brokerNetworkActiveMQ.unsubscribeForTopic(ServiceRegisterListener.SUBSCRIPTION_ID);
		}
	}

	public void unsubscribeForUnregister() throws JMSException {
		if (serviceRegisterConsumer != null) {
			brokerNetworkActiveMQ.unsubscribeForTopic(ServiceUnregisterListener.SUBSCRIPTION_ID);
		}
	}

	public void subscribeForUnregister() throws JMSException {
		if (serviceUnregisterSubscriber != null) {
			final String eventServiceUnregisterTopicName = serviceRegistryClient
					.getServiceUnregisterTopicName(EventMessageTypes.SERVICE_NAME);
			brokerNetworkActiveMQ.createTopicSubscriber(eventServiceUnregisterTopicName, serviceUnregisterListener,
					ServiceUnregisterListener.SUBSCRIPTION_ID);
		}
	}
}