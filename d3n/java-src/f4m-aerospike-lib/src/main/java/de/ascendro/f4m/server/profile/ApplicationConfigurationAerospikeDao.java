package de.ascendro.f4m.server.profile;

import com.google.gson.JsonElement;

import de.ascendro.f4m.server.profile.model.AppConfig;

public interface ApplicationConfigurationAerospikeDao {
	public static final String BLOB_BIN_NAME = "value";
	
	/**
	 * Select Application Configurations as String
	 * 
	 * @param tenantId
	 *            - tenantId for default tenant configuration
	 * @param appId
	 *            - Application id for application to select (optional to retrieve tenant-only appConfig)
	 * @return Application Configuration JSON blob as JsonElement
	 */
	JsonElement getAppConfigurationAsJsonElement(String tenantId, String appId);

	/**
	 * Select Application Configuration as Java object (not all attributes available)
	 * 
	 * @param tenantId
	 *            - tenantId for default tenant configuration
	 * @param appId
	 *            - Application id for application to select (optional to retrieve tenant-only default appConfig)
	 * @return
	 */
	AppConfig getAppConfiguration(String tenantId, String appId);

}
