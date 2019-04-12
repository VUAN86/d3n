package de.ascendro.f4m.server.transaction.log.util;

import javax.inject.Inject;

import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.service.config.Config;

public class TransactionLogPrimaryKeyUtil extends PrimaryKeyUtil<String> {

	public static final String AEROSPIKE_KEY_PREFIX_TRANSACTION_LOG = "transactionLog";

	@Inject
	public TransactionLogPrimaryKeyUtil(Config config) {
		super(config);
	}

	@Override
	protected String getServiceName() {
		return AEROSPIKE_KEY_PREFIX_TRANSACTION_LOG;
	}

}
