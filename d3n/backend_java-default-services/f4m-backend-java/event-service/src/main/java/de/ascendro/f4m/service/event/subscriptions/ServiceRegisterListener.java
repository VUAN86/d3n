package de.ascendro.f4m.service.event.subscriptions;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.event.activemq.BrokerNetworkActiveMQ;
import de.ascendro.f4m.service.event.activemq.EventMessageListener;
import de.ascendro.f4m.service.event.model.publish.PublishMessageContent;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.registry.model.ServiceRegistryRegisterRequest;

/**
 * Service Registry service register event subscription listener.
 */
public class ServiceRegisterListener extends EventMessageListener {
	public static final long SUBSCRIPTION_ID = 1L;

	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceRegisterListener.class);

	private final BrokerNetworkActiveMQ brokerNetworkActiveMQ;

	public ServiceRegisterListener(BrokerNetworkActiveMQ brokerNetworkActiveMQ, Config config, JsonMessageUtil jsonUtil,
			LoggingUtil loggingUtil) {
		super(jsonUtil, config, loggingUtil);
		this.brokerNetworkActiveMQ = brokerNetworkActiveMQ;
	}

	@Override
	protected void onMessage(PublishMessageContent publishContent) {
		try {
			final JsonElement serviceRegisterJsonElement = publishContent.getNotificationContent();

			final ServiceRegistryRegisterRequest serviceRegisterContent = jsonUtil.fromJson(serviceRegisterJsonElement,
					ServiceRegistryRegisterRequest.class);

			//FIXME: serviceRegisterContent.getUri() is Jetty URI, not MQ - must create InfoEvent!
			brokerNetworkActiveMQ.addBrokerIntoNetwork(URI.create(serviceRegisterContent.getUri()));
		} catch (Exception e) {
			LOGGER.error("Cannot read add ActiveMQ network broker on service reigster event for message [{}]: {}",
					publishContent, e);
		}
	}

}
