package de.ascendro.f4m.service.event;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.inject.Injector;
import com.google.inject.Stage;
import com.mycila.guice.ext.closeable.CloseableInjector;

import de.ascendro.f4m.service.config.Config;

public class EventServiceStartupTest {

	@Mock
	private Injector injector;
	
	@Mock
	private CloseableInjector closeableInjector;

	@Mock
	private Config config;

	private EventServiceStartup eventServiceStartup;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		when(injector.getInstance(CloseableInjector.class)).thenReturn(closeableInjector);
		doNothing().when(closeableInjector).close();

		eventServiceStartup = spy(new EventServiceStartup(Stage.PRODUCTION) {
			@Override
			public Injector createInjector(Stage stage) {
				return injector;
			}
		});
	}

	@Test
	public void testStart() throws Exception {
		doNothing().when(eventServiceStartup).startActiveMQ();
		doNothing().when(eventServiceStartup).discoverEventServices();

		when(injector.getInstance(Config.class)).thenReturn(config);

		doNothing().when(eventServiceStartup).startupJetty();
		doNothing().when(eventServiceStartup).register();
		doNothing().when(eventServiceStartup).discoverDependentServices();
		doNothing().when(eventServiceStartup).startMonitoring();

		eventServiceStartup.start();

		verify(eventServiceStartup, times(1)).startActiveMQ();
		verify(eventServiceStartup, times(1)).discoverEventServices();

		verify(eventServiceStartup, times(1)).startupJetty();
		verify(eventServiceStartup, times(1)).register();
		verify(eventServiceStartup, times(1)).discoverDependentServices();

		assertTrue("Event service does not have any external dependencies excpet all instances of itself",
				eventServiceStartup.getDefaultDependentServiceNames().isEmpty());
	}

	@Test
	public void testStop() throws Exception {		
		doNothing().when(eventServiceStartup).stopActiveMQ();
		doNothing().when(eventServiceStartup).unsubscribeFromEventServices();

		when(injector.getInstance(Config.class)).thenReturn(config);

		doNothing().when(eventServiceStartup).stopJetty();
		doNothing().when(eventServiceStartup).unregister();

		eventServiceStartup.stop();

		verify(eventServiceStartup, times(1)).stopActiveMQ();
		verify(eventServiceStartup, times(1)).unsubscribeFromEventServices();

		verify(eventServiceStartup, times(1)).stopJetty();
		verify(eventServiceStartup, times(1)).unregister();
	}

}
