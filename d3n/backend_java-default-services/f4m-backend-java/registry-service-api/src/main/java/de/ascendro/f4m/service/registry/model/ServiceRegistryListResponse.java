package de.ascendro.f4m.service.registry.model;

import java.util.List;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class ServiceRegistryListResponse implements JsonMessageContent {
	private List<ServiceConnectionInformation> services;

	public List<ServiceConnectionInformation> getServices() {
		return services;
	}

	public void setServices(List<ServiceConnectionInformation> services) {
		this.services = services;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ServiceRegistryListResponse [services=");
		builder.append(services);
		builder.append("]");
		return builder.toString();
	}

}
