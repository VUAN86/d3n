package de.ascendro.f4m.service.util.register;

import javax.inject.Inject;

import de.ascendro.f4m.service.registry.model.monitor.ServiceStatistics;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class HeartbeatMonitoringProvider {
	private Long lastHeartbeatTimestamp;

	@Inject
	public HeartbeatMonitoringProvider(ServiceMonitoringRegister serviceMonitoringRegister) {
		serviceMonitoringRegister.register(this::updateStatistics);
	}

	public void markHeartbeatResponseSent() {
		lastHeartbeatTimestamp = DateTimeUtil.getUTCTimestamp() / 1000;
	}

	protected void updateStatistics(ServiceStatistics statistics) {
		statistics.setLastHeartbeatTimestamp(lastHeartbeatTimestamp);
	}
}
