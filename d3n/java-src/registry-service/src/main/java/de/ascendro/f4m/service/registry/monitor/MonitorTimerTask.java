package de.ascendro.f4m.service.registry.monitor;

import java.util.List;
import java.util.TimerTask;

import javax.inject.Inject;

import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.logging.MonitoringInformation;
import de.ascendro.f4m.service.registry.model.monitor.ServiceStatistics;
import de.ascendro.f4m.service.registry.store.ServiceRegistry;
import de.ascendro.f4m.service.util.register.MonitoringTimerTask;

public class MonitorTimerTask extends TimerTask {
	private final ServiceRegistry serviceRegistry;
	private final LoggingUtil loggingUtil;
	private final MonitoringTimerTask monitoringTimerTask;

	@Inject
	public MonitorTimerTask(MonitoringTimerTask monitoringTimerTask, ServiceRegistry serviceRegistry, LoggingUtil loggingUtil) {
		this.serviceRegistry = serviceRegistry;
		this.loggingUtil = loggingUtil;
		this.monitoringTimerTask = monitoringTimerTask;
	}

	@Override
	public void run() {
		loggingUtil.saveBasicInformationInThreadContext();
		List<ServiceStatistics> serviceStats = serviceRegistry.getServiceStatisticsList();
		serviceStats.add(monitoringTimerTask.prepareServiceStatisticsData());
		loggingUtil.logServiceStatistics(new MonitoringInformation(serviceStats));
	}

}
