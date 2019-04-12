package de.ascendro.f4m.service.dao.aerospike;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.server.AerospikeDaoImpl;
import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;

public class AerospikeMockDaoImpl extends AerospikeDaoImpl<PrimaryKeyUtil<String>> {

	private static final Logger LOGGER = LoggerFactory.getLogger(AerospikeMockDaoImpl.class);

	public AerospikeMockDaoImpl(Config config, PrimaryKeyUtil<String> primaryKeyUtil, JsonUtil jsonUtil,
			AerospikeClientMockProvider aerospikeClientMockProvider) {
		super(config, primaryKeyUtil, jsonUtil, aerospikeClientMockProvider);
	}

	@Override
	protected void registerUdf(File file) {
		//Register of UDF file is not supported within Aerospike mock
		LOGGER.warn("Register UDF executed for Aerospike Mock");
	}

}
