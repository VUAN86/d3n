package de.ascendro.f4m.service.event.activemq;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.service.event.config.EventConfig;
import de.ascendro.f4m.service.util.KeyStoreUtil;

public class BrokerNetworkActiveMQUnitTest {
	@Mock
	private EventConfig config;
	@Mock
	private KeyStoreUtil keyStoreUtil;
	@InjectMocks
	private BrokerNetworkActiveMQ activeMQ;
	
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void testInternalTopicPatternsRegex() throws Exception {
		when(config.getProperty(EventConfig.WILDCARD_SEPARATOR)).thenReturn(".");
		assertEquals("a.b.c", activeMQ.toInternalTopicPattern("a.b.c"));
	}

	@Test
	public void testInternalTopicPatternsSlash() throws Exception {
		when(config.getProperty(EventConfig.WILDCARD_SEPARATOR)).thenReturn("/");
		assertEquals("a.b.c", activeMQ.toInternalTopicPattern("a/b/c"));
	}
}
