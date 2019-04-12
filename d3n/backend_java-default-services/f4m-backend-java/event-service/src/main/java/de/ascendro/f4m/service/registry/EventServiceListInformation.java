package de.ascendro.f4m.service.registry;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;

/**
 * Additional Event Service information: ActiveMQ (jms) port. Stores all Event Services JMS ports.
 */
public class EventServiceListInformation extends ServiceConnectionInformation {
	private static final long JMS_PORT_UNKNOWN = -1L;
	private final Map<String, Long> jmsPorts = new ConcurrentHashMap<>();

	public EventServiceListInformation() {
	}
	
	public EventServiceListInformation(ServiceConnectionInformation serviceConnectionInformation) {
		super(serviceConnectionInformation.getServiceName(), serviceConnectionInformation.getUri(), serviceConnectionInformation.getServiceNamespaces());
	}

	public EventServiceListInformation(String serviceName, String uri, List<String> serviceNamespaces) {
		super(serviceName, uri, serviceNamespaces);
	}

	public void setJmsPort(String eventServiceURI, long jmsPort) {
		jmsPorts.put(eventServiceURI, jmsPort);
	}

	public Long getJmsPort(String eventServiceURI) {
		Long port = jmsPorts.get(eventServiceURI);
		if (port != null && port == JMS_PORT_UNKNOWN) {
			port = null;
		}
		return port;
	}
	
	public void addUri(String eventServiceURI) {
		if (!jmsPorts.containsKey(eventServiceURI)) {
			jmsPorts.put(eventServiceURI, JMS_PORT_UNKNOWN);
		}
	}

	public Set<String> getUris() {
		return jmsPorts.keySet();
	}
}
