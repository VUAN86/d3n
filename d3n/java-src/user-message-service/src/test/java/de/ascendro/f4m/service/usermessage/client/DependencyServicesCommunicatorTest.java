package de.ascendro.f4m.service.usermessage.client;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class DependencyServicesCommunicatorTest {

	@InjectMocks
	private DependencyServicesCommunicator communicator;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testGatewayUrlNormalization() {
		String expectedDnsUrl = "wss://gateway-dev.dev4m.com:80"; //expected valid value for gateway URL
		assertEquals(expectedDnsUrl, communicator.normalizeWebsocketURL(expectedDnsUrl));
		assertEquals(expectedDnsUrl, communicator.normalizeWebsocketURL("gateway-dev.dev4m.com:80")); //current value for gateway URL
		
		assertEquals("wss://52.57.113.91:80", communicator.normalizeWebsocketURL("52.57.113.91:80"));
	}

}
