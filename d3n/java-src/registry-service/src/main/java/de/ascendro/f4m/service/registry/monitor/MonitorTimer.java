package de.ascendro.f4m.service.registry.monitor;

import java.util.Timer;

import javax.annotation.PreDestroy;
import javax.inject.Inject;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.registry.config.ServiceRegistryConfig;

public class MonitorTimer extends Timer {
	private final Config config;
	private final MonitorTimerTask task;

	@Inject
	public MonitorTimer(Config config, MonitorTimerTask task) {
		super("monitor");
		this.config = config;
		this.task = task;
	}

	public void startMonitor() {
		schedule(task, config.getPropertyAsLong(ServiceRegistryConfig.MONITOR_START_DELAY),
				config.getPropertyAsLong(ServiceRegistryConfig.MONITOR_START_INTERVAL));
	}

	@PreDestroy
	public void stopTimer() {
		this.cancel();
	}
}
