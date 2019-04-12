package de.ascendro.f4m.service.event.activemq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.TextMessage;

import org.apache.activemq.store.memory.MemoryPersistenceAdapter;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.event.config.EventConfig;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;
import de.ascendro.f4m.service.util.KeyStoreUtil;

public class BrokerNetworkActiveMQTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(BrokerNetworkActiveMQTest.class);

	private static final int QUEUE_COUNT = 3;
	private static final String ACTIVE_MQ_TOPIC = "GAME/ABC";
	private static final String ACTIVE_MQ_VIRTUAL_TOPIC = "GAME/VIRTUALABC";
	private static final String ACTIVE_MQ_TEXT_MESSAGE = "Aus Lorepsum ipsum lores aus Lorepsum ipsum lores aus " +
			"Lorepsum ipsum lores aus Lorepsum ipsum lores aus Lorepsum ipsum lores aus Lorepsum ipsum lores aus" +
			"Lorepsum ipsum lores aus Lorepsum ipsum lores aus Lorepsum ipsum lores aus Lorepsum ipsum lores aus " +
			"Lorepsum ipsum lores aus Lorepsum ipsum lores auss Lorepsum ipsum lores aus Lorepsum ipsum lores aus " +
			"Lorepsum ipsum lores aus Lorepsum";
	private static final String CONSUMER_NAME = "testConsumer";
	
	private final BrokerNetworkActiveMQ[] activeMQs = new BrokerNetworkActiveMQ[QUEUE_COUNT];
	private final EventConfig[] eventConfigs = new EventConfig[QUEUE_COUNT];
	private final int[] activeMqPorts = new int[QUEUE_COUNT];
	private final MessageConsumer[] consumers = new MessageConsumer[QUEUE_COUNT * 2];

	@ClassRule
	public static TemporaryFolder keystoreFolder = new TemporaryFolder();

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		KeyStoreTestUtil.initKeyStore(keystoreFolder);
	}

	@Before
	public void setUp() throws Exception {
		initActiveMQs();
		startAllEmbeddedActiveMQs();
		discoverEachOther();
	}

	@After
	public void tearDown() throws Exception {
		stopAllEmbeddedActiveMQs();
	}

	private void initActiveMQs() throws Exception {
		int activeMqDefaultPort = 9000;
		final Config config = new F4MConfigImpl();
		final KeyStoreUtil keyStoreUtil = new KeyStoreUtil(config);

		for (int i = 0; i < QUEUE_COUNT; i++) {
			activeMqPorts[i] = ++activeMqDefaultPort;

			eventConfigs[i] = new EventConfig();
			eventConfigs[i].setProperty(EventConfig.ACTIVE_MQ_PORT, activeMqPorts[i]);

			activeMQs[i] = new BrokerNetworkActiveMQ(eventConfigs[i], keyStoreUtil);
			activeMQs[i].setPersistent(false);
			activeMQs[i].setPersistenceAdapter(new MemoryPersistenceAdapter());//InMemory ActiveMQ instance
		}
	}

	private void discoverEachOther() throws Exception {
		for (int i = 0; i < QUEUE_COUNT; i++) {
			final List<URI> externalUris = new ArrayList<>(QUEUE_COUNT - 1);
			for (int j = 0; j < QUEUE_COUNT; j++) {
				if (i != j) {
					externalUris.add(activeMQs[j].createExternalUri());
				}
			}
			activeMQs[i].addBrokerIntoNetwork(externalUris.toArray(new URI[QUEUE_COUNT - 1]));
		}
	}

	@Test
	public void testBrokerNetwork() throws Exception {
		registerDurableSubscribersForAllOtherJMS(ACTIVE_MQ_TOPIC, false);
		int randomIndex = new SecureRandom().nextInt(QUEUE_COUNT);
		LOGGER.debug("sending topic from activeMQ[{}]", randomIndex);
		activeMQs[randomIndex].sendTopic(ACTIVE_MQ_TOPIC, false, ACTIVE_MQ_TEXT_MESSAGE, null);

		assertAllConsumersReceivedMessage();
	}

	@Test
	public void testVirtualTopics() throws Exception {
		registerDurableSubscribersForAllOtherJMS(ACTIVE_MQ_VIRTUAL_TOPIC, true);
		int randomIndex = new SecureRandom().nextInt(QUEUE_COUNT);
		LOGGER.debug("sending topic from activeMQ[{}]", randomIndex);
		for (int i = 0 ; i < 4 * QUEUE_COUNT ; i++) { // Send 2x as many messages as there are queues to ensure that every consumer gets some
			activeMQs[randomIndex].sendTopic(ACTIVE_MQ_VIRTUAL_TOPIC, true, String.valueOf(i), null);
		}

		assertAllConsumersReceivedMessage();
		
		// Now, assure that each message was delivered to some consumer
		for (int i = 0 ; i < 4 * QUEUE_COUNT ; i++) {
			// For each message find the queue it was delivered to
			boolean found = false;
			for (int j = 0 ; j < 2 * QUEUE_COUNT && ! found; j++) {
				final CachedMessageListener cachedMessageListener = getTopicSubscriberMessageListener(j);
				final int finalI = i;
				found = cachedMessageListener.getReceivedMessage().stream().anyMatch(m -> {
					try {
						return ((TextMessage) m).getText().equals(String.valueOf(finalI));
					} catch (JMSException e) {
						return false;
					}
				});
			}
			assertTrue("Message " + i + " not delivered to any queue", found);
		}
		
		// Now, assure that messages were delivered to exactly one consumer
		int count = 0;
		for (int i = 0 ; i < 2 * QUEUE_COUNT ; i++) {
			final CachedMessageListener cachedMessageListener = getTopicSubscriberMessageListener(i);
			count += cachedMessageListener.getReceivedMessage().size();
		}
		assertEquals(4 * QUEUE_COUNT, count);
	}
	
	private void registerDurableSubscribersForAllOtherJMS(String topicName, boolean virtual) throws JMSException {
		int j = 0;
		for (int i = 0; i < QUEUE_COUNT; i++) {
			// Generate 2 consumers on each node
			for (int z = 0 ; z < 2 ; z++) {
				consumers[j] = virtual 
						? activeMQs[i].createVirtualTopicSubscriber(CONSUMER_NAME, topicName, new CachedMessageListener(), j)
						: activeMQs[i].createTopicSubscriber(topicName, new CachedMessageListener(), j);

				// Wait for the subscriber to finish starting up
				final int finalJ = j;
				RetriedAssert.assertWithWait(() -> assertNotNull(getTopicSubscriberMessageListener(finalJ)));
				j++;
			}
		}
	}

	private void assertAllConsumersReceivedMessage() throws InterruptedException {
		for (int i = 0; i < QUEUE_COUNT * 2 ; i++) {
			final int j = i;//copy of the variable for async calls within assert (must be final)
			final int timeout = RetriedAssert.DEFAULT_TIMEOUT_MS;
			LOGGER.debug("Processing item {} out of {} from queue", j, QUEUE_COUNT * 2 - 1);

			LOGGER.debug("Waiting for getTopicSubscriberMessageListener not null, timeout {} ms",
					timeout);
			RetriedAssert.assertWithWait(() -> assertNotNull(getTopicSubscriberMessageListener(j)), timeout);
			final CachedMessageListener cachedMessageListener = getTopicSubscriberMessageListener(j);

			
			LOGGER.debug("Waiting to receive produced message, timeout {} ms", timeout);
			RetriedAssert.assertWithWait(
					() -> assertTrue("ActiveMQ[" + j + "] has no received produced message", ! cachedMessageListener
							.getReceivedMessage().isEmpty()), timeout);
		}
	}

	private CachedMessageListener getTopicSubscriberMessageListener(int i) {
		CachedMessageListener cachedMessageListener = null;
		try {
			cachedMessageListener = (CachedMessageListener) consumers[i].getMessageListener();
		} catch (JMSException e) {
			fail(e.getMessage());
		}
		return cachedMessageListener;
	}

	private void stopAllEmbeddedActiveMQs() throws Exception {
		for (int i = 0; i < QUEUE_COUNT; i++) {
			activeMQs[i].stop();
		}
	}

	private void startAllEmbeddedActiveMQs() throws Exception {
		for (int i = 0; i < QUEUE_COUNT; i++) {
			activeMQs[i].start();
		}
	}

}
