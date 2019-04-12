package de.ascendro.f4m.server.achievement.dao;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeOperateDaoImpl;
import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.server.achievement.config.AchievementConfig;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;

public class TenantsWithAchievementsAerospikeDaoImpl extends AerospikeOperateDaoImpl<PrimaryKeyUtil<String>>
		implements TenantsWithAchievementsAerospikeDao {

	private static final String PRIMARY_KEY = "tenants";
	private static final String BLOB_BIN_NAME = "tenantIdSet";

	@Inject
	public TenantsWithAchievementsAerospikeDaoImpl(Config config, PrimaryKeyUtil<String> primaryKeyUtil,
			JsonUtil jsonUtil, AerospikeClientProvider aerospikeClientProvider) {
		super(config, primaryKeyUtil, jsonUtil, aerospikeClientProvider);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<String> getTenantIdList() {
		final String tenantIdList = readJson(getTenantsWithAchievementsSet(), PRIMARY_KEY, BLOB_BIN_NAME);
		List<String> listResult = StringUtils.isNotBlank(tenantIdList) ? jsonUtil.fromJson(tenantIdList, List.class)
				: Collections.emptyList();
		return listResult;
	}

	private String getTenantsWithAchievementsSet() {
		return config.getProperty(AchievementConfig.AEROSPIKE_TENANTS_WITH_ACHIEVEMENTS_SET);
	}

}
