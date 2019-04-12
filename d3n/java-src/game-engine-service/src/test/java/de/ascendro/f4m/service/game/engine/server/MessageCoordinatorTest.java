package de.ascendro.f4m.service.game.engine.server;

import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.MGI_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.service.game.engine.client.results.ResultEngineCommunicator;
import de.ascendro.f4m.service.game.engine.client.voucher.VoucherCommunicator;
import de.ascendro.f4m.service.game.engine.dao.instance.GameInstanceAerospikeDao;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.engine.model.cancel.CancelGameRequest;
import de.ascendro.f4m.service.game.engine.model.end.GameEndStatus;
import de.ascendro.f4m.service.game.engine.multiplayer.MultiplayerGameManager;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.game.ResultConfiguration;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.session.SessionWrapper;

public class MessageCoordinatorTest {
	private static final String SPECIAL_PRIZE_VOUCHER_ID = "specialPrize-voucherId-1";
	private static final String GAME_INSTANCE_ID = "gameInstanceId-1";
	
	@Mock
	private GameInstance gameInstance;
	@Mock 
	private Game game;
	@Mock
	private ClientInfo clientInfo;
	@Mock 
	private SessionWrapper userSession;
	@Mock
	private ResultConfiguration resultConfiguration;
	
	@Mock
	private ResultEngineCommunicator resultEngineCommunicator;
	@Mock
	private GameInstanceAerospikeDao gameInstanceAerospikeDao; 
	@Mock 
	private GameEngine gameEngine;
	@Mock
	private VoucherCommunicator voucherCommunicator;
	@Mock
	private MultiplayerGameManager multiplayerGameManager;
	
	@InjectMocks
	private MessageCoordinator messageCoordinator;
	
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		when(resultConfiguration.isSpecialPrizeEnabled()).thenReturn(true);
		when(resultConfiguration.getSpecialPrizeVoucherId()).thenReturn(SPECIAL_PRIZE_VOUCHER_ID);
		
		when(game.getResultConfiguration()).thenReturn(resultConfiguration);		
		when(gameInstance.getGame()).thenReturn(game);
		when(gameInstanceAerospikeDao.getGameInstance(GAME_INSTANCE_ID)).thenReturn(gameInstance);
	}

	@Test
	public void testCancelGameForGameInProgress() {
		when(gameEngine.cancelGameByClient(clientInfo, GAME_INSTANCE_ID))
				.thenReturn(GameEndStatus.CALCULATING_RESULT);

		final JsonMessage<CancelGameRequest> cancelGameRequestMessage = getCancelGameRequestMessage(clientInfo);
		
		messageCoordinator.cancelGame(cancelGameRequestMessage, userSession);
		verify(gameEngine, times(1)).cancelGameByClient(clientInfo, GAME_INSTANCE_ID);
		verify(resultEngineCommunicator, times(1)).requestCalculateResults(clientInfo, gameInstance, userSession);
		verify(voucherCommunicator, times(1)).requestUserVoucherRelease(SPECIAL_PRIZE_VOUCHER_ID,
				cancelGameRequestMessage, userSession);
		verify(multiplayerGameManager, never()).requestCalculateMultiplayerResultsIfPossible(any(), any(), any(), any());
	}
	
	@Test
	public void testCancelGameForRegisteredGame() {
		when(gameEngine.cancelGameByClient(clientInfo, GAME_INSTANCE_ID))
			.thenReturn(null);
		
		//Single player
		messageCoordinator.cancelGame(getCancelGameRequestMessage(clientInfo), userSession);
		verify(resultEngineCommunicator, never()).requestCalculateResults(clientInfo, gameInstance, userSession);
		verify(multiplayerGameManager, never()).requestCalculateMultiplayerResultsIfPossible(any(), any(), any(), any());
		
		reset(resultEngineCommunicator);
		
		//Multiplayer
		when(game.getType()).thenReturn(GameType.DUEL);		
		when(gameInstance.getMgiId()).thenReturn(MGI_ID);
		messageCoordinator.cancelGame(getCancelGameRequestMessage(clientInfo), userSession);
		verify(multiplayerGameManager, times(1)).requestCalculateMultiplayerResultsIfPossible(clientInfo, MGI_ID, game, userSession);
	}

	private JsonMessage<CancelGameRequest> getCancelGameRequestMessage(final ClientInfo clientInfo) {
		final JsonMessage<CancelGameRequest> cancelGameRequestMessage = new JsonMessage<>();
		cancelGameRequestMessage.setClientInfo(clientInfo);
		final CancelGameRequest cancelGameRequest = new CancelGameRequest();
		cancelGameRequest.setGameInstanceId(GAME_INSTANCE_ID);
		cancelGameRequestMessage.setContent(cancelGameRequest);
		return cancelGameRequestMessage;
	}

}
