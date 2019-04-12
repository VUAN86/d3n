package de.ascendro.f4m.service.util.register;

import java.util.Timer;

import javax.annotation.PreDestroy;
import javax.inject.Inject;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;

public class MonitoringTimer extends Timer {
	private Config config;
	private MonitoringTimerTask task;

	@Inject
	public MonitoringTimer(Config config, MonitoringTimerTask task) {
		super("monitoring-push-timer");
		this.config = config;
		this.task = task;
	}

	public void startMonitoring() {
		schedule(task, config.getPropertyAsLong(F4MConfigImpl.MONITORING_START_DELAY),
				config.getPropertyAsLong(F4MConfigImpl.MONITORING_INTERVAL));
	}
	
	@PreDestroy
	public void stopTimer() {
		this.cancel();
	}
}
