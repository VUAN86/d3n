package de.ascendro.f4m.service.game.selection.model.multiplayer;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Arrays;

import de.ascendro.f4m.service.game.selection.model.game.EntryFee;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.payment.model.Currency;

public class CustomGameConfig implements EntryFee {

	public static final String PROPERTY_GAME_TYPE = "gameType";
	public static final String PROPERTY_EXPIRY_DATE = "expiryDateTime";

	private String id;
	private String tenantId;
	private String appId;
	private String gameId;
	private GameType gameType;
	private String gameTitle;
	private String gameDescription;
	private String[] playingRegions;
	private String[] poolIds;
	private Integer numberOfQuestions;
	private Integer maxNumberOfParticipants;
	private BigDecimal entryFeeAmount;
	private Currency entryFeeCurrency;
	private String gameCreatorId;
	private long createTimestamp;
	
	/**
	 * Invitation expiration date-time based on MGI create date-time
	 */
	private ZonedDateTime expiryDateTime;
	/**
	 * Multiplayer game visibility start date-time
	 */
	private ZonedDateTime startDateTime;
	/**
	 * Multiplayer game visibility end date-time
	 */
	private ZonedDateTime endDateTime;
	
	/**
	 * Expected date-time when multiplayer game going to be started (used for Live Tournaments)
	 */
	private ZonedDateTime playDateTime;

	private Integer minimumPlayerNeeded;
	private Integer gameCancellationPriorGameStart; //in minutes
	private Integer gameStartWarningMessage; //in minutes
	private Boolean emailNotification; // [0,1] values
	private Integer playerGameReadiness; // in seconds
	private Integer minimumJackpotGarantie; //optional for live tournaments games
	private Boolean publicGame; //if game will be public or private with invites (currently used only for duels)
	private boolean rematch; // if game is a rematch (currently can be true only for duels)

	public CustomGameConfig() {
	}

	public CustomGameConfig(String gameId) {
		this.gameId = gameId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getGameId() {
		return gameId;
	}

	public void setGameId(String gameId) {
		this.gameId = gameId;
	}
	
	public GameType getGameType() {
		return gameType;
	}

	public void setGameType(GameType gameType) {
		this.gameType = gameType;
	}
	
	public String getGameTitle() {
		return gameTitle;
	}

	public void setGameTitle(String gameTitle) {
		this.gameTitle = gameTitle;
	}

	public String getGameDescription() {
		return gameDescription;
	}

	public void setGameDescription(String gameDescription) {
		this.gameDescription = gameDescription;
	}

	public String[] getPlayingRegions() {
		return playingRegions;
	}

	public void setPlayingRegions(String[] playingRegions) {
		this.playingRegions = playingRegions;
	}

	public String[] getPoolIds() {
		return poolIds;
	}

	public void setPoolIds(String[] poolIds) {
		this.poolIds = poolIds;
	}

	public Integer getNumberOfQuestions() {
		return numberOfQuestions;
	}

	public void setNumberOfQuestions(Integer numberOfQuestions) {
		this.numberOfQuestions = numberOfQuestions;
	}

	public Integer getMaxNumberOfParticipants() {
		return maxNumberOfParticipants;
	}

	public void setMaxNumberOfParticipants(Integer maxNumberOfParticipants) {
		this.maxNumberOfParticipants = maxNumberOfParticipants;
	}

	@Override
	public BigDecimal getEntryFeeAmount() {
		return entryFeeAmount;
	}

	public void setEntryFeeAmount(BigDecimal entryFeeAmount) {
		this.entryFeeAmount = entryFeeAmount;
	}

	@Override
	public Currency getEntryFeeCurrency() {
		return entryFeeCurrency;
	}
	
	public void setEntryFeeCurrency(Currency entryFeeCurrency) {
		this.entryFeeCurrency = entryFeeCurrency;
	}

	public String getGameCreatorId() {
		return gameCreatorId;
	}

	public void setGameCreatorId(String gameCreatorId) {
		this.gameCreatorId = gameCreatorId;
	}

	public long getCreateTimestamp() {
		return createTimestamp;
	}

	public void setCreateTimestamp(long createTimestamp) {
		this.createTimestamp = createTimestamp;
	}

	/**
	 * @return invitation expiration date-time based on MGI create date-time
	 */
	public ZonedDateTime getExpiryDateTime() {
		return expiryDateTime;
	}

	/**
	 * @param expiryDateTime - invitation expiration date-time based on MGI create date-time
	 */
	public void setExpiryDateTime(ZonedDateTime expiryDateTime) {
		this.expiryDateTime = expiryDateTime;
	}

	/**
	 * @return visibility start date-time
	 */
	public ZonedDateTime getStartDateTime() {
		return startDateTime;
	}

	/**
	 * @param startDateTime - visibility start date-time
	 */
	public void setStartDateTime(ZonedDateTime startDateTime) {
		this.startDateTime = startDateTime;
	}

	/**
	 * @return visibility end date-time
	 */
	public ZonedDateTime getEndDateTime() {
		return endDateTime;
	}

	/**
	 * @param endDateTime - visibility end date-time
	 */
	public void setEndDateTime(ZonedDateTime endDateTime) {
		this.endDateTime = endDateTime;
	}
	
	@Override
	public boolean isFree() {
		return entryFeeAmount == null || entryFeeAmount.compareTo(BigDecimal.ZERO) == 0 || entryFeeCurrency == null;
	}
	
	/**
	 * @return expected date-time when multiplayer game going to be started (used for Live Tournaments)
	 */
	public ZonedDateTime getPlayDateTime() {
		return playDateTime;
	}
	
	/**
	 * @param playDateTime - expected date-time when multiplayer game going to be started (used for Live Tournaments)
	 */
	public void setPlayDateTime(ZonedDateTime playDateTime) {
		this.playDateTime = playDateTime;
	}

	public Integer getMinimumPlayerNeeded() {
		return minimumPlayerNeeded;
	}

	public void setMinimumPlayerNeeded(Integer minimumPlayerNeeded) {
		this.minimumPlayerNeeded = minimumPlayerNeeded;
	}

	public Integer getGameCancellationPriorGameStart() {
		return gameCancellationPriorGameStart;
	}

	public void setGameCancellationPriorGameStart(Integer gameCancellationPriorGameStart) {
		this.gameCancellationPriorGameStart = gameCancellationPriorGameStart;
	}

	public Integer getGameStartWarningMessage() {
		return gameStartWarningMessage;
	}

	public void setGameStartWarningMessage(Integer gameStartWarningMessage) {
		this.gameStartWarningMessage = gameStartWarningMessage;
	}

	public Boolean getEmailNotification() {
		return emailNotification;
	}

	public void setEmailNotification(Boolean emailNotification) {
		this.emailNotification = emailNotification;
	}

	public Integer getPlayerGameReadiness() {
		return playerGameReadiness;
	}

	public void setPlayerGameReadiness(Integer playerGameReadiness) {
		this.playerGameReadiness = playerGameReadiness;
	}

	public Integer getMinimumJackpotGarantie() {
		return minimumJackpotGarantie;
	}

	public void setMinimumJackpotGarantie(Integer minimumJackpotGarantie) {
		this.minimumJackpotGarantie = minimumJackpotGarantie;
	}

	public Boolean getPublicGame() {
		return publicGame;
	}

	public void setPublicGame(Boolean publicGame) {
		this.publicGame = publicGame;
	}

	public boolean isRematch() {
		return rematch;
	}
	
	public void setRematch(boolean rematch) {
		this.rematch = rematch;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CustomGameConfig [id=");
		builder.append(id);
		builder.append(", tenantId=");
		builder.append(tenantId);
		builder.append(", appId=");
		builder.append(appId);
		builder.append(", gameId=");
		builder.append(gameId);
		builder.append(", gameType=");
		builder.append(gameType);
		builder.append(", gameTitle=");
		builder.append(gameTitle);
		builder.append(", gameDescription=");
		builder.append(gameDescription);
		builder.append(", playingRegions=");
		builder.append(Arrays.toString(playingRegions));
		builder.append(", poolIds=");
		builder.append(Arrays.toString(poolIds));
		builder.append(", numberOfQuestions=");
		builder.append(numberOfQuestions);
		builder.append(", maxNumberOfParticipants=");
		builder.append(maxNumberOfParticipants);
		builder.append(", entryFeeAmount=");
		builder.append(entryFeeAmount);
		builder.append(", entryFeeCurrency=");
		builder.append(entryFeeCurrency);
		builder.append(", gameCreatorId=");
		builder.append(gameCreatorId);
		builder.append(", createTimestamp=");
		builder.append(createTimestamp);
		builder.append(", expiryDateTime=");
		builder.append(expiryDateTime);
		builder.append(", startDateTime=");
		builder.append(startDateTime);
		builder.append(", endDateTime=");
		builder.append(endDateTime);
		builder.append(", playDateTime=");
		builder.append(playDateTime);
        builder.append(", minimumPlayerNeeded=").append(minimumPlayerNeeded);
        builder.append(", gameCancellationPriorGameStart=").append(getGameCancellationPriorGameStart());
        builder.append(", gameStartWarningMessage=").append(getGameStartWarningMessage());
        builder.append(", emailNotification=").append(getEmailNotification());
        builder.append(", playerGameReadiness=").append(getPlayerGameReadiness());
        builder.append(", minimumJackpotGarantie=").append(getMinimumJackpotGarantie());
		builder.append(", publicGame=");
		builder.append(publicGame);
		builder.append(", rematch=");
		builder.append(rematch);
		builder.append("]");
		return builder.toString();
	}
}
