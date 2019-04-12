package de.ascendro.f4m.server.result;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.ascendro.f4m.service.game.engine.model.Answer;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.engine.model.Question;
import de.ascendro.f4m.service.json.model.JsonObjectWrapper;
import de.ascendro.f4m.service.payment.model.Currency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

import static de.ascendro.f4m.service.util.F4MEnumUtils.getEnum;

/**
 * Results of a single game.
 */
public class Results extends JsonObjectWrapper {

	private static final Logger LOGGER = LoggerFactory.getLogger(Results.class);

	public static final String PROPERTY_RESULT_ITEMS = "resultItems";
	public static final String PROPERTY_ANSWER_RESULTS = "answerResults";
	public static final String PROPERTY_GAME_INSTANCE_ID = "gameInstanceId";
	public static final String PROPERTY_GAME_ID = "gameId";
	public static final String PROPERTY_USER_ID = "userId";
	public static final String PROPERTY_TENANT_ID = "tenantId";
	public static final String PROPERTY_APP_ID = "appId";
	public static final String PROPERTY_CLIENT_IP = "clientIp";
	public static final String PROPERTY_USER_INFO = "userInfo";
	public static final String PROPERTY_MULTIPLAYER_GAME_INSTANCE_ID = "multiplayerGameInstanceId";
	public static final String PROPERTY_USER_HANDICAP = "userHandicap";
	public static final String PROPERTY_GAME_FINISHED = "gameFinished";
	public static final String PROPERTY_GAME_OUTCOME = "gameOutcome";
	public static final String PROPERTY_TRAINING_MODE = "trainingMode";
	public static final String PROPERTY_HANDICAP_RANGE_ID = "handicapRangeId";
	public static final String PROPERTY_HANDICAP_FROM = "handicapFrom";
	public static final String PROPERTY_HANDICAP_TO = "handicapTo";
	public static final String PROPERTY_USER_INTERACTIONS = "userInteractions";
	public static final String PROPERTY_ELIGIBLE_TO_WINNINGS = "eligibleToWinnings";
	public static final String PROPERTY_PAID_WINNING_COMPONENT_ID = "paidWinningComponentId";
	public static final String PROPERTY_FREE_WINNING_COMPONENT_ID = "freeWinningComponentId";
	public static final String PROPERTY_USER_WINNING_COMPONENT_ID = "userWinningComponentId";
	public static final String PROPERTY_SPECIAL_PRIZE_VOUCHER_ID = "specialPrizeVoucherId";
	public static final String PROPERTY_ENTRY_FEE_PAID = "entryFeePaid";
	public static final String PROPERTY_ENTRY_FEE_CURRENCY = "entryFeeCurrency";
	public static final String PROPERTY_REFUND_REASON = "refundReason";
	public static final String PROPERTY_JACKPOT_WINNING = "jackpotWinning";
	public static final String PROPERTY_JACKPOT_WINNING_CURRENCY = "jackpotWinningCurrency";
	public static final String PROPERTY_DUEL_OPPONENT_GAME_INSTANCE_ID = "duelOpponentGameInstanceId";
	public static final String PROPERTY_EXPIRY_DATETIME = "expiryDateTime";
	public static final String PROPERTY_END_DATETIME = "endDateTime";

	public Results() {
		// Initialize empty results
	}
	
	public Results(String gameInstanceId, String gameId, String userId, String tenantId, String appId, 
			String clientIp, String multiplayerGameInstanceId, Double userHandicap, boolean gameFinished) {
		setGameInstanceId(gameInstanceId);
		setGameId(gameId);
		setUserId(userId);
		setTenantId(tenantId);
		setAppId(appId);
		setClientIp(clientIp);
		setMultiplayerGameInstanceId(multiplayerGameInstanceId);
		setUserHandicap(userHandicap == null ? 0.0 : userHandicap);
		setGameFinished(gameFinished);
	}
	
	public Results(JsonObject results) {
		super(results);
	}
	
	public String getGameInstanceId() {
		return getPropertyAsString(PROPERTY_GAME_INSTANCE_ID);
	}
	
	public void setGameInstanceId(String gameInstanceId) {
		setProperty(PROPERTY_GAME_INSTANCE_ID, gameInstanceId);
	}

	public String getGameId() {
		return getPropertyAsString(PROPERTY_GAME_ID);
	}
	
	public void setGameId(String gameId) {
		setProperty(PROPERTY_GAME_ID, gameId);
	}

	public String getUserId() {
		return getPropertyAsString(PROPERTY_USER_ID);
	}
	
	public void setUserId(String userId) {
		setProperty(PROPERTY_USER_ID, userId);
	}
	
	public String getTenantId() {
		return getPropertyAsString(PROPERTY_TENANT_ID);
	}
	
	public void setTenantId(String tenantId) {
		setProperty(PROPERTY_TENANT_ID, tenantId);
	}
	
	public String getAppId() {
		return getPropertyAsString(PROPERTY_APP_ID);
	}
	
	public void setAppId(String appId) {
		setProperty(PROPERTY_APP_ID, appId);
	}
	
	public String getClientIp() {
		return getPropertyAsString(PROPERTY_CLIENT_IP);
	}
	
	public void setClientIp(String clientIp) {
		setProperty(PROPERTY_CLIENT_IP, clientIp);
	}
	
	public JsonObject getUserInfo() {
		return getPropertyAsJsonObject(PROPERTY_USER_INFO);
	}
	
	public void setUserInfo(JsonElement userInfo) {
		setProperty(PROPERTY_USER_INFO, userInfo);
	}

	public String getMultiplayerGameInstanceId() {
		return getPropertyAsString(PROPERTY_MULTIPLAYER_GAME_INSTANCE_ID);
	}
	
	public void setMultiplayerGameInstanceId(String multiplayerGameInstanceId) {
		setProperty(PROPERTY_MULTIPLAYER_GAME_INSTANCE_ID, multiplayerGameInstanceId);
	}

	public double getUserHandicap() {
		return getPropertyAsDouble(PROPERTY_USER_HANDICAP);
	}
	
	public Double getNewUserHandicap() {
		ResultItem newHandicap = getResultItems().get(ResultType.NEW_HANDICAP);
		return newHandicap == null ? null : newHandicap.getAmount();
	}
	
	public void setUserHandicap(double userHandicap) {
		setProperty(PROPERTY_USER_HANDICAP, userHandicap);
	}
	
	public boolean isGameFinished() {
		return getPropertyAsBoolean(PROPERTY_GAME_FINISHED);
	}
	
	public void setGameFinished(boolean gameFinished) {
		setProperty(PROPERTY_GAME_FINISHED, gameFinished);
	}
	
	public GameOutcome getGameOutcome() {
		return GameOutcome.fromString(getPropertyAsString(PROPERTY_GAME_OUTCOME));
	}
	
	public void setGameOutcome(GameOutcome gameOutcome) {
		setProperty(PROPERTY_GAME_OUTCOME, gameOutcome.getValue());
	}

	public boolean getTrainingMode(){
		return getPropertyAsBoolean(PROPERTY_TRAINING_MODE);
	}

	public void setTrainingMode(boolean trainingMode){
		setProperty(PROPERTY_TRAINING_MODE, trainingMode);
	}

	public int getHandicapRangeId() {
		return getPropertyAsInt(PROPERTY_HANDICAP_RANGE_ID);
	}
	
	public void setHandicapRangeId(int handicapRangeId) {
		setProperty(PROPERTY_HANDICAP_RANGE_ID, handicapRangeId);
	}
	
	public int getHandicapFrom() {
		return getPropertyAsInt(PROPERTY_HANDICAP_FROM);
	}
	
	public void setHandicapFrom(int handicapFrom) {
		setProperty(PROPERTY_HANDICAP_FROM, handicapFrom);
	}
	
	public int getHandicapTo() {
		return getPropertyAsInt(PROPERTY_HANDICAP_TO);
	}
	
	public void setHandicapTo(int handicapTo) {
		setProperty(PROPERTY_HANDICAP_TO, handicapTo);
	}
	
	public Map<ResultType, ResultItem> getResultItems() {
		JsonArray resultItems = getArray(PROPERTY_RESULT_ITEMS);
		if (resultItems == null || resultItems.size() == 0) {
			return Collections.emptyMap();
		}
		Map<ResultType, ResultItem> results = new EnumMap<>(ResultType.class);
		resultItems.forEach(item -> {
			if (item.isJsonObject()) {
				ResultItem resultItem = new ResultItem(item.getAsJsonObject());
				if (resultItem.getResultType() != null) {
					results.put(resultItem.getResultType(), resultItem);
				}
			}
		});
		return results;
	}

	public void addResultItem(ResultItem resultItem) {
		JsonArray resultItems = getArray(PROPERTY_RESULT_ITEMS);
		if (resultItems == null) {
			resultItems = new JsonArray();
			jsonObject.add(PROPERTY_RESULT_ITEMS, resultItems);
		}
		resultItems.add(resultItem.getJsonObject());
	}

	public Map<Integer, AnswerResults> getAnswerResults() {
		JsonArray answerResults = getArray(PROPERTY_ANSWER_RESULTS);
		if (answerResults == null || answerResults.size() == 0) {
			return Collections.emptyMap();
		}
		Map<Integer, AnswerResults> results = new HashMap<>();
		answerResults.forEach(item -> {
			if (item.isJsonObject()) {
				AnswerResults resultItem = new AnswerResults(item.getAsJsonObject());
				results.put(resultItem.getQuestionIndex(), resultItem);
			}
		});
		return results;
	}


	// update the answers
	public void addOriginalOrderAnswersToQuestionInfo(GameInstance gameInstance) {
		if (gameInstance != null) getArray(PROPERTY_ANSWER_RESULTS).forEach(item -> addOriginalOrderAnswers(item, gameInstance));
	}

	// adds the originalOrderAnswers field to question
	private void addOriginalOrderAnswers(JsonElement jsonObject, GameInstance gameInstance) {
		if (jsonObject.isJsonObject()) {
			AnswerResults resultItem = new AnswerResults(jsonObject.getAsJsonObject());
			Question question = gameInstance.getQuestion(resultItem.getQuestionIndex());
			if (question != null && question.getAnswers() != null)
				resultItem.addOriginalOrderAnswers(question.getAnswers());
		}
	}




	public Map<Integer, AnswerResults> getCorrectAnswerResults() {
		Map<Integer, AnswerResults> correctAnswerResults = new HashMap<>();
		getAnswerResults().forEach((index, answerResults) -> {
			if (answerResults.getQuestionInfo().isAnsweredCorrectly()) {
				correctAnswerResults.put(index, answerResults);
			}
		});
		return correctAnswerResults;
	}
	
	public int getCorrectAnswerCount() {
		int count = 0;
		for (AnswerResults answerResults : getAnswerResults().values()) {
			if (answerResults.getQuestionInfo().isAnsweredCorrectly()) {
				count++;
			}
		}
		return count;
	}
	
	public int getTotalQuestionCount() {
		return (int) getResultItems().get(ResultType.TOTAL_QUESTIONS).getAmount();
	}

	public int getPlacement() {
		ResultItem placeResultItem = getResultItems().get(ResultType.PLACE);
		int result = 0;
		if (placeResultItem != null) {
			// ammount parameter is generic and can hold double or int depending on type of result item,
			// so cast is ok here as placement is always int
			result = (int) placeResultItem.getAmount();
		}
		return result;
	}
	
	public AnswerResults addCorrectAnswer(int questionIndex, Question question, Answer answer) {
		QuestionInfo questionInfo = new QuestionInfo(questionIndex, question, answer, true, false);
		return addAnswer(questionInfo);
	}

	public AnswerResults addIncorrectAnswer(int questionIndex, Question question, Answer answer) {
		QuestionInfo questionInfo = new QuestionInfo(questionIndex, question, answer, false, false);
		return addAnswer(questionInfo);
	}

	public AnswerResults addSkippedAnswer(int questionIndex, Question question) {
		QuestionInfo questionInfo = new QuestionInfo(questionIndex, question, null, false, true);
		return addAnswer(questionInfo);
	}

	private AnswerResults addAnswer(QuestionInfo questionInfo) {
		JsonArray answerResults = getArray(PROPERTY_ANSWER_RESULTS);
		if (answerResults == null) {
			answerResults = new JsonArray();
			jsonObject.add(PROPERTY_ANSWER_RESULTS, answerResults);
		}
		AnswerResults results = new AnswerResults(questionInfo.getQuestionIndex());
		results.addQuestionInfo(questionInfo);
		answerResults.add(results.getJsonObject());
		return results;
	}
	
	public double getGamePointsWithBonus() {
		ResultItem gamePointsWithBonus = getResultItems().get(ResultType.TOTAL_GAME_POINTS_WITH_BONUS);
		return gamePointsWithBonus == null ? 0.0 : gamePointsWithBonus.getAmount();
	}

	public Integer getBonusPointsWon() {
		ResultItem bonusPoints = getResultItems().get(ResultType.BONUS_POINTS);
		return bonusPoints == null ? null : (int) bonusPoints.getAmount();
	}

	public void setBonusPointsWon(int amount) {
		JsonArray resultItems = getArray(PROPERTY_RESULT_ITEMS);
		if (resultItems == null) {
			resultItems = new JsonArray();
			jsonObject.add(PROPERTY_RESULT_ITEMS, resultItems);
		}
		for(int i = 0; i < resultItems.size(); i++)
		{
			ResultItem resultItem = new ResultItem(resultItems.get(i).getAsJsonObject());
			if(resultItem.getResultType() == ResultType.BONUS_POINTS)
			{
				resultItem.setAmount(amount);
				resultItems.set(i, resultItem.getJsonObject());
				return;
			}
		}
		ResultItem resultItem = new ResultItem(ResultType.BONUS_POINTS, amount);
		resultItems.add(resultItem.getJsonObject());
	}

	public Set<UserInteractionType> getUserInteractions() {
		JsonArray userInteractions = getArray(PROPERTY_USER_INTERACTIONS);
		if (userInteractions == null || userInteractions.size() == 0) {
			return Collections.emptySet();
		} else {
			Set<UserInteractionType> result = EnumSet.noneOf(UserInteractionType.class);
			userInteractions.forEach(interaction -> {
				UserInteractionType interactionType = UserInteractionType.fromString(interaction.getAsString());
				if (interactionType != null) {
					result.add(interactionType);
				}
			});
			return result;
		}
	}
	
	public void addUserInteraction(UserInteractionType userInteraction) {
		LOGGER.debug("addUserInteraction userInteraction {} ", userInteraction);
		JsonArray userInteractions = getArray(PROPERTY_USER_INTERACTIONS);
		if (userInteractions == null) {
			userInteractions = new JsonArray();
			setProperty(PROPERTY_USER_INTERACTIONS, userInteractions);
		}
		userInteractions.add(userInteraction.getValue());
	}
	
	public void removeUserInteraction(UserInteractionType userInteraction) {
		LOGGER.debug("removeUserInteraction userInteraction {} ", userInteraction);
		JsonArray userInteractions = getArray(PROPERTY_USER_INTERACTIONS);
		String interaction = userInteraction.getValue();
		if (userInteractions != null) {
			for (Iterator<JsonElement> it = userInteractions.iterator() ; it.hasNext() ; ) {
				if (interaction.equals(it.next().getAsString())) {
					it.remove();
					break;
				}
			}
		}
	}

	public boolean isEligibleToWinnings() {
		Boolean result = getPropertyAsBoolean(PROPERTY_ELIGIBLE_TO_WINNINGS);
		return result == null ? false : result;
	}
	
	public void setEligibleToWinnings(boolean eligibleToWinnings) {
		setProperty(PROPERTY_ELIGIBLE_TO_WINNINGS, eligibleToWinnings);
	}
	
	public String getPaidWinningComponentId() {
		return getPropertyAsString(PROPERTY_PAID_WINNING_COMPONENT_ID);
	}

	public void setPaidWinningComponentId(String paidWinningComponentId) {
		setProperty(PROPERTY_PAID_WINNING_COMPONENT_ID, paidWinningComponentId);
	}
	
	public String getFreeWinningComponentId() {
		return getPropertyAsString(PROPERTY_FREE_WINNING_COMPONENT_ID);
	}

	public void setFreeWinningComponentId(String freeWinningComponentId) {
		setProperty(PROPERTY_FREE_WINNING_COMPONENT_ID, freeWinningComponentId);
	}
	
	public String getUserWinningComponentId() {
		return getPropertyAsString(PROPERTY_USER_WINNING_COMPONENT_ID);
	}

	public void setUserWinningComponentId(String userWinningComponentId) {
		setProperty(PROPERTY_USER_WINNING_COMPONENT_ID, userWinningComponentId);
	}
	
	public String getSpecialPrizeVoucherId() {
		return getPropertyAsString(PROPERTY_SPECIAL_PRIZE_VOUCHER_ID);
	}
	
	public void setSpecialPrizeVoucherId(String specialPrizeVoucherId) {
		setProperty(PROPERTY_SPECIAL_PRIZE_VOUCHER_ID, specialPrizeVoucherId);
	}
	
	public BigDecimal getEntryFeePaid() {
		return getPropertyAsBigDecimal(PROPERTY_ENTRY_FEE_PAID);
	}

	public void setEntryFeePaid(BigDecimal entryFeePaid, Currency currency) {
		if (entryFeePaid == null) {
			jsonObject.remove(PROPERTY_ENTRY_FEE_PAID);
		} else {
			setProperty(PROPERTY_ENTRY_FEE_PAID, entryFeePaid);
		}
		if (currency == null) {
			jsonObject.remove(PROPERTY_ENTRY_FEE_CURRENCY);
		} else {
			setProperty(PROPERTY_ENTRY_FEE_CURRENCY, currency.name());
		}
	}
	
	public Currency getEntryFeeCurrency() {
		return getEnum(Currency.class, getPropertyAsString(PROPERTY_ENTRY_FEE_CURRENCY));
	}
	
	public RefundReason getRefundReason() {
		return RefundReason.fromString(getPropertyAsString(PROPERTY_REFUND_REASON));
	}
	
	public void setRefundReason(RefundReason refundReason) {
		if (refundReason != null) {
			setProperty(PROPERTY_REFUND_REASON, refundReason.getValue());
		} else {
			jsonObject.remove(PROPERTY_REFUND_REASON);
		}
	}
	
	public BigDecimal getJackpotWinning() {
		return getPropertyAsBigDecimal(PROPERTY_JACKPOT_WINNING);
	}
	
	public Currency getJackpotWinningCurrency() {
		return getEnum(Currency.class, getPropertyAsString(PROPERTY_JACKPOT_WINNING_CURRENCY));
	}

	public void setJackpotWinning(Currency currency) {
		if (currency == null) {
			jsonObject.remove(PROPERTY_JACKPOT_WINNING_CURRENCY);
		} else {
			setProperty(PROPERTY_JACKPOT_WINNING_CURRENCY, currency.name());
		}
	}
	public void setJackpotWinning(BigDecimal jackpotWinning, Currency currency) {
		if (jackpotWinning == null) {
			jsonObject.remove(PROPERTY_JACKPOT_WINNING);
		} else {
			setProperty(PROPERTY_JACKPOT_WINNING, jackpotWinning);
		}
		if (currency == null) {
			jsonObject.remove(PROPERTY_JACKPOT_WINNING_CURRENCY);
		} else {
			setProperty(PROPERTY_JACKPOT_WINNING_CURRENCY, currency.name());
		}
	}

	public void addJackpotWinning(BigDecimal jackpotWinning, Currency currency) {
		if (getPropertyAsBigDecimal(PROPERTY_JACKPOT_WINNING) == null) {
			setProperty(PROPERTY_JACKPOT_WINNING, jackpotWinning);
			if (currency == null) {
				jsonObject.remove(PROPERTY_JACKPOT_WINNING_CURRENCY);
			} else {
				setProperty(PROPERTY_JACKPOT_WINNING_CURRENCY, currency.name());
			}
		} else {
			setProperty(PROPERTY_JACKPOT_WINNING, getPropertyAsBigDecimal(PROPERTY_JACKPOT_WINNING).add(jackpotWinning));
		}

	}
	
	public String getDuelOpponentGameInstanceId() {
		return getPropertyAsString(PROPERTY_DUEL_OPPONENT_GAME_INSTANCE_ID);
	}
	
	public void setDuelOpponentGameInstanceId(String duelOpponentGameInstanceId) {
		setProperty(PROPERTY_DUEL_OPPONENT_GAME_INSTANCE_ID, duelOpponentGameInstanceId);
	}

	public ZonedDateTime getExpiryDateTime() {
		return getPropertyAsZonedDateTime(PROPERTY_EXPIRY_DATETIME);
	}

	public void setExpiryDateTime(ZonedDateTime expiryDateTime) {
		setProperty(PROPERTY_EXPIRY_DATETIME, expiryDateTime);
	}

	public ZonedDateTime getEndDateTime() {
		return getPropertyAsZonedDateTime(PROPERTY_END_DATETIME);
	}

	public void setEndDateTime(ZonedDateTime endDateTime) {
		setProperty(PROPERTY_END_DATETIME, endDateTime);
	}


}
