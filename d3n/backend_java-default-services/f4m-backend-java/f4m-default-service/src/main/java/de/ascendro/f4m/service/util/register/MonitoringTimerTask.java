package de.ascendro.f4m.service.util.register;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TimerTask;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfig;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.gateway.GatewayMessageTypes;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.registry.model.monitor.MonitoringConnectionStatus;
import de.ascendro.f4m.service.registry.model.monitor.MonitoringDbConnectionInfo;
import de.ascendro.f4m.service.registry.model.monitor.MonitoringServiceConnectionInfo;
import de.ascendro.f4m.service.registry.model.monitor.ServiceStatistics;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.session.pool.SessionPool;
import de.ascendro.f4m.service.session.pool.SessionStore;

public class MonitoringTimerTask extends TimerTask {
	private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringTimerTask.class);
	
	private Config config;
	private ServiceRegistryClient serviceRegistryClient;
	private SessionPool sessionPool;
	private ServiceMonitoringRegister serviceMonitoringRegister;
	private LoggingUtil loggingUtil;

	@Inject
	public MonitoringTimerTask(Config config, ServiceRegistryClient serviceRegistryClient,
			SessionPool sessionPool, ServiceMonitoringRegister serviceMonitoringRegister, LoggingUtil loggingUtil) {
		this.config = config;
		this.serviceRegistryClient = serviceRegistryClient;
		this.sessionPool = sessionPool;
		this.serviceMonitoringRegister = serviceMonitoringRegister;
		this.loggingUtil = loggingUtil;
	}

	@Override
	public void run() {
		loggingUtil.saveBasicInformationInThreadContext();
		ServiceStatistics statisticsData = prepareServiceStatisticsData();
		serviceRegistryClient.pushMonitoringServiceStatistics(statisticsData);
	}

	public ServiceStatistics prepareServiceStatisticsData() {
		Stopwatch stopwatch = Stopwatch.createStarted();
		ServiceStatistics statistics = new ServiceStatistics();
		prepareServiceName(statistics);
		prepareMXBeanInfo(statistics);
		prepareConnectionInfo(statistics);
		prepareMissingDependencyInfo(statistics);
		prepareDefaultDbConnectionInfo(statistics);
		serviceMonitoringRegister.updateServiceStatisticsFromAllProviders(statistics);
		LOGGER.trace("Service statistics prepared in {} with result", stopwatch.stop(), statistics);
		return statistics;
	}

	protected void prepareServiceName(ServiceStatistics statistics) {
		statistics.setServiceName(config.getProperty(F4MConfig.SERVICE_NAME));
		try {
			statistics.setUri(((F4MConfigImpl)config).getServiceURI().toString());
		} catch (URISyntaxException e) {
			LOGGER.error("Could not parse service URI", e);
		}
	}

	protected void prepareMXBeanInfo(ServiceStatistics statistics) {
		RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
		statistics.setUptime(rb.getUptime());
		
		MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
		//used = represents the amount of memory currently used (in bytes).
		//committed = represents the amount of memory (in bytes) that is guaranteed to be available for use by the Java virtual machine.
		//That is - commit memory is what the JVM reserved for itself from the OS, which can be seen from OS, therefore we provide really used amount.
		long heapMemoryUsage = memoryMXBean.getHeapMemoryUsage().getUsed();
		long nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage().getUsed();
		LOGGER.trace("Memory usage heap {}, non-heap {}", heapMemoryUsage, nonHeapMemoryUsage);
		statistics.setMemoryUsage(heapMemoryUsage + nonHeapMemoryUsage);
		
		OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
		if (operatingSystemMXBean instanceof com.sun.management.OperatingSystemMXBean) {
			com.sun.management.OperatingSystemMXBean sunOsBean = (com.sun.management.OperatingSystemMXBean) operatingSystemMXBean;
			double cpuLoad = sunOsBean.getProcessCpuLoad();
			if (cpuLoad >= 0) { // If the Java Virtual Machine recent CPU usage is not available, the method returns a negative value
				statistics.setCpuUsage(BigDecimal.valueOf(cpuLoad * 100.0));
			}
		} else {
			LOGGER.trace("Cannot provide cpuUsage for monitoring - {} was not instance of {}", operatingSystemMXBean,
					com.sun.management.OperatingSystemMXBean.class);
		}
	}

	protected void prepareConnectionInfo(ServiceStatistics statistics) {
		Collection<SessionStore> sessionStores = sessionPool.allSessionStores();
		statistics.setConnectionsFromService(new ArrayList<>(sessionStores.size()));
		statistics.setConnectionsToService(new ArrayList<>(sessionStores.size()));
		int countGatewayConnections = 0;
		int countGatewayUserSessions = 0;
		boolean clientCacheEnabled = isClientCacheEnabled();
		for (SessionStore store : sessionStores) {
			MonitoringServiceConnectionInfo connectionInfo = new MonitoringServiceConnectionInfo();
			SessionWrapper session = store.getSessionWrapper();
			InetSocketAddress address = session.getRemoteAddress();
			connectionInfo.setAddress(address.getHostString());
			connectionInfo.setPort(address.getPort());
			if (session.isClient()) {
				connectionInfo.setServiceName(session.getServiceConnectionInformation().getServiceName());
				statistics.getConnectionsFromService().add(connectionInfo);
			} else {
				connectionInfo.setServiceName(session.getConnectedClientServiceName());
				statistics.getConnectionsToService().add(connectionInfo);
				if (GatewayMessageTypes.SERVICE_NAME.equals(connectionInfo.getServiceName())) {
					countGatewayConnections++;
					if (clientCacheEnabled) {
						countGatewayUserSessions += store.getClientCount(); //should we count also forwarded sessions from other service? probably not...
					}
				}
			}
		}
		statistics.setCountConnectionsFromService(statistics.getConnectionsFromService().size());
		statistics.setCountConnectionsToService(statistics.getConnectionsToService().size());
		
		statistics.setCountGatewayConnections(countGatewayConnections);
		if (clientCacheEnabled) {
			statistics.setCountSessionsToService(countGatewayUserSessions);
		}
	}
	
	private boolean isClientCacheEnabled() {
    	final long clientsCacheTimeToLive = config.getPropertyAsLong(F4MConfigImpl.USERS_CACHE_TIME_TO_LIVE);
    	return clientsCacheTimeToLive > 0;
	}

	protected void prepareMissingDependencyInfo(ServiceStatistics statistics) {
		List<String> missingDependentServiceNames = new ArrayList<>();
		for (String serviceName : serviceRegistryClient.getDependentServiceNames()) {
			if (serviceRegistryClient.getServiceConnInfoFromStore(serviceName) == null) {
				missingDependentServiceNames.add(serviceName);
			}
		}
		statistics.setMissingDependentServices(missingDependentServiceNames);
	}

	protected void prepareDefaultDbConnectionInfo(ServiceStatistics statistics) {
		MonitoringDbConnectionInfo connectionsToDb = new MonitoringDbConnectionInfo();
		statistics.setConnectionsToDb(connectionsToDb);
		connectionsToDb.setAerospike(MonitoringConnectionStatus.NA);
		connectionsToDb.setElastic(MonitoringConnectionStatus.NA);
		connectionsToDb.setMysql(MonitoringConnectionStatus.NA);
		connectionsToDb.setSpark(MonitoringConnectionStatus.NA); //TODO: implement spark connection checking for analytics-service
	}
}
