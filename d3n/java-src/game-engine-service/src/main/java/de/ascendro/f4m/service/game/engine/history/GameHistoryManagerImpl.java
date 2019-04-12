package de.ascendro.f4m.service.game.engine.history;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.query.PredExp;
import de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.request.jackpot.PaymentServiceCommunicator;
import de.ascendro.f4m.server.result.RefundReason;
import de.ascendro.f4m.service.game.engine.client.results.ResultEngineCommunicator;
import de.ascendro.f4m.service.game.engine.config.GameEngineConfig;
import de.ascendro.f4m.service.game.engine.dao.history.GameHistoryDao;
import de.ascendro.f4m.service.game.engine.dao.instance.ActiveGameInstanceDao;
import de.ascendro.f4m.service.game.engine.dao.instance.ActiveGameInstanceDaoImpl;
import de.ascendro.f4m.service.game.engine.dao.instance.GameInstanceAerospikeDao;
import de.ascendro.f4m.service.game.engine.model.*;
import de.ascendro.f4m.service.game.engine.model.end.GameEndStatus;
import de.ascendro.f4m.service.game.engine.multiplayer.MultiplayerGameManager;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.game.selection.model.multiplayer.MultiplayerUserGameInstance;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.ServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

public class GameHistoryManagerImpl implements GameHistoryManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(GameHistoryManagerImpl.class);

    private final GameEngineConfig config;
	private final GameHistoryDao gameHistoryDao;
	private final ServiceUtil serviceUtil;
	private final ActiveGameInstanceDao activeGameInstanceDao;
	private final GameInstanceAerospikeDao gameInstanceAerospikeDao;
	private final CommonMultiplayerGameInstanceDao commonMultiplayerGameInstanceDao;
	private final ResultEngineCommunicator resultEngineCommunicator;

	private final MultiplayerGameManager multiplayerGameManager;
	private final CommonProfileAerospikeDao commonProfileAerospikeDao;

    private final PaymentServiceCommunicator paymentServiceCommunicator;

    @Inject
    public GameHistoryManagerImpl(GameHistoryDao gameHistoryDao, ServiceUtil serviceUtil,
                                  ActiveGameInstanceDao activeGameInstanceDao,
                                  GameInstanceAerospikeDao gameInstanceAerospikeDao,
                                  CommonMultiplayerGameInstanceDao commonMultiplayerGameInstanceDao,
                                  ResultEngineCommunicator resultEngineCommunicator, MultiplayerGameManager multiplayerGameManager,
                                  CommonProfileAerospikeDao commonProfileAerospikeDao, PaymentServiceCommunicator paymentServiceCommunicator,
                                  GameEngineConfig gameEngineConfig) {
        this.gameHistoryDao = gameHistoryDao;
        this.serviceUtil = serviceUtil;
        this.activeGameInstanceDao = activeGameInstanceDao;
        this.gameInstanceAerospikeDao = gameInstanceAerospikeDao;
        this.commonMultiplayerGameInstanceDao = commonMultiplayerGameInstanceDao;
        this.resultEngineCommunicator = resultEngineCommunicator;
        this.multiplayerGameManager = multiplayerGameManager;
        this.commonProfileAerospikeDao = commonProfileAerospikeDao;
        this.paymentServiceCommunicator = paymentServiceCommunicator;
        this.config = gameEngineConfig;
    }
	
	@Override
	public void recordGameRegistration(String mgiId, String userId, String gameInstanceId, String gameId) {
		final GameHistory initialGameHistory = new GameHistory(GameStatus.REGISTERED);
		initialGameHistory.setGameInstanceId(gameInstanceId);
		initialGameHistory.setGameId(gameId);
		initialGameHistory.setMgiId(mgiId);
		initialGameHistory.setTimestamp(serviceUtil.getMessageTimestamp());
		gameHistoryDao.createUserGameHistoryEntry(userId, gameInstanceId, initialGameHistory);
	}
	
	@Override
	public void recordGamePrepared(String userId, String gameInstanceId) {
		updateGameHistoryStatus(userId, gameInstanceId, GameStatus.PREPARED, null);
	}

	@Override
	public void recordGameReadyToPlay(String userId, String gameInstanceId) {
		updateGameHistoryStatus(userId, gameInstanceId, GameStatus.READY_TO_PLAY, null);
	}

	@Override
	public void recordStartPlaying(String userId, String gameInstanceId) {
		updateGameHistoryStatus(userId, gameInstanceId, GameStatus.IN_PROGRESS, null);
	}

	@Override
	public void recordGameCompleted(String userId, String gameInstanceId) {
		final GameStatus newGameStatus = getCompletedOrCancelled(userId, gameInstanceId);
		updateGameHistoryStatus(userId, gameInstanceId, newGameStatus, GameEndStatus.CALCULATING_RESULT);
	}

	@Override
	public void recordMultiplayerResultsCalculated(String userId, String gameInstanceId) {
		final GameStatus newGameStatus = getCompletedOrCancelled(userId, gameInstanceId);
		updateGameHistoryStatus(userId, gameInstanceId, newGameStatus, GameEndStatus.CALCULATED_RESULT);
	}

	@Override
	public void recordGameCancellation(String userId, String gameInstanceId) {
		updateGameHistoryStatus(userId, gameInstanceId, GameStatus.CANCELLED, null);
	}
	
	private void updateGameHistoryStatus(String userId, String gameInstanceId, GameStatus gameStatus,
			GameEndStatus endStatus) {
		gameHistoryDao.updateGameHistoryStatus(userId, gameInstanceId, gameStatus, endStatus);
	}
	
	@Override
	public boolean isQuestionPlayed(String userId, String questionId) {
		return gameHistoryDao.isQuestionPlayed(userId, questionId);
	}

	@Override
	public void recordQuestionsPlayed(String userId, String gameId, String... additionalPlayedQuestions) {
		for (String questionId : additionalPlayedQuestions) {
			try {
				gameHistoryDao.addPlayedQuestions(userId, questionId);
			} catch (AerospikeException aEx) {
				LOGGER.error("Failed to add question[{}] as played for user[{}] within game[{}]", questionId, userId,
						gameId, aEx);
			}
		}
	}

	@Override
	public void recordMultiplayerResultsNotCalculated(String userId, String gameInstanceId) {
		final GameStatus newGameStatus = getCompletedOrCancelled(userId, gameInstanceId);
		updateGameHistoryStatus(userId, gameInstanceId, newGameStatus, GameEndStatus.CALCULATING_RESULTS_FAILED);
	}
	
	private GameStatus getCompletedOrCancelled(String userId, String gameInstanceId) {
		final GameStatus newGameStatus;
		if (isCancelled(userId, gameInstanceId)) {
			newGameStatus = GameStatus.CANCELLED;
		} else {
			newGameStatus = GameStatus.COMPLETED;
		}
		return newGameStatus;
	}
	
	private boolean isCancelled(String userId, String gameInstanceId){
		return gameHistoryDao.getGameHistory(userId, gameInstanceId).getStatus() == GameStatus.CANCELLED;
	}
	
	/* (non-Javadoc)
	 * @see de.ascendro.f4m.service.game.engine.history.GameHistoryManager#cleanUpActiveGameInstances()
	 */
	@Override
	public void cleanUpActiveGameInstances() {
		//gamePlayExpirationTimestamp <= currentTimestamp OR invitationExpirationTimestamp <= currentTimestamp
		final PredExp[] query = new PredExp[]{
			PredExp.integerBin(ActiveGameInstanceDaoImpl.GAME_PLAY_EXPIRATION_TS_BIN_NAME),
			PredExp.integerValue(DateTimeUtil.getUTCTimestamp()),
			PredExp.integerLessEq(),
			
			PredExp.integerBin(ActiveGameInstanceDaoImpl.INVITATION_EXPIRATION_TS_BIN_NAME),
			PredExp.integerValue(DateTimeUtil.getUTCTimestamp()),
			PredExp.integerLessEq(),
			
			PredExp.or(2)
		};
		processExpiredDuel();
		processExpiredTournament();

		activeGameInstanceDao.processActiveRecords(query, activeGameInstance-> {
			try {
                if(activeGameInstanceDao.exists(activeGameInstance.getId())){
					if (activeGameInstance.getInvitationExpirationTimestamp() <= DateTimeUtil.getUTCTimestamp() &&
							activeGameInstance.getGameType() != GameType.TOURNAMENT) {

						if (activeGameInstance.isMultiUserGame()) {
							processExpiredInvitationGame(activeGameInstance);
						}
					} else if (activeGameInstance.getGamePlayExpirationTimestamp() <= DateTimeUtil.getUTCTimestamp()) {
						processExpiredGame(activeGameInstance);
					} else {
						LOGGER.warn("Unexpected active game instance [{}] state ", activeGameInstance);
						assert false;
					}
				}
			} catch (Exception e) {
				LOGGER.error("Failed to process active game instance record {}", activeGameInstance, e);
			}
		});
	}


    //   The end of the duel, if anyone of the players flew either in a duel played only single player.(the timer runs every ten minutes)
    private void processExpiredDuel() {
        List<String> mgiIds = commonMultiplayerGameInstanceDao.getExtendedMgiDuel();
        for (String mgiId : mgiIds) {
            try {
                CustomGameConfig customGameConfig = commonMultiplayerGameInstanceDao.getConfig(mgiId);
                List<MultiplayerUserGameInstance> multiplayerUserGameInstances = commonMultiplayerGameInstanceDao.getGameInstances(mgiId);
                if (!isOnlyInviterRegistered(mgiId) && commonMultiplayerGameInstanceDao.getNotYetCalculatedGameInstancesCount(mgiId) == 1) {
                    //   if one of your friends is out of the game.
                    GameInstance gameInstanceUser1 = gameInstanceAerospikeDao.calculateIfUnfinished(getGameInstanceId(multiplayerUserGameInstances), CloseUpReason.EXPIRED, null, null, true);
                    resultEngineCommunicator.requestCalculateResults(null, gameInstanceUser1, null);
                } else if (!isOnlyInviterRegistered(mgiId) && commonMultiplayerGameInstanceDao.getNotYetCalculatedGameInstancesCount(mgiId) == 2) {
                    //   if both players are out of the game.
                    GameInstance gameInstanceUser1 = gameInstanceAerospikeDao.calculateIfUnfinished(multiplayerUserGameInstances.get(0).getGameInstanceId(), CloseUpReason.EXPIRED, null, null, true);
                    resultEngineCommunicator.requestCalculateResults(null, gameInstanceUser1, null);
                    GameInstance gameInstanceUser2 = gameInstanceAerospikeDao.calculateIfUnfinished(multiplayerUserGameInstances.get(1).getGameInstanceId(), CloseUpReason.EXPIRED, null, null, true);
                    resultEngineCommunicator.requestCalculateResults(null, gameInstanceUser2, null);
                } else if ((isOnlyInviterRegistered(mgiId))) {
                    //   if the duel was played by only one player.
					paymentServiceCommunicator.refundPayment(mgiId, commonMultiplayerGameInstanceDao.getConfig(mgiId));
                }
                commonMultiplayerGameInstanceDao.deleteDuel(mgiId);
                deleteActiveGameInstanceDao(multiplayerUserGameInstances);
            } catch (Exception ex) {
                LOGGER.error("Failed to process expired duel mgi [{}], exception [{}]", mgiId, ex.getMessage());
            }
        }
    }


    private String getGameInstanceId(List<MultiplayerUserGameInstance> multiplayerUserGameInstances) {
		String gameInstanceId = "";
        try {
			gameInstanceId = activeGameInstanceDao.getActiveGameInstance(multiplayerUserGameInstances.get(0).getGameInstanceId()) == null ?
					multiplayerUserGameInstances.get(1).getGameInstanceId() :
					multiplayerUserGameInstances.get(0).getGameInstanceId();
        } catch (Exception e) {
            LOGGER.error("Failed to process getUserId exception [{}]", e.getMessage());
        }
        return gameInstanceId;
	}

    private void deleteActiveGameInstanceDao(List<MultiplayerUserGameInstance> multiplayerUserGameInstances) {
        try {
            if(!multiplayerUserGameInstances.isEmpty()) {
                String gameInstanceIdToDel = activeGameInstanceDao.getActiveGameInstance(multiplayerUserGameInstances.get(0).getGameInstanceId()) != null
                        ? multiplayerUserGameInstances.get(0).getGameInstanceId()
                        : multiplayerUserGameInstances.get(1).getGameInstanceId();
				activeGameInstanceDao.delete(gameInstanceIdToDel);
            }
        } catch(Exception e){
            LOGGER.error("Failed to process deleteActiveGameInstanceDao exception [{}]", e.getMessage());
        }
    }

	private void processExpiredTournament() {
		List<String> mgiIds = commonMultiplayerGameInstanceDao.getExtendedMgiTournament();

		for (String mgiId : mgiIds) {
			try {
				List<MultiplayerUserGameInstance> gameInstances = commonMultiplayerGameInstanceDao.getGameInstances(mgiId);
				GameInstance gameInstance = null;
				for (MultiplayerUserGameInstance multiplayerUserGameInstance : gameInstances) {
					gameInstance = gameInstanceAerospikeDao.calculateIfUnfinished(multiplayerUserGameInstance.getGameInstanceId(), CloseUpReason.EXPIRED, null, null, false);
				}
				if (gameInstance != null) {
					final ClientInfo clientInfoWithoutSession = buildClientInfo(gameInstance);
						multiplayerGameManager.requestCalculateMultiplayerResultsIfPossible(clientInfoWithoutSession, mgiId, gameInstance.getGame(), null);
					activeGameInstanceDao.delete(gameInstance.getId());
				}
			} catch (Exception e) {
				LOGGER.error("Failed to process expired tournament mgi [{}], exception [{}]", mgiId, e);
			}
		}
	}

	private void processExpiredGame(ActiveGameInstance activeGameInstance) {
		if (activeGameInstance.isMultiUserGame() && isNoPlayerCompletedTheGame(activeGameInstance.getMgiId())) {
			//No player completed the game -> no refund
			final List<MultiplayerUserGameInstance> gameInstances = commonMultiplayerGameInstanceDao
					.getGameInstances(activeGameInstance.getMgiId(), MultiplayerGameInstanceState.STARTED);
			terminateRelatedMultiplayerGameInstanceIfNotSet(gameInstances);
		} else {
			if (activeGameInstance.getEndStatus() != GameEndStatus.CALCULATING_RESULTS_FAILED) {
				//game not finished for some reason
				calculateIfUnfinished(activeGameInstance.getId(), CloseUpReason.EXPIRED, null, null);
				activeGameInstanceDao.delete(activeGameInstance.getId());
			}
		}
	}

	private void terminateRelatedMultiplayerGameInstanceIfNotSet(final List<MultiplayerUserGameInstance> gameInstances) {
		for(MultiplayerUserGameInstance multiplayerGameInstance : gameInstances){
			terminatedIfNotSet(multiplayerGameInstance.getGameInstanceId(), CloseUpReason.NO_PLAYER_COMPLETED, null, null);
			activeGameInstanceDao.delete(multiplayerGameInstance.getGameInstanceId());
		}
	}
	
	/**
	 * Check if no player completed the game.
	 * Check count of created game instances and not yet calculated game instances.
	 * Also each game instance end status is checked for not presence.
	 * @param mgiId - multiplayer game instance id
	 * @return true if none of the game instances are actually ended
	 */
	private boolean isNoPlayerCompletedTheGame(String mgiId){
		final int gameInstanceCount = commonMultiplayerGameInstanceDao.getGameInstancesCount(mgiId);
		final int notYetCalculatedGameInstancesCount = commonMultiplayerGameInstanceDao.getNotYetCalculatedGameInstancesCount(mgiId);
		
		boolean noPlayersCompletedTheGame = false;
		if (gameInstanceCount == notYetCalculatedGameInstancesCount) {
			try {
				final long notCompletedGameCount = commonMultiplayerGameInstanceDao.getGameInstances(mgiId).stream()
						.map(mgi -> gameInstanceAerospikeDao.getGameInstance(mgi.getGameInstanceId()))
						.filter(gi -> gi.getGameState() == null || gi.getGameState().getGameEndStatus() == null)
						.count();
				noPlayersCompletedTheGame = notCompletedGameCount == gameInstanceCount;
			} catch (Exception e) {
				LOGGER.error("Failed to check all multiplayer game instances end statuses", e);
			}
		}
		return noPlayersCompletedTheGame;
	}

	private void  processExpiredInvitationGame(ActiveGameInstance activeGameInstance) {
		LOGGER.debug("Processing game instance with expired invitation {}", activeGameInstance);
		if (isOnlyInviterRegistered(activeGameInstance.getMgiId())) { // only inviter registered
			//move mgi status
			RefundReason refundReason = null;
			if (!activeGameInstance.isFree()) {
				// PAID: refund
				// - No invited people showing up (invitation expired) -> refund Duel entry fee
				// - No invited people showing up -> refund Tournament entry fee
				refundReason = RefundReason.NO_OPPONENT;
			}
			terminatedIfNotSet(activeGameInstance.getId(), CloseUpReason.NO_INVITEE, null, refundReason);
			activeGameInstanceDao.delete(activeGameInstance.getId());
		}
	}
	
	@Override
	public void terminatedIfNotSet(String gameInstanceId, CloseUpReason closeUpReason, String errorMessage, RefundReason refundReason) {
		LOGGER.debug("Terminating GameInstance [{}] with CloseUpReason [{}], RefundReason [{}] and Error Message [{}]",
				gameInstanceId, closeUpReason, refundReason, errorMessage);
		final GameInstance terminatedGameInstance = gameInstanceAerospikeDao.terminateIfNotYet(gameInstanceId, closeUpReason, errorMessage, refundReason);
		if (terminatedGameInstance.getMgiId() != null && terminatedGameInstance.getGameState().isCancelled()) {
            multiplayerGameManager.cancelGame(terminatedGameInstance.getUserId(), terminatedGameInstance.getMgiId(), gameInstanceId);
            final ClientInfo clientInfoWithoutSession = buildClientInfo(terminatedGameInstance);
            multiplayerGameManager.requestCalculateMultiplayerResultsIfPossible(clientInfoWithoutSession,
                    terminatedGameInstance.getMgiId(), terminatedGameInstance.getGame(), null);
		}
	}
	
	@Override
	public ClientInfo buildClientInfo(GameInstance gameInstance){
		final ClientInfo clientInfo = new ClientInfo(gameInstance.getTenantId(), gameInstance.getAppId(),
				gameInstance.getUserId(), gameInstance.getUserIp(), gameInstance.getUserHandicap());
		final Profile profile = commonProfileAerospikeDao.getProfile(gameInstance.getUserId());
		final String[] userRoles = profile == null ? new String[] {} : profile.getRoles(gameInstance.getTenantId());
		clientInfo.setRoles(userRoles);
		return clientInfo;
	}
	
	@Override
	public void calculateIfUnfinished(String gameInstanceId, CloseUpReason closeUpReason, String errorMessage,
			RefundReason refundReason){
		calculateIfUnfinished(null, gameInstanceId, closeUpReason, errorMessage, refundReason);
	}
	
	@Override
	public void calculateIfUnfinished(ClientInfo clientInfo, String gameInstanceId, CloseUpReason closeUpReason,
			String errorMessage, RefundReason refundReason) {
		final GameInstance gameInstance = gameInstanceAerospikeDao.calculateIfUnfinished(gameInstanceId, closeUpReason,
				errorMessage, refundReason, false);
        final ClientInfo clientInfoWithoutSession = Optional.ofNullable(clientInfo)
				.orElse(buildClientInfo(gameInstance));
        if (gameInstance.getGameState().getGameEndStatus() == GameEndStatus.CALCULATING_RESULT) {
                    resultEngineCommunicator.requestCalculateResults(clientInfoWithoutSession, gameInstance, null);
        } else if (gameInstance.getMgiId() != null && gameInstance.getGameState().isCancelled()) {
            multiplayerGameManager.cancelGame(gameInstance.getUserId(), gameInstance.getMgiId(), gameInstanceId);
            multiplayerGameManager.requestCalculateMultiplayerResultsIfPossible(clientInfoWithoutSession, gameInstance.getMgiId(),
                    gameInstance.getGame(), null);
        }
	}

	private boolean isOnlyInviterRegistered(String mgiId) {
		return commonMultiplayerGameInstanceDao.getGameInstancesCount(mgiId) == 1;
	}
}
