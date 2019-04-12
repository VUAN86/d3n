package de.ascendro.f4m.service.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.spy;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;

public class EventServiceInformationTest {

	private EventServiceListInformation eventServiceInformation;

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testSetJmsPort() {
		eventServiceInformation = spy(new EventServiceListInformation());

		final String uri = "wss://fake:678";
		final Long jmsPort = 859L;

		eventServiceInformation.setJmsPort(uri, jmsPort);
		assertEquals(jmsPort, eventServiceInformation.getJmsPort(uri));
		assertNull(eventServiceInformation.getServiceName());
	}
	
	@Test
	public void testCreateWithServiceAndPort() {		
		final String uri = "wss://test:678";
		
		final String serviceName = "testService";
		eventServiceInformation = spy(new EventServiceListInformation(serviceName, uri, Collections.emptyList()));
	
		assertNull(eventServiceInformation.getJmsPort(uri));
		assertEquals(serviceName, eventServiceInformation.getServiceName());
	}
	

	@Test
	public void testCreateBasedOnServiceInfo() {		
		final String uri = "wss://test:678";		
		final String serviceName = "testService";
		
		final ServiceConnectionInformation info = new ServiceConnectionInformation(serviceName, uri, Collections.emptyList());
		eventServiceInformation = spy(new EventServiceListInformation(info));
	
		assertNull(eventServiceInformation.getJmsPort(uri));
		assertEquals(serviceName, eventServiceInformation.getServiceName());
	}

}
