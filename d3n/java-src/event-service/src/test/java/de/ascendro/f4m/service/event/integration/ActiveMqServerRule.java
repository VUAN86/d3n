package de.ascendro.f4m.service.event.integration;

import java.io.IOException;

import org.apache.activemq.store.memory.MemoryPersistenceAdapter;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.event.EventServiceStartup;
import de.ascendro.f4m.service.event.activemq.EmbeddedActiveMQ;
import de.ascendro.f4m.service.event.config.EventConfig;

public class ActiveMqServerRule extends ExternalResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(ActiveMqServerRule.class);

	private final EventServiceStartup eventServiceStartup;
	private final int activeMqPort;

	public ActiveMqServerRule(EventServiceStartup eventServiceStartup, int activeMqPort, String externalHost) {
		this.eventServiceStartup = eventServiceStartup;
		this.activeMqPort = activeMqPort;

		eventServiceStartup.getInjector().getInstance(Config.class)
				.setProperty(EventConfig.ACTIVE_MQ_PORT, activeMqPort);
		eventServiceStartup.getInjector().getInstance(Config.class)
				.setProperty(EventConfig.ACTIVE_MQ_EXTERNAL_COMMUNICATION_HOST, externalHost);

		switchEmbeddedActiveMqToTemporaryMode();
	}

	protected void switchEmbeddedActiveMqToTemporaryMode() {
		try {
			final EmbeddedActiveMQ activeMq = eventServiceStartup.getInjector().getInstance(EmbeddedActiveMQ.class);
			activeMq.setPersistent(false);
			activeMq.setPersistenceAdapter(new MemoryPersistenceAdapter());
		} catch (IOException e) {
			LOGGER.error("Cannot switch ActiveMQ instante to temporary mode");
		}
	}

	@Override
	protected void before() throws Throwable {
		long time = System.currentTimeMillis();
		LOGGER.debug("ActiveMqServerRule - starting ActiveMQ");
		getEventServiceStartup().startActiveMQ();
		LOGGER.debug("ActiveMqServerRule - ActiveMQ started");
		LOGGER.info("Server started up in {} ms", (System.currentTimeMillis() - time));
	}

	@Override
	protected void after() {
		try {
			getEventServiceStartup().stopActiveMQ();
		} catch (Exception e) {
			//TODO: throw F4MTestException?
			LOGGER.warn("Could not stop service server", e);
		}
	}

	public EventServiceStartup getEventServiceStartup() {
		return eventServiceStartup;
	}

	public int getActiveMqPort() {
		return activeMqPort;
	}

}
