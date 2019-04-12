package de.ascendro.f4m.service.registry.model;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

/**
 * Service unregister event published via Event Service to all subscribers
 *
 */
public class ServiceRegistryUnregisterEvent implements JsonMessageContent {
	private String serviceName;
	private String uri;

	public ServiceRegistryUnregisterEvent() {
	}

	public ServiceRegistryUnregisterEvent(String serviceName, String uri) {
		this.serviceName = serviceName;
		this.uri = uri;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
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
		builder.append("ServiceRegistryUnregisterEvent [serviceName=");
		builder.append(serviceName);
		builder.append(", uri=");
		builder.append(uri);
		builder.append("]");
		return builder.toString();
	}
}
