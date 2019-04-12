package de.ascendro.f4m.service.gateway.model;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class GatewaySendUserMessageResponse implements JsonMessageContent {
	public enum Status {
		SUCCESS, FAIL
	}

	private Status deliveryStatus;
	private String deviceUUID;

	public String getDeviceUUID() {
		return deviceUUID;
	}

	public void setDeviceUUID(String deviceUUID) {
		this.deviceUUID = deviceUUID;
	}

	public Status getDeliveryStatus() {
		return deliveryStatus;
	}

	public void setDeliveryStatus(Status deliveryStatus) {
		this.deliveryStatus = deliveryStatus;
	}
}
