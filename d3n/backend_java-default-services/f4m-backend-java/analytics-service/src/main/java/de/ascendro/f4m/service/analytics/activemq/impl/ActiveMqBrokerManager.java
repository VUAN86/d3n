package de.ascendro.f4m.service.analytics.activemq.impl;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQTopicSubscriber;
import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.TransportConnector;
import org.apache.activemq.broker.region.policy.DeadLetterStrategy;
import org.apache.activemq.broker.region.policy.PolicyEntry;
import org.apache.activemq.broker.region.policy.PolicyMap;
import org.apache.activemq.broker.region.policy.SharedDeadLetterStrategy;
import org.apache.activemq.plugin.StatisticsBrokerPlugin;
import org.apache.activemq.store.kahadb.KahaDBStore;
import org.slf4j.Logger;

import de.ascendro.f4m.server.analytics.exception.F4MAnalyticsFatalErrorException;
import de.ascendro.f4m.service.analytics.activemq.IQueueBrokerManager;
import de.ascendro.f4m.service.analytics.activemq.router.AdvisoryMessageListener;
import de.ascendro.f4m.service.analytics.activemq.router.AnalyticMessageListener;
import de.ascendro.f4m.service.analytics.config.AnalyticsConfig;
import de.ascendro.f4m.service.analytics.logging.InjectLogger;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;

public class ActiveMqBrokerManager implements IQueueBrokerManager {
    @InjectLogger
    private static Logger LOGGER;
    private BrokerService broker;
    private final Config config;
    private final Set<AnalyticMessageListener> messageListeners;
    private final Set<ActiveMQTopicSubscriber> topicSubscribers =  new HashSet<>();
    private static final String AMQ_STATISTICS_QUEUE_NAME = "ActiveMQ.Statistics.Broker";

    public static final String  DEFAULT_WILDCARD_SEPARATOR = ".";

    private static final String BROKER_NAME_PREFIX = "localhost";
    private static final String CLIENT_NAME_PREFIX = "localhostClient";
    public static final String TOPIC_ROOT = "f4m.analytics";

    public static final String NOTIFICATION_TOPIC = TOPIC_ROOT + ".notification";
    public static final String STATISTIC_TOPIC = TOPIC_ROOT +  ".statistic";
    public static final String LIVE_MAP_TOPIC = TOPIC_ROOT + ".map";
    public static final String SPARK_TOPIC = TOPIC_ROOT + ".spark";
    public static final String JOB_TOPIC = TOPIC_ROOT + ".job";

    public static final String SPARK_EXTERNAL_TOPIC = TOPIC_ROOT + ".ext.spark";

    private static final int KAHA_MAX_FILE_LENGTH = 1024 * 1204 * 100;
    private static final int KAHA_BATCH_SIZE = 100;

    private Connection connection = null;
    private ConnectionFactory connectionFactory = null;
    private TransportConnector transportConnector = null;

    @Inject
    ActiveMqBrokerManager(AnalyticsConfig config, Set<AnalyticMessageListener> messageListeners) {
        this.config = config;
        this.messageListeners = messageListeners;
    }

    @Override
    public boolean start() throws Exception {
        this.broker = createBrokerService();
        if (!broker.isStarted()) {
            broker.setBrokerName(createBrokerName());
            broker.setOfflineDurableSubscriberTimeout(
                    config.getPropertyAsInteger(AnalyticsConfig.MESSAGE_DURABLE_SUBSCRIBEE_OFLINE_TIMEOUT));

            enableAMQStatistics(broker);

            LOGGER.debug("Starting ActiveMQ Broker[{}] at [{}] persistent[{}] using PersistenceAdapter[{}]",
                    broker.getBrokerName(), createInternalUri(), broker.isPersistent(),
                    broker.getPersistenceAdapter().getClass().getSimpleName());

            //initialize external transport
            addTransportConnector();
            //start advisory support
            broker.setAdvisorySupport(true);


            broker.start();
            broker.waitUntilStarted();
        } else {
            throw new F4MFatalErrorException("ActiveMQ already started");
        }
        return broker.waitUntilStarted();
    }

    private void enableAMQStatistics(BrokerService brokerService) {
        // Start our stats plugin
        StatisticsBrokerPlugin statsPlugin = new StatisticsBrokerPlugin();
        // Find what plugins are already present
        BrokerPlugin[] actualPlugins = broker.getPlugins();
        if (actualPlugins == null) {
            actualPlugins = new BrokerPlugin[] {};
        }
        // Add stats to the list
        List<BrokerPlugin> brokerPlugins = new ArrayList<>();
        brokerPlugins.addAll(Arrays.asList(actualPlugins));
        brokerPlugins.add(statsPlugin);
        // Setup the broker
        brokerService.setPlugins(brokerPlugins.toArray(actualPlugins));
        brokerService.setEnableStatistics(true);
    }

    @Override
    public void stop() {
        try {
            LOGGER.info("Active MQ Broker Service - STOPPING");
            if (isStarted()) {
                removeTransportConnector();
	            broker.stop();
	            broker.waitUntilStopped();
	            broker = null;
	            LOGGER.info("Active MQ Broker Service - STOPPED");
            } else {
            	LOGGER.info("Active MQ Broker Service was already STOPPED");
            }
        } catch (Exception e) {
            LOGGER.error("Error shuting down broker", e);
        }
    }

    @Override
    public void stopSubscribers() {
        topicSubscribers.forEach(t->t.stop());
    }

    @Override
    public boolean isStarted() {
        return !(broker==null || !broker.isStarted());
    }


    private BrokerService createBrokerService() {
        final BrokerService newBroker = new BrokerService();
        newBroker.setUseShutdownHook(true);
        newBroker.setDeleteAllMessagesOnStartup(false);
        newBroker.setUseJmx(false);

        DeadLetterStrategy strategy = new SharedDeadLetterStrategy();
        strategy.setProcessExpired(true);
        PolicyEntry policy = new PolicyEntry();
        policy.setDeadLetterStrategy(strategy);

        PolicyMap policyMap = new PolicyMap();
        policyMap.setDefaultEntry(policy);
        newBroker.setDestinationPolicy(policyMap);

        if (config.getPropertyAsBoolean(AnalyticsConfig.ACTIVE_MQ_PERSISTENT)) {
            newBroker.setPersistent(true);
            try {
                initKahaDBPersistence(newBroker);
            } catch (IOException e) {
                LOGGER.error("Cannot start ActiveMQ instance to KahaDB mode", e);
            }
        } else {
            newBroker.setPersistent(false);
        }

        return newBroker;
    }

    private void initKahaDBPersistence(BrokerService targetBroker) throws IOException {
        File dataFileDir = new File("target/activemq-data/kahadb");

        KahaDBStore kaha = new KahaDBStore();
        kaha.setDirectory(dataFileDir);
        kaha.setJournalMaxFileLength(KAHA_MAX_FILE_LENGTH);
        kaha.setIndexWriteBatchSize(KAHA_BATCH_SIZE);
        kaha.setEnableIndexWriteAsync(true);

        targetBroker.setPersistenceAdapter(kaha);
    }

    private String createBrokerName() {
        return BROKER_NAME_PREFIX + config.getProperty(AnalyticsConfig.ACTIVE_MQ_PORT);
    }

    private URI createInternalUri() {
        final String internalTransportProtocol = config
                .getProperty(AnalyticsConfig.ACTIVE_MQ_INTERNAL_COMMUNICATION_PROTOCOL);
        return URI.create(internalTransportProtocol + "://" + createBrokerName() + "?broker.persistent=false");
    }

    private String createClientId() {
        return CLIENT_NAME_PREFIX + config.getProperty(AnalyticsConfig.ACTIVE_MQ_PORT);
    }

    @Override
    public void initConsumers() throws JMSException {
        for (AnalyticMessageListener messageListner : messageListeners) {
            topicSubscribers.add(createTopicSubscriber(messageListner, messageListner.getClass().getSimpleName()));
            messageListner.initAdvisorySupport();
        }
    }

    public synchronized Connection getConnection() throws JMSException {
        if (this.connection == null) {
            this.connection = getConnectionFactory().createConnection();
            this.connection.setExceptionListener(new JMSExceptionListener());
            this.connection.start();
        }
        return connection;
    }

    private ConnectionFactory getConnectionFactory() {
        if (connectionFactory == null) {
            connectionFactory = createConnectionFactory();
        }
        return connectionFactory;
    }

    private ConnectionFactory createConnectionFactory() {
        final ActiveMQConnectionFactory mqConnectionFactory = new ActiveMQConnectionFactory(createInternalUri());
        mqConnectionFactory.buildFromProperties(config.getProperties());
        mqConnectionFactory.setClientID(createClientId());
        mqConnectionFactory.setWatchTopicAdvisories(true);
        mqConnectionFactory.setStatsEnabled(true);
        return mqConnectionFactory;
    }

    @Override
    public void disposeConnection() throws JMSException {
        if (connection != null) {
            connection.stop();
            connection.close();
            connection = null;
        }
    }

    protected void addTransportConnector() throws Exception {
        final URI externalActiveMqURI = createExternalUri();
        LOGGER.debug("Adding ActiveMQ transportConnector on {}", externalActiveMqURI);
        transportConnector = broker.addConnector(externalActiveMqURI);
    }

    @Override
    public void startAdvisory(String monitoredTopic, AdvisoryMessageListener messageListner) throws JMSException {
        final Session session = getSession();
        final Destination destination = session.createTopic(monitoredTopic);

        Destination advisoryDestination = session.createTopic(messageListner.getAdvisoryTopic(destination));

        final MessageConsumer advisoryConsumer = session.createConsumer(advisoryDestination);
        advisoryConsumer.setMessageListener(messageListner);
    }

    protected void removeTransportConnector() throws Exception {
        if (transportConnector != null) {
            try {
                transportConnector.stop();
            } finally {
                broker.removeConnector(transportConnector);
                transportConnector = null;
            }
        }
    }

    protected URI createExternalUri() {
        final String externalTransportProtocol = config
                .getProperty(AnalyticsConfig.ACTIVE_MQ_EXTERNAL_COMMUNICATION_PROTOCOL);
        final String externalTransportHost = config.getProperty(AnalyticsConfig.ACTIVE_MQ_EXTERNAL_COMMUNICATION_HOST);
        return URI.create(externalTransportProtocol + "://" + externalTransportHost + ":"
                + config.getProperty(AnalyticsConfig.ACTIVE_MQ_PORT));
     }


    @Override
    public ActiveMQTopicSubscriber createTopicSubscriber(AnalyticMessageListener messageListner,
                                                         String subscription) throws JMSException {
        final Connection conn = getConnection();

        final Session session = conn.createSession(false, Session.CLIENT_ACKNOWLEDGE);
        final Topic topic = session.createTopic(messageListner.getTopic());
        final ActiveMQTopicSubscriber topicSubscriber = (ActiveMQTopicSubscriber) session.createDurableSubscriber(topic,
                subscription);
        topicSubscriber.setMessageListener(messageListner);

        return topicSubscriber;
    }

    @Override
    public void sendTopic(String topicName, String content) throws JMSException {
        final Session session = getSession();
        final Destination destination = session.createTopic(topicName);
        
        final MessageProducer messageProducer = session.createProducer(destination);
        messageProducer.setTimeToLive(config.getPropertyAsInteger(AnalyticsConfig.MESSAGE_PRODUCER_MESSAGE_TIME_TO_LIVE));
        messageProducer.setDeliveryMode(DeliveryMode.PERSISTENT);

        final TextMessage textMessage = session.createTextMessage();
        textMessage.setText(content);

        messageProducer.send(textMessage);

        messageProducer.close();
        session.close();

        LOGGER.debug("Send to topic {}, message {}", topicName, content);
    }

    @Override
	public MapMessage gatherAMQStatistics() throws JMSException {
        final Session session = getSession();
        final Queue replyTo = session.createTemporaryQueue();
        final MessageConsumer messageConsumer = session.createConsumer(replyTo);

        // Use ActiveMQ.Statistics.Destination.> or ActiveMQ.Statistics.Subscription
        // to display different AMQ statistics
        final Queue testQueue = session.createQueue(AMQ_STATISTICS_QUEUE_NAME);
        final MessageProducer messageProducer = session.createProducer(testQueue);
        final Message msg = session.createMessage();
        msg.setJMSReplyTo(replyTo);
        messageProducer.send(msg);

        final MapMessage reply = (MapMessage) messageConsumer.receive();

        messageProducer.close();
        messageConsumer.close();
        session.close();

        if (reply != null) {
            return reply;
        } else {
            throw new F4MAnalyticsFatalErrorException("Invalid AMQStatistic");
        }
    }

    @Override
    public String toInternalTopicPattern(String externalTopic) {
        return externalTopic == null ? null
                : externalTopic.replace(config.getProperty(AnalyticsConfig.WILDCARD_SEPARATOR).toCharArray()[0],
                ActiveMqBrokerManager.DEFAULT_WILDCARD_SEPARATOR.toCharArray()[0]);
    }

    @Override
    public String toExternalTopicPattern(String internalTopic) {
        return internalTopic == null ? null
                : internalTopic.replace(ActiveMqBrokerManager.DEFAULT_WILDCARD_SEPARATOR.toCharArray()[0],
                config.getProperty(AnalyticsConfig.WILDCARD_SEPARATOR).toCharArray()[0]);
    }

    @Override
    public Session getSession() throws JMSException {
        final Connection conn = getConnection();
        return conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

}

