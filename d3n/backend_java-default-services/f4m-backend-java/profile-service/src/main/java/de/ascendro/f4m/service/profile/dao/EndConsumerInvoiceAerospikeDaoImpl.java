package de.ascendro.f4m.service.profile.dao;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeOperateDaoImpl;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.profile.config.ProfileConfig;
import de.ascendro.f4m.service.profile.util.EndConsumerInvoicePrimaryKeyUtil;

public class EndConsumerInvoiceAerospikeDaoImpl extends AerospikeOperateDaoImpl<EndConsumerInvoicePrimaryKeyUtil> implements EndConsumerInvoiceAerospikeDao {

	
	@Inject
	public EndConsumerInvoiceAerospikeDaoImpl(Config config, EndConsumerInvoicePrimaryKeyUtil primaryKeyUtil,
			JsonUtil jsonUtil, AerospikeClientProvider aerospikeClientProvider) {
		super(config, primaryKeyUtil, jsonUtil, aerospikeClientProvider);
	}

	@Override
	public List<JsonObject> getInvoiceList(String tenantId, String userId, long offset, int limit) {
		List<Object> items = readList(getSet(), primaryKeyUtil.createPrimaryKey(tenantId, userId), BLOB_BIN_NAME);
		if (items != null) {
			return items.stream().map(e -> jsonUtil.fromJson((String) e, JsonObject.class))
					.collect(Collectors.toList());
		} else {
			return Collections.emptyList();
		}		
	}

	private String getSet() {
		return config.getProperty(ProfileConfig.AEROSPIKE_END_CONSUMER_INVOICE_SET);
	}

}
