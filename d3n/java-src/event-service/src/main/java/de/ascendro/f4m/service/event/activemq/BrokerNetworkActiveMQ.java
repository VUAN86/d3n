package de.ascendro.f4m.service.event.activemq;

import java.net.URI;
import java.util.Arrays;

import javax.inject.Inject;

import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.activemq.network.DiscoveryNetworkConnector;
import org.apache.activemq.network.NetworkConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.event.config.EventConfig;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.util.KeyStoreUtil;

/**
 * Extension of Embedded ActiveMQ for Broker Network support. Possible to connect ActiveMQs within network to
 * communicate with each other.
 */
public class BrokerNetworkActiveMQ extends EmbeddedActiveMQ {

	private static final Logger LOGGER = LoggerFactory.getLogger(BrokerNetworkActiveMQ.class);

	@Inject
	public BrokerNetworkActiveMQ(EventConfig eventConfig, KeyStoreUtil keyStoreUtil) throws F4MException {
		super(eventConfig, keyStoreUtil);
	}

	/**
	 * Added new ActiveMQ broker within Broker Network. Auto-start added connector.
	 * 
	 * @param brokerDiscoveryAddress
	 *            - URI for broker to be discovered and added to network. Broker address is wrapped into ActiveMq
	 *            discovery protocol (@see EventConfig.ACTIVE_MQ_NETWORK_DISCOVERY_PROTOCOL)
	 */
	protected void addNetworkConnector(URI brokerDiscoveryAddress, boolean isQueue) {
		try {
			final String networkDiscoveryProtocol = config
					.getProperty(EventConfig.ACTIVE_MQ_NETWORK_DISCOVERY_PROTOCOL);
			final URI networkDiscoveryAddress = URI
					.create(networkDiscoveryProtocol + ":(" + brokerDiscoveryAddress + ")");
			final NetworkConnector networkConnector = new DiscoveryNetworkConnector(networkDiscoveryAddress);

			final String networkConnectorName = brokerService.getBrokerName() + "_" + brokerDiscoveryAddress.getHost()
					+ brokerDiscoveryAddress.getPort() + "_" + (isQueue ? "queue" : "topic");
			networkConnector.setName(networkConnectorName);
			
			// Disable conduit subscriptions for queues
			networkConnector.addDynamicallyIncludedDestination(isQueue 
					? new ActiveMQQueue(DEFAULT_RECURSIVE_WILDCARD) : new ActiveMQTopic(DEFAULT_RECURSIVE_WILDCARD));
			if (isQueue) {
				networkConnector.setConduitSubscriptions(false); // to enable better load balancing
			}
			
			brokerService.addNetworkConnector(networkConnector);

			LOGGER.debug("Adding new Network connector[{}] using Discovery address[{}] for Broker[{}]:started[{}]:isQueue[{}]",
					networkConnectorName, networkDiscoveryAddress, brokerService.getBrokerName(),
					brokerService.isStarted(), isQueue);

			networkConnector.start();
		} catch (Exception e) {
			LOGGER.error("Cannot add new network broker [" + brokerDiscoveryAddress + "], isQueue[" + isQueue + "]", e);
		}
	}

	/**
	 * Remove already connected ActiveMQ from Broker Network. Try to stop broker connector before removing.
	 * 
	 * @param brokerNetworkURI
	 */
	protected void removeBrokerFromNetwork(URI brokerNetworkURI) {
		final String networkConnectorName = createNetworkConnectorName(brokerNetworkURI);
		final NetworkConnector networkConnector = brokerService.getNetworkConnectorByName(networkConnectorName);
		if (networkConnector != null) {
			try {
				networkConnector.stop();
			} catch (Exception e) {
				LOGGER.error("Failed to stop network connector by name[{}] via URI[{}]: {}", networkConnector,
						brokerNetworkURI.toString(), e);
			} finally {
				brokerService.removeNetworkConnector(networkConnector);
			}
		} else {
			LOGGER.warn("Network connector is not found by name[{}] via URI[{}]", networkConnector,
					brokerNetworkURI.toString());
		}
	}

	/**
	 * Added provided ActiveMQ brokers into network
	 * 
	 * @param brokerNetworkURIs
	 *            - brokers' URI to be added
	 */
	public void addBrokerIntoNetwork(URI... brokerNetworkURIs) {
		Arrays.stream(brokerNetworkURIs).forEach(brokerURI -> {
			addNetworkConnector(brokerURI, true);
			addNetworkConnector(brokerURI, false);		
		});
	}

	/**
	 * Removed provided ActiveMQ brokers from network
	 * @param brokerNetworkURIs
	 *            - brokers' URI to be removed
	 */
	public void removeBrokerFromNetwork(URI... brokerNetworkURIs) {
		Arrays.stream(brokerNetworkURIs).forEach(brokerNetworkURI -> removeBrokerFromNetwork(brokerNetworkURI));
	}

	/**
	 * Creates Broker connector name based on URI provided, so brokers can be uniquely identified when added or removed.
	 * 
	 * @param brokerNetworkURI
	 *            - Broker Network URI to be used for name creation.
	 * @return broker network based on provided URI
	 */
	protected String createNetworkConnectorName(URI brokerNetworkURI) {
		return brokerService.getBrokerName() + "-" + brokerNetworkURI.getHost() + brokerNetworkURI.getPort();
	}
}
