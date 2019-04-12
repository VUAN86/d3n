package de.ascendro.f4m.service.registry.model;

import java.util.List;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class ServiceRegistryRegisterRequest implements JsonMessageContent {
	private String serviceName;
	private String uri;
	private List<String> serviceNamespaces;

	public ServiceRegistryRegisterRequest() {
	}

	public ServiceRegistryRegisterRequest(String serviceName, String uri, List<String> serviceNamespaces) {
		this.serviceName = serviceName;
		this.uri = uri;
		this.serviceNamespaces = serviceNamespaces;
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

	public List<String> getServiceNamespaces() {
		return serviceNamespaces;
	}
	
	public void setServiceNamespaces(List<String> serviceNamespaces) {
		this.serviceNamespaces = serviceNamespaces;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ServiceRegistryRegisterRequest [serviceName=");
		builder.append(serviceName);
		builder.append(", uri=");
		builder.append(uri);
		builder.append(", serviceNamespaces=");
		builder.append(serviceNamespaces);
		builder.append("]");
		return builder.toString();
	}
}
