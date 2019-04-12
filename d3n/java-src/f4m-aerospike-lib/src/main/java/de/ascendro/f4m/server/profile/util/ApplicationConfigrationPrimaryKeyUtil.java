package de.ascendro.f4m.server.profile.util;

import javax.inject.Inject;

import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.service.config.Config;

public class ApplicationConfigrationPrimaryKeyUtil extends PrimaryKeyUtil<String> {
	public static final String AEROSPIKE_KEY_PREFIX_APPCONFIG = "appConfig";

	@Inject
	public ApplicationConfigrationPrimaryKeyUtil(Config config) {
		super(config);
	}

	public String createPrimaryKey(String tenantId, String appId) {
		return AEROSPIKE_KEY_PREFIX_APPCONFIG + KEY_ITEM_SEPARATOR + tenantId + KEY_ITEM_SEPARATOR + appId;
	}

	public String createDefaultTenantConfigPrimaryKey(String tenantId) {
		return AEROSPIKE_KEY_PREFIX_APPCONFIG + KEY_ITEM_SEPARATOR + tenantId;
	}
}
