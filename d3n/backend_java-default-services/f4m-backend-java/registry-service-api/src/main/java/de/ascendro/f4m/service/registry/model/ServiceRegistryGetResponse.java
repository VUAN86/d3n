package de.ascendro.f4m.service.registry.model;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonRequiredNullable;

public class ServiceRegistryGetResponse implements JsonMessageContent {
	@JsonRequiredNullable
	private ServiceConnectionInformation service;

	public ServiceRegistryGetResponse() {
	}

	public ServiceRegistryGetResponse(ServiceConnectionInformation service) {
		this.service = service;
	}

	public ServiceConnectionInformation getService() {
		return service;
	}

	public void setService(ServiceConnectionInformation service) {
		this.service = service;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ServiceRegistryGetResponse [service=");
		builder.append(service);
		builder.append("]");
		return builder.toString();
	}
}
