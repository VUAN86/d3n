package de.ascendro.f4m.service.game.engine.model;

import java.math.BigDecimal;

import org.apache.commons.lang3.ArrayUtils;

import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.game.selection.model.singleplayer.SinglePlayerGameParameters;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.model.Currency;

public class GameInstanceBuilder {
	private final String userId;
    private final String tenantId;
    private final String appId;
    private final String userIp;

	private final Game game;
	
	private boolean trainningMode = false;
	
	private String mgiId;
	private String tournamentId;
	private Integer numberOfQuestions;
	
	private String[] questionPoolIds;
	
	private GameStatus gameStatus = GameStatus.REGISTERED;
	
	private String entryFeeTransactionId;
	private BigDecimal entryFeeAmount;
	private Currency entryFeeCurrency;
	
    private GameInstanceBuilder(ClientInfo clientInfo, Game game) {
        this.userId = clientInfo.getUserId();
        this.tenantId = clientInfo.getTenantId();
        this.appId = clientInfo.getAppId();
        this.userIp = clientInfo.getIp();
		this.game = game;
	}
	
    public static GameInstanceBuilder create(ClientInfo clientInfo, Game game){
		final GameInstanceBuilder gameInstanceBuilder = new GameInstanceBuilder(clientInfo, game);
		gameInstanceBuilder.withNumberOfQuestions(game.getNumberOfQuestions());
		gameInstanceBuilder.withQuestionPools(game.getAssignedPools());
		gameInstanceBuilder.withTournamentId(game.isTournament() ? game.getGameId() : null);
		
		return gameInstanceBuilder;
	}

	public GameInstanceBuilder withQuestionPools(String[] questionPoolIds) {
		this.questionPoolIds = questionPoolIds;
		return this;
	}

	public GameInstanceBuilder withNumberOfQuestions(Integer numberOfQuestions) {
		this.numberOfQuestions = numberOfQuestions;
		return this;
	} 
	
	public GameInstanceBuilder withMgiId(String mgiId) {
		this.mgiId = mgiId;
		return this;
	}

	public GameInstanceBuilder withTournamentId(String tournamentId) {
		this.tournamentId = tournamentId;
		return this;
	} 
	
	public GameInstanceBuilder inTrainningMode(boolean trainningMode){
		this.trainningMode = trainningMode;
		return this;
	}
	
	public GameInstanceBuilder withEntryFeeTransactionId(String entryFeeTransactionId) {
		this.entryFeeTransactionId = entryFeeTransactionId;
		return this;
	}
	
	public GameInstanceBuilder withEntryFee(BigDecimal entryFeeAmount, Currency entryFeeCurrency){
		this.entryFeeAmount = entryFeeAmount;
		this.entryFeeCurrency = entryFeeCurrency;
		return this;
	}
	
	public GameInstanceBuilder applySinglePlayerGameConfig(SinglePlayerGameParameters singlePlayerGameConfig) {
		if (singlePlayerGameConfig != null) {
			trainningMode = singlePlayerGameConfig.isTrainingMode();
			if (game != null) {
				if (game.canCustomizeNumberOfQuestions() && singlePlayerGameConfig.getNumberOfQuestions() != null) {
					this.numberOfQuestions = singlePlayerGameConfig.getNumberOfQuestions();
				}
				if (game.getUserCanOverridePools() && !ArrayUtils.isEmpty(singlePlayerGameConfig.getPoolIds())) {
					this.questionPoolIds = singlePlayerGameConfig.getPoolIds();
				}
			}
		}
		return this;
	}
	
	public GameInstanceBuilder applyMultiplayerGameConfig(CustomGameConfig multiplayerGameConfig) {
		if (multiplayerGameConfig != null && game != null) {
			if (game.getUserCanOverridePools()) {
				this.questionPoolIds = multiplayerGameConfig.getPoolIds();
			}
			if (game.canCustomizeNumberOfQuestions()) {
				this.numberOfQuestions = multiplayerGameConfig.getNumberOfQuestions();
			}
			if (game.isEntryFeeDecidedByPlayer()) {
				this.entryFeeAmount = multiplayerGameConfig.getEntryFeeAmount();
				this.entryFeeCurrency = multiplayerGameConfig.getEntryFeeCurrency();
			}
		}
		return this;
	}
	
	public GameInstance build(){
		final GameInstance gameInstance = new GameInstance(game);
		
		gameInstance.setGameState(new GameState(gameStatus, entryFeeTransactionId));
		gameInstance.setUserId(userId);
        gameInstance.setTenantId(tenantId);
        gameInstance.setAppId(appId);
        gameInstance.setUserIp(userIp);
		gameInstance.setMgiId(mgiId);
		gameInstance.setTrainingMode(trainningMode);
		gameInstance.setTournamentId(tournamentId);
		gameInstance.setNumberOfQuestions(numberOfQuestions);
		gameInstance.setQuestionPoolIds(questionPoolIds);
		
		gameInstance.setEntryFeeAmount(entryFeeAmount);
		gameInstance.setEntryFeeCurrency(entryFeeCurrency);
		
		return gameInstance;
	}
}
