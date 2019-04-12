package de.ascendro.f4m.service.gateway.model.key;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class InstanceDisconnectEvent implements JsonMessageContent {
	private String serviceName;

	public InstanceDisconnectEvent() {
	}

	public InstanceDisconnectEvent(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
}
