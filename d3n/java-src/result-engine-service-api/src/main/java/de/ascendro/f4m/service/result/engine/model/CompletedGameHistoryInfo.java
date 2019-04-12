package de.ascendro.f4m.service.result.engine.model;


import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.result.GameOutcome;
import de.ascendro.f4m.server.result.Results;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.engine.model.end.GameEndStatus;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.game.Jackpot;
import de.ascendro.f4m.service.game.selection.model.multiplayer.InvitationGame;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.profile.model.api.ApiProfileBasicInfo;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class CompletedGameHistoryInfo {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CompletedGameHistoryInfo.class);

	private String userId;
	private String tenantId;
	private String multiplayerGameInstanceId;
    private String gameInstanceId;
    private InvitationGame game = new InvitationGame();
	private boolean gameIsFree;
	private boolean isPublicGame;
	private BigDecimal gameEntryFeeAmount;
	private Currency gameEntryFeeCurrency;
	private JsonObject shortPrize;
	private ApiProfileBasicInfo creator = new ApiProfileBasicInfo();
	private ApiProfileBasicInfo inviter = new ApiProfileBasicInfo();
	private String[] playingRegions;
	private String[] poolIds;
	private int numberOfQuestions;
	private int numberOfCorrectQuestions;
	private ZonedDateTime startDateTime;
    private String endDateTime;
    private long totalTime;
    private boolean isMultiplayerGameFinished;
    private int placement;
    private String gameOutcome;
    private BigDecimal prizeWonAmount;
    private Currency prizeWonCurrency;
    private Integer bonusPointsWonAmount;
    private List<ApiProfileBasicInfo> opponents = new ArrayList<>();
    private GameEndStatus status;
    private Jackpot jackpot;

	private Integer minimumPlayerNeeded;
	private Integer gameCancellationPriorGameStart; //in minutes
	private Integer gameStartWarningMessage; //in minutes
	private Boolean emailNotification; // [0,1] values
	private Integer playerGameReadiness; // in seconds
	private Integer minimumJackpotGarantie; //optional for live tournaments games
    
    /** @deprecated use game instead */
	@Deprecated
    private GameType gameType;
	/** @deprecated use game instead */
	@Deprecated
    private String gameId;
	/** @deprecated use game instead */
	@Deprecated
    private String gameImageId;
	/** @deprecated use game instead */
	@Deprecated
    private String title;
	/** @deprecated use opponents instead */
	@Deprecated
    private String opponentId;
	/** @deprecated use opponents instead */
	@Deprecated
    private String opponentName;
	
	public CompletedGameHistoryInfo() {
		// initialize empty object
	}

    public CompletedGameHistoryInfo(GameInstance gameInstance, Results results, ZonedDateTime endDateTime) {
				this.userId = gameInstance.getUserId();
				this.gameInstanceId = gameInstance.getId();
				this.totalTime = gameInstance.getDurationInSeconds();
				this.startDateTime = gameInstance.getStartDateTime();

				this.tenantId = results.getTenantId();
				this.numberOfQuestions = results.getTotalQuestionCount();
				this.numberOfCorrectQuestions = results.getCorrectAnswerCount();
				this.bonusPointsWonAmount = results.getBonusPointsWon();

				this.endDateTime = DateTimeUtil.formatISODateTime(endDateTime);
				Game playedGame = gameInstance.getGame();
				if (playedGame != null) {
					this.game = new InvitationGame(playedGame);
					this.gameIsFree = playedGame.isFree();
					this.gameEntryFeeAmount = playedGame.getEntryFeeAmount();
					this.gameEntryFeeCurrency = playedGame.getEntryFeeCurrency();
					this.shortPrize = playedGame.getShortPrize();
					this.gameType = playedGame.getType();
					this.gameId = playedGame.getGameId();
					this.gameImageId = playedGame.getPictureId();
					this.title = playedGame.getTitle();

					this.minimumPlayerNeeded = playedGame.getMinimumPlayerNeeded();
					this.gameCancellationPriorGameStart = playedGame.getGameCancellationPriorGameStart();
			this.gameStartWarningMessage = playedGame.getGameStartWarningMessage();
			this.emailNotification = playedGame.getEmailNotification();
			this.playerGameReadiness = playedGame.getPlayerGameReadiness();
			this.minimumJackpotGarantie = playedGame.getMinimumJackpotGarantie();
        } else {
        	LOGGER.warn("Game of gameInstanceId [{}] not found", gameInstanceId);
        }
        
        if (gameInstance.getGameState() != null) {
        	this.status = gameInstance.getGameState().getGameEndStatus();
        }
    }

    public String getUserId() {
    	return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getGameInstanceId() {
        return gameInstanceId;
    }
    
    public void setGameInstanceId(String gameInstanceId) {
        this.gameInstanceId = gameInstanceId;
    }

    public ZonedDateTime getEndDateTime() {
        return endDateTime == null ? null : DateTimeUtil.parseISODateTimeString(endDateTime);
    }
    
    public void setEndDateTime(ZonedDateTime endDateTime) {
    	this.endDateTime = DateTimeUtil.formatISODateTime(endDateTime);
    }

    public int getNumberOfQuestions() {
        return numberOfQuestions;
    }

    public void setNumberOfQuestions(int numberOfQuestions) {
        this.numberOfQuestions = numberOfQuestions;
    }

    public int getNumberOfCorrectQuestions() {
        return numberOfCorrectQuestions;
    }

    public void setNumberOfCorrectQuestions(int numberOfCorrectQuestions) {
        this.numberOfCorrectQuestions = numberOfCorrectQuestions;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
    }

    public boolean isMultiplayerGameFinished() {
        return isMultiplayerGameFinished;
    }

    public void setMultiplayerGameFinished(boolean multiplayerGameFinished) {
        isMultiplayerGameFinished = multiplayerGameFinished;
    }

    public int getPlacement() {
        return placement;
    }

    public void setPlacement(int placement) {
        this.placement = placement;
    }

    public GameOutcome getGameOutcome() {
    	return gameOutcome == null ? null : GameOutcome.fromString(gameOutcome);
    }
    
	public void setGameOutcome(GameOutcome gameOutcome) {
		this.gameOutcome = gameOutcome == null ? null : gameOutcome.getValue();
	}
    
    public BigDecimal getPrizeWonAmount() {
        return prizeWonAmount;
    }

    public void setPrizeWonAmount(BigDecimal prizeWonAmount) {
        this.prizeWonAmount = prizeWonAmount;
    }

    public Currency getPrizeWonCurrency() {
        return prizeWonCurrency;
    }

    public void setPrizeWonCurrency(Currency prizeWonCurrency) {
        this.prizeWonCurrency = prizeWonCurrency;
    }

    public Integer getBonusPointsWonAmount() {
    	return bonusPointsWonAmount;
    }

    public void setBonusPointsWonAmount(Integer bonusPointsWonAmount) {
    	this.bonusPointsWonAmount = bonusPointsWonAmount;
    }
    
	public String getMultiplayerGameInstanceId() {
		return multiplayerGameInstanceId;
	}

	public void setMultiplayerGameInstanceId(String multiplayerGameInstanceId) {
		this.multiplayerGameInstanceId = multiplayerGameInstanceId;
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

	public void setIsPublicGame(boolean publicGame) {
		isPublicGame = publicGame;
	}

	public boolean isPublicGame() {
		return isPublicGame;
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

	public ApiProfileBasicInfo getCreator() {
		return creator;
	}

	public void setCreator(ApiProfileBasicInfo creator) {
		this.creator = creator;
	}

	public ApiProfileBasicInfo getInviter() {
		return inviter;
	}

	public void setInviter(ApiProfileBasicInfo inviter) {
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

	public ZonedDateTime getStartDateTime() {
		return startDateTime;
	}

	public void setStartDateTime(ZonedDateTime startDateTime) {
		this.startDateTime = startDateTime;
	}

	public List<ApiProfileBasicInfo> getOpponents() {
		return opponents;
	}

	public void setOpponents(List<ApiProfileBasicInfo> opponents) {
		this.opponents = opponents;
		
		if (CollectionUtils.isNotEmpty(opponents)) {
			ApiProfileBasicInfo opponent = opponents.get(0);
			this.opponentId = opponent.getUserId();
			this.opponentName = opponent.getName();
		}
	}
	
	public void addOpponent(ApiProfileBasicInfo opponent) {
		opponents.add(opponent);
		
		this.opponentId = opponent.getUserId();
		this.opponentName = opponent.getName();
	}
	
	public GameEndStatus getStatus() {
		return status;
	}

	public void setStatus(GameEndStatus status) {
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

	/** @deprecated use opponents instead */
	@Deprecated
	public GameType getGameType() {
    	return gameType;
    }

	/** @deprecated use game  instead */
	@Deprecated
    public String getGameId() {
        return gameId;
    }

	/** @deprecated use game instead */
	@Deprecated
    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

	/** @deprecated use opponents instead */
	@Deprecated
    public String getGameImageId() {
        return gameImageId;
    }

	/** @deprecated use opponents instead */
	@Deprecated
    public void setGameImageId(String gameImageId) {
        this.gameImageId = gameImageId;
    }

	/** @deprecated use opponents instead */
	@Deprecated
    public String getTitle() {
        return title;
    }

	/** @deprecated use opponents instead */
	@Deprecated
    public void setTitle(String title) {
        this.title = title;
    }

	/** @deprecated use opponents instead */
	@Deprecated
    public String getOpponentId() {
        return opponentId;
    }

	/** @deprecated use opponents instead */
	@Deprecated
    public void setOpponentId(String opponentId) {
        this.opponentId = opponentId;
    }

	/** @deprecated use opponents instead */
	@Deprecated
    public String getOpponentName() {
    	return opponentName;
    }

	/** @deprecated use opponents instead */
	@Deprecated
    public void setOpponentName(String opponentName) {
    	this.opponentName = opponentName;
    }

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CompletedGameHistoryInfo [userId=");
		builder.append(userId);
		builder.append(", tenantId=");
		builder.append(tenantId);
		builder.append(", multiplayerGameInstanceId=");
		builder.append(multiplayerGameInstanceId);
		builder.append(", gameInstanceId=");
		builder.append(gameInstanceId);
		builder.append(", game=");
		builder.append(game);
		builder.append(", gameIsFree=");
		builder.append(gameIsFree);
		builder.append(", isPublicGame=");
		builder.append(isPublicGame);
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
		builder.append(", numberOfCorrectQuestions=");
		builder.append(numberOfCorrectQuestions);
		builder.append(", startDateTime=");
		builder.append(startDateTime);
		builder.append(", endDateTime=");
		builder.append(endDateTime);
		builder.append(", totalTime=");
		builder.append(totalTime);
		builder.append(", isMultiplayerGameFinished=");
		builder.append(isMultiplayerGameFinished);
		builder.append(", placement=");
		builder.append(placement);
		builder.append(", gameOutcome=");
		builder.append(gameOutcome);
		builder.append(", prizeWonAmount=");
		builder.append(prizeWonAmount);
		builder.append(", prizeWonCurrency=");
		builder.append(prizeWonCurrency);
		builder.append(", bonusPointsWonAmount=");
		builder.append(bonusPointsWonAmount);
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
		builder.append(", gameType=");
		builder.append(gameType);
		builder.append(", gameId=");
		builder.append(gameId);
		builder.append(", gameImageId=");
		builder.append(gameImageId);
		builder.append(", title=");
		builder.append(title);
		builder.append(", opponentId=");
		builder.append(opponentId);
		builder.append(", opponentName=");
		builder.append(opponentName);
		builder.append("]");
		return builder.toString();
	}

	public boolean isGameFree() {
		return this.gameIsFree;
	}

	public void setGameFree(boolean isGameFree) {
		this.gameIsFree = isGameFree;
	}

	public Jackpot getJackpot() {
		return this.jackpot;
	}

	public void setJackpot(Jackpot jackpot) {
		this.jackpot = jackpot;
		
	}

}