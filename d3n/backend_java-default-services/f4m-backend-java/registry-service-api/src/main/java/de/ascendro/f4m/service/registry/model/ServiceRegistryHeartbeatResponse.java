package de.ascendro.f4m.service.registry.model;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class ServiceRegistryHeartbeatResponse implements JsonMessageContent {
	private String status;

	public ServiceRegistryHeartbeatResponse() {
	}

	public ServiceRegistryHeartbeatResponse(String status) {
		this.status = status;
	}

	public ServiceRegistryHeartbeatResponse(ServiceStatus status) {
		setStatus(status);
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public void setStatus(ServiceStatus status) {
		this.status = status.name();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ServiceRegistryHeartbeatResponse [status=");
		builder.append(status);
		builder.append("]");
		return builder.toString();
	}

}
