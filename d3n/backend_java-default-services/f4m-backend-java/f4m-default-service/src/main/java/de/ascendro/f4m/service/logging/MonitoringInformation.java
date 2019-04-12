package de.ascendro.f4m.service.logging;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.ascendro.f4m.service.registry.model.monitor.ServiceStatistics;

public class MonitoringInformation {

	private Map<String, ServiceStatistics> monitoringInformation;
	
	public MonitoringInformation(List<ServiceStatistics> serviceStats) {
		monitoringInformation = serviceStats == null ? Collections.emptyMap() 
				: serviceStats.stream().collect(Collectors.toMap(s -> s.getServiceName(), s -> s));
	}
	
	public Map<String, ServiceStatistics> getMonitoringInformation() {
		return monitoringInformation;
	}
	
}
