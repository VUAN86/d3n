package de.ascendro.f4m.service.game.selection.model.multiplayer;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import org.apache.commons.lang3.ArrayUtils;

import de.ascendro.f4m.service.game.selection.model.game.EntryFee;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class CustomGameConfigBuilder {

	private String id;
	private String tenantId;
	private String appId;
	private String gameId;
	private GameType gameType;
	private String gameTitle;
	private String gameDescription;
	private String[] playingRegions;
	private String[] poolIds = {};
	private Integer numberOfQuestions;
	private Integer maxNumberOfParticipants;
	private BigDecimal entryFeeAmount;
	private Currency entryFeeCurrency;
	private String gameCreatorId;
	private long createTimestamp;
	private ZonedDateTime expiryDateTime;
	private ZonedDateTime startDateTime;
	private ZonedDateTime endDateTime;
	private ZonedDateTime playDateTime;
	private Integer minimumPlayerNeeded;
	private Integer gameCancellationPriorGameStart; //in minutes
	private Integer gameStartWarningMessage; //in minutes
	private Boolean emailNotification; // [0,1] values
	private Integer playerGameReadiness; // in seconds
	private Integer minimumJackpotGarantie; //optional for live tournaments games
	private Boolean publicGame;
	private boolean rematch;

	protected CustomGameConfigBuilder(String gameCreatorId) {
		if (GameType.DUEL != gameType) {
			startDateTime = DateTimeUtil.getCurrentDateTime().with(LocalTime.MIN).withNano(0);
			endDateTime = DateTimeUtil.getCurrentDateTime().with(LocalTime.MAX).withNano(0);
		}
		this.gameCreatorId = gameCreatorId;
		this.createTimestamp = DateTimeUtil.getUTCTimestamp();
	}

	protected CustomGameConfigBuilder(String tenantId, String appId, String gameId, String gameCreatorId) {
		this.tenantId = tenantId;
		this.appId = appId;
		this.gameId = gameId;
		this.gameCreatorId = gameCreatorId;
		this.createTimestamp = DateTimeUtil.getUTCTimestamp();
	}

	public static CustomGameConfigBuilder create(String gameCreatorId) {
		return new CustomGameConfigBuilder(gameCreatorId);
	}

	public static CustomGameConfigBuilder create(String tenantId, String appId, String gameId, String gameCreatorId) {
		return new CustomGameConfigBuilder(tenantId, appId, gameId, gameCreatorId);
	}

	public static CustomGameConfigBuilder createDuel(String gameCreatorId) {
		return new CustomGameConfigBuilder(gameCreatorId).withMaxNumberOfParticipants(2);
	}
	
	public static CustomGameConfigBuilder createLiveTournament(){
		return new CustomGameConfigBuilder(null);
	}

	public CustomGameConfig build(String id) {
		return withId(id).build();
	}

	public CustomGameConfig build() {
		CustomGameConfig config = new CustomGameConfig(gameId);
		config.setId(id);
		config.setGameType(gameType);
		config.setGameTitle(gameTitle);
		config.setGameDescription(gameDescription);
		config.setTenantId(tenantId);
		config.setAppId(appId);
		config.setPlayingRegions(playingRegions);
		config.setPoolIds(poolIds);
		config.setNumberOfQuestions(numberOfQuestions);
		config.setMaxNumberOfParticipants(maxNumberOfParticipants);
		config.setEntryFeeAmount(entryFeeAmount);
		config.setEntryFeeCurrency(entryFeeCurrency);
		config.setGameCreatorId(gameCreatorId);
		config.setCreateTimestamp(createTimestamp);
		config.setExpiryDateTime(expiryDateTime);
		config.setStartDateTime(startDateTime);
		config.setEndDateTime(endDateTime);
		config.setPlayDateTime(playDateTime);

		config.setMinimumPlayerNeeded(minimumPlayerNeeded);
		config.setGameCancellationPriorGameStart(gameCancellationPriorGameStart);
		config.setGameStartWarningMessage(gameStartWarningMessage);
		config.setEmailNotification(emailNotification);
		config.setPlayerGameReadiness(playerGameReadiness);
		config.setMinimumJackpotGarantie(minimumJackpotGarantie);
		config.setPublicGame(publicGame);
		config.setRematch(rematch);
		return config;
	}

	public MultiplayerGameParameters buildMultiplayerGameParameters() {
		MultiplayerGameParameters params = new MultiplayerGameParameters(gameId);
		params.setGameId(gameId);
		params.setPoolIds(poolIds);
		params.setNumberOfQuestions(numberOfQuestions);
		params.setMaxNumberOfParticipants(maxNumberOfParticipants);
		params.setEntryFeeAmount(entryFeeAmount);
		params.setEntryFeeCurrency(entryFeeCurrency);
		params.setStartDateTime(startDateTime);
		params.setEndDateTime(endDateTime);
		params.setPlayDateTime(playDateTime);
		return params;
	}
	
	public CustomGameConfigBuilder applyGame(Game game, ZonedDateTime expiryDateTime) {
		if (ArrayUtils.isEmpty(this.poolIds)) {
			this.poolIds = game.getAssignedPools();
		}
		if (this.numberOfQuestions == null) {
			this.numberOfQuestions = game.getNumberOfQuestions();
		}
		if (!game.isFree()) {
			if (this.entryFeeAmount == null || !game.isEntryFeeDecidedByPlayer()) {
				this.entryFeeAmount = game.getEntryFeeAmount();
			}
			if (this.entryFeeCurrency == null || !game.isEntryFeeDecidedByPlayer()) {
				this.entryFeeCurrency = game.getEntryFeeCurrency();
			}
		} else if (!game.isEntryFeeDecidedByPlayer()){
			this.entryFeeAmount = null;
			this.entryFeeCurrency = null;
		}
		
		if (this.startDateTime == null) {
			this.startDateTime = game.getStartDateTime();
		}
		if (this.endDateTime == null) {
			this.endDateTime = game.getEndDateTime();
		}

		this.playingRegions = game.getPlayingRegions();
		this.gameType = game.getType();
		this.gameTitle = game.getTitle();
		this.gameDescription = game.getDescription();
		this.expiryDateTime = expiryDateTime;
		if (game.isDuel()) {
			this.maxNumberOfParticipants = Game.DUEL_PARTICIPANT_COUNT;
		}

		this.minimumPlayerNeeded = game.getMinimumPlayerNeeded();
		this.gameCancellationPriorGameStart = game.getGameCancellationPriorGameStart();
		this.gameStartWarningMessage = game.getGameStartWarningMessage();
		this.emailNotification = game.getEmailNotification();
		this.playerGameReadiness = game.getPlayerGameReadiness();
		this.minimumJackpotGarantie = game.getMinimumJackpotGarantie();
		return this;
	}

	public CustomGameConfigBuilder applyMultiplayerGameParameters(MultiplayerGameParameters params) {
		this.gameId = params.getGameId();
		this.poolIds = params.getPoolIds();
		this.numberOfQuestions = params.getNumberOfQuestions();
		this.maxNumberOfParticipants = params.getMaxNumberOfParticipants();
		this.entryFeeAmount = params.getEntryFeeAmount();
		this.entryFeeCurrency = params.getEntryFeeCurrency();
		this.startDateTime = params.getStartDateTime();
		this.endDateTime = params.getEndDateTime();
		this.playDateTime = params.getPlayDateTime();
		return this;
	}

	public CustomGameConfigBuilder withId(String id) {
		this.id = id;
		return this;
	}

	public CustomGameConfigBuilder withTenant(String tenantId) {
		this.tenantId = tenantId;
		return this;
	}

	public CustomGameConfigBuilder withApp(String apptId) {
		this.appId = apptId;
		return this;
	}

	public CustomGameConfigBuilder forGame(String gameId) {
		this.gameId = gameId;
		return this;
	}
	
	public CustomGameConfigBuilder withGameType(GameType gameType) {
		this.gameType = gameType;
		return this;
	}
	
	public CustomGameConfigBuilder withGameTitle(String gameTitle) {
		this.gameTitle = gameTitle;
		return this;
	}

	public CustomGameConfigBuilder withGameDescription(String gameDescription) {
		this.gameDescription = gameDescription;
		return this;
	}

	public CustomGameConfigBuilder withPlayingRegions(String... playingRegions) {
		this.playingRegions = playingRegions;
		return this;
	}
	
	public CustomGameConfigBuilder withPools(String... pools) {
		this.poolIds = pools;
		return this;
	}

	public CustomGameConfigBuilder withNumberOfQuestions(Integer numberOfQuestions) {
		this.numberOfQuestions = numberOfQuestions;
		return this;
	}

	public CustomGameConfigBuilder withMaxNumberOfParticipants(Integer maxNumberOfParticipants) {
		this.maxNumberOfParticipants = maxNumberOfParticipants;
		return this;
	}

	public CustomGameConfigBuilder withEntryFee(BigDecimal entryFeeAmount, Currency entryFeeCurrency) {
		this.entryFeeAmount = entryFeeAmount;
		this.entryFeeCurrency = entryFeeCurrency;
		return this;
	}
	
	public CustomGameConfigBuilder withEntryFee(EntryFee entryFee) {
		if (entryFee != null) {
			this.entryFeeAmount = entryFee.getEntryFeeAmount();
			this.entryFeeCurrency = entryFee.getEntryFeeCurrency();
		} else {
			this.entryFeeAmount = null;
			this.entryFeeCurrency = null;
		}
		return this;
	}

	public CustomGameConfigBuilder withGameCreatorId(String gameCreatorId) {
		this.gameCreatorId = gameCreatorId;
		return this;
	}

	public CustomGameConfigBuilder withStartDateTime(ZonedDateTime startDateTime) {
		this.startDateTime = startDateTime;
		return this;
	}

	public CustomGameConfigBuilder withExpiryDateTime(ZonedDateTime expiryDateTime) {
		this.expiryDateTime = expiryDateTime;
		return this;
	}

	public CustomGameConfigBuilder withEndDateTime(ZonedDateTime endDateTime) {
		this.endDateTime = endDateTime;
		return this;
	}
	
	public CustomGameConfigBuilder withPlayDateTime(ZonedDateTime playDateTime) {
		this.playDateTime = playDateTime;
		return this;
	}
	
	public CustomGameConfigBuilder withMinimumPlayerNeeded(Integer minimumPlayerNeeded) {
		this.minimumPlayerNeeded = minimumPlayerNeeded;
		return this;
	}
	
	public CustomGameConfigBuilder withGameCancellationPriorGameStart(Integer gameCancellationPriorGameStart) {
		this.gameCancellationPriorGameStart = gameCancellationPriorGameStart;
		return this;
	}
	
	public CustomGameConfigBuilder withGameStartWarningMessage(Integer gameStartWarningMessage) {
		this.gameStartWarningMessage = gameStartWarningMessage;
		return this;
	}
	
	public CustomGameConfigBuilder withEmailNotification(Boolean emailNotification) {
		this.emailNotification = emailNotification;
		return this;
	}
	
	public CustomGameConfigBuilder withPlayerGameReadiness(Integer playerGameReadiness) {
		this.playerGameReadiness = playerGameReadiness;
		return this;
	}
	
	public CustomGameConfigBuilder withMinimumJackpotGarantie(Integer minimumJackpotGarantie) {
		this.minimumJackpotGarantie = minimumJackpotGarantie;
		return this;
	}

	public CustomGameConfigBuilder withPublicGame(Boolean publicGame) {
		this.publicGame = publicGame;
		return this;
	}
	
	public CustomGameConfigBuilder withRematch(boolean rematch) {
		this.rematch = rematch;
		return this;
	}
}
