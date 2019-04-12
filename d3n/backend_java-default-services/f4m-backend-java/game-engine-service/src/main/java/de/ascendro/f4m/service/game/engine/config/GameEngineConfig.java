package de.ascendro.f4m.service.game.engine.config;

import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.config.ElasticConfigImpl;
import de.ascendro.f4m.server.config.GameConfigImpl;
import de.ascendro.f4m.service.config.F4MConfigImpl;

public class GameEngineConfig extends F4MConfigImpl {
	public static final String AEROSPIKE_QUESTION_POOL_SET = "aerospike.set.questionPool";
	public static final String AEROSPIKE_HEALTH_CHECK_SET = "aerospike.set.healthCheck";
	public static final String AEROSPIKE_PRESELECTED_QUESTIONS_SET = "aerospike.set.preselectedQuestions";
	public static final String AEROSPIKE_MULTIPLAYER_GAME_INSTANCE_SET = "aerospike.set.multiplayerGameInstance";
	public static final String AEROSPIKE_ADVERTISEMENT_SET = "aerospike.set.advertisement";
	public static final String AEROSPIKE_ACTIVE_GAME_INSTANCE_SET = "aerospike.set.activeGameInstance";
	public static final String AEROSPIKE_ACTIVE_GAME_INSTANCE_NAMESPACE = "aerospike.namespace.activeGameInstance";
	
	public static final String QUESTION_PULL_NO_REPEATS_RETRY_COUNT = "questionPool.noRepeatsRetryCount";
	public static final int QUESTION_PULL_NO_REPEATS_RETRY_COUNT_DEFAULT = 20;

	public static final String QUESTION_PULL_FRAGMENTATION_RETRY_COUNT = "questionPool.fragmentationRetryCount";
	public static final int QUESTION_PULL_FRAGMENTATION_RETRY_COUNT_DEFAULT = 20;
	/**
	 * Percentage of jackpot to be paid out, when payout is determined by entry fee.
	 */
	public static final String JACKPOT_PAYOUT_PERCENT = "result.jackpot.payout.percent";

	public static final String ADVERTISEMENT_PATH_FORMAT = "advertisement.blobKeyFormat";
	public static final String ADVERTISEMENT_PATH_FORMAT_DEFAULT = "provider_%d_advertisement_%d.json";
	
	public static final String QUESTION_FEEDER_THREAD_POOL_CORE_SIZE = "questionFeeder.threadPool.coreSize";
	public static final int QUESTION_FEEDER_THREAD_POOL_CORE_SIZE_DEFAULT = 2;
	
	public static final String QUESTION_FEEDER_DISTRIBUTION_DELAY_IN_BETWEEN_QUESTIONS = "questionFeeder.distributionDealayInBetweenQuestions";
	public static final long QUESTION_FEEDER_DIRIBUTION_DELAY_IN_BETWEEN_QUESTIONS_DEFAULT = 20_000;//20s
	
	public static final String QUESTION_FEEDER_GAME_START_MIN_DELAY = "questionFeeder.gameStartMinDelay";
	public static final long QUESTION_FEEDER_GAME_START_MIN_DELAY_DEFAULT = 60_000;//60s

	/**
	 * Number of last health check measurements, which will be taken into account, when calculating propagation delay.
	 */
	public static final String HEALTH_CHECK_IMPORTANT_MEASUREMENT_COUNT = "healthCheck.measurement.count";
	/**
	 * Maximum roundtrip value in seconds which is taken into account when calculating propagation delay. Too big delays
	 * are ignored to avoid data pollution due to network failures.
	 */
	public static final String HEALTH_CHECK_ROUNDTRIP_THRESHOLD = "healthCheck.measurement.threshold";
	/**
	 * 30 second delay shows that user won't be able to really play a game anyway.
	 */
	public static final int HEALTH_CHECK_ROUNDTRIP_THRESHOLD_DEFAULT = 30;
	
	public static final String PLAYED_QUESTION_ENTRY_TIME_TO_LIVE_IN_DAYS= "played.question.timeToLiveInDays";
	public static final int PLAYED_QUESTION_ENTRY_TIME_TO_LIVE_DEFAULT_IN_DAYS= 30;
	
	public GameEngineConfig() {
		super(new AerospikeConfigImpl(), new GameConfigImpl(), new ElasticConfigImpl());

		setProperty(USERS_CACHE_TIME_TO_LIVE, 60 * 60 * 1000); //cache client ids received within session for 1h
		setProperty(JACKPOT_PAYOUT_PERCENT, 0.90);
		setProperty(AEROSPIKE_QUESTION_POOL_SET, "questionPool");
		setProperty(AEROSPIKE_HEALTH_CHECK_SET, "healthCheck");
		setProperty(AEROSPIKE_MULTIPLAYER_GAME_INSTANCE_SET, "multiplayerGameInstance");
		setProperty(AEROSPIKE_PRESELECTED_QUESTIONS_SET, "preselectedQuestions");
		setProperty(AEROSPIKE_ADVERTISEMENT_SET, "advertisement");
		setProperty(AEROSPIKE_ACTIVE_GAME_INSTANCE_SET, "activeGameInstance");
		setProperty(AEROSPIKE_ACTIVE_GAME_INSTANCE_NAMESPACE, getProperty(AerospikeConfigImpl.AEROSPIKE_NAMESPACE));
		
		setProperty(QUESTION_PULL_NO_REPEATS_RETRY_COUNT, QUESTION_PULL_NO_REPEATS_RETRY_COUNT_DEFAULT);
		setProperty(QUESTION_PULL_FRAGMENTATION_RETRY_COUNT, QUESTION_PULL_FRAGMENTATION_RETRY_COUNT_DEFAULT);
		
		setProperty(HEALTH_CHECK_IMPORTANT_MEASUREMENT_COUNT, 10);
		setProperty(HEALTH_CHECK_ROUNDTRIP_THRESHOLD, HEALTH_CHECK_ROUNDTRIP_THRESHOLD_DEFAULT);

		setProperty(PLAYED_QUESTION_ENTRY_TIME_TO_LIVE_IN_DAYS, PLAYED_QUESTION_ENTRY_TIME_TO_LIVE_DEFAULT_IN_DAYS);
		
		setProperty(ADVERTISEMENT_PATH_FORMAT, ADVERTISEMENT_PATH_FORMAT_DEFAULT);
		setProperty(QUESTION_FEEDER_THREAD_POOL_CORE_SIZE, QUESTION_FEEDER_THREAD_POOL_CORE_SIZE_DEFAULT);
		setProperty(QUESTION_FEEDER_DISTRIBUTION_DELAY_IN_BETWEEN_QUESTIONS, QUESTION_FEEDER_DIRIBUTION_DELAY_IN_BETWEEN_QUESTIONS_DEFAULT);
		setProperty(QUESTION_FEEDER_GAME_START_MIN_DELAY, QUESTION_FEEDER_GAME_START_MIN_DELAY_DEFAULT);
		
		loadProperties();
	}
}
