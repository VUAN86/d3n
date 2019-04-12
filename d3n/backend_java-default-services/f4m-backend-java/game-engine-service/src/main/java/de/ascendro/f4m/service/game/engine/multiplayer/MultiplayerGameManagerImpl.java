package de.ascendro.f4m.service.game.engine.multiplayer;

import de.ascendro.f4m.server.game.GameAerospikeDao;
import de.ascendro.f4m.server.game.GameUtil;
import de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDao;
import de.ascendro.f4m.server.multiplayer.dao.PublicGameElasticDao;
import de.ascendro.f4m.service.game.engine.client.results.ResultEngineCommunicator;
import de.ascendro.f4m.service.game.engine.dao.instance.ActiveGameInstanceDao;
import de.ascendro.f4m.service.game.engine.dao.instance.GameInstanceAerospikeDao;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.game.selection.model.multiplayer.MultiplayerUserGameInstance;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.session.SessionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

public class MultiplayerGameManagerImpl implements MultiplayerGameManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(MultiplayerGameManagerImpl.class);
	
	private final CommonMultiplayerGameInstanceDao commonMultiplayerGameInstanceDao;
	private final ResultEngineCommunicator resultEngineCommunicator;
	private final PublicGameElasticDao publicGameDao;
	private final GameAerospikeDao gameDao;
	private final GameInstanceAerospikeDao gameInstanceDao;
	private final ActiveGameInstanceDao activeGameInstanceDao;

	@Inject
	public MultiplayerGameManagerImpl(CommonMultiplayerGameInstanceDao commonMultiplayerGameInstanceDao,
			ResultEngineCommunicator resultEngineCommunicatorImpl, PublicGameElasticDao publicGameDao,
			GameAerospikeDao gameDao, GameInstanceAerospikeDao gameInstanceDao,
			ActiveGameInstanceDao activeGameInstanceDao) {
		this.commonMultiplayerGameInstanceDao = commonMultiplayerGameInstanceDao;
		this.resultEngineCommunicator = resultEngineCommunicatorImpl;
		this.publicGameDao = publicGameDao;
		this.gameDao = gameDao;
		this.gameInstanceDao = gameInstanceDao;
		this.activeGameInstanceDao = activeGameInstanceDao;
	}
	
	@Override
	public void joinGame(String mgiId, String gameInstanceId, String userId, Double userHandicap) {
		commonMultiplayerGameInstanceDao.joinGame(mgiId, gameInstanceId, userId, userHandicap);
	}
	
	@Override
	public void registerForGame(String mgiId, ClientInfo clientInfo, String gameInstanceId, CustomGameConfig customGameConfig) {
		commonMultiplayerGameInstanceDao.registerForGame(mgiId, clientInfo, gameInstanceId);
		String previousGameInstanceId = "";
		if (customGameConfig.getGameType().isTournament()) {
			previousGameInstanceId = commonMultiplayerGameInstanceDao.getGameInstanceOfThePreviousGame(mgiId, clientInfo.getUserId());
			commonMultiplayerGameInstanceDao.createOrUpdateGameInstanceIdOfPreviousGame(mgiId, clientInfo.getUserId(), gameInstanceId);
		}
		if (/*!(customGameConfig.getGameType() == GameType.TIPP_TOURNAMENT) || */ commonMultiplayerGameInstanceDao.numberOfAttempts(mgiId, clientInfo.getUserId()) <= 1) {
			commonMultiplayerGameInstanceDao.addToGameInstancesCounter(mgiId, +1);
			commonMultiplayerGameInstanceDao.addToNotYetCalculatedCounter(mgiId, +1);
		}
		if (commonMultiplayerGameInstanceDao.numberOfAttempts(mgiId, clientInfo.getUserId()) > 1) {
			commonMultiplayerGameInstanceDao.removalOfThePreviousAttempt(mgiId, clientInfo, previousGameInstanceId);
		}
	}

	@Override
	public void markUserResultsAsCalculated(String mgiId, String gameInstanceId, String userId) {
		commonMultiplayerGameInstanceDao.markGameInstanceAsCalculated(mgiId, gameInstanceId, userId);
		commonMultiplayerGameInstanceDao.addToNotYetCalculatedCounter(mgiId, -1);
	}
	
	@Override
	public void requestCalculateMultiplayerResultsIfPossible(ClientInfo clientInfo, String mgiId, Game game,
			SessionWrapper sourceSession){
		LOGGER.debug("requestCalculateMultiplayerResultsIfPossible 1 {} ");
		final int gameInstancesCount = commonMultiplayerGameInstanceDao.getGameInstancesCount(mgiId);
		if (gameInstancesCount > 0) {
			LOGGER.debug("requestCalculateMultiplayerResultsIfPossible 2 {} ");
			final CustomGameConfig customGameConfig = commonMultiplayerGameInstanceDao.getConfig(mgiId);
			LOGGER.debug("requestCalculateMultiplayerResultsIfPossible 3 {} ");
			if(isCalculateOfMultiplayerResultsPossible(customGameConfig, game.getType(), gameInstancesCount)){
				LOGGER.debug("requestCalculateMultiplayerResultsIfPossible 4 {} ");
				resultEngineCommunicator.requestCalculateMultiplayerResults(clientInfo, mgiId, game.getType(),
						sourceSession);
			}			
		} else {
			LOGGER.error("Failed to request game [{}] calculation result as zero multiplayer [{}] game instances registered",
					game.getGameId(), mgiId);
		}
	}
	
	private boolean isCalculateOfMultiplayerResultsPossible(CustomGameConfig customGameConfig, GameType gameType, int gameInstancesCount){
		final boolean requestCalculateMultiplayerResults; 
		final boolean allRegistered = gameInstancesCount >= firstNonNull(customGameConfig.getMaxNumberOfParticipants(), Integer.MAX_VALUE);
		if (allRegistered || gameType.isLive()) {
			requestCalculateMultiplayerResults = commonMultiplayerGameInstanceDao
					.hasNoRemainingGameInstances(customGameConfig.getId());
		} else if (gameType == GameType.USER_TOURNAMENT) {
			requestCalculateMultiplayerResults = GameUtil.isGameInvitationExpired(customGameConfig)
					|| !GameUtil.isGameAvailable(customGameConfig);
		} else {
			requestCalculateMultiplayerResults = !GameUtil.isGameAvailable(customGameConfig);
		}

		final boolean atLeastOneCalculated = commonMultiplayerGameInstanceDao.hasAnyCalculated(customGameConfig.getId());
		LOGGER.debug("isCalculateOfMultiplayerResultsPossible allRegistered {}   !GameUtil.isGameAvailable(customGameConfig) {}  atLeastOneCalculated {} ", allRegistered, !GameUtil
				.isGameAvailable(customGameConfig), atLeastOneCalculated);

		return requestCalculateMultiplayerResults && atLeastOneCalculated;
	}
	
	@Override
	public void cancelGame(String userId, String mgiId, String gameInstanceId) {
		final MultiplayerGameInstanceState initialState = commonMultiplayerGameInstanceDao.cancelGameInstance(mgiId,
				gameInstanceId, userId);
		if(initialState == MultiplayerGameInstanceState.STARTED || initialState == MultiplayerGameInstanceState.REGISTERED){
			commonMultiplayerGameInstanceDao.addToNotYetCalculatedCounter(mgiId, -1);
		}
	}
	
	@Override
	public CustomGameConfig getMultiplayerGameConfig(String mgiId) {
		return commonMultiplayerGameInstanceDao.getConfig(mgiId);
	}

	@Override
	public void markAsExpired(String mgiId) {
		commonMultiplayerGameInstanceDao.markAsExpired(mgiId);
	}

	public List<MultiplayerUserGameInstance> getMultiplayerGameInstances(String mgiId) {
		return commonMultiplayerGameInstanceDao.getGameInstances(mgiId);
	}
	@Override
	public void markTournamentAsEnded(String mgiId) {
		commonMultiplayerGameInstanceDao.markTournamentAsEnded(mgiId);
		publicGameDao.delete(mgiId, true, true);
	}
	
	@Override
	public void cleanUpPublicGameList() {
		publicGameDao.removeLiveTournamentsWithExpiredPlayDateTime();
	}

	@Override
	public boolean hasEnoughPlayersToPlay(String mgiId) {
		CustomGameConfig multiplayerGameConfig = commonMultiplayerGameInstanceDao.getConfig(mgiId);
		Game game = gameDao.getGame(multiplayerGameConfig.getGameId());
		int minimumPlayerNeeded = game.getMinimumPlayerNeeded();
		Map<String, String> allUsersOfMgi = commonMultiplayerGameInstanceDao.getAllUsersOfMgi(mgiId);
		long countOfPlayers = allUsersOfMgi.values().stream()
				.filter(state -> Objects.equals(state, MultiplayerGameInstanceState.REGISTERED.name())
						|| Objects.equals(state, MultiplayerGameInstanceState.STARTED.name()))
				.count();
		return countOfPlayers >= minimumPlayerNeeded;
	}
	
	@Override
	public void cancelLiveTournament(String mgiId) {
		List<MultiplayerUserGameInstance> userGameInstances = commonMultiplayerGameInstanceDao.getGameInstances(mgiId,
				MultiplayerGameInstanceState.REGISTERED, MultiplayerGameInstanceState.STARTED);
		for (MultiplayerUserGameInstance userGameInstance : userGameInstances) {
			String gameInstanceId = userGameInstance.getGameInstanceId();
			gameInstanceDao.cancelGameInstance(gameInstanceId);
			cancelGame(userGameInstance.getUserId(), mgiId, gameInstanceId);
			activeGameInstanceDao.delete(gameInstanceId);
		}
		markTournamentAsEnded(mgiId);
	}
	
}
