package de.ascendro.f4m.service.registry.model;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonRequiredNullable;

public class ServiceConnectionInformation implements JsonMessageContent {
	private String serviceName;

	@JsonRequiredNullable
	private String uri;
	
	private List<String> serviceNamespaces;

	public ServiceConnectionInformation() {
	}

	public ServiceConnectionInformation(String serviceName, String uri, List<String> serviceNamespaces) {
		this.serviceName = serviceName;
		this.uri = uri;
		this.serviceNamespaces = serviceNamespaces;
	}
	
	public ServiceConnectionInformation(String serviceName, String uri, String serviceNamespace) {
		this.serviceName = serviceName;
		this.uri = uri;
		this.serviceNamespaces = serviceNamespace != null ? Arrays.asList(serviceNamespace): Collections.emptyList();
	}
	
	public ServiceConnectionInformation(String serviceName, URI uri, String serviceNamespace){
		this(serviceName, uri.toString(), serviceNamespace);
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
		builder.append("ServiceInformation [serviceName=");
		builder.append(serviceName);
		builder.append(", uri=");
		builder.append(uri);
		builder.append(", serviceNamespaces=");
		builder.append(serviceNamespaces);
		builder.append("]");
		return builder.toString();
	}
}
