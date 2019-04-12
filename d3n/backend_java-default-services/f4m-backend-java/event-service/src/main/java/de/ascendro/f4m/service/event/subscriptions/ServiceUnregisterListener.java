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
import de.ascendro.f4m.service.registry.model.ServiceRegistryUnregisterEvent;

/**
 * Service Registry service unregister event subscription listener.
 */
public class ServiceUnregisterListener extends EventMessageListener {

	public static final long SUBSCRIPTION_ID = 2L;

	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceRegisterListener.class);

	private final BrokerNetworkActiveMQ brokerNetworkActiveMQ;

	public ServiceUnregisterListener(BrokerNetworkActiveMQ brokerNetworkActiveMQ, Config config,
			JsonMessageUtil jsonUtil, LoggingUtil loggingUtil) {
		super(jsonUtil, config, loggingUtil);
		this.brokerNetworkActiveMQ = brokerNetworkActiveMQ;
	}

	@Override
	protected void onMessage(PublishMessageContent publishContent) {
		try {
			final JsonElement serviceUnregisterJsonElement = publishContent.getNotificationContent();

			final ServiceRegistryUnregisterEvent serviceRegisterContent = jsonUtil
					.fromJson(serviceUnregisterJsonElement, ServiceRegistryUnregisterEvent.class);
			final URI serviceURI = URI.create(serviceRegisterContent.getUri());
			brokerNetworkActiveMQ.removeBrokerFromNetwork(serviceURI);
		} catch (Exception e) {
			LOGGER.error("Cannot read add ActiveMQ network broker on service reigster event for message [{}]: {}",
					publishContent, e);
		}
	}
}
