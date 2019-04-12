package de.ascendro.f4m.service.game.engine.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.game.engine.model.end.GameEndStatus;
import de.ascendro.f4m.service.json.model.JsonObjectWrapper;
import de.ascendro.f4m.service.util.F4MEnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameState extends JsonObjectWrapper {
	private static final Logger LOGGER = LoggerFactory.getLogger(GameState.class);

	private static final String ENTRY_FEE_TRANSACTION_ID_PROPERTY = "entryFeeTransactionId";
	private static final String STATUS_PROPERTY = "status";
	private static final String END_STATUS_PROPERTY = "endStatus";

	private static final String ANSWERS_PROPERTY = "answers";
	private static final String CURRENT_QUESTION_STEP_PROPERTY = "currentQuestionStep";
	
	private static final String CURRENT_ADVERTISEMENT_INDEX_PROPERTY = "currentAdvertisementIndex";
	// flag used to mark if a showAdvertisement call was made, to be used in the following nextStep call after it
	private static final String ADVERTISEMENT_SENT_BEFORE_NEXT_STEP_PROPERTY = "advertisementSentBeforeNextStep";
	
	public static final String PROPERTY_REFUNDABLE = "refundable";
	public static final String PROPERTY_REFUND_REASON = "refundReason";
	
	public static final String PROPERTY_CLOSE_UP_REASON = "closeUpReason";
	public static final String PROPERTY_CLOSE_UP_ERROR_MESSAGE = "closeUpErrorMessage";

	private static final String[] EMPTY_ANSWER= {"00000000-0000-0000-00000000000000000"};


	public GameState(JsonObject jsonObject) {
		this.jsonObject = jsonObject;
	}

	public GameState(GameStatus gameStatus, String entryFeeTransactionId) {
		this.jsonObject = new JsonObject();

		jsonObject.add(ANSWERS_PROPERTY, new JsonArray());
		jsonObject.add(CURRENT_QUESTION_STEP_PROPERTY, null);
		setGameStatus(gameStatus != null ? gameStatus : GameStatus.REGISTERED);
		setEntryFeeTransactionId(entryFeeTransactionId);
		setRefundable(false);
	}
	
	public GameState(GameStatus gameStatus) {
		this(gameStatus, null);
	}

	public void setCurrentQuestionStep(QuestionStep questionStep) {
		jsonObject.add(CURRENT_QUESTION_STEP_PROPERTY, questionStep.getJsonObject());
	}

	public QuestionStep getCurrentQuestionStep() {
		final JsonElement questionStepElement = jsonObject.get(CURRENT_QUESTION_STEP_PROPERTY);
		final QuestionStep questionStep;
		if (questionStepElement != null && !questionStepElement.isJsonNull()) {
			questionStep = new QuestionStep(questionStepElement.getAsJsonObject());
		} else {
			questionStep = null;
		}
		return questionStep;
	}
	
	public void startPlaying(int firstQuestionStepCount, long startTime, boolean isLiveGame) {
		setCurrentQuestionStep(new QuestionStep(0, 0, firstQuestionStepCount));
		setGameStatus(GameStatus.IN_PROGRESS);
		registerStepStartTime(isLiveGame);
	}

	public GameStatus getGameStatus() {
		final JsonElement gameStatusElement = jsonObject.get(STATUS_PROPERTY);
		final GameStatus gameStatus;
		if (gameStatusElement != null) {
			gameStatus = GameStatus.valueOf(gameStatusElement.getAsString());
		} else {
			gameStatus = null;
		}
		return gameStatus;
	}

	private void setGameStatus(GameStatus gameStatus) {
		setProperty(STATUS_PROPERTY, gameStatus.name());
	}
	
	public GameEndStatus getGameEndStatus() {
		final JsonElement gameEndStatusElement = jsonObject.get(END_STATUS_PROPERTY);
		final GameEndStatus gameEndStatus;
		if (gameEndStatusElement != null) {
			gameEndStatus = GameEndStatus.valueOf(gameEndStatusElement.getAsString());
		} else {
			gameEndStatus = null;
		}
		return gameEndStatus;
	}

	public void setGameEndStatus(GameEndStatus gameEndStatus) {
		setProperty(END_STATUS_PROPERTY, gameEndStatus.name());
	}

	public void nextQuestion(Question nextQuestion, boolean isLiveGame) {
		final QuestionStep currentQuestionStep = getCurrentQuestionStep();
		currentQuestionStep.nextQuestion(nextQuestion.getStepCount());
		registerStepStartTime(isLiveGame);
	}

	public boolean nextStep(boolean isLiveGame) {
		final QuestionStep currentQuestionStep = getCurrentQuestionStep();

		final boolean hasNextStep;
		if (currentQuestionStep.hasNextStep()) {
			currentQuestionStep.nextStep();
			registerStepStartTime(isLiveGame);
			hasNextStep = true;
		} else {
			hasNextStep = false;
		}
		return hasNextStep;
	}

	public boolean hasNextQuestion(int totalQuestionCount) {
		return getCurrentQuestionStep() == null || getCurrentQuestionStep().getQuestion() + 1 < totalQuestionCount;
	}

	public void addAnswer(Answer answer) {
		final JsonArray answers = getArray(ANSWERS_PROPERTY);
		answers.add(answer.getJsonObject());
	}

	public void registerAnswer(int question, String[] answers, Long clientMsT, Long prDelMsT, long receiveTimeStamp) {
		final Answer answer = getAnswer(question);
		int step = getCurrentQuestionStep().getStep();
		answer.answer(step, answers, clientMsT, prDelMsT, receiveTimeStamp, false);
	}

	public long[] getServerMsT(int question) {
		final Answer answer = getAnswer(question);
		return answer.getServerMsT();
	}


	public void registerStepSwitch(Long clientMsT, Long prDelMsT, long receiveTimeStamp) {
		int question = getCurrentQuestionStep().getQuestion();
		int step = getCurrentQuestionStep().getStep();
		if (hasAnswer(question)) {
			final Answer answer = getAnswer(question);
			answer.registerStep(step, clientMsT, prDelMsT, receiveTimeStamp, false);
		} else {
			throw new F4MFatalErrorException("No answer present for question: " + question);
		}
	}

	private void registerStepStartTime(boolean isLiveGame) {
		final QuestionStep currentStep = getCurrentQuestionStep();
		final Answer answer;
		if (currentStep.getStep() == 0) {
			answer = new Answer(currentStep, System.currentTimeMillis(), isLiveGame);
			addAnswer(answer);
		} else {
			answer = getAnswer(currentStep.getQuestion());
			answer.setStartStepMsT(currentStep.getStep(), System.currentTimeMillis());
		}		
	}

	public boolean hasAnswer(int question) {
		final JsonArray answersArray = getArray(ANSWERS_PROPERTY);
		return question < answersArray.size();
	}

	/**
	 * Select provided answers for the questions
	 * 
	 * @return array of answers, where index is order of the question within flow
	 */
	public Answer[] getAnswers() {
		final JsonArray answersArray = getArray(ANSWERS_PROPERTY);
		final Answer[] answers = new Answer[answersArray.size()];

		for (int i = 0 ; i  < answers.length ; i++) {
			final JsonElement answerElement = answersArray.get(i);
			final Answer answer = (answerElement != null && ! answerElement.isJsonNull()) 
					? new Answer(answerElement.getAsJsonObject()) : null;
			answers[i] = answer;
		}

		return answers;
	}

	public Answer getAnswer(int questionIndex) {
		final JsonArray answersArray = getArray(ANSWERS_PROPERTY);
		final Answer answer;
		if (questionIndex < answersArray.size()) {
			final JsonElement answerElement = answersArray.get(questionIndex);
			answer = answerElement != null ? new Answer(answerElement.getAsJsonObject()) : null;
		} else {
			answer = null;
		}
		return answer;
	}

	public void endGame(GameInstance gameInstance) {
		setGameStatus(GameStatus.COMPLETED);
		setGameEndStatus(GameEndStatus.CALCULATING_RESULT);
		fillInMissingAnswersAsEmpty(gameInstance, false);
	}
	
	public void setEntryFeeTransactionId(String transactionId){
		setProperty(ENTRY_FEE_TRANSACTION_ID_PROPERTY, transactionId);
	}

	public String getEntryFeeTransactionId(){
		return getPropertyAsString(ENTRY_FEE_TRANSACTION_ID_PROPERTY);
	}
	
	public boolean hasEntryFeeTransactionId(){
		return !StringUtils.isEmpty(getPropertyAsString(ENTRY_FEE_TRANSACTION_ID_PROPERTY));
	}

	public void startGame() {
		setGameStatus(GameStatus.PREPARED);		
	}

	public void cancelGame() {		
		setGameStatus(GameStatus.CANCELLED);
	}
	
	public boolean hasAnyQuestionAnswered() {
		boolean anyQuestionAnswered = false;
		final Answer[] answers = getAnswers();
		if (ArrayUtils.isNotEmpty(answers)) {
			for (Answer answer : answers) {
				if (ArrayUtils.isNotEmpty(answer.getAnswers())) {
					anyQuestionAnswered = true;
					break;
				}
			}
		}
		return anyQuestionAnswered;
	}

	public void fillInMissingAnswersAsEmpty(GameInstance gameInstance, boolean isProcessExpiredDuel) {
		final List<Answer> answers = new ArrayList<>();
		for (int i = 0; i < gameInstance.getNumberOfQuestionsIncludingSkipped(); i++) {
			Answer answer = getAnswer(i);
			if (answer == null && !isProcessExpiredDuel) {
				answer = getEmptyAnswer(gameInstance, i);
			} else if (isProcessExpiredDuel) {
				answer = getEmptyAnswer(gameInstance, i);
				answer.answer(i, EMPTY_ANSWER, 6900, null, System.currentTimeMillis(), isProcessExpiredDuel);
			}
			answers.add(answer);
			if (isProcessExpiredDuel) {
				Question question = gameInstance.getQuestion(i);
				setCurrentQuestionStep(new QuestionStep(i, 0, question.getStepCount()));
				setGameStatus(GameStatus.COMPLETED);
			}
		}
		final JsonArray actualAnswers = new JsonArray();
		answers.forEach(a -> actualAnswers.add(a.getJsonObject()));
		setProperty(ANSWERS_PROPERTY, actualAnswers);
	}
	
	private Answer getEmptyAnswer(GameInstance gameInstance, int questionNumber) {
		Question question = gameInstance.getQuestion(questionNumber);
		QuestionStep questionStep = new QuestionStep(questionNumber, 0, question.getStepCount());
		Answer answer = new Answer(questionStep, System.currentTimeMillis(), gameInstance.getGame().isLiveGame());
		answer.noAnswer();
		return answer;
	}

	public Integer getCurrentAdvertisementIndex(){
		return getPropertyAsInteger(CURRENT_ADVERTISEMENT_INDEX_PROPERTY);
	}
	
	public void setCurrentAdvertisementIndex(int currentAdvertisementIndex){
		setProperty(CURRENT_ADVERTISEMENT_INDEX_PROPERTY, currentAdvertisementIndex);		
	}

	public boolean isAdvertisementSentBeforeNextStep() {
		return getPropertyAsBoolean(ADVERTISEMENT_SENT_BEFORE_NEXT_STEP_PROPERTY) == Boolean.TRUE;
	}

	public void setAdvertisementSentBeforeNextStep(boolean advertisementSentBeforeNextStep) {
		setProperty(ADVERTISEMENT_SENT_BEFORE_NEXT_STEP_PROPERTY, advertisementSentBeforeNextStep);
	}

	public void readyToPlay() {
		setGameStatus(GameStatus.READY_TO_PLAY);
	}

	public boolean isCompleted() {
		return getGameStatus() == GameStatus.COMPLETED;
	}
	
	public boolean isCancelled() {
		return getGameStatus() == GameStatus.CANCELLED;
	}
	
	public boolean isRefundable(){
		return getPropertyAsBoolean(PROPERTY_REFUNDABLE) == Boolean.TRUE;
	}
	
	private void setRefundable(boolean refundable){
		setProperty(PROPERTY_REFUNDABLE, refundable);
	}
	
	public String getRefundReason(){
		return getPropertyAsString(PROPERTY_REFUND_REASON);
	}
	
	private void setRefundReason(String refundReason){
		setProperty(PROPERTY_REFUND_REASON, refundReason);
	}
	
	public void refund(String refundReason){
		setRefundable(refundReason != null);
		setRefundReason(refundReason);
	}

	public void noRefund() {
		refund(null);		
	}
	
	public boolean hasAllAnswers(int numberOfQuestions) {
		final JsonArray answersArray = getArray(ANSWERS_PROPERTY);
		return answersArray != null && answersArray.size() == numberOfQuestions;
	}
	
	public void setCloseUpExplanation(CloseUpReason closeUpReason, String closeUpErrorMessage){
		setProperty(PROPERTY_CLOSE_UP_REASON, closeUpReason.name());
		setProperty(PROPERTY_CLOSE_UP_ERROR_MESSAGE, closeUpErrorMessage);
	}
	
	public CloseUpReason getCloseUpReason(){
		return F4MEnumUtils.getEnum(CloseUpReason.class, getPropertyAsString(PROPERTY_CLOSE_UP_REASON));
	}
	
	public String getCloseUpErrorMessage(){
		return getPropertyAsString(PROPERTY_CLOSE_UP_ERROR_MESSAGE);
	}
}
