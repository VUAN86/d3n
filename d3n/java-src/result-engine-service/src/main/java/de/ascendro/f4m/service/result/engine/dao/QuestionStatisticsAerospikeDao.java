package de.ascendro.f4m.service.result.engine.dao;

public interface QuestionStatisticsAerospikeDao {

	public static final String BLOB_BIN_NAME = "value";

	/**
     * Update the average answer time for the question.
     * @return average answer time in miliseconds, or <code>null</code>, if no average calculated yet (not enough data)
     */
	public Double updateAverageAnswerTime(String questionId, double answerMsT);

}
