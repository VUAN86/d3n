package de.ascendro.f4m.service.usermessage.model;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

/**
 * Object containing mobile device identifier for usage in JSON arrays.
 */
public class DeviceIdInfo implements JsonMessageContent {
	private String deviceUUID;

	public String getDeviceUUID() {
		return deviceUUID;
	}

	public void setDeviceUUID(String deviceUUID) {
		this.deviceUUID = deviceUUID;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DeviceIdInfo [deviceUUID=");
		builder.append(deviceUUID);
		builder.append("]");
		return builder.toString();
	}
}
