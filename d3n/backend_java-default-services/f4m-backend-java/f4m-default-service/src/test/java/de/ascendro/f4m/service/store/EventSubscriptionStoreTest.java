package de.ascendro.f4m.service.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.event.store.EventSubscription;
import de.ascendro.f4m.service.event.store.EventSubscriptionStore;
import de.ascendro.f4m.service.logging.LoggingUtil;

public class EventSubscriptionStoreTest {

	private final static Long SUBSCRIPTION_ID_1 = 100L;
	private final static Long SUBSCRIPTION_ID_2 = 200L;
	private final static String TOPIC_1 = "topic 1";
	private final static String TOPIC_2 = "topic 2";
	private final static String CLIENT_ID_1 = "client_id_1";
	private final static String CLIENT_ID_2 = "client_id_2";

	@Mock
	private Config config;
	@Mock
	private LoggingUtil loggingUtil;

	private EventSubscriptionStore store;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		this.store = new EventSubscriptionStore(config, EventSubscription::new, loggingUtil);
	}

	@Test
	public void testUnregisterSubscription() {
		store.addSubscriber(false, null, TOPIC_1, CLIENT_ID_1);
		store.setSubscriptionId(false, null, TOPIC_1, SUBSCRIPTION_ID_1);
		store.addSubscriber(false, null, TOPIC_2, CLIENT_ID_2);
		store.setSubscriptionId(false, null, TOPIC_2, SUBSCRIPTION_ID_2);
		assertEquals(2, store.getSubscriptions().size());

		store.unregisterSubscription(TOPIC_1);
		assertEquals(1, store.getSubscriptions().size());
		assertNull(store.getSubscription(TOPIC_1));
		assertNotNull(store.getSubscription(TOPIC_2));
	}

	@Test
	public void testAddSubscriber() {
		store.addSubscriber(false, null, TOPIC_1, CLIENT_ID_1);
		store.addSubscriber(false, null, TOPIC_1, CLIENT_ID_2);

		EventSubscription subscription = store.getSubscription(TOPIC_1);
		assertEquals(2, subscription.getClients().size());
		assertTrue(subscription.getClients().contains(CLIENT_ID_1));
		assertTrue(subscription.getClients().contains(CLIENT_ID_2));
	}

	@Test
	public void testGetSubscriptionIdOfNotExistingTopic() {
		assertNull(store.getSubscriptionId(TOPIC_1));
	}

	@Test
	public void testGetSubscriptionIdOfTopicWithoutSubscriptionId() {
		store.addSubscriber(false, null, TOPIC_1, CLIENT_ID_1);
		assertNull(store.getSubscriptionId(TOPIC_1));
	}

	@Test
	public void testSetAndGetSubscriptionId() {
		store.addSubscriber(false, null, TOPIC_1, CLIENT_ID_1);
		store.setSubscriptionId(false, null, TOPIC_1, SUBSCRIPTION_ID_1);
		assertEquals(new Long(SUBSCRIPTION_ID_1), store.getSubscriptionId(TOPIC_1));
	}

	@Test
	public void testGetSubscribtion() {
		store.addSubscriber(false, null, TOPIC_1, CLIENT_ID_1);
		store.addSubscriber(false, null, TOPIC_1, CLIENT_ID_2);
		store.setSubscriptionId(false, null, TOPIC_1, SUBSCRIPTION_ID_1);

		EventSubscription subscription = store.getSubscription(TOPIC_1);
		assertEquals(TOPIC_1, subscription.getTopic());
		assertEquals(SUBSCRIPTION_ID_1, subscription.getSubscriptionId());
		assertEquals(2, subscription.getClients().size());
		assertTrue(subscription.getClients().contains(CLIENT_ID_1));
		assertTrue(subscription.getClients().contains(CLIENT_ID_2));
	}

	@Test
	public void testRemoveSubsciber() {
		store.addSubscriber(false, null, TOPIC_1, CLIENT_ID_1);
		store.addSubscriber(false, null, TOPIC_1, CLIENT_ID_2);

		store.removeSubscriber(TOPIC_1, CLIENT_ID_1);

		EventSubscription subscription = store.getSubscription(TOPIC_1);
		assertEquals(1, subscription.getClients().size());
		assertFalse(subscription.getClients().contains(CLIENT_ID_1));
		assertTrue(subscription.getClients().contains(CLIENT_ID_2));
	}

}
