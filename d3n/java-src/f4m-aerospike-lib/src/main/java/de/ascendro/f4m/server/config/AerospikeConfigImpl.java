package de.ascendro.f4m.server.config;

import com.aerospike.client.policy.CommitLevel;
import com.aerospike.client.policy.GenerationPolicy;
import com.aerospike.client.policy.RecordExistsAction;

import de.ascendro.f4m.service.config.F4MConfig;

public class AerospikeConfigImpl extends F4MConfig {

	public static final String AEROSPIKE_SERVER_HOST = "aerospike.server.host";
	public static final String AEROSPIKE_SERVER_PORT = "aerospike.server.port";

	public static final String AEROSPIKE_SERVER_USER = "aerospike.server.user";
	public static final String AEROSPIKE_SERVER_PASSWORD = "aerospike.server.password";

	public static final String AEROSPIKE_UDF_LOCATION = "aerospike.udf.location";
	public static final String AEROSPIKE_NAMESPACE = "aerospike.namespace";
	public static final String AEROSPIKE_NAMESPACE_DEFAULT = "test";

	public static final String AEROSPIKE_WRITE_BYTES_CHARSET = "aerospike.write.bytes.charset";

	public static final String AEROSPIKE_CLIENT_POLICY_TIMEOUT = "aerospike.policy.client.timeout";

	public static final String AEROSPIKE_POLICY_WRITE_EXPIRATION = "aerospike.policy.write.expiration";
	public static final String AEROSPIKE_POLICY_WRITE_GENERATION = "aerospike.policy.write.generationPolicy";

	public static final String AEROSPIKE_POLICY_WRITE_RECORD_EXISTS_ACTION = "aerospike.policy.write.recordExistsAction";
	public static final String AEROSPIKE_POLICY_WRITE_COMMIT_LEVEL = "aerospike.policy.write.commitLevel";

	public static final String AEROSPIKE_GAME_SET = "aerospike.set.game";
	public static final String AEROSPIKE_GAME_KEY_PREFIX = "aerospike.game.key.prefix";
	
	public static final String AEROSPIKE_EXPIRED_RECORD_CLEANER_SCHEDULE = "expired.record.cleaner.schedule";
	public static final String AEROSPIKE_EXPIRED_RECORD_CLEANER_START = "expired.record.cleaner.schedule.start"; // milliseconds from midnight
	public static final String AEROSPIKE_EXPIRED_RECORD_CLEANER_PERIOD = "expired.record.cleaner.schedule.period";
	public static final String AEROSPIKE_EXPIRED_RECORD_TTL = "expired.record.ttl";
	public static final String AEROSPIKE_ACTIVE_CLEANER_RECORD_TTL = "active.cleaner.record.ttl"; // seconds
	
	public static final String AEROSPIKE_GAME_INSTANCE_SET = "aerospike.set.gameInstance";
	public static final String EVENT_LOG_SET = "eventLog.aerospike.set";
	public static final String TRANSACTION_LOG_SET = "transactionLog.aerospike.set";
	public static final String AEROSPIKE_PROFILE_SET = "profile.aerospike.set";
	public static final String AEROSPIKE_VOUCHER_SET = "voucher.aerospike.set";
	public static final String AEROSPIKE_MERGED_PROFILE_SET = "merged.profile.aerospike.set";
    public static final String AEROSPIKE_USER_VOUCHER_SET = "userVoucher.aerospike.set";
    /**
     * Country list where gambling is prohibited 
     */
    public static final String NO_GAMBLING_COUNTRY_LIST = "no.gambling.countries";
	
	
	public AerospikeConfigImpl() {
		setProperty(AEROSPIKE_SERVER_PORT, 3000);
		setProperty(AEROSPIKE_SERVER_HOST, "3.120.166.165");
		setProperty(AEROSPIKE_UDF_LOCATION, "src/main/resources/udf");
		setProperty(AEROSPIKE_NAMESPACE, AEROSPIKE_NAMESPACE_DEFAULT);

		setProperty(AEROSPIKE_SERVER_USER, "");
		setProperty(AEROSPIKE_SERVER_PASSWORD, "");

		setProperty(AEROSPIKE_WRITE_BYTES_CHARSET, "UTF-8");
		setProperty(AEROSPIKE_CLIENT_POLICY_TIMEOUT, 500);

		setProperty(AEROSPIKE_POLICY_WRITE_EXPIRATION, -1);//Never expire
		setProperty(AEROSPIKE_POLICY_WRITE_GENERATION, GenerationPolicy.EXPECT_GEN_EQUAL.name());
		setProperty(AEROSPIKE_POLICY_WRITE_RECORD_EXISTS_ACTION, RecordExistsAction.UPDATE_ONLY.name());
		setProperty(AEROSPIKE_POLICY_WRITE_COMMIT_LEVEL, CommitLevel.COMMIT_ALL.name());

		setProperty(AEROSPIKE_GAME_SET, "game");
		setProperty(AEROSPIKE_GAME_KEY_PREFIX, "game");
		
		setProperty(AEROSPIKE_EXPIRED_RECORD_CLEANER_SCHEDULE, false);
		setProperty(AEROSPIKE_EXPIRED_RECORD_CLEANER_START, 3 * 60 * 60 * 1000L); // 3 am
		setProperty(AEROSPIKE_EXPIRED_RECORD_CLEANER_PERIOD, 24 * 60 * 60 * 1000L); // every 24 h
		setProperty(AEROSPIKE_EXPIRED_RECORD_TTL, 7 * 24 * 60 * 60 * 1000L); // 7 days
		setProperty(AEROSPIKE_ACTIVE_CLEANER_RECORD_TTL, 2 * 60 * 60); // 2 hours
		
		setProperty(AEROSPIKE_GAME_INSTANCE_SET, "gameInstance");
		setProperty(EVENT_LOG_SET, "eventLog");
		setProperty(TRANSACTION_LOG_SET, "transactionLog");
		setProperty(AEROSPIKE_PROFILE_SET, "profile");
		setProperty(AEROSPIKE_MERGED_PROFILE_SET, "profile_merged");
        setProperty(NO_GAMBLING_COUNTRY_LIST, "SA"); //Saudi Arabia currently

		setProperty(AEROSPIKE_VOUCHER_SET, "voucher");
		setProperty(AEROSPIKE_USER_VOUCHER_SET, "userVoucher");
	}

}
