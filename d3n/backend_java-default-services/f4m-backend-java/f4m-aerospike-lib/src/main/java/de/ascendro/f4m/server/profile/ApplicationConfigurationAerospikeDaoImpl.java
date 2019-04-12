package de.ascendro.f4m.server.profile;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeDaoImpl;
import de.ascendro.f4m.server.profile.model.AppConfig;
import de.ascendro.f4m.server.profile.util.ApplicationConfigrationPrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;

public class ApplicationConfigurationAerospikeDaoImpl extends AerospikeDaoImpl<ApplicationConfigrationPrimaryKeyUtil> implements
        ApplicationConfigurationAerospikeDao {
	private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfigurationAerospikeDaoImpl.class);
	private final ApplicationConfigrationPrimaryKeyUtil applicationConfigrationPrimaryKeyUtil;

	@Inject
	public ApplicationConfigurationAerospikeDaoImpl(Config config,
			ApplicationConfigrationPrimaryKeyUtil applicationConfigrationPrimaryKeyUtil,
			JsonUtil jsonUtil, 
			AerospikeClientProvider aerospikeClientProvider) {
		super(config, applicationConfigrationPrimaryKeyUtil, jsonUtil, aerospikeClientProvider);
		this.applicationConfigrationPrimaryKeyUtil = applicationConfigrationPrimaryKeyUtil;
	}

	private String getAppConfigurationAsString(String tenantId, String appId) {
		final String appPrimaryKey;
		if (StringUtils.isEmpty(appId)) {
			appPrimaryKey = applicationConfigrationPrimaryKeyUtil.createDefaultTenantConfigPrimaryKey(tenantId);
		} else {
			appPrimaryKey = applicationConfigrationPrimaryKeyUtil.createPrimaryKey(tenantId, appId);
		}
		String configurationAsString = readJson(getSet(), appPrimaryKey, BLOB_BIN_NAME);
		if (StringUtils.isEmpty(configurationAsString)) {
			LOGGER.error("AppConfig with tenant '{}' and app '{}' not found", tenantId, appId);
			throw new F4MEntryNotFoundException("AppConfig not found");
		}
		return configurationAsString;
	}

	@Override
	public JsonElement getAppConfigurationAsJsonElement(String tenantId, String appId) {
		final String configurationAsString = getAppConfigurationAsString(tenantId, appId);
		return jsonUtil.fromJson(configurationAsString, JsonElement.class);
	}

	@Override
	public AppConfig getAppConfiguration(String tenantId, String appId) {
		final String configurationAsString = getAppConfigurationAsString(tenantId, appId);
		return jsonUtil.fromJson(configurationAsString, AppConfig.class);
	}

	public String getSet() {
		return ApplicationConfigrationPrimaryKeyUtil.AEROSPIKE_KEY_PREFIX_APPCONFIG;
	}
}
