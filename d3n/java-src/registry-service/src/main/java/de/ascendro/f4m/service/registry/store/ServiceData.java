package de.ascendro.f4m.service.registry.store;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import de.ascendro.f4m.service.registry.model.monitor.ServiceStatistics;
import de.ascendro.f4m.service.session.SessionWrapper;

public class ServiceData {
	private String serviceName;
	private String uri;
	private Set<String> serviceNamespaces;

	private SessionWrapper session;
	private ZonedDateTime lastHeartbeatResponse;
	private ServiceStatistics latestStatistics;

	public ServiceData(String serviceName, String uri, Collection<String> serviceNamespaces, SessionWrapper session) {
		this.serviceName = serviceName;
		this.uri = uri;
		this.serviceNamespaces = new HashSet<>(serviceNamespaces);
		this.session = session;
	}

	public String getServiceName() {
		return serviceName;
	}
	
	public String getUri() {
		return uri;
	}
	
	public Set<String> getServiceNamespaces() {
		return serviceNamespaces;
	}

	public SessionWrapper getSession() {
		return session;
	}

	public ZonedDateTime getLastHeartbeatResponse() {
		return lastHeartbeatResponse;
	}

	public void setLastHeartbeatResponse(ZonedDateTime lastHeartbeatResponse) {
		this.lastHeartbeatResponse = lastHeartbeatResponse;
	}

	public ServiceStatistics getLatestStatistics() {
		return latestStatistics;
	}

	public void setLatestStatistics(ServiceStatistics latestStatistics) {
		this.latestStatistics = latestStatistics;
	}
}
