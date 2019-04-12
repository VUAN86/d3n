package de.ascendro.f4m.service.game.selection.model.game;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.util.DateTimeUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static de.ascendro.f4m.service.util.F4MEnumUtils.getEnum;

public class Game implements EntryFee, QuestionPoolSelectionProperties{

	private static final int DEFAULT_BATCH_SIZE = 1;

	public static final int DUEL_PARTICIPANT_COUNT = 2;

	private static final Boolean DEFAULT_MULTIPLE_PURCHASE_ALLOWED = true;

	public static final String ENTRY_FEE_TYPE_FIXED = "FIXED";
	public static final String ENTRY_FEE_TYPE_STEP = "STEP";
	public static final String ENTRY_FEE_TYPE_DEFAULT = "STEP";

	private String gameId;
	private String tenantId;
	private String[] applications;
	
	private String multiplayerGameInstanceId;
	private String title;
	private String description;
	private String type;
	private Integer numberOfQuestions;
	private Double handicap;
	private String prizes;
	private JsonObject shortPrize;
	
	@SerializedName(value="isFree")
	private boolean free = true;
	@SerializedName(value="isSpecialPrices")
	private boolean hasSpecialPrize;
	@SerializedName(value="isOffline")
	private boolean offline;
	
	private String[] playingRegions;
	private String[] playingLanguages;
	/**
	 * Game visibility start date-time
	 */
	private String startDateTime;
	/**
	 *  Game visibility end date-time
	 */
	private String endDateTime;
	/**
	 *  Live tournament's start date-time
	 */
	private ZonedDateTime liveTournamentDateTime;
	private Boolean instantAnswerFeedback;
	private Integer instantAnswerFeedbackDelay;
	private Boolean entryFeeDecidedByPlayer;
	private Boolean userCanOverridePools;
	private Integer timeToAcceptInvites;
	private JsonObject resultConfiguration;
	private JokerConfiguration[] jokerConfiguration;
	private String pictureId;
	private String iconId;
	private String imageId;

	// Question pools
	private Boolean hideCategories;
	private String[] assignedPools;
	private String[] assignedPoolsNames;
	private String[] assignedPoolsIcons;
	private String[] assignedPoolsColors;
	private String[] questionPools;

	// Question selection
	private String[] questionTypes;
	private int[] amountOfQuestions;
	private String questionTypeSpread;
	private int[] complexityStructure;
	private String complexitySpread;
	private String questionTypeUsage;
	private String questionSelection;
	private String questionOverwriteUsage;

	// Game components
	private GameWinningComponentListItem[] winningComponents;
	private String[] voucherIds;

	// Advertisement
	private AdvertisementFrequency advertisementFrequency;
	private Integer advFreqXQuestionDefinition;
	private Long advertisementProviderId;
	private Integer advertisementDuration;
	private String[] advertisementIds;
	private boolean advertisementSkipping;
	private Long loadingScreenProviderId;
	private boolean loadingScreen;
    private Integer loadingScreenDuration;

	// Entry fee
	private BigDecimal entryFeeAmount;
	private String entryFeeCurrency;
	private String entryFeeType = Game.ENTRY_FEE_TYPE_DEFAULT;
	private GameEntryFeeSettings entryFeeSettings;
	private GameEntryFeeValues entryFeeValues;
	private Integer entryFeeBatchSize = DEFAULT_BATCH_SIZE;
	@SerializedName(value="isMultiplePurchaseAllowed")
	private Boolean multiplePurchaseAllowed = DEFAULT_MULTIPLE_PURCHASE_ALLOWED;

	private GameTypeConfiguration typeConfiguration;

	private boolean isRegionalLimitationEnabled = false;
	
	private Jackpot jackpot;
	private String gameMaxJackpotType;
	private BigDecimal gameMaxJackpotAmount;

	// GAME SEARCH PRIMARY KEY FIELDS: e.g.
	// game:tenant:<tenant_id>:app:<app_id>:pool:<pool>
	// NOTE: Used in game selection process

	// string search fields
	public static final String SEARCH_FIELD_TYPE = "type";
	public static final String SEARCH_FIELD_POOL = "pool";
	public static final String SEARCH_FIELD_CREATOR = "creator";

	// boolean search fields
	public static final String SEARCH_FIELD_FREE = "free";
	public static final String SEARCH_FIELD_OFFLINE = "offline";
	public static final String SEARCH_FIELD_SPECIAL_PRIZE = "specialPrize";

	// Event topics
	private static final String TOPIC_WILDCARD_ANY = "*";

	private static final String TOPIC_TOURNAMENT_PREFIX = "tournament";
	private static final String TOPIC_OPEN_REGISTRATION_PREFIX = "tournament/openRegistration/";
	
	private static final String TOPIC_LT_START_GAME_PREFIX = "tournament/live/startGame/";

	private static final String TOPIC_LT_START_MULTIPLAYER_GAME_PREFIX = "tournament/startMultiplayerGame/";
	private static final String TOPIC_TIPP_MULTIPLAYER_GAME_PREFIX = "tournament/tippMultiplayerTournament/";
	private static final String TOPIC_FINISH_TIPP_MULTIPLAYER_GAME_PREFIX = "tournament/tipp/finishGame/";
	private static final String TOPIC_LT_PREFIX = "tournament/live/";
	private static final String TOPIC_LT_START_STEP_SUFFIX = "/startStep";
	private static final String TOPIC_LT_END_GAME_SUFFIX = "/endGame";
	private static final String TOPIC_LT_START_STEP_PATTERN = TOPIC_LT_PREFIX + "%s"
			+ TOPIC_LT_START_STEP_SUFFIX;
	private static final String TOPIC_LT_END_GAME_PATTERN = TOPIC_LT_PREFIX + "%s"
			+ TOPIC_LT_END_GAME_SUFFIX;

	public static final String REGIONAL_LIMITATION_ENABLED = "regionalLimitationEnabled";

	public Game() {
		// Initialize empty Game object
	}

	/**
	 * Check if game's handicap is in interval
	 * 
	 * @param from
	 *            {@link double}
	 * @param to
	 *            {@link double}
	 * @return true if game's handicap is in interval
	 */
	public boolean withinHandicap(double from, double to) {
		return to <= 0.0d || handicap >= from && handicap <= to;
	}

	/**
	 * Check if game's number of questions is in interval
	 * 
	 * @param from
	 *            {@link int}
	 * @param to
	 *            {@link int}
	 * @return true if game's number of questions is in interval
	 */
	public boolean withinNumberOfQuestions(int from, int to) {
		return to <= 0 || numberOfQuestions >= from && numberOfQuestions <= to;
	}

	public GameType getType() {
		return getEnum(GameType.class, type);
	}

	public void setType(GameType type) {
		this.type = type.name();
	}

	public String getGameId() {
		return gameId;
	}

	public void setGameId(String gameId) {
		this.gameId = gameId;
	}

	public void setOffline(boolean offline) {
		this.offline = offline;
	}
	
	public Boolean isOffline() {
		return offline;
	}

	@Override
	public boolean isFree() {
		return free;
	}

	public void setFree(boolean free) {
		this.free = free;
	}

	public boolean isLiveGame() {
		return isType(t -> t.isLive());
	}

	public boolean isDuel() {
		return isType(t -> t.isDuel());
	}

	public boolean isTournament() {
		return isType(t -> t.isTournament());
	}

	public boolean isPlayoff() {
		return isType(t -> t.isPlayoff());
	}

	public boolean isMultiUserGame() {
		return isType(t -> t.isMultiUser());
	}

	private boolean isType(Function<GameType, Boolean> checkType) {
		GameType gameType = getType();
		if (gameType != null) {
			return checkType.apply(gameType);
		} else {
			throw new IllegalArgumentException("Game Type is undefined");
		}
	}

	public Integer getNumberOfQuestions() {
		return numberOfQuestions;
	}

	public void setNumberOfQuestions(Integer numberOfQuestions) {
		this.numberOfQuestions = numberOfQuestions;
	}

	public String geStringResultConfiguration() {
		return resultConfiguration == null ? null : resultConfiguration.toString();
	}
	public ResultConfiguration getResultConfiguration() {
		return resultConfiguration == null ? null : new ResultConfiguration(resultConfiguration);
	}

	public void setResultConfiguration(ResultConfiguration resultConfiguration) {
		this.resultConfiguration = resultConfiguration.getJsonObject();
	}

	public Map<JokerType, JokerConfiguration> getJokerConfiguration() {
		Map<JokerType, JokerConfiguration> result = new EnumMap<>(JokerType.class);
		if (jokerConfiguration != null) {
			for (JokerConfiguration config : jokerConfiguration) {
				result.put(config.getType(), config);
			}
		}
		return result;
	}

	public void setJokerConfiguration(JokerConfiguration... jokerConfiguration) {
		this.jokerConfiguration = jokerConfiguration;
	}

	@Override
	public String[] getQuestionTypes() {
		return questionTypes;
	}
	
	public void setQuestionTypes(String[] questionTypes) {
		this.questionTypes = questionTypes;
	}

	@Override
	public int[] getAmountOfQuestions() {
		return amountOfQuestions;
	}

	public void setAmountOfQuestions(int... questions) {
		this.amountOfQuestions = questions;
	}

	@Override
	public QuestionTypeSpread getQuestionTypeSpread() {
		return QuestionTypeSpread.getByText(questionTypeSpread);
	}

	public void setQuestionTypeSpread(QuestionTypeSpread questionTypeSpread) {
		this.questionTypeSpread = questionTypeSpread.name();
	}

	@Override
	public int[] getComplexityStructure() {
		return complexityStructure;
	}
	
	public void setComplexityStructure(int[] complexityStructure) {
		this.complexityStructure = complexityStructure;
	}

	@Override
	public QuestionTypeUsage getQuestionTypeUsage() {
		return QuestionTypeUsage.getByText(questionTypeUsage);
	}
	
	@Override
	public void setQuestionTypeUsage(QuestionTypeUsage questionTypeUsage) {
		this.questionTypeUsage = questionTypeUsage.getText();
	}

	public QuestionSelection getQuestionSelection() {
		return QuestionSelection.getByText(questionSelection);
	}
	
	public void setQuestionSelection(QuestionSelection questionSelection) {
		this.questionSelection = questionSelection.getText();
	}

	@Override
	public QuestionOverwriteUsage getQuestionOverwriteUsage() {
		return Optional.ofNullable(QuestionOverwriteUsage.getByText(questionOverwriteUsage))
				.orElse(QuestionOverwriteUsage.NO_REPEATS);
	}
	
	public void setQuestionOverwriteUsage(QuestionOverwriteUsage questionOverwriteUsage) {
		this.questionOverwriteUsage = questionOverwriteUsage.getText();
	}

	@Override
	public QuestionComplexitySpread getQuestionComplexitySpread() {
		return QuestionComplexitySpread.getByText(complexitySpread);
	}
	
	public void setComplexitySpread(QuestionComplexitySpread complexitySpread) {
		this.complexitySpread = complexitySpread.getText();
	}

	public Boolean getHideCategories() {
		return hideCategories;
	}

	public void setHideCategories(Boolean hideCategories) {
		this.hideCategories = hideCategories;
	}

	public String[] getAssignedPools() {
		return assignedPools;
	}

	public void setAssignedPools(String[] assignedPools) {
		this.assignedPools = assignedPools;
	}

	public Integer getLevelCount() {
		int[] structure = getComplexityStructure();
		return structure == null ? null : structure.length;
	}

	/**
	 * @return visibility start date-time
	 */
	public ZonedDateTime getStartDateTime() {
		return DateTimeUtil.parseISODateTimeString(startDateTime);
	}

	/**
	 * @param startDateTime - visibility start date-time
	 */
	public void setStartDateTime(String startDateTime) {
		this.startDateTime = startDateTime;
	}

	/**
	 * @return visibility end date-time
	 */
	public ZonedDateTime getEndDateTime() {
		return DateTimeUtil.parseISODateTimeString(endDateTime);
	}

	/**
	 * @param endDateTime - visibility end date-time
	 */
	public void setEndDateTime(String endDateTime) {
		this.endDateTime = endDateTime;
	}

	/**
	 * @return live tournament's start date-time
	 */
	public ZonedDateTime getLiveTournamentDateTime() {
		return liveTournamentDateTime;
	}

	/**
	 * @param liveTournamentDateTime - live tournament's start date-time
	 */
	public void setLiveTournamentDateTime(ZonedDateTime liveTournamentDateTime) {
		this.liveTournamentDateTime = liveTournamentDateTime;
	}

	public String getGameDescription() {
		return description;
	}

	public GameWinningComponentListItem[] getWinningComponents() {
		return winningComponents;
	}
	
	public void setWinningComponents(GameWinningComponentListItem[] winningComponents) {
		this.winningComponents = winningComponents;
	}
	
	public GameWinningComponentListItem getWinningComponent(String winningComponentId) {
		return winningComponents == null ? null : Arrays.stream(winningComponents)
				.filter(winningComponent -> winningComponent.getWinningComponentId().equals(winningComponentId))
				.findAny().orElse(null);
	}
	
	public String[] getVoucherIds() {
		return voucherIds;
	}
	
	public void setVoucherIds(String[] voucherIds) {
		this.voucherIds = voucherIds;
	}

	public boolean getUserCanOverridePools() {
		return userCanOverridePools == Boolean.TRUE;
	}
	
	public void setUserCanOverridePools(boolean userCanOverridePools) {
		this.userCanOverridePools = userCanOverridePools;
	}

	public Integer getTimeToAcceptInvites() {
		return timeToAcceptInvites;
	}
	
	public void setTimeToAcceptInvites(Integer timeToAcceptInvites) {
		this.timeToAcceptInvites = timeToAcceptInvites;
	}

	public String getMultiplayerGameInstanceId() {
		return multiplayerGameInstanceId;
	}

	public void setMultiplayerGameInstanceId(String multiplayerGameInstanceId) {
		this.multiplayerGameInstanceId = multiplayerGameInstanceId;
	}

	@Override
	public Currency getEntryFeeCurrency() {
		return getEnum(Currency.class, entryFeeCurrency);
	}	
	
	public void setEntryFeeCurrency(Currency entryFeeCurrency) {
		this.entryFeeCurrency = entryFeeCurrency != null ? entryFeeCurrency.name() : null;
	}

	@Override
	public BigDecimal getEntryFeeAmount() {
		return entryFeeAmount;
	}
	
	public void setEntryFeeAmount(BigDecimal entryFeeAmount) {
		this.entryFeeAmount = entryFeeAmount;
	}

	public String getEntryFeeType() {
		return entryFeeType;
	}

	public GameEntryFeeSettings getEntryFeeSettings() {
		return entryFeeSettings;
	}

	public GameEntryFeeValues getEntryFeeValues() {
		return entryFeeValues;
	}
	
	public Integer getEntryFeeBatchSize() {
		return entryFeeBatchSize != null ? entryFeeBatchSize : DEFAULT_BATCH_SIZE;
	}

	public void setEntryFeeBatchSize(Integer entryFeeBatchSize) {
		this.entryFeeBatchSize = entryFeeBatchSize;
	}

	public Boolean isMultiplePurchaseAllowed() {
		return multiplePurchaseAllowed != null ? multiplePurchaseAllowed : DEFAULT_MULTIPLE_PURCHASE_ALLOWED;
	}

	public void setMultiplePurchaseAllowed(Boolean multiplePurchaseAllowed) {
		this.multiplePurchaseAllowed = multiplePurchaseAllowed;
	}

	public boolean hasEntryFee() {
		return entryFeeAmount != null && getEntryFeeCurrency() != null;
	}

	public boolean canCustomizeNumberOfQuestions() {
		return numberOfQuestions != null && numberOfQuestions == 0;
	}

	public boolean hasAdvertisements() {
		return advertisementProviderId != null && advertisementProviderId > 0;
	}

	public Long getAdvertisementProviderId() {
		return advertisementProviderId;
	}
	
	public void setAdvertisementProviderId(Long advertisementProviderId) {
		this.advertisementProviderId = advertisementProviderId;
	}

	public AdvertisementFrequency getAdvertisementFrequency() {
		return advertisementFrequency;
	}
	
	public void setAdvertisementFrequency(AdvertisementFrequency advertisementFrequency) {
		this.advertisementFrequency = advertisementFrequency;
	}

	public Integer getAdvertisementFrequencyXQuestionDefinition() {
		return advFreqXQuestionDefinition;
	}

	public boolean hasLoadingScreen() {
		return loadingScreenProviderId != null && loadingScreenProviderId > 0;
	}

	public boolean isAdvertisementSkipping() {
		return advertisementSkipping;
	}

	public void setAdvertisementSkipping(boolean advertisementSkipping) {
		this.advertisementSkipping = advertisementSkipping;
	}

	public Long getLoadingScreenProviderId() {
		return loadingScreenProviderId;
	}

	public void setLoadingScreenProviderId(Long loadingScreenProviderId) {
		this.loadingScreenProviderId = loadingScreenProviderId;
	}

    public Integer getLoadingScreenDuration() {
        return loadingScreenDuration == null ? 0 : loadingScreenDuration;
    }

    public void setLoadingScreenDuration(Integer loadingScreenWait) {
        this.loadingScreenDuration = loadingScreenDuration;
    }

    public void setLoadingScreen(boolean loadingScreen) {
		this.loadingScreen = loadingScreen;
	}

	public boolean isTypeOf(GameType... types) {
		return ArrayUtils.contains(types, getType());
	}

	public boolean isEntryFeeDecidedByPlayer() {
		return entryFeeDecidedByPlayer == Boolean.TRUE;
	}
	
	public void setEntryFeeDecidedByPlayer(boolean entryFeeDecidedByPlayer) {
		this.entryFeeDecidedByPlayer = entryFeeDecidedByPlayer;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public Double getHandicap() {
		return handicap;
	}

	public String getPrizes() {
		return prizes;
	}

	public JsonObject getShortPrize() {
		return shortPrize;
	}

	public boolean hasSpecialPrize() {
		return hasSpecialPrize;
	}
	
	public String[] getPlayingRegions() {
		return playingRegions;
	}
	
	public void setPlayingRegions(String[] playingRegions) {
		this.playingRegions = playingRegions;
	}

	@Override
	public String[] getPlayingLanguages() {
		return playingLanguages;
	}
	
	/**
	 * @return {@code true} if {@code playingLanguages} contains at least 2
	 *         different languages. Otherwise, {@code false}.
	 */
	@Override
	public boolean hasInternationalQuestions() {
		boolean result = false;
		if (ArrayUtils.isNotEmpty(playingLanguages)) {
			result = Arrays.stream(playingLanguages)
					.distinct()
					.toArray(String[]::new)
					.length > 1;
		}
		return result;
	}

	public void setPlayingLanguages(String[] playingLanguages) {
		this.playingLanguages = playingLanguages;
	}
	
	public boolean isInstantAnswerFeedback() {
		return instantAnswerFeedback;
	}

	public Integer getInstantAnswerFeedbackDelay() {
		return instantAnswerFeedbackDelay;
	}
	
	public void setInstantAnswerFeedbackDelay(Integer instantAnswerFeedbackDelay) {
		this.instantAnswerFeedbackDelay = instantAnswerFeedbackDelay;
	}
	
	public void setInstantAnswerFeedback(boolean instantAnswerFeedback) {
		this.instantAnswerFeedback = instantAnswerFeedback;
	}
	
	public String[] getAssignedPoolsNames() {
		return assignedPoolsNames;
	}

	public void setAssignedPoolsNames(String[] assignedPoolsNames) {
		this.assignedPoolsNames = assignedPoolsNames;
	}

	public String[] getAssignedPoolsIcons() {
		return assignedPoolsIcons;
	}
	
	public String getFirstAssignedPoolsIcon() {
		return ArrayUtils.isEmpty(assignedPoolsIcons) ? null : assignedPoolsIcons[0];
	}

	public void setAssignedPoolsIcons(String[] assignedPoolsIcons) {
		this.assignedPoolsIcons = assignedPoolsIcons;
	}
	
	public String[] getAssignedPoolsColors() {
		return assignedPoolsColors;
	}

	public void setAssignedPoolsColors(String[] assignedPoolsColors) {
		this.assignedPoolsColors = assignedPoolsColors;
	}

	public String[] getQuestionPools() {
		return questionPools;
	}

	public void setQuestionPools(String[] questionPools) {
		this.questionPools = questionPools;
	}
	
	public Integer getAdvFreqXQuestionDefinition() {
		return advFreqXQuestionDefinition;
	}
	
	public void setAdvFreqXQuestionDefinition(Integer advFreqXQuestionDefinition) {
		this.advFreqXQuestionDefinition = advFreqXQuestionDefinition;
	}

	public Integer getAdvertisementDuration() {
		return advertisementDuration == null ? 0 : advertisementDuration;
	}

	public void setAdvertisementDuration(Integer advertisementDuration) {
		this.advertisementDuration = advertisementDuration;
	}

	public String[] getAdvertisementIds() {
		return advertisementIds;
	}

	public String getPictureId() {
		return pictureId;
	}

	public String getIconId() {
		return iconId;
	}


	public void setImageId(String imageId) {
		this.imageId = imageId;
	}

	public String getImageId() {
		return imageId;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}
	
	public String[] getApplications() {
		return applications == null ? new String[0] : applications;
	}

	public void setApplications(String[] applications) {
		this.applications = applications;
	}

	public GameTypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}
	
	public void setTypeConfiguration(GameTypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;
	}

	public boolean getRegionalLimitationEnabled() {
		return this.isRegionalLimitationEnabled;
	}

	public void setRegionalLimitationEnabled(boolean regionalLimitationEnabled) {
		this.isRegionalLimitationEnabled = regionalLimitationEnabled;
	}

	public Integer getMinimumPlayerNeeded() {
		return getGameTypeConfiguration().getMinimumPlayerNeeded(getType());
	}
	
	public Integer getGameCancellationPriorGameStart() {
		return getGameTypeConfiguration().getGameCancellationPriorGameStart(getType());
	}

	public Integer getGameStartWarningMessage() {
		return getGameTypeConfiguration().getGameStartWarningMessage(getType());		
	}
	
	public Boolean getEmailNotification() {
		Integer configurationValue = getGameTypeConfiguration().getEmailNotification(getType());
		return configurationValue != null ? Boolean.valueOf(1 == configurationValue) : null;		
	}
	
	public Integer getPlayerGameReadiness() {
		return getGameTypeConfiguration().getPlayerGameReadiness(getType());
	}

	public Integer getMinimumJackpotGarantie() {
		return getGameTypeConfiguration().getMinimumJackpotGarantie(getType());		
	}
	
	public boolean isSpecial() {
		return getGameTypeConfiguration().isSpecial(getType());
	}
	
	public Double getWeight() {
		return getGameTypeConfiguration().getWeight(getType());
	}
	
	public String getBannerMediaId() {
		return getGameTypeConfiguration().getBannerMediaId(getType());
	}
	
	private GameTypeConfiguration getGameTypeConfiguration() {
		return Optional.ofNullable(typeConfiguration).orElseGet(GameTypeConfiguration::new);
	}
	
	public Jackpot getJackpot() {
		return jackpot;
	}

	public void setJackpot(Jackpot jackpot) {
		this.jackpot = jackpot;
	}


	public String getGameMaxJackpotType() {
		return gameMaxJackpotType;
	}

	public BigDecimal getGameMaxJackpotAmount() {
		return gameMaxJackpotAmount;
	}

	public void setGameMaxJackpotType(String gameMaxJackpotType) {
		this.gameMaxJackpotType = gameMaxJackpotType;
	}

	public void setGameMaxJackpotAmount(BigDecimal gameMaxJackpotAmount) {
		this.gameMaxJackpotAmount = gameMaxJackpotAmount;
	}

	public static boolean isTournamentTopic(String topic) {
		return StringUtils.startsWith(topic, TOPIC_TOURNAMENT_PREFIX);
	}
	
	public static String getOpenRegistrationTopic() {
		return TOPIC_OPEN_REGISTRATION_PREFIX + TOPIC_WILDCARD_ANY;
	}
	public static String getFinishTippMultiplayerGameTopic() {
		return TOPIC_FINISH_TIPP_MULTIPLAYER_GAME_PREFIX + TOPIC_WILDCARD_ANY;
	}
	public static String getOpenRegistrationTopic(String gameId) {
		return TOPIC_OPEN_REGISTRATION_PREFIX + gameId;
	}

	public static boolean isOpenRegistrationTopic(String topic) {
		return StringUtils.startsWith(topic, TOPIC_OPEN_REGISTRATION_PREFIX);
	}

	public static String getStartGameTopic() {
		return TOPIC_LT_START_GAME_PREFIX + TOPIC_WILDCARD_ANY;
	}
	
	public static String getStartGameTopic(String gameId) {
		return TOPIC_LT_START_GAME_PREFIX + gameId;
	}

	public static boolean isStartGameTopic(String topic) {
		return StringUtils.startsWith(topic, TOPIC_LT_START_GAME_PREFIX);
	}
	
	public static String getStartMultiplayerGameTopicAny() {
		return TOPIC_LT_START_MULTIPLAYER_GAME_PREFIX + TOPIC_WILDCARD_ANY;
	}

	public static String getStartMultiplayerGameTopic(String mgiId) {
		return TOPIC_LT_START_MULTIPLAYER_GAME_PREFIX + mgiId;
	}
	public static String getTippMultiplayerGameTopic(String mgiId) {
		return TOPIC_TIPP_MULTIPLAYER_GAME_PREFIX + mgiId;
	}

	public static boolean isTippMultiplayerGameTopic(String topic) {
		return StringUtils.startsWith(topic, TOPIC_TIPP_MULTIPLAYER_GAME_PREFIX);
	}

	public static boolean isFinishTippTournament(String topic) {
		return StringUtils.startsWith(topic, TOPIC_FINISH_TIPP_MULTIPLAYER_GAME_PREFIX);
	}

	public static boolean isStartMultiplayerGameTopic(String topic) {
		return StringUtils.startsWith(topic, TOPIC_LT_START_MULTIPLAYER_GAME_PREFIX);
	}

	public static String getLiveTournamentStartStepTopic(String mgiId) {
		return String.format(TOPIC_LT_START_STEP_PATTERN, mgiId);
	}

	public static String getLiveTournamentEndGameTopic(String mgiId) {
		return String.format(TOPIC_LT_END_GAME_PATTERN, mgiId);
	}

	public static boolean isLiveTournamentStartStepTopic(String topic) {
		return !StringUtils.isBlank(topic) && topic.startsWith(TOPIC_LT_PREFIX)
				&& topic.endsWith(TOPIC_LT_START_STEP_SUFFIX);
	}

	public static boolean isLiveTournamentEndGameTopic(String topic) {
		return !StringUtils.isBlank(topic) && topic.startsWith(TOPIC_LT_PREFIX)
				&& topic.endsWith(TOPIC_LT_END_GAME_SUFFIX);
	}

	public int getTotalSkipsAvailable() {
		final JokerConfiguration skipJokerConfiguration = getJokerConfiguration().get(JokerType.SKIP);
		return skipJokerConfiguration == null || getType() != GameType.QUIZ24 
				? 0 : skipJokerConfiguration.getAvailableCount();
	}

	@Override
	public String toString() {
		return "Game [gameId=" + gameId +
				", title=" + title +
				", type=" + type +
				", startDateTime=" + startDateTime +
				", endDateTime=" + endDateTime +
				"]";
	}

}
