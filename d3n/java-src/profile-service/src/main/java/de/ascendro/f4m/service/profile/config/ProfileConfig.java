package de.ascendro.f4m.service.profile.config;

import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.service.config.F4MConfigImpl;

public class ProfileConfig extends F4MConfigImpl {

	public static final String AEROSPIKE_APP_CONFIG_SET = "app.config.aerospike.set";
	public static final String AEROSPIKE_END_CONSUMER_INVOICE_SET = "end.consumer.invoice.aerospike.set";
	public static final String AEROSPIKE_PROFILE_SYNC_SET = "profile.aerospike.sync.set";
	public static final String AEROSPIKE_PROFILE_SYNC_RANDOM_BOUNDARY = "profile.aerospike.sync.random.boundary";
	public static final String PROFILE_NICKNAME_GENERATE_TRY_COUNT = "profile.nickname.try.count";
	
	public static final String AEROSPIKE_PROFILE_RANDOM_VALUE_INDEX_NAME = "profile.randomValueIndex.name";

	public ProfileConfig() {
		super(new AerospikeConfigImpl());
		setProperty(AEROSPIKE_APP_CONFIG_SET, "appConfig");
		setProperty(AEROSPIKE_END_CONSUMER_INVOICE_SET, "endConsumerInvoice");
		setProperty(AEROSPIKE_PROFILE_SYNC_SET, "profileSync");
		setProperty(AEROSPIKE_PROFILE_SYNC_RANDOM_BOUNDARY, 1000L);
		setProperty(AEROSPIKE_PROFILE_RANDOM_VALUE_INDEX_NAME, "randomValue-i");
		setProperty(PROFILE_NICKNAME_GENERATE_TRY_COUNT, 20);
		
		loadProperties();
	}
}
