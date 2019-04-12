package de.ascendro.f4m.service.game.engine.model;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import de.ascendro.f4m.service.game.selection.model.game.EntryFee;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.JokerConfiguration;
import de.ascendro.f4m.service.game.selection.model.game.JokerType;
import de.ascendro.f4m.service.json.model.JsonObjectWrapper;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.F4MEnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Game instance in play
 */
public class GameInstance extends JsonObjectWrapper implements EntryFee {

	private static final Logger LOGGER = LoggerFactory.getLogger(GameInstance.class);


	public static final String PROPERTY_ID = "id";

	public static final String PROPERTY_USER_ID = "userId";
    public static final String PROPERTY_TENANT_ID = "tenantId";
    public static final String PROPERTY_APP_ID = "appId";
    public static final String PROPERTY_USER_IP = "userIp";

	public static final String PROPERTY_MGI_ID = "mgiId";
	public static final String PROPERTY_TOURNAMENT_ID = "tournamentId";
	
	public static final String PROPERTY_USER_HANDICAP = "userHandicap";
	public static final String PROPERTY_USER_LANGUAGE = "userLanguage";
	
	public static final String PROPERTY_QUESTION_POOL_IDS = "questionPoolIds";
	public static final String PROPERTY_NUMBER_OF_QUESTIONS = "numberOfQuestions";
	
	// Entry fee
	public static final String PROPERTY_ENTRY_FEE_AMOUNT = "entryFeeAmount";
	public static final String PROPERTY_ENTRY_FEE_CURRENCY = "entryFeeCurrency";

	public static final String PROPERTY_TRAINING_MODE = "trainingMode";
	
	public static final String PROPERTY_GAME = "game";
	public static final String PROPERTY_STATE = "state";
	public static final String PROPERTY_QUESTIONS = "questions";
	
	public static final String PROPERTY_USER_WINNING_COMPONENT_ID = "userWinningComponentId";
	
	private static final String PROPERTY_ADVERTISEMENT_BLOB_KEYS = "advertisementBlobKeys";

	private static final String PROPERTY_LOADING_BLOB_KEY = "loadingBlobKey";
	private static final String PROPERTY_ADVERTISEMENT_DURATION = "advertisementDuration";
	private static final String PROPERTY_LOADING_SCREEN_DURATION = "loadingScreenDuration";
	private static final String PROPERTY_ADVERTISEMENT_SKIPPING = "advertisementSkipping";

	public static final String PROPERTY_START_DATETIME = "startDateTime";
	public static final String PROPERTY_END_DATETIME = "endDateTime";

	public static final String PROPERTY_JOKERS_USED = "jokersUsed";

	public static final String PROPERTY_TYPE_CONFIGURATION = "typeConfiguration";
	public static final String PROPERTY_QUIZ24 = "gameQuiz24";
	

	private Gson gson = new Gson();
	
	public GameInstance(Game game) {
		this.jsonObject = new JsonObject();
		setGame(game);
		setJokersUsed(new HashMap<>());
	}

	public GameInstance(JsonObject jsonObject) {
		this.jsonObject = jsonObject;
	}

	public void setUserId(String userId) {
		setProperty(PROPERTY_USER_ID, userId);
	}

	public String getUserId() {
		return getPropertyAsString(PROPERTY_USER_ID);
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

    public String getUserIp() {
        return getPropertyAsString(PROPERTY_USER_IP);
    }

    public void setUserIp(String userIp) {
        setProperty(PROPERTY_USER_IP, userIp);
    }

	public String getMgiId() {
		return getPropertyAsString(PROPERTY_MGI_ID);
	}
	
	public void setMgiId(String mgiId) {
		setProperty(PROPERTY_MGI_ID, mgiId);
	}

	public String getTournamentId() {
		return getPropertyAsString(PROPERTY_TOURNAMENT_ID);
	}
	
	public void setTournamentId(String tournamentId) {
		setProperty(PROPERTY_TOURNAMENT_ID, tournamentId);
	}

	public String[] getAllQuestionBlobKeys() {
		return getAllKeys(i -> getQuestion(i).getQuestionBlobKeys());
	}
	
	public String[] getAllQuestionImageBlobKeys() {
		return getAllKeys(i -> getQuestion(i).getQuestionImageBlobKeys());
	}

	public String[] getAllDecryptionKeys() {
		return getAllKeys(i -> getQuestion(i).getDecryptionKeys());
	}
	
	private String[] getAllKeys(Function<Integer, Set<String>> getKeysFunction) {
		final Set<String> keys = new HashSet<>();

		if(getQuestionsMap() != null){
			IntStream.range(0, getNumberOfQuestions() + getGame().getTotalSkipsAvailable())
				.forEach(i -> keys.addAll(getKeysFunction.apply(i)));
		}
		
		return keys.toArray(new String[keys.size()]);
	}

	public String getId() {
		return getPropertyAsString(PROPERTY_ID);
	}

	public void setId(String id) {
		setProperty(PROPERTY_ID, id);
	}

	public void setQuestions(List<Question> questions) {
		final JsonObject questionsMap = new JsonObject();

		for (int i = 0; i < questions.size(); i++) {
			questionsMap.add(String.valueOf(i), gson.toJsonTree(questions.get(i)));
		}

		setQuestionMap(questionsMap);
	}

	public Question getQuestion(int index) {
		JsonObject question = null;

		final JsonObject questionsMap = getPropertyAsJsonObject(PROPERTY_QUESTIONS);
		if (questionsMap != null) {
			final JsonElement questionElement = questionsMap.get(String.valueOf(index));
			if (questionElement != null) {
				question = questionElement.getAsJsonObject();
			}
		}

		return question != null ? gson.fromJson(question, Question.class) : null;
	}

	public JsonObject getQuestionsMap() {
		return getPropertyAsJsonObject(PROPERTY_QUESTIONS);
	}

	public void setQuestionMap(JsonObject questionsMap) {
		jsonObject.add(PROPERTY_QUESTIONS, questionsMap);
	}

	public boolean isOfflineGame() {
		return getGame().isOffline();
	}

	public void setGame(Game game) {
		jsonObject.add(PROPERTY_GAME, gson.toJsonTree(game));
	}

	public Game getGame() {
		final JsonElement gameElement = jsonObject.get(PROPERTY_GAME);
		final Game game;
		if (gameElement != null) {
			game = gson.fromJson(gameElement, Game.class);
		} else {
			game = null;
		}
		return game;
	}
	
	public void setGameState(GameState gameState){
		if(gameState != null){
			jsonObject.add(PROPERTY_STATE, gameState.getJsonObject());
		}
	}
	
	public GameState getGameState(){
		final JsonElement stateElement = jsonObject.get(PROPERTY_STATE);
		final GameState gameState;		
		if(stateElement != null){
			gameState = new GameState(stateElement.getAsJsonObject());
		}else{
			gameState = null;
		}
		return gameState;
	}

	@Override
	public JsonObject getJsonObject() {
		if (jsonObject != null) {
			jsonObject.add(PROPERTY_GAME, gson.toJsonTree(getGame()));
		}
		return super.getJsonObject();
	}
	
	public String[] getQuestionIds(){
		return getQuestionsMap().entrySet().stream()
				.map(e-> e.getValue().getAsJsonObject().get(Question.ID_PROPERTY).getAsString())
				.toArray(String[]::new);
	}
	
	public Double getUserHandicap(){
		return getPropertyAsDoubleObject(PROPERTY_USER_HANDICAP);
	}
	
	public void setUserHandicap(Double userHandicap){
		setProperty(PROPERTY_USER_HANDICAP, userHandicap);
	}
	
	public String getUserLanguage(){
		return getPropertyAsString(PROPERTY_USER_LANGUAGE);
	}
	
	public void setUserLanguage(String userLanguage){
		setProperty(PROPERTY_USER_LANGUAGE, userLanguage);
	}
	
	public boolean instantlySendAnswer() {
		Game game = getGame();
		return game == null ? false : game.isInstantAnswerFeedback();
	}

	public boolean isTrainingMode() {
		Boolean trainingMode = getPropertyAsBoolean(PROPERTY_TRAINING_MODE);
		return trainingMode == null ? false : trainingMode;
	}
	
	public void setTrainingMode(boolean trainingMode){
		setProperty(PROPERTY_TRAINING_MODE, trainingMode);
	}
	
	public void setNumberOfQuestions(int numberOfQuestions) {
		setProperty(PROPERTY_NUMBER_OF_QUESTIONS, numberOfQuestions);
	}

	public int getNumberOfQuestions() {
		return getPropertyAsInt(PROPERTY_NUMBER_OF_QUESTIONS);
	}

	public void setQuestionPoolIds(String[] questionPoolIds) {
		setArray(PROPERTY_QUESTION_POOL_IDS, questionPoolIds);
	}

	public String[] getQuestionPoolIds() {
		return getPropertyAsStringArray(PROPERTY_QUESTION_POOL_IDS);
	}

	public void setUserWinningComponentId(String userWinningComponentId,String winningComponentType) {
		setProperty(PROPERTY_USER_WINNING_COMPONENT_ID+"_"+winningComponentType, userWinningComponentId);
	}

	public String getUserWinningComponentId(String winningComponentType) {
		return getPropertyAsString(PROPERTY_USER_WINNING_COMPONENT_ID+"_"+winningComponentType);
	}
	
	public String[] getAdvertisementBlobKeys() {
		return getPropertyAsStringArray(PROPERTY_ADVERTISEMENT_BLOB_KEYS);
	}
	
	public void setAdvertisementBlobKeys(String[] advertisementBlobKeys) {
		setArray(PROPERTY_ADVERTISEMENT_BLOB_KEYS, advertisementBlobKeys);
	}

	public String getLoadingBlobKey() {
		return getPropertyAsString(PROPERTY_LOADING_BLOB_KEY);
	}

	public void setLoadingBlobKey(String loadingBlobKey) {
		setProperty(PROPERTY_LOADING_BLOB_KEY, loadingBlobKey);
	}

	public int getLoadingScreenDuration() {
		return getPropertyAsInt(PROPERTY_LOADING_SCREEN_DURATION);
	}

	public void setLoadingScreenDuration(String loadingScreenDuration) {
		setProperty(PROPERTY_LOADING_SCREEN_DURATION, loadingScreenDuration);
	}

	public int getAdvertisementDuration() {
		return getPropertyAsInt(PROPERTY_ADVERTISEMENT_DURATION);
	}

	public void setAdvertisementDuration(String advertisementDuration) {
		setProperty(PROPERTY_ADVERTISEMENT_DURATION, advertisementDuration);
	}

	public boolean getAdvertisementSkipping() {
		return getPropertyAsBoolean(PROPERTY_ADVERTISEMENT_SKIPPING);
	}

	public void setAdvertisementSkipping(String advertisementSkipping) {
		setProperty(PROPERTY_ADVERTISEMENT_SKIPPING, advertisementSkipping);
	}
	
	public void startPlaying(long messageReceiveTimestamp){
		final int firstQuestionStepCount = getQuestion(0).getStepCount();
		final GameState gameState = getGameState();
		gameState.startPlaying(firstQuestionStepCount, messageReceiveTimestamp, getGame().isLiveGame());
	}
	
	@Override
	public Currency getEntryFeeCurrency() {
		return F4MEnumUtils.getEnum(Currency.class, getPropertyAsString(PROPERTY_ENTRY_FEE_CURRENCY));
	}
	
	public void setEntryFeeCurrency(Currency currency) {
		if (currency != null) {
			setProperty(PROPERTY_ENTRY_FEE_CURRENCY, currency.toString());
		} else {
			jsonObject.remove(PROPERTY_ENTRY_FEE_CURRENCY);
		}
	}
	
	@Override
	public BigDecimal getEntryFeeAmount() {
		return getPropertyAsBigDecimal(PROPERTY_ENTRY_FEE_AMOUNT);
	}
	
	public void setEntryFeeAmount(BigDecimal amount) {
		if (amount != null) {
			setProperty(PROPERTY_ENTRY_FEE_AMOUNT, amount);
		} else {
			jsonObject.remove(PROPERTY_ENTRY_FEE_AMOUNT);
		}
	}
	
	public boolean hasEntryFee() {
		return getEntryFeeAmount() != null && getEntryFeeCurrency() != null;
	}

	/**
	 * @return date-time of game instance starting (ge/startGame)
	 */
	public ZonedDateTime getStartDateTime(){
		return getPropertyAsZonedDateTime(PROPERTY_START_DATETIME);
	}

	public void setStartDateTime(ZonedDateTime startDateTime){
		setProperty(PROPERTY_START_DATETIME, startDateTime);
	}

	/**
	 * @return date-time game instance completion
	 */
	public ZonedDateTime getEndDateTime(){
		return getPropertyAsZonedDateTime(PROPERTY_END_DATETIME);
	}

	public void setEndDateTime(ZonedDateTime endDateTime){
		setProperty(PROPERTY_END_DATETIME, endDateTime);
	}

	public Long getDurationInSeconds() {
		long result = 0;

		final ZonedDateTime startDateTime = getStartDateTime();
		final ZonedDateTime endDateTime = getEndDateTime();
		if (startDateTime != null && endDateTime != null) {
			result = DateTimeUtil.getSecondsBetween(startDateTime, endDateTime);
		}

		return result;
	}

	public boolean hasAnyStepOrQuestion() {
		final GameState gameState = getGameState();
		final int numberOfQuestions = getNumberOfQuestionsIncludingSkipped();
		return gameState.hasNextQuestion(numberOfQuestions) || gameState.getCurrentQuestionStep().hasNextStep();
	}

	public boolean hasAnyQuestion() {
		final GameState gameState = getGameState();
		final int numberOfQuestions = getNumberOfQuestionsIncludingSkipped();
		return gameState.hasNextQuestion(numberOfQuestions);
	}

	public boolean hasAdvertisements() {
		return getGame().hasAdvertisements()
				&& jsonObject.has(PROPERTY_ADVERTISEMENT_BLOB_KEYS)
				&& jsonObject.get(PROPERTY_ADVERTISEMENT_BLOB_KEYS).isJsonArray();
	}

	public boolean hasLoadingScreen() {
		return getGame().hasLoadingScreen()
				&& jsonObject.has(PROPERTY_LOADING_BLOB_KEY);
	}
	
	@Override
	public boolean isFree() {
		return getEntryFeeCurrency() == null || getEntryFeeAmount() == null
				|| BigDecimal.ZERO.compareTo(getEntryFeeAmount()) <= 0;
	}

	public long getGameDuration() {
		if (getStartDateTime() != null && getEndDateTime() != null) {
			return Duration.between(getStartDateTime(), getEndDateTime()).getSeconds();
		} else
			return 0;
	}


	public void setJokersUsed(Map<JokerType, Set<Integer>> jokersUsed) {
		final JsonObject jokersUsedMap = new JsonObject();

		for(Map.Entry<JokerType, Set<Integer>> item : jokersUsed.entrySet()){
			final JsonArray questionIndexes = new JsonArray();
			item.getValue().forEach(questionIndexes::add);
			jokersUsedMap.add(item.getKey().getValue(), questionIndexes);
		}

		setJokersUsedMap(jokersUsedMap);
	}

	public Set<Integer> getJokersUsed(JokerType jokerType) {
		Set<Integer> jokersUsed = getJokersUsed().get(jokerType);
		return jokersUsed == null ? Collections.emptySet() : jokersUsed;
	}
	
	public Map<JokerType, Set<Integer>> getJokersUsed() {
		Map<JokerType, Set<Integer>> result = new EnumMap<>(JokerType.class);

		final JsonObject jokersJsonMap = getJokersUsedMap();
		if (jokersJsonMap != null) {
			Type questionIndexesType = new TypeToken<Set<Integer>>(){}.getType();
			for(Map.Entry<String, JsonElement> questionIndexes : jokersJsonMap.entrySet()) {
				Set<Integer> questionIndexesSet = gson.fromJson(questionIndexes.getValue(), questionIndexesType);
				result.put(JokerType.fromString(questionIndexes.getKey()), questionIndexesSet);
			}
		}
		return result;
	}

	/**
	 * Get the configuration of the given joker type.
	 * @return Configuration or <code>null</code> if joker not available.
	 */
	public JokerConfiguration getJokerConfigurationIfAvailable(JokerType jokerType) {
		Game game = getGame();
		JokerConfiguration jokerConfig = game.getJokerConfiguration().get(jokerType);
		Set<Integer> jokersUsed = getJokersUsed().get(jokerType);
		
		if (jokerConfig == null || ! jokerConfig.isEnabled()) {
			return null;
		}
		
		if (jokerConfig.getAvailableCount() == null ||
				(jokerConfig.getAvailableCount() - (jokersUsed == null ? 0 : jokersUsed.size())) > 0) {
			return jokerConfig;
		} else {
			return null;
		}
	}
	
	private JsonObject getJokersUsedMap() {
		return getPropertyAsJsonObject(PROPERTY_JOKERS_USED);
	}

	private void setJokersUsedMap(JsonObject jokersUsedMap) {
		jsonObject.add(PROPERTY_JOKERS_USED, jokersUsedMap);
	}

	/**
	 * Get the hint for the question.
	 */
	public String getQuestionHint(int questionIndex) {
		Question question = getQuestion(questionIndex);
		return question == null ? null : question.getHint();
	}

	/**
	 * Get the answers to be removed when 50/50 chance joker is played.
	 */
	public String[] calculateQuestionRemovedAnswers(int questionIndex) {
		Question question = getQuestion(questionIndex);
		return question == null ? null : question.calculateRemovedAnswers();
	}

	/**
	 * Have to add questions as many skips are used.
	 */
	public int getNumberOfQuestionsIncludingSkipped() {
		return getNumberOfQuestions() + getSkipsUsed();
	}
	
	public int getSkipsUsed() {
		Set<Integer> skipsUsed = getJokersUsed().get(JokerType.SKIP);
		return skipsUsed == null ? 0 : skipsUsed.size();
	}
	
	
}
