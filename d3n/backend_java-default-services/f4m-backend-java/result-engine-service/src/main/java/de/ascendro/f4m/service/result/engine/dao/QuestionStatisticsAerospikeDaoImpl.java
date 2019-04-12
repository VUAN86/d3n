package de.ascendro.f4m.service.result.engine.dao;

import javax.inject.Inject;

import com.aerospike.client.policy.GenerationPolicy;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.WritePolicy;
import com.google.gson.JsonObject;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeDaoImpl;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.result.engine.config.ResultEngineConfig;
import de.ascendro.f4m.service.result.engine.model.QuestionStatistics;
import de.ascendro.f4m.service.result.engine.util.QuestionStatisticsPrimaryKeyUtil;

public class QuestionStatisticsAerospikeDaoImpl extends AerospikeDaoImpl<QuestionStatisticsPrimaryKeyUtil> implements QuestionStatisticsAerospikeDao {

	private final JsonMessageUtil jsonMessageUtil;

	@Inject
	public QuestionStatisticsAerospikeDaoImpl(Config config, QuestionStatisticsPrimaryKeyUtil primaryKeyUtil,
			AerospikeClientProvider aerospikeClientProvider, JsonUtil jsonUtil, JsonMessageUtil jsonMessageUtil) {
		super(config, primaryKeyUtil, jsonUtil, aerospikeClientProvider);
		this.jsonMessageUtil = jsonMessageUtil;
	}

	private String getSet() {
		return config.getProperty(ResultEngineConfig.AEROSPIKE_QUESTION_STATISTICS_SET);
	}

	@Override
	public Double updateAverageAnswerTime(String questionId, double answerMsT) {
		final String key = primaryKeyUtil.createPrimaryKey(questionId);
		final String statisticsJson = readJson(getSet(), key, BLOB_BIN_NAME);

		QuestionStatistics statistics = statisticsJson == null ? null
				: new QuestionStatistics(jsonMessageUtil.fromJson(statisticsJson, JsonObject.class));

		if (statistics == null) {
			// create new
			statistics = new QuestionStatistics();
			statistics.setId(questionId);
			writeStats(key, statistics, 1L, answerMsT);
			return null;
		} else {
			// update existing
			long usersAnsweredCount = statistics.getUsersAnswered() == null ? 0L : statistics.getUsersAnswered();
			Double averageAnswerTime = statistics.getAverageAnswerTime();
			long newUsersAnsweredCount = usersAnsweredCount + 1;
			double newAverageAnswerTime = averageAnswerTime == null ? answerMsT 
					: (averageAnswerTime * usersAnsweredCount + answerMsT) / newUsersAnsweredCount;

			if (usersAnsweredCount < config.getPropertyAsLong(ResultEngineConfig.MINIMUM_NUMBER_OF_ANSWERS_FOR_AVERAGE_ANSWER_TIME_CALCULATION)) {
				// If too few data for analysis => simply update average and return null
				writeStats(key, statistics, newUsersAnsweredCount, newAverageAnswerTime);
				return null;
			} else {
				// Otherwise also start excluding extremes and return average result
				double threshold = config.getPropertyAsDouble(ResultEngineConfig.THRESHOLD_FOR_INCLUDING_ANSWER_TIME_IN_AVERAGE_CALCULATION_PERCENT);
				if (answerMsT > averageAnswerTime * threshold 
						&& answerMsT < averageAnswerTime * (1 + threshold)) {
					// If answer time is not extreme (too low or too high) => record it for new average
					writeStats(key, statistics, newUsersAnsweredCount, newAverageAnswerTime);
					return newAverageAnswerTime;
				} else {
					// Otherwise don't record the new data
					return averageAnswerTime;
				}
			}
		}
	}

	private void writeStats(String key, QuestionStatistics statistics, long usersAnswered, double averageAnswerTime) {
		statistics.setUsersAnswered(usersAnswered);
		statistics.setAverageAnswerTime(averageAnswerTime);

		// Don't need to worry if some statistics get lost => create appropriate write policy
		final WritePolicy writePolicy = getWritePolicy();
		writePolicy.generationPolicy = GenerationPolicy.NONE;
		writePolicy.recordExistsAction = RecordExistsAction.REPLACE; 
		writeBin(writePolicy, getSet(), key, getJsonBin(BLOB_BIN_NAME, statistics.getAsString()));
	}

}
