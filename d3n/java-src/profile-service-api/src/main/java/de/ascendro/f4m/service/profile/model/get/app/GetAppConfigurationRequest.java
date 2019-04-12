package de.ascendro.f4m.service.profile.model.get.app;

import com.google.gson.JsonElement;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class GetAppConfigurationRequest implements JsonMessageContent {

	/**
	 * Application id to be selected
	 */
	private String appId;

	/**
	 * Id of the user's device
	 */
	private String deviceUUID;

	/**
	 * IMEI of the device
	 */
	private String IMEI;

	/**
	 * User's tenant
	 */
	private String tenantId;

	/**
	 * OneSignal playerID for the device
	 */
	private String oneSignalDeviceId;

	/**
	 * Properties of user's device
	 */
	private JsonElement device;

	public GetAppConfigurationRequest() {
	}

	public GetAppConfigurationRequest(String appId) {
		this.appId = appId;
	}

	public String getDeviceUUID() {
		return deviceUUID;
	}

	public void setDeviceUUID(String deviceUUID) {
		this.deviceUUID = deviceUUID;
	}

	public JsonElement getDevice() {
		return device;
	}

	public void setDevice(JsonElement device) {
		this.device = device;
	}

	public String getAppId() {
		return appId;
	}

	public String getIMEI() {
		return IMEI;
	}

	public void setIMEI(String IMEI) {
		this.IMEI = IMEI;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getOneSignalDeviceId() {
		return oneSignalDeviceId;
	}

	public void setOneSignalDeviceId(String oneSignalDeviceId) {
		this.oneSignalDeviceId = oneSignalDeviceId;
	}
}
