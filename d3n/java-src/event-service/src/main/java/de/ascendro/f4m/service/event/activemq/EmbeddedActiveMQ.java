package de.ascendro.f4m.service.event.activemq;

import java.io.IOException;
import java.net.URI;
import java.security.SecureRandom;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.SslBrokerService;
import org.apache.activemq.broker.SslContext;
import org.apache.activemq.broker.TransportConnector;
import org.apache.activemq.store.PersistenceAdapter;
import org.apache.activemq.transport.TransportFactory;
import org.apache.activemq.transport.tcp.SslTransportFactory;
import org.apache.activemq.util.ServiceStopper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.event.config.EventConfig;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.util.KeyStoreUtil;

/**
 * Within JVM embedded ActiveMQ instance, which can be accessed via VM protocol.
 */
public abstract class EmbeddedActiveMQ {
	private static final String BROKER_NAME_PREFIX = "localhost";
	private static final String CLIENT_NAME_PREFIX = "localhostClient";

	public static final String DEFAULT_WILDCARD_SEPARATOR = ".";
	public static final String DEFAULT_VIRTUAL_TOPIC_KEYWORD = "VirtualTopic";
	public static final String DEFAULT_CONSUMER_KEYWORD = "Consumer";
	public static final String DEFAULT_WILDCARD = "*";
	public static final String DEFAULT_RECURSIVE_WILDCARD = ">";

	private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedActiveMQ.class);

	private final SecureRandom secureRandom = new SecureRandom();

	protected final Config config;
	protected final KeyStoreUtil keyStoreUtil;

	protected final BrokerService brokerService;

	private ConnectionFactory connectionFactory = null;
	private Connection connection = null;
	private TransportConnector transportConnector = null;

	public EmbeddedActiveMQ(Config config, KeyStoreUtil keyStoreUtil) throws F4MException {
		this.config = config;
		this.keyStoreUtil = keyStoreUtil;

		registerSslTransportFactory();

		this.brokerService = createBrokerService();
	}

	public void setPersistenceAdapter(PersistenceAdapter persistenceAdapter) throws IOException {
		brokerService.setPersistenceAdapter(persistenceAdapter);
	}

	protected BrokerService createBrokerService() {
		final SslBrokerService sslBrokerService = new SslBrokerService();
		sslBrokerService.setUseShutdownHook(true);
		sslBrokerService.setPersistent(true);
		sslBrokerService.setUseJmx(false);
		sslBrokerService.setSchedulerSupport(true);
		return sslBrokerService;
	}

	public void setOfflineDurableSubscriberTimeout(long offlineDurableSubscriberTimeout) {
		brokerService.setOfflineDurableSubscriberTimeout(offlineDurableSubscriberTimeout);
	}

	protected void addTransportConnector() throws Exception {
		final URI externalActiveMqURI = createExternalUri();
		LOGGER.debug("Adding ActiveMQ transportConnector on {}", externalActiveMqURI);
		if (brokerService instanceof SslBrokerService) {
			transportConnector = ((SslBrokerService) brokerService).addSslConnector(externalActiveMqURI,
					keyStoreUtil.getKeyManagers(), keyStoreUtil.getTrustManagers(), secureRandom);
		} else {
			transportConnector = brokerService.addConnector(externalActiveMqURI);
		}
	}

	/**
	 * Registers TLS SSL certificates within ActiveMQ to provide secure communication
	 * 
	 * @throws F4MException
	 *             -
	 */
	protected void registerSslTransportFactory() throws F4MException {
		final SslTransportFactory sslFactory = new SslTransportFactory();
		final SslContext sslContext = new SslContext(keyStoreUtil.getKeyManagers(), keyStoreUtil.getTrustManagers(),
				secureRandom);

		SslContext.setCurrentSslContext(sslContext);
		TransportFactory.registerTransportFactory("ssl", sslFactory);
	}

	public boolean start() throws Exception {
		if (!brokerService.isStarted()) {
			brokerService.setBrokerName(createBrokerName());
			brokerService.setOfflineDurableSubscriberTimeout(
					config.getPropertyAsInteger(EventConfig.MESSAGE_DURABLE_SUBSCRIBEE_OFLINE_TIMEOUT));

			LOGGER.debug("Starting ActiveMQ Broker[{}] at [{}] persistent[{}] using PersistenceAdapter[{}]",
					brokerService.getBrokerName(), createInternalUri(), brokerService.isPersistent(),
					brokerService.getPersistenceAdapter().getClass().getSimpleName());
			addTransportConnector();
			brokerService.start();
		} else {
			throw new F4MFatalErrorException("ActiveMQ already started");
		}
		return brokerService.waitUntilStarted();
	}

	public ConnectionFactory getConnectionFactory() {
		if (connectionFactory == null) {
			connectionFactory = createConnectionFactory();
		}
		return connectionFactory;
	}

	protected void removeTransportConnector() throws Exception {
		if (transportConnector != null) {
			try {
				transportConnector.stop();
			} finally {
				brokerService.removeConnector(transportConnector);
				transportConnector = null;
			}
		}
	}

	public void stop() throws Exception {
		if (brokerService.isStarted()) {
			removeTransportConnector();
			disposeConnection();
			brokerService.stopAllConnectors(new ServiceStopper() {
				@Override
				protected void logError(Object service, Throwable e) {
					LOGGER.error("Service[" + service + "] failed to stop connectors", e);
				}
			});
			brokerService.stop();
			brokerService.waitUntilStopped();
		} else {
			LOGGER.warn("Attempting to stop not running ActiveMQ");
		}
	}

	public void disposeConnection() throws JMSException {
		if (connection != null) {
			connection.stop();
			connection.close();
		}
	}

	public MessageConsumer createTopicSubscriber(String topicName, 
			EventMessageListener eventMessageListener, long subscription) throws JMSException {
		final Connection conn = getConnection();
		final Session session = conn.createSession(false, Session.CLIENT_ACKNOWLEDGE);
		final Topic topic = session.createTopic(toInternalTopicPattern(topicName));
		final MessageConsumer consumer = session.createDurableSubscriber(topic, String.valueOf(subscription));
		consumer.setMessageListener(eventMessageListener);
		return consumer;
	}
	
	public MessageConsumer createVirtualTopicSubscriber(String consumerName, String topicName,  
			EventMessageListener eventMessageListener, long subscription) throws JMSException {
		final Connection conn = getConnection();
		final Session session = conn.createSession(false, Session.CLIENT_ACKNOWLEDGE);
		final Queue queue = session.createQueue(new StringBuilder(DEFAULT_CONSUMER_KEYWORD).append(DEFAULT_WILDCARD_SEPARATOR)
				.append(consumerName).append(DEFAULT_WILDCARD_SEPARATOR).append(DEFAULT_VIRTUAL_TOPIC_KEYWORD)
				.append(DEFAULT_WILDCARD_SEPARATOR).append(toInternalTopicPattern(topicName)).toString());
		final MessageConsumer consumer = session.createConsumer(queue);
		consumer.setMessageListener(eventMessageListener);
		return consumer;
	}
	
	public void unsubscribeForTopic(long subscriptionId) throws JMSException {
		final Connection conn = getConnection();
		final Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);

		session.unsubscribe(Long.toString(subscriptionId));

		session.close();
	}

	public void sendTopic(String topicName, boolean virtual, String content, Long delay) throws JMSException {
		final Connection conn = getConnection();

		final Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
		StringBuilder subscribedTopicName = new StringBuilder();
		if (virtual) {
			subscribedTopicName.append(DEFAULT_VIRTUAL_TOPIC_KEYWORD).append(DEFAULT_WILDCARD_SEPARATOR);
		}
		subscribedTopicName.append(toInternalTopicPattern(topicName));
		final Destination destination = session.createTopic(subscribedTopicName.toString());

		final MessageProducer messageProducer = session.createProducer(destination);
		Integer timeToLive = config.getPropertyAsInteger(EventConfig.MESSAGE_PRODUCER_MESSAGE_TIME_TO_LIVE);
		messageProducer.setTimeToLive(timeToLive);

		final TextMessage textMessage = session.createTextMessage();
		textMessage.setText(content);
		if (delay != null && delay > 0) {
			if (delay >= timeToLive) {
				throw new F4MValidationFailedException("Delay too big");
			} else {
				textMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, delay);
				LOGGER.debug("Message {} will be sent with delay {}", textMessage, delay);
			}
		}

		messageProducer.send(textMessage);
		messageProducer.close();
		session.close();
	}

	public Connection getConnection() throws JMSException {
		if (this.connection == null) {
			synchronized(this) {
				if (this.connection == null) {
					this.connection = getConnectionFactory().createConnection();
					this.connection.setExceptionListener(new JMSExceptionListener());
					this.connection.start();
				}
			}
		}
		return connection;
	}

	protected String createClientId() {
		return CLIENT_NAME_PREFIX + config.getProperty(EventConfig.ACTIVE_MQ_PORT);
	}

	protected URI createInternalUri() {
		final String internalTransportProtocol = config
				.getProperty(EventConfig.ACTIVE_MQ_INTERNAL_COMMUNICATION_PROTOCOL);
		return URI.create(internalTransportProtocol + "://" + createBrokerName());
	}

	public void setPersistent(boolean persistent) {
		brokerService.setPersistent(persistent);
	}

	protected URI createExternalUri() {
		final String externalTransportProtocol = config
				.getProperty(EventConfig.ACTIVE_MQ_EXTERNAL_COMMUNICATION_PROTOCOL);
		final String externalTransportHost = config.getProperty(EventConfig.ACTIVE_MQ_EXTERNAL_COMMUNICATION_HOST);
		return URI.create(externalTransportProtocol + "://" + externalTransportHost + ":"
				+ config.getProperty(EventConfig.ACTIVE_MQ_PORT));
	}

	protected String createBrokerName() {
		return BROKER_NAME_PREFIX + config.getProperty(EventConfig.ACTIVE_MQ_PORT);
	}

	protected ConnectionFactory createConnectionFactory() {
		final ActiveMQConnectionFactory mqConnectionFactory = new ActiveMQConnectionFactory(createInternalUri());
		mqConnectionFactory.buildFromProperties(config.getProperties());
		mqConnectionFactory.setClientID(createClientId());
		return mqConnectionFactory;
	}

	protected String toInternalTopicPattern(String externalTopic) {
		return externalTopic == null ? null 
				: externalTopic.replace(config.getProperty(EventConfig.WILDCARD_SEPARATOR), 
						EmbeddedActiveMQ.DEFAULT_WILDCARD_SEPARATOR);
	}
}
