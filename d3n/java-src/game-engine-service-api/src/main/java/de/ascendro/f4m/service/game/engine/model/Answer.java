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
		jsonObject.addProperty(QUESTION_PROPERTY, questionStep.getQuestion());
		setStartStepMsT(0, tStepStartMs);
		
		if (isLiveGame) {
			// setting default values for all steps except last - which is actual answer to the question
			for (int step = 0; step < questionStep.getStepCount() - 1; step++) {
				registerStep(step, DEFAULT_CLIENT_MS_T, DEFAULT_PR_DEL_MS_T, getLastStepStartMsT() + DEFAULT_CLIENT_MS_T, false);
			}
		}
	}

	protected void setAnswers(String[] answers) {
		setArray(ANSWERS_PROPERTY, answers);
	}

	public void answer(int step, String[] answers, long clientMsT, Long propagationDealy, long answerReceiveMsT, boolean isProcessExpiredDuel) {
		setAnswers(answers);
		registerStep(step, clientMsT, propagationDealy, answerReceiveMsT,isProcessExpiredDuel);
	}

	public void registerStep(int step, long clientMsT, Long propagationDealy, long stepSwitchMsT, boolean isProcessExpiredDuel) {
		setClientMsT(step, clientMsT, isProcessExpiredDuel);
		setPrdelMsT(step, propagationDealy, isProcessExpiredDuel);
		setAnswerMsT(step, stepSwitchMsT, isProcessExpiredDuel);
		setServerMsT(step, getLastStepStartMsT(), stepSwitchMsT, isProcessExpiredDuel);
	}

	private void setClientMsT(int step, long clientMsT, boolean isProcessExpiredDuel) {
		setNumberToArray(CLIENT_MS_T_PROPERTY, step, clientMsT, isProcessExpiredDuel);

	}
	
	private void setPrdelMsT(int step, Long prdelMsT, boolean isProcessExpiredDuel) {
		setNumberToArray(PRDEL_PROPERTY, step, prdelMsT, isProcessExpiredDuel);
	}

	private void setAnswerMsT(int step, long answerReceiveMsT, boolean isProcessExpiredDuel) {
		setNumberToArray(ANSWER_RECEIVE_T_PROPERTY, step, answerReceiveMsT, isProcessExpiredDuel);
	}

	private void setServerMsT(int step, long stepStartMsT, long answerReceiveMsT, boolean isProcessExpiredDuel) {
		setNumberToArray(SERVER_MS_T_PROPERTY, step, answerReceiveMsT - stepStartMsT, isProcessExpiredDuel);
	}

	public void setStartStepMsT(int step, long stepStartMsT) {
		setNumberToArray(STEP_START_T_PROPERTY, step, stepStartMsT, false);
	}

	private void setNumberToArray(String propertyName, int index, Number value, boolean isProcessExpiredDuel) {
		setElementOfArray(propertyName, index, value != null ? new JsonPrimitive(value) : JsonNull.INSTANCE, isProcessExpiredDuel);
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
