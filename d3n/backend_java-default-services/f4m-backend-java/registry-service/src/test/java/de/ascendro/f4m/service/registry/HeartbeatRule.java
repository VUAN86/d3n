package de.ascendro.f4m.service.registry;

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.integration.rule.JettyServerRule;
import de.ascendro.f4m.service.registry.heartbeat.HeartbeatTimer;

public class HeartbeatRule extends ExternalResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(JettyServerRule.class);

	private HeartbeatTimer heartbeatTimer;

	public HeartbeatRule(HeartbeatTimer heartbeatTimer) {
		this.heartbeatTimer = heartbeatTimer;
	}

	@Override
	protected void before() throws Throwable {
		long time = System.currentTimeMillis();
		LOGGER.debug("HeartbeatRule - starting heartbeat");
		heartbeatTimer.startHeartbeat();
		LOGGER.debug("HeartbeatRule - heartbeat started");
		LOGGER.info("Heartbeat started up in {} ms", (System.currentTimeMillis() - time));
	}

	@Override
	protected void after() {
		try {
			heartbeatTimer.cancel();
		} catch (Exception e) {
			//TODO: throw F4MTestException?
			LOGGER.warn("Could not stop service heartbeat", e);
		}
	}

	public void setHeartbeatTimer(HeartbeatTimer heartbeatTimer) {
		this.heartbeatTimer = heartbeatTimer;
	}

	public HeartbeatTimer getHeartbeatTimer() {
		return heartbeatTimer;
	}
}
