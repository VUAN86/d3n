package de.ascendro.f4m.service.event.integration;

import java.net.URI;

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.event.EventServiceStartup;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;

public class DiscoveryRule extends ExternalResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryRule.class);

	private final EventServiceStartup eventServiceStartup;
	private final URI serviceRegistryURI;

	public DiscoveryRule(EventServiceStartup eventServiceStartup, URI serviceRegistryURI) {
		this.eventServiceStartup = eventServiceStartup;
		this.serviceRegistryURI = serviceRegistryURI;

		final Injector injector = eventServiceStartup.getInjector();

		final Config config = injector.getInstance(Config.class);
		config.setProperty(F4MConfigImpl.SERVICE_REGISTRY_URI, serviceRegistryURI.toString());

		injector.getInstance(ServiceRegistryClient.class).refreshServiceRegistryUriQueue();
	}

	@Override
	protected void before() throws Throwable {
		long time = System.currentTimeMillis();
		LOGGER.debug("DiscoveryRule - starting Event Service Discovery procedure");
		getEventServiceStartup().discoverEventServices();
		LOGGER.debug("DiscoveryRule - Event Service Discovery initiated");
		LOGGER.info("Server started up in {} ms", (System.currentTimeMillis() - time));
	}

	@Override
	protected void after() {
		try {
			getEventServiceStartup().unsubscribeFromEventServices();
		} catch (Exception e) {
			//TODO: throw F4MTestException?
			LOGGER.warn("Could not stop service server", e);
		}
	}

	public EventServiceStartup getEventServiceStartup() {
		return eventServiceStartup;
	}

	public URI getServiceRegistryURI() {
		return serviceRegistryURI;
	}
}
