package de.ascendro.f4m.service.game.engine.history;

import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.MGI_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_USER_ID;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.result.RefundReason;
import de.ascendro.f4m.service.game.engine.client.results.ResultEngineCommunicator;
import de.ascendro.f4m.service.game.engine.dao.history.GameHistoryDao;
import de.ascendro.f4m.service.game.engine.dao.instance.ActiveGameInstanceDao;
import de.ascendro.f4m.service.game.engine.dao.instance.GameInstanceAerospikeDao;
import de.ascendro.f4m.service.game.engine.model.ActiveGameInstance;
import de.ascendro.f4m.service.game.engine.model.CloseUpReason;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.engine.model.GameState;
import de.ascendro.f4m.service.game.engine.model.GameStatus;
import de.ascendro.f4m.service.game.engine.model.end.GameEndStatus;
import de.ascendro.f4m.service.game.engine.multiplayer.MultiplayerGameManager;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.multiplayer.MultiplayerUserGameInstance;
import de.ascendro.f4m.service.json.model.user.UserRole;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;

public class GameHistoryManagerImplTest {
	
	private static final String EXPIRED_INVITATION_INSTANCE_ID = "expired-invite";
	private static final String EXPIRED_GAME_PLAY_INSTANCE_ID = "expired-game-play";

	@Mock
	private LoggingUtil loggingUtil;
	@Mock
	private GameHistoryDao gameHistoryDao;
	@Mock
	private ActiveGameInstanceDao activeGameInstanceDao;
	@Mock
	private GameInstanceAerospikeDao gameInstanceAerospikeDao;
	@Mock
	private CommonMultiplayerGameInstanceDao commonMultiplayerGameInstanceDao;
	@Mock
	private ResultEngineCommunicator resultEngineCommunicator;
	@Mock
	private MultiplayerGameManager multiplayerGameManager;
	@Mock
	private CommonProfileAerospikeDao commonProfileAerospikeDao;
	
	private final Game game = new Game();;
	
	@InjectMocks
	private GameHistoryManagerImpl gameHistoryManagerImpl;
	
	private List<ActiveGameInstance> activeGameInstances = new ArrayList<>();
	
	private final ActiveGameInstance expiredInvitation = new ActiveGameInstance();
	private final GameInstance expiredInvitationGameInstance = new GameInstance(game);
	
	private final ActiveGameInstance expiredGamePlay = new ActiveGameInstance();
	private final GameInstance expiredGamePlayGameInstance = new GameInstance(game);
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				final Consumer<ActiveGameInstance> consumer = invocation.getArgument(1);
				
				activeGameInstances.forEach(activeGameInstance -> 
					consumer.accept(activeGameInstance));
				return null;
			}
		}).when(activeGameInstanceDao).processActiveRecords(ArgumentMatchers.any(), ArgumentMatchers.any());
	
		
		when(activeGameInstanceDao.exists(EXPIRED_INVITATION_INSTANCE_ID)).thenReturn(true);
		when(activeGameInstanceDao.exists(EXPIRED_GAME_PLAY_INSTANCE_ID)).thenReturn(true);
		
		when(gameInstanceAerospikeDao.getGameInstance(EXPIRED_GAME_PLAY_INSTANCE_ID))
				.thenReturn(expiredGamePlayGameInstance);
		when(gameInstanceAerospikeDao.getGameInstance(EXPIRED_INVITATION_INSTANCE_ID))
				.thenReturn(expiredInvitationGameInstance);
		
		final GameInstance cancelledAndTerminted = mockedGameInstance(GameStatus.CANCELLED, GameEndStatus.TERMINATED);
		when(gameInstanceAerospikeDao.terminateIfNotYet(any(), any(), any(), any()))
			.thenReturn(cancelledAndTerminted);
		when(gameInstanceAerospikeDao.calculateIfUnfinished(any(), any(), any(), any()))
			.thenReturn(cancelledAndTerminted);
		
		final Profile profile = mock(Profile.class);
		when(profile.getRoles(ArgumentMatchers.anyString())).thenReturn(new String[] { UserRole.ANONYMOUS.name() });
		when(commonProfileAerospikeDao.getProfile(ANONYMOUS_USER_ID)).thenReturn(profile);
		
		when(commonMultiplayerGameInstanceDao.getGameInstances(MGI_ID, MultiplayerGameInstanceState.STARTED))
				.thenReturn(Arrays.asList(
						new MultiplayerUserGameInstance(EXPIRED_INVITATION_INSTANCE_ID, ANONYMOUS_CLIENT_INFO),
						new MultiplayerUserGameInstance(EXPIRED_GAME_PLAY_INSTANCE_ID, ANONYMOUS_CLIENT_INFO)
				));
		
		//Expired invitation
		expiredInvitationGameInstance.setId(EXPIRED_INVITATION_INSTANCE_ID);
		
		expiredInvitation.setId(EXPIRED_INVITATION_INSTANCE_ID);
		expiredInvitation.setMgiId(MGI_ID);
		expiredInvitation.setGameType(GameType.USER_TOURNAMENT);
		expiredInvitation.setGamePlayExpirationTimestamp(Long.MAX_VALUE);
		expiredInvitation.setInvitationExpirationTimestamp(DateTimeUtil.getCurrentDateTime()
				.minusMinutes(1).toInstant().toEpochMilli());
		expiredInvitation.setStatus(GameStatus.REGISTERED);
		expiredInvitation.setEntryFeeAmount(new BigDecimal("5"));
		expiredInvitation.setEntryFeeCurrency(Currency.BONUS);
		
		//Expired invitation
		expiredGamePlayGameInstance.setId(EXPIRED_GAME_PLAY_INSTANCE_ID);
		expiredGamePlay.setId(EXPIRED_GAME_PLAY_INSTANCE_ID);
		expiredGamePlay.setMgiId(MGI_ID);
		expiredGamePlay.setGameType(GameType.TOURNAMENT);
		expiredGamePlay.setGamePlayExpirationTimestamp(DateTimeUtil.getCurrentDateTime()
				.minusMinutes(1).toInstant().toEpochMilli());
		expiredGamePlay.setInvitationExpirationTimestamp(Long.MAX_VALUE);
		expiredGamePlay.setStatus(GameStatus.REGISTERED);
		expiredGamePlay.setEntryFeeAmount(new BigDecimal("5"));
		expiredGamePlay.setEntryFeeCurrency(Currency.BONUS);
	}
	
	private GameInstance mockedGameInstance(GameStatus gameStatus, GameEndStatus gameEndStatus){
		final GameInstance gameInstance = mock(GameInstance.class);
		when(gameInstance.getMgiId()).thenReturn(MGI_ID);
		when(gameInstance.getGame()).thenReturn(game);
		
		if(gameStatus != null && gameEndStatus != null){
			final GameState state = new GameState(gameStatus);
			state.setGameEndStatus(gameEndStatus);
			when(gameInstance.getGameState()).thenReturn(state);
		}
		
		when(gameInstance.getUserId()).thenReturn(KeyStoreTestUtil.ANONYMOUS_USER_ID);
		
		return gameInstance;
	}

	/**
	 * No invited people showing up (invitation expired) -> refund Duel entry fee
	 * For paid game.
	 */
	@Test
	public void testNoInvitedPeopleShowingUpUntilInvitationExpiredForPaidGame() {
		Collections.addAll(activeGameInstances, expiredInvitation);
		
		when(commonMultiplayerGameInstanceDao.getGameInstances(MGI_ID))
			.thenReturn(asList(
					new MultiplayerUserGameInstance(EXPIRED_INVITATION_INSTANCE_ID, ANONYMOUS_CLIENT_INFO), 
					new MultiplayerUserGameInstance(EXPIRED_GAME_PLAY_INSTANCE_ID, ANONYMOUS_CLIENT_INFO)
			));
		when(commonMultiplayerGameInstanceDao.getGameInstancesCount(MGI_ID)).thenReturn(1);
		
		assertFalse(expiredInvitation.isFree());
		gameHistoryManagerImpl.cleanUpActiveGameInstances();
		//expired invite
		verify(gameInstanceAerospikeDao, times(1)).terminateIfNotYet(EXPIRED_INVITATION_INSTANCE_ID,
				CloseUpReason.NO_INVITEE, null, RefundReason.NO_OPPONENT);
		verify(multiplayerGameManager, times(1)).cancelGame(ANONYMOUS_USER_ID, MGI_ID, EXPIRED_INVITATION_INSTANCE_ID);
		verify(activeGameInstanceDao, times(1)).delete(EXPIRED_INVITATION_INSTANCE_ID);
		//expired game play
		verify(gameInstanceAerospikeDao, never()).terminateIfNotYet(eq(EXPIRED_GAME_PLAY_INSTANCE_ID),
				any(), any(), eq(RefundReason.NO_OPPONENT));
		verify(multiplayerGameManager, never()).cancelGame(ANONYMOUS_USER_ID, MGI_ID, EXPIRED_GAME_PLAY_INSTANCE_ID);
		verify(activeGameInstanceDao, never()).delete(EXPIRED_GAME_PLAY_INSTANCE_ID);
		
		verify(activeGameInstanceDao, times(1)).delete(EXPIRED_INVITATION_INSTANCE_ID);
	}
	
	@Test
	public void testNoInvitedPeopleShowingUpUntilInvitationExpiredForFreeGame() {
		Collections.addAll(activeGameInstances, expiredInvitation);
		
		when(commonMultiplayerGameInstanceDao.getGameInstances(MGI_ID))
			.thenReturn(asList(
					new MultiplayerUserGameInstance(EXPIRED_INVITATION_INSTANCE_ID, ANONYMOUS_CLIENT_INFO), 
					new MultiplayerUserGameInstance(EXPIRED_GAME_PLAY_INSTANCE_ID, ANONYMOUS_CLIENT_INFO)
			));
		when(commonMultiplayerGameInstanceDao.getGameInstancesCount(MGI_ID)).thenReturn(1);
		
		expiredInvitation.setEntryFeeAmount(BigDecimal.ZERO);
		assertTrue(expiredInvitation.isFree());
		gameHistoryManagerImpl.cleanUpActiveGameInstances();
		verify(gameInstanceAerospikeDao, times(1)).terminateIfNotYet(EXPIRED_INVITATION_INSTANCE_ID,
				CloseUpReason.NO_INVITEE, null, null);//no refund for free game
		verify(multiplayerGameManager, times(1)).cancelGame(ANONYMOUS_USER_ID, MGI_ID, EXPIRED_INVITATION_INSTANCE_ID);
		verify(activeGameInstanceDao, times(1)).delete(EXPIRED_INVITATION_INSTANCE_ID);
	}
	
	@Test
	public void testNoPlayerCompletedTheGame() {
		Collections.addAll(activeGameInstances, expiredGamePlay);
		
		when(commonMultiplayerGameInstanceDao.getGameInstances(MGI_ID))
				.thenReturn(asList(new MultiplayerUserGameInstance(EXPIRED_GAME_PLAY_INSTANCE_ID, ANONYMOUS_CLIENT_INFO)));
		
		when(commonMultiplayerGameInstanceDao.getGameInstancesCount(MGI_ID)).thenReturn(1);
		when(commonMultiplayerGameInstanceDao.getNotYetCalculatedGameInstancesCount(MGI_ID)).thenReturn(1);
		
		//No end status and GI.register = GI.noYetCalculated 
		gameHistoryManagerImpl.cleanUpActiveGameInstances();
		verify(gameInstanceAerospikeDao, times(1)).terminateIfNotYet(EXPIRED_GAME_PLAY_INSTANCE_ID,
				CloseUpReason.NO_PLAYER_COMPLETED, null, null);//no refund
		verify(multiplayerGameManager, times(1)).cancelGame(ANONYMOUS_USER_ID, MGI_ID, EXPIRED_GAME_PLAY_INSTANCE_ID);
		verify(activeGameInstanceDao, times(1)).delete(EXPIRED_GAME_PLAY_INSTANCE_ID);
	}
	
	@Test
	public void testExpiredGamePlayInProgress(){
		Collections.addAll(activeGameInstances, expiredGamePlay);
		expiredGamePlayGameInstance.setGameState(new GameState(GameStatus.IN_PROGRESS));
		final GameInstance cancelledAndCalculating = mockedGameInstance(GameStatus.CANCELLED, GameEndStatus.CALCULATING_RESULT);
		
		when(gameInstanceAerospikeDao.getGameInstance(EXPIRED_GAME_PLAY_INSTANCE_ID))
			.thenReturn(expiredGamePlayGameInstance);
		when(gameInstanceAerospikeDao.calculateIfUnfinished(any(), any(), any(), any()))
			.thenReturn(cancelledAndCalculating);
		
		when(commonMultiplayerGameInstanceDao.getGameInstances(MGI_ID))
				.thenReturn(asList(new MultiplayerUserGameInstance(EXPIRED_GAME_PLAY_INSTANCE_ID, ANONYMOUS_CLIENT_INFO)));
		
		when(commonMultiplayerGameInstanceDao.getGameInstancesCount(MGI_ID)).thenReturn(5);
		when(commonMultiplayerGameInstanceDao.getNotYetCalculatedGameInstancesCount(MGI_ID)).thenReturn(3);
		
		//expired game play
		gameHistoryManagerImpl.cleanUpActiveGameInstances();
		verify(gameInstanceAerospikeDao, times(1)).calculateIfUnfinished(EXPIRED_GAME_PLAY_INSTANCE_ID,
				CloseUpReason.EXPIRED, null, null);//no refund
		verify(multiplayerGameManager, never()).cancelGame(ANONYMOUS_USER_ID, MGI_ID, EXPIRED_GAME_PLAY_INSTANCE_ID);
		verify(resultEngineCommunicator, times(1)).requestCalculateResults(notNull(), eq(cancelledAndCalculating), isNull());
		verify(activeGameInstanceDao, times(1)).delete(EXPIRED_GAME_PLAY_INSTANCE_ID);
	}

	@Test
	public void testExpiredGamePlayRegistered(){
		Collections.addAll(activeGameInstances, expiredGamePlay);
		expiredGamePlayGameInstance.setGameState(new GameState(GameStatus.IN_PROGRESS));
		when(gameInstanceAerospikeDao.getGameInstance(EXPIRED_GAME_PLAY_INSTANCE_ID))
			.thenReturn(expiredGamePlayGameInstance);
		
		when(commonMultiplayerGameInstanceDao.getGameInstances(MGI_ID))
				.thenReturn(asList(new MultiplayerUserGameInstance(EXPIRED_GAME_PLAY_INSTANCE_ID, ANONYMOUS_CLIENT_INFO)));
		
		when(commonMultiplayerGameInstanceDao.getGameInstancesCount(MGI_ID)).thenReturn(5);
		when(commonMultiplayerGameInstanceDao.getNotYetCalculatedGameInstancesCount(MGI_ID)).thenReturn(3);
		
		//expired game play
		gameHistoryManagerImpl.cleanUpActiveGameInstances();
		verify(gameInstanceAerospikeDao, times(1)).calculateIfUnfinished(EXPIRED_GAME_PLAY_INSTANCE_ID,
				CloseUpReason.EXPIRED, null, null);//no refund
		verify(multiplayerGameManager, times(1)).cancelGame(ANONYMOUS_USER_ID, MGI_ID, EXPIRED_GAME_PLAY_INSTANCE_ID);
		verify(resultEngineCommunicator, never()).requestCalculateResults(null, expiredGamePlayGameInstance, null);
		verify(activeGameInstanceDao, times(1)).delete(EXPIRED_GAME_PLAY_INSTANCE_ID);
	}
	
	@Test
	public void testCalculateIfUnfinishedForBackedFailure(){
		final GameInstance canceledAndTerminatedGameInstance = mockedGameInstance(GameStatus.CANCELLED, GameEndStatus.TERMINATED);
		when(gameInstanceAerospikeDao.calculateIfUnfinished(EXPIRED_GAME_PLAY_INSTANCE_ID, null, null, null))
				.thenReturn(canceledAndTerminatedGameInstance);
		gameHistoryManagerImpl.calculateIfUnfinished(EXPIRED_GAME_PLAY_INSTANCE_ID, null, null, null);
		verify(multiplayerGameManager, times(1)).cancelGame(ANONYMOUS_USER_ID, MGI_ID, EXPIRED_GAME_PLAY_INSTANCE_ID);
		verify(multiplayerGameManager, times(1)).requestCalculateMultiplayerResultsIfPossible(notNull(), eq(MGI_ID),
				eq(game), isNull());
		verify(resultEngineCommunicator, never()).requestCalculateResults(null, canceledAndTerminatedGameInstance,
				null);
		
		reset(commonMultiplayerGameInstanceDao);
		reset(resultEngineCommunicator);
		reset(multiplayerGameManager);
		
		final GameInstance canceledAndCalculatingGameInstance = mockedGameInstance(GameStatus.CANCELLED, GameEndStatus.CALCULATING_RESULT);
		when(gameInstanceAerospikeDao.calculateIfUnfinished(EXPIRED_GAME_PLAY_INSTANCE_ID, null, null, null))
			.thenReturn(canceledAndCalculatingGameInstance);
		gameHistoryManagerImpl.calculateIfUnfinished(EXPIRED_GAME_PLAY_INSTANCE_ID, null, null, null);
		verify(multiplayerGameManager, never()).cancelGame(ANONYMOUS_USER_ID, MGI_ID, EXPIRED_GAME_PLAY_INSTANCE_ID);
		verify(multiplayerGameManager, never()).requestCalculateMultiplayerResultsIfPossible(notNull(), eq(MGI_ID),
				eq(game), isNull());
		verify(resultEngineCommunicator, times(1)).requestCalculateResults(notNull(), eq(canceledAndCalculatingGameInstance),
				isNull());
	}
	
	@Test
	public void testTerminatedIfNotSet(){
		gameHistoryManagerImpl.terminatedIfNotSet(EXPIRED_GAME_PLAY_INSTANCE_ID, null, null, null);
		verify(multiplayerGameManager, times(1)).cancelGame(ANONYMOUS_USER_ID, MGI_ID, EXPIRED_GAME_PLAY_INSTANCE_ID);
		verify(multiplayerGameManager, times(1)).requestCalculateMultiplayerResultsIfPossible(notNull(), eq(MGI_ID),
				eq(game), isNull());
	}
	
}
