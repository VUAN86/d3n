package de.ascendro.f4m.service.profile.dao;

public interface ApplicationConfigrationAerospikeDao {
	public static final String BLOB_BIN_NAME = "value";

	/**
	 * Select Application Configurations as String
	 *
	 * @param tenantId
	 * 			- Tenant id for the application to select
	 *
	 * @param appId
	 *            - Application id for application to select
	 * @return Application Configuration JSON blob as String
	 */
	String getAppConfiguration(String tenantId, String appId);

	/**
	 * Select Application Configurations as String
	 *
	 * @param tenantId
	 *            - tenantId for default tenant configuration
	 * @return Application Configuration JSON blob as String
	 */
	String getAppConfigurationByTenantId(String tenantId);
}
