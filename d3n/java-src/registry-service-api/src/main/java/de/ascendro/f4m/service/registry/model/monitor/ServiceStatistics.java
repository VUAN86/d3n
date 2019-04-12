package de.ascendro.f4m.service.registry.model.monitor;

import java.math.BigDecimal;
import java.util.List;

public class ServiceStatistics {
	private String serviceName;
	private String uri;
	private Long uptime;
	private Long memoryUsage;
	private BigDecimal cpuUsage;
	private Integer countGatewayConnections;
	private Integer countConnectionsToService;
	private Integer countConnectionsFromService;
	private Integer countSessionsToService;
	private Long lastHeartbeatTimestamp;
	private List<String> missingDependentServices;
	private MonitoringDbConnectionInfo connectionsToDb;
	private List<MonitoringServiceConnectionInfo> connectionsFromService;
	private List<MonitoringServiceConnectionInfo> connectionsToService;
	
	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public Long getUptime() {
		return uptime;
	}

	public void setUptime(Long uptime) {
		this.uptime = uptime;
	}

	public Long getMemoryUsage() {
		return memoryUsage;
	}

	public void setMemoryUsage(Long memoryUsage) {
		this.memoryUsage = memoryUsage;
	}

	public BigDecimal getCpuUsage() {
		return cpuUsage;
	}

	public void setCpuUsage(BigDecimal cpuUsage) {
		this.cpuUsage = cpuUsage;
	}

	public Integer getCountGatewayConnections() {
		return countGatewayConnections;
	}

	public void setCountGatewayConnections(Integer countGatewayConnections) {
		this.countGatewayConnections = countGatewayConnections;
	}

	public Integer getCountConnectionsToService() {
		return countConnectionsToService;
	}

	public void setCountConnectionsToService(Integer countConnectionsToService) {
		this.countConnectionsToService = countConnectionsToService;
	}

	public Integer getCountConnectionsFromService() {
		return countConnectionsFromService;
	}

	public void setCountConnectionsFromService(Integer countConnectionsFromService) {
		this.countConnectionsFromService = countConnectionsFromService;
	}

	public Integer getCountSessionsToService() {
		return countSessionsToService;
	}

	public void setCountSessionsToService(Integer countSessionsToService) {
		this.countSessionsToService = countSessionsToService;
	}

	public Long getLastHeartbeatTimestamp() {
		return lastHeartbeatTimestamp;
	}

	public void setLastHeartbeatTimestamp(Long lastHeartbeatTimestamp) {
		this.lastHeartbeatTimestamp = lastHeartbeatTimestamp;
	}

	public List<String> getMissingDependentServices() {
		return missingDependentServices;
	}

	public void setMissingDependentServices(List<String> missingDependentServices) {
		this.missingDependentServices = missingDependentServices;
	}

	public MonitoringDbConnectionInfo getConnectionsToDb() {
		return connectionsToDb;
	}

	public void setConnectionsToDb(MonitoringDbConnectionInfo connectionsToDb) {
		this.connectionsToDb = connectionsToDb;
	}

	public List<MonitoringServiceConnectionInfo> getConnectionsFromService() {
		return connectionsFromService;
	}

	public void setConnectionsFromService(List<MonitoringServiceConnectionInfo> connectionsFromService) {
		this.connectionsFromService = connectionsFromService;
	}

	public List<MonitoringServiceConnectionInfo> getConnectionsToService() {
		return connectionsToService;
	}

	public void setConnectionsToService(List<MonitoringServiceConnectionInfo> connectionsToService) {
		this.connectionsToService = connectionsToService;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ServiceStatistics [serviceName=");
		builder.append(serviceName);
		builder.append(", uri=");
		builder.append(uri);
		builder.append(", uptime=");
		builder.append(uptime);
		builder.append(", memoryUsage=");
		builder.append(memoryUsage);
		builder.append(", cpuUsage=");
		builder.append(cpuUsage);
		builder.append(", countGatewayConnections=");
		builder.append(countGatewayConnections);
		builder.append(", countConnectionsToService=");
		builder.append(countConnectionsToService);
		builder.append(", countConnectionsFromService=");
		builder.append(countConnectionsFromService);
		builder.append(", countSessionsToService=");
		builder.append(countSessionsToService);
		builder.append(", lastHeartbeatTimestamp=");
		builder.append(lastHeartbeatTimestamp);
		builder.append(", missingDependentServices=");
		builder.append(missingDependentServices);
		builder.append(", connectionsToDb=");
		builder.append(connectionsToDb);
		builder.append(", connectionsFromService=");
		builder.append(connectionsFromService);
		builder.append(", connectionsToService=");
		builder.append(connectionsToService);
		builder.append("]");
		return builder.toString();
	}
}
