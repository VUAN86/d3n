package de.ascendro.f4m.service.registry.heartbeat;

import java.util.Timer;

import javax.annotation.PreDestroy;
import javax.inject.Inject;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.registry.config.ServiceRegistryConfig;

public class HeartbeatTimer extends Timer {
	private final Config config;
	private final HeartbeatTimerTask task;

	@Inject
	public HeartbeatTimer(Config config, HeartbeatTimerTask task) {
		super("heartbeat");
		this.config = config;
		this.task = task;
	}

	public void startHeartbeat() {
		schedule(task, config.getPropertyAsLong(ServiceRegistryConfig.HEARTBEAT_START_DELAY),
				config.getPropertyAsLong(ServiceRegistryConfig.HEARTBEAT_START_INTERVAL));
	}

	@PreDestroy
	public void stopTimer() {
		this.cancel();
	}
}
