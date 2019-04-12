package de.ascendro.f4m.service.registry.model;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class ServiceRegistryListRequest implements JsonMessageContent {
	private String serviceName;
	private String serviceNamespace;

	public ServiceRegistryListRequest() {
	}

	public ServiceRegistryListRequest(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getServiceNamespace() {
		return serviceNamespace;
	}
	
	public void setServiceNamespace(String serviceNamespace) {
		this.serviceNamespace = serviceNamespace;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ServiceRegistryListRequest [serviceName=");
		builder.append(serviceName);
		builder.append(", serviceNamespace=");
		builder.append(serviceNamespace);
		builder.append("]");
		return builder.toString();
	}
}
