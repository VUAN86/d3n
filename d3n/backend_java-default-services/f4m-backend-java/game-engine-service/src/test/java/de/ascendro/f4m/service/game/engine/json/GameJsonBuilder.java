package de.ascendro.f4m.service.game.engine.json;

import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.AMOUNT_OF_QUESTIONS;
import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.ASSIGNED_POOLS;
import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.COMPLEXITY_PERCENTAGE;
import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.COMPLEXITY_STRUCTURE;
import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.WINNING_COMPONENTS;
import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.NO_REPEATS;
import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.PLAYING_LANGUAGES;
import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.PLAYING_REGIONS;
import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.QUESTION_POOLS;
import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.QUESTION_TYPES;
import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.TYPE_PERCENTAGE;
import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.VOUCHER_IDS;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.APP_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.TENANT_ID;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.game.engine.integration.TestDataLoader;
import de.ascendro.f4m.service.game.selection.model.game.AdvertisementFrequency;
import de.ascendro.f4m.service.game.selection.model.game.EntryFee;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.game.GameTypeConfiguration;
import de.ascendro.f4m.service.game.selection.model.game.GameWinningComponentListItem;
import de.ascendro.f4m.service.game.selection.model.game.JokerConfiguration;
import de.ascendro.f4m.service.game.selection.model.game.MultiplayerGameTypeConfigurationData;
import de.ascendro.f4m.service.game.selection.model.game.QuestionComplexitySpread;
import de.ascendro.f4m.service.game.selection.model.game.QuestionOverwriteUsage;
import de.ascendro.f4m.service.game.selection.model.game.QuestionSelection;
import de.ascendro.f4m.service.game.selection.model.game.QuestionTypeSpread;
import de.ascendro.f4m.service.game.selection.model.game.QuestionTypeUsage;
import de.ascendro.f4m.service.game.selection.model.game.ResultConfiguration;
import de.ascendro.f4m.service.game.selection.model.game.SingleplayerGameTypeConfigurationData;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class GameJsonBuilder {
	private final String gameId;
	
	private boolean offline = false;
	private boolean free = true;
	private String tenantId = TENANT_ID;
	private String[] applications = new String[] { APP_ID };
	private boolean instantAnswerFeedback = false;
	private Integer instantAnswerFeedbackDelay;

	private String[] assignedPools  = ASSIGNED_POOLS;
	private String[] questionPools = QUESTION_POOLS;
	private String[] questionTypes = QUESTION_TYPES;
	private int[] amountOfQuestions = AMOUNT_OF_QUESTIONS;
	private QuestionTypeSpread questionTypeSpread = TYPE_PERCENTAGE;
	private int[] complexityStructure = COMPLEXITY_STRUCTURE;
	private QuestionComplexitySpread complexitySpread = COMPLEXITY_PERCENTAGE;
	private QuestionTypeUsage questionTypeUsage = QuestionTypeUsage.RANDOM;
	private QuestionSelection questionSelection = QuestionSelection.RANDOM_PER_PLAYER;
	private QuestionOverwriteUsage questionOverwriteUsage = NO_REPEATS;
	private String[] playingRegions = PLAYING_REGIONS;
	private String[] playingLanguages = PLAYING_LANGUAGES;
	private GameWinningComponentListItem[] winningComponents = WINNING_COMPONENTS;
	private String[] voucherIds = VOUCHER_IDS;
	private ZonedDateTime startDateTime = TestDataLoader.getTodayStarts();
	private ZonedDateTime endDateTime = TestDataLoader.getTodayEnds();
	private boolean userCanOverridePools = false;	
	
	private AdvertisementFrequency advertisementFrequency;
	private Integer advFreqXQuestionDefinition;
	private Long advertisementProviderId;
	
	private BigDecimal entryFeeAmount;
	private Currency entryFeeCurrency;
	private boolean entryFeeDecidedByPlayer = false;
	
	private int numberOfQuestions;
	private GameType gameType;
	
	private String multiplayerGameInstanceId;
	private ResultConfiguration resultConfiguration = new ResultConfiguration();

	private JokerConfiguration[] jokerConfigurations;

	private int timeToAcceptInvites = 500;
	
	private boolean multiplePurchaseAllowed = false;
	
	private int maxPlayTimes;
	private boolean multipleGamesFee = false;
	
	private GameTypeConfiguration gameTypeConfiguration;
	
	private GameJsonBuilder(String gameId){
		this.gameId = gameId;
	}
	
	public static GameJsonBuilder createGame(String gameId){		
		return new GameJsonBuilder(gameId);
	}
	
	public static GameJsonBuilder createLiveTournament(String gameId, String mgiId) {
		MultiplayerGameTypeConfigurationData liveTournament = new MultiplayerGameTypeConfigurationData();
		liveTournament.setMinimumPlayerNeeded(2);
		return createGame(gameId).withMultiplayerGameInstanceId(mgiId).withGameType(GameType.LIVE_TOURNAMENT)
				.withGameTypeConfiguration(new GameTypeConfiguration(liveTournament, null, null));
	}
	
	public String getGameId() {
		return gameId;
	}
	
	public GameJsonBuilder withNumberOfQuestions(int numberOfQuestions){
		this.numberOfQuestions = numberOfQuestions;
		return this;
	}
	
	public GameJsonBuilder withWinningComponents(GameWinningComponentListItem[] winningComponents) {
		this.winningComponents = winningComponents;
		return this;
	}

	public GameJsonBuilder withGameType(GameType gameType){
		this.gameType = gameType;
		return this;
	}
    
    public GameJsonBuilder withGameTypeConfiguration(GameTypeConfiguration gameTypeConfiguration){
        this.gameTypeConfiguration = gameTypeConfiguration;
        return this;
    }
	
	public GameJsonBuilder withResultConfiguration(ResultConfiguration resultConfiguration){
		this.resultConfiguration = resultConfiguration;
		return this;
	}

	public GameJsonBuilder withJokersConfiguration(JokerConfiguration[] jokersConfiguration){
		this.jokerConfigurations = jokersConfiguration;
		return this;
	}

	public GameJsonBuilder inOfflineMode(){
		this.offline = true;
		return this;
	}
	
	public GameJsonBuilder withInstantAnswerFeedback(){
		this.instantAnswerFeedback = true;
		return this;
	}
	
	public GameJsonBuilder withInstantAnswerFeedbackDelay(int instantAnswerFeedbackDelay) {
		this.instantAnswerFeedbackDelay = instantAnswerFeedbackDelay;
		return this;
	}
	
	public GameJsonBuilder withCustomUserPools(boolean userCanOverridePools){
		this.userCanOverridePools = userCanOverridePools;
		return this;
	}
	
	public GameJsonBuilder withStartDateTime(ZonedDateTime startDateTime){
		this.startDateTime = startDateTime;
		return this;
	}
	
	public GameJsonBuilder withEntryFee(BigDecimal entryFeeAmount, Currency entryFeeCurrency){
		this.free = false;
		this.entryFeeAmount = entryFeeAmount;
		this.entryFeeCurrency = entryFeeCurrency;
		
		return this;
	}
	
	public GameJsonBuilder withEntryFee(EntryFee entryFee){
		if (entryFee != null) {
			withEntryFee(entryFee.getEntryFeeAmount(), entryFee.getEntryFeeCurrency());
			this.free = entryFee.isFree();
		} else {
			this.free = true;
			this.entryFeeAmount = null;
			this.entryFeeCurrency = null;
		}
		return this;
	}
	
	public GameJsonBuilder withAdvertisement(Long providerId, AdvertisementFrequency advertisementFrequency){
		this.advertisementProviderId = providerId;
		this.advertisementFrequency = advertisementFrequency;
		return this;
	}
	
	public GameJsonBuilder withAdvertisement(Long providerId, int advFreqXQuestionDefinition){
		this.advertisementProviderId = providerId;
		this.advertisementFrequency = AdvertisementFrequency.AFTER_EVERY_X_QUESTION;
		this.advFreqXQuestionDefinition = advFreqXQuestionDefinition;
		return this;
	}
	
	public GameJsonBuilder withMultiplayerGameInstanceId(String multiplayerGameInstanceId){
		this.multiplayerGameInstanceId = multiplayerGameInstanceId;
		return this;
	}
	
	public String getMultiplayerGameInstanceId() {
		return multiplayerGameInstanceId;
	}
	
	public GameJsonBuilder withEndDateTime(ZonedDateTime endDateTime){
		this.endDateTime = endDateTime;
		return this;
	}
	
	public ZonedDateTime getEndDateTime() {
		return endDateTime;
	}
	
	public ZonedDateTime getStartDateTime() {
		return startDateTime;
	}

	public boolean isOfflineGame() {
		return offline;
	}
	
	public GameJsonBuilder withEntryFeeDecidedByPlayer(boolean entryFeeDecidedByPlayer){
		this.entryFeeDecidedByPlayer = entryFeeDecidedByPlayer;
		return this;
	}
	
	public GameJsonBuilder withTimeToAcceptInvites(int timeToAcceptInvites) {
		this.timeToAcceptInvites = timeToAcceptInvites;
		return this;
	}
	
	public GameJsonBuilder withSpecialGame(int maxPlayTimes) {
		this.maxPlayTimes = maxPlayTimes;
		this.multipleGamesFee = true;
		this.multiplePurchaseAllowed = false;
		return this;
	}

	public GameJsonBuilder withMultipleFeeGame(int maxPlayTimes) {
		this.maxPlayTimes = maxPlayTimes;
		this.multipleGamesFee = true;
		this.multiplePurchaseAllowed = true;
		return this;
	}

	public GameJsonBuilder withMultiplePurchaseAllowed(boolean allowed) {
		this.multiplePurchaseAllowed = allowed;
		return this;
	}
	
	public Game buildGame(JsonUtil jsonUtil) {
		return jsonUtil.fromJson(buildJson(jsonUtil), Game.class);
	}
	
	public boolean isMultipleGamesFee() {
		return multipleGamesFee;
	}

	public void setMultipleGamesFee(boolean multipleGamesFee) {
		this.multipleGamesFee = multipleGamesFee;
	}
	
	
	public String buildJson(JsonUtil jsonUtil) {
		final Game game = new Game();
		game.setGameId(gameId);
		game.setTenantId(tenantId);
		game.setApplications(applications);
		game.setType(gameType);
		game.setOffline(offline);
		game.setFree(free);
		
		game.setNumberOfQuestions(numberOfQuestions);
		game.setInstantAnswerFeedback(instantAnswerFeedback);
		game.setInstantAnswerFeedbackDelay(instantAnswerFeedbackDelay);
		game.setUserCanOverridePools(userCanOverridePools);
		
		//DateTime
		game.setStartDateTime(DateTimeUtil.formatISODateTime(startDateTime));
		game.setEndDateTime(DateTimeUtil.formatISODateTime(endDateTime));
		game.setTimeToAcceptInvites(timeToAcceptInvites);
		
		//Entry fee
		game.setEntryFeeAmount(entryFeeAmount);
		game.setEntryFeeCurrency(entryFeeCurrency);
		game.setEntryFeeDecidedByPlayer(entryFeeDecidedByPlayer);

		//Playing region
		game.setPlayingRegions(playingRegions);
		game.setPlayingLanguages(playingLanguages);

		//Pools
		game.setAssignedPools(assignedPools);
		game.setQuestionPools(questionPools);

		//Question type
		game.setQuestionTypes(questionTypes);
		game.setAmountOfQuestions(amountOfQuestions);
		game.setQuestionTypeSpread(questionTypeSpread);

		game.setComplexityStructure(complexityStructure);
		game.setComplexitySpread(complexitySpread);

		game.setQuestionTypeUsage(questionTypeUsage);
		game.setQuestionSelection(questionSelection);
		game.setQuestionOverwriteUsage(questionOverwriteUsage);

		//Winning components/Vouchers
		game.setWinningComponents(winningComponents);
		game.setVoucherIds(voucherIds);

		//Advertisement
		game.setAdvertisementFrequency(advertisementFrequency);
		game.setAdvFreqXQuestionDefinition(advFreqXQuestionDefinition);
		game.setAdvertisementProviderId(advertisementProviderId);

		game.setResultConfiguration(resultConfiguration);
		game.setMultiplayerGameInstanceId(multiplayerGameInstanceId);

		// Jokers :
		game.setJokerConfiguration(jokerConfigurations);

		//Special games
		game.setMultiplePurchaseAllowed(this.multiplePurchaseAllowed);

		game.setTypeConfiguration(gameTypeConfiguration);
		
		if (multipleGamesFee && maxPlayTimes > 0) {
			game.setEntryFeeBatchSize(maxPlayTimes);
			SingleplayerGameTypeConfigurationData spc = new SingleplayerGameTypeConfigurationData(true, 0, "");
			GameTypeConfiguration typeConfiguration = new GameTypeConfiguration(null, null, spc);
			typeConfiguration.setGameQuickQuiz(spc);
			game.setTypeConfiguration(typeConfiguration);
		}
		return jsonUtil.toJson(game);
	}
	
}
