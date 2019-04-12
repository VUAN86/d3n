package de.ascendro.f4m.service.game.engine.model;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Answer extends JsonObjectWrapper {

	private static final Logger LOGGER = LoggerFactory.getLogger(Answer.class);

	private static final int DEFAULT_CLIENT_MS_T = 1;
	private static final Long DEFAULT_PR_DEL_MS_T = 1L;
	
	private static final String QUESTION_PROPERTY = "question";

	private static final String ANSWERS_PROPERTY = "answers";

	private static final String CLIENT_MS_T_PROPERTY = "tClientMs";

	private static final String SERVER_MS_T_PROPERTY = "tServerMs";//tAnswerReceiveMs-tStepStartMs
	private static final String STEP_START_T_PROPERTY = "tStepStartMs";
	private static final String ANSWER_RECEIVE_T_PROPERTY = "tAnswerReceiveMs";

	private static final String PRDEL_PROPERTY = "tPrDelMs";

	public Answer(JsonObject jsonObject) {
		super(jsonObject);
	}

	/**
	 * Create answer for question with specified timing
	 * 
	 * @param question
	 *            - question to which answer is expected
	 * @param tStepStartMs
	 *            - question step 0 start time
	 */
	public Answer(int question, long tStepStartMs) {
		jsonObject.addProperty(QUESTION_PROPERTY, question);
		setStartStepMsT(0, tStepStartMs);
	}
	
	public Answer(QuestionStep questionStep, long tStepStartMs, boolean isLiveGame) {
		LOGGER.debug("registerStep Answer");
		jsonObject.addProperty(QUESTION_PROPERTY, questionStep.getQuestion());
		setStartStepMsT(0, tStepStartMs);
		
		if (isLiveGame) {
			// setting default values for all steps except last - which is actual answer to the question
			for (int step = 0; step < questionStep.getStepCount() - 1; step++) {
				registerStep(step, DEFAULT_CLIENT_MS_T, DEFAULT_PR_DEL_MS_T, getLastStepStartMsT() + DEFAULT_CLIENT_MS_T, false,false);
			}
		}
	}

	protected void setAnswers(String[] answers) {
		setArray(ANSWERS_PROPERTY, answers);
	}

	public void answer(int step, String[] answers, long clientMsT, Long propagationDealy, long answerReceiveMsT, boolean isProcessExpiredDuel) {
		LOGGER.debug("answer 1");
		setAnswers(answers);
		LOGGER.debug("answer 2");
		registerStep(step, clientMsT, propagationDealy, answerReceiveMsT, isProcessExpiredDuel,false);
	}

	 void registerStep(int step, long clientMsT, Long propagationDealy, long stepSwitchMsT, boolean isProcessExpiredDuel,boolean isRegisteredStepSwitch) {
		LOGGER.debug("registerStep 1");
		 setClientMsT(step, clientMsT, isProcessExpiredDuel, isRegisteredStepSwitch);
		LOGGER.debug("registerStep 2");
		 setPrdelMsT(step, propagationDealy, isProcessExpiredDuel, isRegisteredStepSwitch);
		LOGGER.debug("registerStep 3");
		 setAnswerMsT(step, stepSwitchMsT, isProcessExpiredDuel, isRegisteredStepSwitch);
		LOGGER.debug("registerStep 4");
		 setServerMsT(step, getLastStepStartMsT(), stepSwitchMsT, isProcessExpiredDuel, isRegisteredStepSwitch);
	}

	private void setClientMsT(int step, long clientMsT, boolean isProcessExpiredDuel, boolean isRegisteredStepSwitch) {
		setNumberToArray(CLIENT_MS_T_PROPERTY, step, clientMsT, isProcessExpiredDuel, isRegisteredStepSwitch);

	}
	
	private void setPrdelMsT(int step, Long prdelMsT, boolean isProcessExpiredDuel, boolean isRegisteredStepSwitch) {
		setNumberToArray(PRDEL_PROPERTY, step, prdelMsT, isProcessExpiredDuel, isRegisteredStepSwitch);
	}

	private void setAnswerMsT(int step, long answerReceiveMsT, boolean isProcessExpiredDuel, boolean isRegisteredStepSwitch) {
		setNumberToArray(ANSWER_RECEIVE_T_PROPERTY, step, answerReceiveMsT, isProcessExpiredDuel, isRegisteredStepSwitch);
	}

	private void setServerMsT(int step, long stepStartMsT, long answerReceiveMsT, boolean isProcessExpiredDuel, boolean isRegisteredStepSwitch) {
		LOGGER.debug("setServerMsT {}--{}--{}", answerReceiveMsT, stepStartMsT, answerReceiveMsT - stepStartMsT);
		setNumberToArray(SERVER_MS_T_PROPERTY, step, answerReceiveMsT - stepStartMsT, isProcessExpiredDuel, isRegisteredStepSwitch);
	}

	public void setStartStepMsT(int step, long stepStartMsT) {
		LOGGER.debug("setStartStepMsT {}--{}", step, stepStartMsT);

		setNumberToArray(STEP_START_T_PROPERTY, step, stepStartMsT, false,false);
	}

	private void setNumberToArray(String propertyName, int index, Number value, boolean isProcessExpiredDuel, boolean isRegisteredStepSwitch) {
		setElementOfArray(propertyName, index, value != null ? new JsonPrimitive(value) : JsonNull.INSTANCE, isProcessExpiredDuel, isRegisteredStepSwitch);
	}
	
	public int getQuestion() {
		return getPropertyAsInt(QUESTION_PROPERTY);
	}

	public String[] getAnswers() {
		return getPropertyAsStringArray(ANSWERS_PROPERTY);
	}

	public long[] getClientMsT() {
		return getPropertyAsLongArray(CLIENT_MS_T_PROPERTY);
	}

	public Long[] getPrDelMsT() {
		return getPropertyAsLongObjectArray(PRDEL_PROPERTY);
	}

	public long[] getAnswerReceiveMsT() {
		return getPropertyAsLongArray(ANSWER_RECEIVE_T_PROPERTY);
	}

	public long[] getServerMsT() {
		return getPropertyAsLongArray(SERVER_MS_T_PROPERTY);
	}

	public long[] getStepStartMsT() {
		return getPropertyAsLongArray(STEP_START_T_PROPERTY);
	}

	private long getLastStepStartMsT() {
		final long[] stepStartTimes = getStepStartMsT();
		if (stepStartTimes.length > 0) {
			return stepStartTimes[stepStartTimes.length - 1];
		} else {
			throw new IllegalStateException("No step start time registered yet");
		}
	}

	public void noAnswer() {		
		setAnswers(null);	
	}
}
