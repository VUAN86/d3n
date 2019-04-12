package de.ascendro.f4m.service.game.selection.model.multiplayer;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.profile.model.api.ApiProfileExtendedBasicInfo;

public class Invitation implements JsonMessageContent {

	private String multiplayerGameInstanceId;
	private String gameInstanceId;
	private GameInstanceInfo gameInstance;
	private InvitationGame game = new InvitationGame();
	private boolean gameIsFree;
	private BigDecimal gameEntryFeeAmount;
	private Currency gameEntryFeeCurrency;
	private JsonObject shortPrize;
	private ApiProfileExtendedBasicInfo creator = new ApiProfileExtendedBasicInfo();
	private ApiProfileExtendedBasicInfo inviter = new ApiProfileExtendedBasicInfo();
	private String[] playingRegions;
	private String[] poolIds;
	private Integer numberOfQuestions;
	private Integer maxNumberOfParticipants;
	private ZonedDateTime expiryDateTime;
	private ZonedDateTime startDateTime;
	private ZonedDateTime endDateTime;
	private ZonedDateTime playDateTime;
	private List<ApiProfileExtendedBasicInfo> opponents = new ArrayList<>();
	private String status;
	private Integer minimumPlayerNeeded;
	private Integer gameCancellationPriorGameStart; //in minutes
	private Integer gameStartWarningMessage; //in minutes
	private Boolean emailNotification; // [0,1] values
	private Integer playerGameReadiness; // in seconds
	private Integer minimumJackpotGarantie; //optional for live tournaments games

	/**
	 * @deprecated use game instead
	 */
	@Deprecated
	private String gameId;
	/**
	 * @deprecated use game instead
	 */
	@Deprecated
	private GameType gameType;
	/**
	 * @deprecated use game instead
	 */
	@Deprecated
	private String gameTitle;
	/**
	 * @deprecated use game instead
	 */
	@Deprecated
	private String gameDescription;
	/**
	 * @deprecated use game instead
	 */
	@Deprecated
	private String poolIcon;
	/**
	 * @deprecated use gameEntryFeeAmount instead
	 */
	@Deprecated
	private BigDecimal entryFeeAmount;
	/**
	 * @deprecated use gameEntryFeeCurrency instead
	 */
	@Deprecated
	private Currency entryFeeCurrency;
	/**
	 * @deprecated use creator instead
	 */
	@Deprecated
	private String gameCreatorId;
	/**
	 * @deprecated use inviter instead
	 */
	@Deprecated
	private String inviterId;
	/**
	 * @deprecated use inviter instead
	 */
	@Deprecated
	private ApiProfileExtendedBasicInfo inviterInfo;

	public Invitation() {
		// initialize empty invitation
	}

	public Invitation(CustomGameConfig customGameConfig, String inviterId) {
		this(customGameConfig, inviterId, null);
	}

	public Invitation(CustomGameConfig customGameConfig, String inviterId, String status) {
		this();
		this.multiplayerGameInstanceId = customGameConfig.getId();
		this.gameIsFree = customGameConfig.isFree();
		this.gameEntryFeeAmount = customGameConfig.getEntryFeeAmount();
		this.gameEntryFeeCurrency = customGameConfig.getEntryFeeCurrency();
		this.playingRegions = customGameConfig.getPlayingRegions();
		this.poolIds = customGameConfig.getPoolIds();
		this.numberOfQuestions = customGameConfig.getNumberOfQuestions();
		this.maxNumberOfParticipants = customGameConfig.getMaxNumberOfParticipants();
		this.expiryDateTime = customGameConfig.getExpiryDateTime();
		this.startDateTime = customGameConfig.getStartDateTime();
		this.endDateTime = customGameConfig.getEndDateTime();
		this.playDateTime = customGameConfig.getPlayDateTime();
		this.status = status;
		this.minimumPlayerNeeded = customGameConfig.getMinimumPlayerNeeded();
		this.gameCancellationPriorGameStart = customGameConfig.getGameCancellationPriorGameStart();
		this.gameStartWarningMessage = customGameConfig.getGameStartWarningMessage();
		this.emailNotification = customGameConfig.getEmailNotification();
		this.playerGameReadiness = customGameConfig.getPlayerGameReadiness();
		this.minimumJackpotGarantie = customGameConfig.getMinimumJackpotGarantie();
		
		game = new InvitationGame(customGameConfig.getGameId(), customGameConfig.getGameTitle(),
				customGameConfig.getGameDescription(), customGameConfig.getGameType());
		creator.setUserId(customGameConfig.getGameCreatorId());
		inviter.setUserId(inviterId);
		
		this.gameId = customGameConfig.getGameId();
		this.gameType = customGameConfig.getGameType();
		this.gameTitle = customGameConfig.getGameTitle();
		this.gameDescription = customGameConfig.getGameDescription();
		this.entryFeeAmount = customGameConfig.getEntryFeeAmount();
		this.entryFeeCurrency = customGameConfig.getEntryFeeCurrency();
		this.gameCreatorId = customGameConfig.getGameCreatorId();
		this.inviterId = inviterId;
	}

	public String getMultiplayerGameInstanceId() {
		return multiplayerGameInstanceId;
	}

	public void setMultiplayerGameInstanceId(String multiplayerGameInstanceId) {
		this.multiplayerGameInstanceId = multiplayerGameInstanceId;
	}

	public String getGameInstanceId() {
		return gameInstanceId;
	}

	public void setGameInstanceId(String gameInstanceId) {
		this.gameInstanceId = gameInstanceId;
	}

	public GameInstanceInfo getGameInstance() {
		return gameInstance;
	}

	public void setGameInstance(GameInstanceInfo gameInstance) {
		this.gameInstance = gameInstance;
	}

	public InvitationGame getGame() {
		return game;
	}

	public void setGame(InvitationGame game) {
		this.game = game;
	}

	public boolean isGameIsFree() {
		return gameIsFree;
	}

	public void setGameIsFree(boolean gameIsFree) {
		this.gameIsFree = gameIsFree;
	}

	public BigDecimal getGameEntryFeeAmount() {
		return gameEntryFeeAmount;
	}

	public void setGameEntryFeeAmount(BigDecimal gameEntryFeeAmount) {
		this.gameEntryFeeAmount = gameEntryFeeAmount;
	}

	public Currency getGameEntryFeeCurrency() {
		return gameEntryFeeCurrency;
	}

	public void setGameEntryFeeCurrency(Currency gameEntryFeeCurrency) {
		this.gameEntryFeeCurrency = gameEntryFeeCurrency;
	}

	public JsonObject getShortPrize() {
		return shortPrize;
	}

	public void setShortPrize(JsonObject shortPrize) {
		this.shortPrize = shortPrize;
	}

	public ApiProfileExtendedBasicInfo getCreator() {
		return creator;
	}

	public void setCreator(ApiProfileExtendedBasicInfo creator) {
		this.creator = creator;
	}

	public ApiProfileExtendedBasicInfo getInviter() {
		return inviter;
	}

	public void setInviter(ApiProfileExtendedBasicInfo inviter) {
		this.inviter = inviter;
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

	public ZonedDateTime getExpiryDateTime() {
		return expiryDateTime;
	}

	public void setExpiryDateTime(ZonedDateTime expiryDateTime) {
		this.expiryDateTime = expiryDateTime;
	}

	public ZonedDateTime getStartDateTime() {
		return startDateTime;
	}

	public void setStartDateTime(ZonedDateTime startDateTime) {
		this.startDateTime = startDateTime;
	}

	public ZonedDateTime getEndDateTime() {
		return endDateTime;
	}

	public void setEndDateTime(ZonedDateTime endDateTime) {
		this.endDateTime = endDateTime;
	}

	public ZonedDateTime getPlayDateTime() {
		return playDateTime;
	}

	public void setPlayDateTime(ZonedDateTime playDateTime) {
		this.playDateTime = playDateTime;
	}

	public List<ApiProfileExtendedBasicInfo> getOpponents() {
		return opponents;
	}

	public void setOpponents(List<ApiProfileExtendedBasicInfo> opponents) {
		this.opponents = opponents;
	}

	public void addOpponent(ApiProfileExtendedBasicInfo opponent) {
		opponents.add(opponent);
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
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

	@Deprecated
	public String getGameId() {
		return gameId;
	}

	@Deprecated
	public void setGameId(String gameId) {
		this.gameId = gameId;
	}

	@Deprecated
	public GameType getGameType() {
		return gameType;
	}

	@Deprecated
	public void setGameType(GameType gameType) {
		this.gameType = gameType;
	}

	@Deprecated
	public String getGameTitle() {
		return gameTitle;
	}

	@Deprecated
	public void setGameTitle(String gameTitle) {
		this.gameTitle = gameTitle;
	}

	@Deprecated
	public String getGameDescription() {
		return gameDescription;
	}

	@Deprecated
	public void setGameDescription(String gameDescription) {
		this.gameDescription = gameDescription;
	}

	@Deprecated
	public String getPoolIcon() {
		return poolIcon;
	}

	@Deprecated
	public void setPoolIcon(String poolIcon) {
		this.poolIcon = poolIcon;
	}

	@Deprecated
	public BigDecimal getEntryFeeAmount() {
		return entryFeeAmount;
	}

	@Deprecated
	public void setEntryFeeAmount(BigDecimal entryFeeAmount) {
		this.entryFeeAmount = entryFeeAmount;
	}

	@Deprecated
	public Currency getEntryFeeCurrency() {
		return entryFeeCurrency;
	}

	@Deprecated
	public void setEntryFeeCurrency(Currency entryFeeCurrency) {
		this.entryFeeCurrency = entryFeeCurrency;
	}

	@Deprecated
	public String getGameCreatorId() {
		return gameCreatorId;
	}

	@Deprecated
	public void setGameCreatorId(String gameCreatorId) {
		this.gameCreatorId = gameCreatorId;
	}

	@Deprecated
	public String getInviterId() {
		return inviterId;
	}

	@Deprecated
	public void setInviterId(String inviterId) {
		this.inviterId = inviterId;
	}

	@Deprecated
	public ApiProfileExtendedBasicInfo getInviterInfo() {
		return inviterInfo;
	}

	@Deprecated
	public void setInviterInfo(ApiProfileExtendedBasicInfo inviterInfo) {
		this.inviterInfo = inviterInfo;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Invitation [multiplayerGameInstanceId=");
		builder.append(multiplayerGameInstanceId);
		builder.append(", gameInstanceId=");
		builder.append(gameInstanceId);
		builder.append(", game=");
		builder.append(game);
		builder.append(", gameIsFree=");
		builder.append(gameIsFree);
		builder.append(", gameEntryFeeAmount=");
		builder.append(gameEntryFeeAmount);
		builder.append(", gameEntryFeeCurrency=");
		builder.append(gameEntryFeeCurrency);
		builder.append(", shortPrize=");
		builder.append(shortPrize);
		builder.append(", creator=");
		builder.append(creator);
		builder.append(", inviter=");
		builder.append(inviter);
		builder.append(", playingRegions=");
		builder.append(Arrays.toString(playingRegions));
		builder.append(", poolIds=");
		builder.append(Arrays.toString(poolIds));
		builder.append(", numberOfQuestions=");
		builder.append(numberOfQuestions);
		builder.append(", maxNumberOfParticipants=");
		builder.append(maxNumberOfParticipants);
		builder.append(", expiryDateTime=");
		builder.append(expiryDateTime);
		builder.append(", startDateTime=");
		builder.append(startDateTime);
		builder.append(", endDateTime=");
		builder.append(endDateTime);
		builder.append(", playDateTime=");
		builder.append(playDateTime);
		builder.append(", opponents=");
		builder.append(opponents);
		builder.append(", status=");
		builder.append(status);
        builder.append(", minimumPlayerNeeded=").append(minimumPlayerNeeded);
        builder.append(", gameCancellationPriorGameStart=").append(getGameCancellationPriorGameStart());
        builder.append(", gameStartWarningMessage=").append(getGameStartWarningMessage());
        builder.append(", emailNotification=").append(getEmailNotification());
        builder.append(", playerGameReadiness=").append(getPlayerGameReadiness());
        builder.append(", minimumJackpotGarantie=").append(getMinimumJackpotGarantie());
		builder.append(", gameId=");
		builder.append(gameId);
		builder.append(", gameType=");
		builder.append(gameType);
		builder.append(", gameTitle=");
		builder.append(gameTitle);
		builder.append(", gameDescription=");
		builder.append(gameDescription);
		builder.append(", poolIcon=");
		builder.append(poolIcon);
		builder.append(", entryFeeAmount=");
		builder.append(entryFeeAmount);
		builder.append(", entryFeeCurrency=");
		builder.append(entryFeeCurrency);
		builder.append(", gameCreatorId=");
		builder.append(gameCreatorId);
		builder.append(", inviterId=");
		builder.append(inviterId);
		builder.append(", inviterInfo=");
		builder.append(inviterInfo);
		builder.append("]");
		return builder.toString();
	}

}
