package de.ascendro.f4m.service.game.engine.multiplayer;

import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.GAME_ID;
import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.MGI_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.server.game.GameAerospikeDao;
import de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDao;
import de.ascendro.f4m.server.multiplayer.dao.PublicGameElasticDao;
import de.ascendro.f4m.service.game.engine.client.results.ResultEngineCommunicatorImpl;
import de.ascendro.f4m.service.game.engine.dao.instance.ActiveGameInstanceDao;
import de.ascendro.f4m.service.game.engine.dao.instance.GameInstanceAerospikeDao;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.game.GameTypeConfiguration;
import de.ascendro.f4m.service.game.selection.model.game.MultiplayerGameTypeConfigurationData;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.game.selection.model.multiplayer.MultiplayerUserGameInstance;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class MultiplayerGameManagerTest {

	@Mock
	private CommonMultiplayerGameInstanceDao commonMultiplayerGameInstanceDao;
	@Mock
	private ResultEngineCommunicatorImpl resultEngineCommunicatorImpl;
	@Mock
	private SessionWrapper sourceSessionMock;
	@Mock
	private PublicGameElasticDao publicGameDao;
	@Mock
	private GameAerospikeDao gameDao;
	@Mock
	private GameInstanceAerospikeDao gameInstanceDao;
	@Mock
	private ActiveGameInstanceDao activeGameInstanceDao;
	
	private MultiplayerGameManagerImpl multiplayerGameManagerImpl;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		multiplayerGameManagerImpl = new MultiplayerGameManagerImpl(commonMultiplayerGameInstanceDao,
				resultEngineCommunicatorImpl, publicGameDao, gameDao, gameInstanceDao, activeGameInstanceDao);
	}

	@Test
	public void testMarkUserResultsAsCalculated() {
		final String mgiId = "anyMgiId";
		final String gameInstanceId = "anyInstanceId";
		final String userId = "anyUserId";
		
		multiplayerGameManagerImpl.markUserResultsAsCalculated(mgiId, gameInstanceId, userId);
		verify(commonMultiplayerGameInstanceDao, times(1)).markGameInstanceAsCalculated(mgiId, gameInstanceId, userId);
		verify(commonMultiplayerGameInstanceDao, times(1)).addToNotYetCalculatedCounter(mgiId, -1);
	}
	
	@Test
	public void testRequestCalculateMultiplayerResultsIfPossible() throws Exception {
		ZonedDateTime gameStartDate = DateTimeUtil.getCurrentDateTime().minusDays(1);
		ZonedDateTime gameEndDate = gameStartDate;
		
		final Game game = mock(Game.class);

		final CustomGameConfig customGameConfig = new CustomGameConfig();
		customGameConfig.setMaxNumberOfParticipants(2);
		customGameConfig.setId(MGI_ID);
		customGameConfig.setStartDateTime(gameStartDate);
		customGameConfig.setEndDateTime(gameEndDate);
		when(commonMultiplayerGameInstanceDao.getConfig(MGI_ID)).thenReturn(customGameConfig);
		
		// test DUEL
		when(game.getType()).thenReturn(GameType.DUEL);
		
		//Still remaining uncalculated results
		reset(resultEngineCommunicatorImpl);
		when(commonMultiplayerGameInstanceDao.hasNoRemainingGameInstances(MGI_ID)).thenReturn(false);
		when(commonMultiplayerGameInstanceDao.getGameInstancesCount(MGI_ID)).thenReturn(2);
		multiplayerGameManagerImpl.requestCalculateMultiplayerResultsIfPossible(ANONYMOUS_CLIENT_INFO, MGI_ID, game,
				sourceSessionMock);
		verify(resultEngineCommunicatorImpl, never()).requestCalculateMultiplayerResults(ANONYMOUS_CLIENT_INFO, MGI_ID,
				GameType.DUEL, sourceSessionMock);
		
		//Two players with all calculated results
		reset(resultEngineCommunicatorImpl);
		when(commonMultiplayerGameInstanceDao.hasNoRemainingGameInstances(MGI_ID)).thenReturn(true);
		when(commonMultiplayerGameInstanceDao.getGameInstancesCount(MGI_ID)).thenReturn(2);
		when(commonMultiplayerGameInstanceDao.hasAnyCalculated(MGI_ID)).thenReturn(true);
		multiplayerGameManagerImpl.requestCalculateMultiplayerResultsIfPossible(ANONYMOUS_CLIENT_INFO, MGI_ID, game,
				sourceSessionMock);
		verify(resultEngineCommunicatorImpl, times(1)).requestCalculateMultiplayerResults(ANONYMOUS_CLIENT_INFO, MGI_ID,
				GameType.DUEL, sourceSessionMock);
			
		//Expired game: one player with calculated results and other not register
		reset(resultEngineCommunicatorImpl);
		when(commonMultiplayerGameInstanceDao.hasNoRemainingGameInstances(MGI_ID)).thenReturn(true);
		when(commonMultiplayerGameInstanceDao.getGameInstancesCount(MGI_ID)).thenReturn(1);		
		multiplayerGameManagerImpl.requestCalculateMultiplayerResultsIfPossible(ANONYMOUS_CLIENT_INFO, MGI_ID, game,
				sourceSessionMock);
		verify(resultEngineCommunicatorImpl, times(1)).requestCalculateMultiplayerResults(ANONYMOUS_CLIENT_INFO, MGI_ID,
				GameType.DUEL, sourceSessionMock);
		
		//Expired game: custom end date and two players
		reset(resultEngineCommunicatorImpl);
		customGameConfig.setEndDateTime(DateTimeUtil.getCurrentDateTime().minusDays(1000)); //custom expiration
		when(commonMultiplayerGameInstanceDao.getGameInstancesCount(MGI_ID)).thenReturn(2);
		multiplayerGameManagerImpl.requestCalculateMultiplayerResultsIfPossible(ANONYMOUS_CLIENT_INFO, MGI_ID, game,
				sourceSessionMock);
		verify(resultEngineCommunicatorImpl, times(1)).requestCalculateMultiplayerResults(ANONYMOUS_CLIENT_INFO, MGI_ID,
				GameType.DUEL, sourceSessionMock);
		
		//unlimited max participants - delayed registration
		reset(resultEngineCommunicatorImpl);
		customGameConfig.setMaxNumberOfParticipants(null);
		customGameConfig.setEndDateTime(DateTimeUtil.getCurrentDateTime().plusDays(1));
		when(commonMultiplayerGameInstanceDao.getGameInstancesCount(MGI_ID)).thenReturn(RandomUtils.nextInt(1, 100));
		multiplayerGameManagerImpl.requestCalculateMultiplayerResultsIfPossible(ANONYMOUS_CLIENT_INFO, MGI_ID, game,
				sourceSessionMock);
		verify(resultEngineCommunicatorImpl, never()).requestCalculateMultiplayerResults(ANONYMOUS_CLIENT_INFO, MGI_ID,
				GameType.DUEL, sourceSessionMock);
		
		// test USER_TOURNAMENT
		when(game.getType()).thenReturn(GameType.USER_TOURNAMENT);
		
		//Expired and ended: all players with calculated results
		reset(resultEngineCommunicatorImpl);
		customGameConfig.setMaxNumberOfParticipants(null);
		customGameConfig.setEndDateTime(DateTimeUtil.getCurrentDateTime().minusHours(1));
		when(commonMultiplayerGameInstanceDao.hasNoRemainingGameInstances(MGI_ID)).thenReturn(true);
		when(commonMultiplayerGameInstanceDao.getGameInstancesCount(MGI_ID)).thenReturn(RandomUtils.nextInt(1, 100));
		multiplayerGameManagerImpl.requestCalculateMultiplayerResultsIfPossible(ANONYMOUS_CLIENT_INFO, MGI_ID, game,
				sourceSessionMock);
		verify(resultEngineCommunicatorImpl, times(1)).requestCalculateMultiplayerResults(ANONYMOUS_CLIENT_INFO, MGI_ID,
				GameType.USER_TOURNAMENT, sourceSessionMock);
		
		//Expired but not ended: all players with calculated results, new players can't join
		reset(resultEngineCommunicatorImpl);
		customGameConfig.setMaxNumberOfParticipants(null);
		customGameConfig.setExpiryDateTime(DateTimeUtil.getCurrentDateTime().minusHours(1));
		customGameConfig.setEndDateTime(DateTimeUtil.getCurrentDateTime().plusHours(2));
		when(commonMultiplayerGameInstanceDao.hasNoRemainingGameInstances(MGI_ID)).thenReturn(true);
		when(commonMultiplayerGameInstanceDao.getGameInstancesCount(MGI_ID)).thenReturn(RandomUtils.nextInt(1, 100));
		multiplayerGameManagerImpl.requestCalculateMultiplayerResultsIfPossible(ANONYMOUS_CLIENT_INFO, MGI_ID, game,
				sourceSessionMock);
		verify(resultEngineCommunicatorImpl, times(1)).requestCalculateMultiplayerResults(ANONYMOUS_CLIENT_INFO, MGI_ID,
				GameType.USER_TOURNAMENT, sourceSessionMock);
		
		//Not expired and not ended: all players with calculated results, new players may join
		reset(resultEngineCommunicatorImpl);
		customGameConfig.setMaxNumberOfParticipants(null);
		customGameConfig.setExpiryDateTime(DateTimeUtil.getCurrentDateTime().plusHours(1));
		customGameConfig.setEndDateTime(DateTimeUtil.getCurrentDateTime().plusHours(2));
		when(commonMultiplayerGameInstanceDao.hasNoRemainingGameInstances(MGI_ID)).thenReturn(true);
		when(commonMultiplayerGameInstanceDao.getGameInstancesCount(MGI_ID)).thenReturn(RandomUtils.nextInt(1, 100));
		multiplayerGameManagerImpl.requestCalculateMultiplayerResultsIfPossible(ANONYMOUS_CLIENT_INFO, MGI_ID, game,
				sourceSessionMock);
		verify(resultEngineCommunicatorImpl, never()).requestCalculateMultiplayerResults(ANONYMOUS_CLIENT_INFO, MGI_ID,
				GameType.USER_TOURNAMENT, sourceSessionMock);
		
		//Not expired and not ended: all players with calculated results, new players can't join as max number of participants is reached
		reset(resultEngineCommunicatorImpl);
		customGameConfig.setMaxNumberOfParticipants(13);
		customGameConfig.setExpiryDateTime(DateTimeUtil.getCurrentDateTime().plusHours(1));
		customGameConfig.setEndDateTime(DateTimeUtil.getCurrentDateTime().plusHours(2));
		when(commonMultiplayerGameInstanceDao.hasNoRemainingGameInstances(MGI_ID)).thenReturn(true);
		when(commonMultiplayerGameInstanceDao.getGameInstancesCount(MGI_ID)).thenReturn(13);
		multiplayerGameManagerImpl.requestCalculateMultiplayerResultsIfPossible(ANONYMOUS_CLIENT_INFO, MGI_ID, game,
				sourceSessionMock);
		verify(resultEngineCommunicatorImpl, times(1)).requestCalculateMultiplayerResults(ANONYMOUS_CLIENT_INFO, MGI_ID,
				GameType.USER_TOURNAMENT, sourceSessionMock);
	}
	
	@Test
	public void testMarkTournamentAsEnded() throws Exception {
		multiplayerGameManagerImpl.markTournamentAsEnded(MGI_ID);
		verify(commonMultiplayerGameInstanceDao).markTournamentAsEnded(MGI_ID);
		verify(publicGameDao).delete(MGI_ID, true, true);
	}
	
	@Test
	public void testHasEnoughPlayersToPlay() throws Exception {
		Map<String, String> usersOfMgi = new HashMap<>();
		Game game = new Game();
		game.setType(GameType.LIVE_TOURNAMENT);
		MultiplayerGameTypeConfigurationData liveTournament = new MultiplayerGameTypeConfigurationData();
		liveTournament.setMinimumPlayerNeeded(1);
		game.setTypeConfiguration(new GameTypeConfiguration(liveTournament, null, null));
		when(commonMultiplayerGameInstanceDao.getConfig(MGI_ID)).thenReturn(new CustomGameConfig(GAME_ID));
		when(commonMultiplayerGameInstanceDao.getAllUsersOfMgi(MGI_ID)).thenReturn(usersOfMgi);
		when(gameDao.getGame(GAME_ID)).thenReturn(game);
		
		
		// default minimumPlayerNeeded (null -> 1); not enough
		usersOfMgi.put("some_user", MultiplayerGameInstanceState.INVITED.name());
		assertFalse(multiplayerGameManagerImpl.hasEnoughPlayersToPlay(MGI_ID));
		
		// default minimumPlayerNeeded (null -> 1); enough
		usersOfMgi.put("some_user", MultiplayerGameInstanceState.REGISTERED.name());
		assertTrue(multiplayerGameManagerImpl.hasEnoughPlayersToPlay(MGI_ID));
		
		// customized minimumPlayerNeeded (2); not enough
		MultiplayerGameTypeConfigurationData gameTournament = new MultiplayerGameTypeConfigurationData();
		gameTournament.setMinimumPlayerNeeded(2);
		game.setTypeConfiguration(new GameTypeConfiguration(gameTournament, 
				null,
				null));
		usersOfMgi.put("another_user", MultiplayerGameInstanceState.INVITED.name());
		assertFalse(multiplayerGameManagerImpl.hasEnoughPlayersToPlay(MGI_ID));
		
		// customized minimumPlayerNeeded (2); enough
		usersOfMgi.put("another_user", MultiplayerGameInstanceState.STARTED.name());
		assertTrue(multiplayerGameManagerImpl.hasEnoughPlayersToPlay(MGI_ID));
	}
	
	@Test
	public void testCancelLiveTournament() throws Exception {
		String gameInstanceId = "game_instance_id";
		String userId = "user_id";
		MultiplayerUserGameInstance userGameInstance = new MultiplayerUserGameInstance(gameInstanceId, new ClientInfo(userId));
		when(commonMultiplayerGameInstanceDao.getGameInstances(eq(MGI_ID), any())).thenReturn(Arrays.asList(userGameInstance));
		
		multiplayerGameManagerImpl.cancelLiveTournament(MGI_ID);
		verify(gameInstanceDao).cancelGameInstance(gameInstanceId);
		verify(activeGameInstanceDao).delete(gameInstanceId);
	}
}
