package de.ascendro.f4m.service.result.engine.config;

import com.google.common.io.Resources;

import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.config.ElasticConfigImpl;
import de.ascendro.f4m.server.config.GameConfigImpl;
import de.ascendro.f4m.service.config.F4MConfigImpl;

public class ResultEngineConfig extends F4MConfigImpl {
    public static final String AEROSPIKE_QUESTION_STATISTICS_SET = "question.statistics.aerospike.set";
    public static final String AEROSPIKE_GAME_STATISTICS_SET = "game.statistics.aerospike.set";
    
    /**
     * Default propagation delay, if calculated not available.
     */
    public static final String DEFAULT_PROPAGATION_DELAY = "result.propagation.defaultDelay";
    
    /**
     * Default amount of bonus points for quick response.
     */
    public static final String DEFAULT_BONUS_POINTS_FOR_QUICK_RESPONSE = "result.calculation.gamePoints.quickResponse";
    /**
     * Quick response default time in milliseconds.
     */
    public static final String DEFAULT_BONUS_POINTS_FOR_QUICK_RESPONSE_MS = "result.calculation.gamePoints.quickResponse.millis";
    /**
     * Default amount of bonus points for quick response.
     */
    public static final String DEFAULT_BONUS_POINTS_FOR_QUICK_RESPONSE_CATEGORY_1 = "result.calculation.gamePoints.quickResponse";
    /**
     * Quick response default time in milliseconds.
     */
    public static final String DEFAULT_BONUS_POINTS_FOR_QUICK_RESPONSE_MS_CATEGORY_1 = "result.calculation.gamePoints.quickResponse.millis";
    /**
     * Default amount of bonus points for quick response.
     */
    public static final String DEFAULT_BONUS_POINTS_FOR_QUICK_RESPONSE_CATEGORY_2 = "result.calculation.gamePoints.quickResponse";
    /**
     * Quick response default time in milliseconds.
     */
    public static final String DEFAULT_BONUS_POINTS_FOR_QUICK_RESPONSE_MS_CATEGORY_2 = "result.calculation.gamePoints.quickResponse.millis";

    /**
     * Default amount of bonus points for quick response.
     */
    public static final String DEFAULT_BONUS_POINTS_FOR_QUICK_RESPONSE_CATEGORY_3 = "result.calculation.gamePoints.quickResponse";
    /**
     * Quick response default time in milliseconds.
     */
    public static final String DEFAULT_BONUS_POINTS_FOR_QUICK_RESPONSE_MS_CATEGORY_3 = "result.calculation.gamePoints.quickResponse.millis";


	/**
     * Bonus for each correct answer. 
     */
    public static final String BONUS_PER_CORRECT_ANSWER = "result.calculation.gamePoints.bonusPerCorrectAnswer";
    
    /**
     * Handicap delta factor if handicap is increasing.
     */
    public static final String HANDICAP_DELTA_FACTOR_POSITIVE = "result.calculation.handicap.deltaFactorPositive";
    
    /**
     * Handicap delta factor if handicap is decreasing.
     */
    public static final String HANDICAP_DELTA_FACTOR_NEGATIVE = "result.calculation.handicap.deltaFactorNegative";
    
    /**
     * Handicap bonus for each correct answer.
     */
    public static final String HANDICAP_BONUS_PER_CORRECT_ANSWER = "result.calculation.handicap.bonusPerCorrectAnswer";
    
    /**
     * Default number of alternative bonus points per correct answer.
     */
    public static final String DEFAULT_ALTERNATIVE_BONUS_POINTS_PER_CORRECT_ANSWER = "result.calculation.bonusPoints.defaultAlternativeBonusPointsPerCorrectAnswer";

    /**
     * Default number of bonus points per correct answer for unpaid games.
     */
    public static final String DEFAULT_BONUS_POINTS_PER_CORRECT_ANSWER_FOR_UNPAID = "result.calculation.bonusPoints.defaultBonusPointsPerCorrectAnswerForUnpaid";
    
    /**
     * Default number of bonus points per game point for paid games.
     */
    public static final String DEFAULT_BONUS_POINTS_PER_GAME_POINT_FOR_PAID = "result.calculation.bonusPoints.defaultBonusPointsPerGamePointForPaid";
    
    /**
     * Threshold for warning on too quick answers (in % from average answer time, 0..1)
     */
    public static final String THRESHOLD_FOR_WARNING_ON_TOO_QUICK_ANSWER_PERCENT = "result.fraudDetection.answerTooQuick.threshold";
    
    /**
     * Threshold for including answer time in average answer time calculation (in % from average answer time, 0..1)
     */
    public static final String THRESHOLD_FOR_INCLUDING_ANSWER_TIME_IN_AVERAGE_CALCULATION_PERCENT = "result.fraudDetection.averageAnswerTimeCalculation.threshold";
    
    /**
     * Minimum number of correct answers to have been recorded in order to be able to say that the average answer time is something meaningful.
     */
    public static final String MINIMUM_NUMBER_OF_ANSWERS_FOR_AVERAGE_ANSWER_TIME_CALCULATION = "result.fraudDetection.minimumAnswersForAverageCalculation";
    
    /**
     * Percentage of jackpot to be paid out, when payout is determined by entry fee.
     */
    public static final String JACKPOT_PAYOUT_PERCENT = "result.jackpot.payout.percent";

	public static final String ELASTIC_INDEX_MGR = "elastic.index.mgr";
	public static final String ELASTIC_TYPE_MGR = "elastic.type.mgr";
	public static final String ELASTIC_MAPPING_INDEX_MGR = "elastic.mapping.index.mgr";
	public static final String ELASTIC_MAPPING_TYPE_MGR = "elastic.mapping.type.mgr";
	
	
	
    public ResultEngineConfig() {
        super(new AerospikeConfigImpl(), new GameConfigImpl(), new ElasticConfigImpl());
        setProperty(AEROSPIKE_QUESTION_STATISTICS_SET, "questionStatistics");
        setProperty(AEROSPIKE_GAME_STATISTICS_SET, "gameStatistics");
        setProperty(DEFAULT_PROPAGATION_DELAY, 3000); // 3 seconds by default
        setProperty(DEFAULT_BONUS_POINTS_FOR_QUICK_RESPONSE, 5);
        setProperty(DEFAULT_BONUS_POINTS_FOR_QUICK_RESPONSE_MS, 5_000); // 5 seconds
        setProperty(DEFAULT_BONUS_POINTS_FOR_QUICK_RESPONSE_CATEGORY_1, 1);
        setProperty(DEFAULT_BONUS_POINTS_FOR_QUICK_RESPONSE_MS_CATEGORY_1, 15_000); // 5 seconds
        setProperty(DEFAULT_BONUS_POINTS_FOR_QUICK_RESPONSE_CATEGORY_2, 2);
        setProperty(DEFAULT_BONUS_POINTS_FOR_QUICK_RESPONSE_MS_CATEGORY_2, 10_000); // 5 seconds
        setProperty(DEFAULT_BONUS_POINTS_FOR_QUICK_RESPONSE_CATEGORY_3, 3);
        setProperty(DEFAULT_BONUS_POINTS_FOR_QUICK_RESPONSE_MS_CATEGORY_3, 5_000); // 5 seconds
        setProperty(BONUS_PER_CORRECT_ANSWER, 0.25);
        setProperty(HANDICAP_DELTA_FACTOR_POSITIVE, 0.2);
        setProperty(HANDICAP_DELTA_FACTOR_NEGATIVE, 0.1);
        setProperty(HANDICAP_BONUS_PER_CORRECT_ANSWER, 0.075);
        setProperty(DEFAULT_ALTERNATIVE_BONUS_POINTS_PER_CORRECT_ANSWER, 100);
        setProperty(DEFAULT_BONUS_POINTS_PER_CORRECT_ANSWER_FOR_UNPAID, 100);
        setProperty(DEFAULT_BONUS_POINTS_PER_GAME_POINT_FOR_PAID, 30);
        setProperty(THRESHOLD_FOR_WARNING_ON_TOO_QUICK_ANSWER_PERCENT, 0.7); // Has to be more than 70% below average
        setProperty(THRESHOLD_FOR_INCLUDING_ANSWER_TIME_IN_AVERAGE_CALCULATION_PERCENT, 0.5); // Has to be no more than 50% above or below average
        setProperty(MINIMUM_NUMBER_OF_ANSWERS_FOR_AVERAGE_ANSWER_TIME_CALCULATION, 100);
        setProperty(JACKPOT_PAYOUT_PERCENT, 0.90);
        setProperty(ELASTIC_INDEX_MGR, "mgr");
        setProperty(ELASTIC_TYPE_MGR, "mgr");
        setProperty(ELASTIC_MAPPING_INDEX_MGR, Resources.getResource(getClass(), "MultiplayerGameResultsIndexESMapping.json"));
        setProperty(ELASTIC_MAPPING_TYPE_MGR, Resources.getResource(getClass(), "MultiplayerGameResultESMapping.json"));
		loadProperties();
    }
    
}
