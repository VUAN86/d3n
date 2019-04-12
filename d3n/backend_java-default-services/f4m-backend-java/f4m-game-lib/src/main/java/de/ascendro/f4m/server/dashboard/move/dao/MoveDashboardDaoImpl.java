package de.ascendro.f4m.server.dashboard.move.dao;

import javax.inject.Inject;

import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aerospike.client.Bin;
import com.aerospike.client.Record;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeOperateDaoImpl;
import de.ascendro.f4m.server.config.GameConfigImpl;
import de.ascendro.f4m.server.dashboard.dao.DashboardPrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;

public class MoveDashboardDaoImpl extends AerospikeOperateDaoImpl<DashboardPrimaryKeyUtil>implements MoveDashboardDao {

	private static final Logger LOGGER = LoggerFactory.getLogger(MoveDashboardDaoImpl.class);

	@Inject
	public MoveDashboardDaoImpl(Config config, DashboardPrimaryKeyUtil primaryKeyUtil, JsonUtil jsonUtil,
			AerospikeClientProvider aerospikeClientProvider) {
		super(config, primaryKeyUtil, jsonUtil, aerospikeClientProvider);
	}

	@Override
	public void moveLastPlayedGame(String tenantId, String sourceUserId, String targetUserId) {
		String sourceKey = primaryKeyUtil.createUserInfoKey(tenantId, sourceUserId);
		Record record = readRecord(getSet(), sourceKey);
		if (record != null && MapUtils.isNotEmpty(record.bins)) {
			String targetKey = primaryKeyUtil.createUserInfoKey(tenantId, targetUserId);
			Bin[] bins = record.bins.entrySet().stream()
					.map(bin -> new Bin(bin.getKey(), bin.getValue()))
					.toArray(Bin[]::new);
			createRecord(getSet(), targetKey, bins);
			delete(getSet(), sourceKey);
		} else {
			LOGGER.warn(
					"Failed to move last played game, record not found; tenantId [{}]; sourceUserId [{}]; targetUserId [{}]",
					tenantId, sourceUserId, targetUserId);
		}
	}

	private String getSet() {
		return config.getProperty(GameConfigImpl.AEROSPIKE_DASHBOARD_SET);
	}

}
