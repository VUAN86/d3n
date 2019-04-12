package de.ascendro.f4m.service.game.selection.server;

import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_USER_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.REGISTERED_USER_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.TENANT_ID;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.server.dashboard.dao.DashboardDao;
import de.ascendro.f4m.server.dashboard.move.dao.MoveDashboardDao;
import de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.service.game.selection.client.communicator.ResultEngineCommunicator;
import de.ascendro.f4m.service.game.selection.model.dashboard.PlayedGameInfo;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.game.selection.model.multiplayer.MultiplayerUserGameInstance;
import de.ascendro.f4m.service.game.usergameaccess.UserGameAccessService;
import de.ascendro.f4m.service.json.model.ISOCountry;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import javafx.util.Pair;

public class DashboardManagerTest {

	private static final String MGI_ID = "mgi_id_1";
	private static final String GAME_INSTANCE_1 = "gi_id_1";
	private static final String GAME_INSTANCE_2 = "gi_id_2";
	private static final String GAME_INSTANCE_3 = "gi_id_3";
	private static final String GAME_ID_1 = "game_id_1";
	private static final String USER_ID_1 = "user_id_1";
	private ClientInfo clientInfo;

	@Mock
	private DashboardDao dashboardDao;
	@Mock
	private UserGameAccessService userGameAccessService;
	@Mock
	private GameSelector gameSelector;
	@Mock
	private MultiplayerGameInstanceManager mgiManager;
	@Mock
	private CommonProfileAerospikeDao profileDao;
	@Mock
	private MoveDashboardDao moveDashboardDao;
	@Mock
	private ResultEngineCommunicator resultEngineCommunicator;

	private DashboardManager dashboardManager;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		dashboardManager = new DashboardManager(dashboardDao, gameSelector, mgiManager, profileDao, moveDashboardDao, resultEngineCommunicator, userGameAccessService);
	}

	private CustomGameConfig getMockGameConfig() {
		CustomGameConfig mockConfig = new CustomGameConfig(GAME_ID_1);
		mockConfig.setGameCreatorId(ANONYMOUS_USER_ID);
		return mockConfig;
	}

	@Test
	public void testUpdateFinishedDuel() {
		Pair<String, String> gi1AndAnonymousUser = new Pair<String, String>(GAME_INSTANCE_1, ANONYMOUS_USER_ID);
		Pair<String, String> gi2AndRegisteredUser = new Pair<String, String>(GAME_INSTANCE_2, REGISTERED_USER_ID);

		clientInfo = new ClientInfo();
		clientInfo.setCountryCode(ISOCountry.DE);
		
		when(mgiManager.getMultiplayerGameConfig(MGI_ID)).thenReturn(getMockGameConfig());
		when(gameSelector.getGame(GAME_ID_1)).thenReturn(getMockedGame(GAME_ID_1, GameType.DUEL));
		when(mgiManager.getMultiplayerUserGameInstances(MGI_ID, MultiplayerGameInstanceState.CALCULATED))
				.thenReturn(getMockedUserGameInstances(Arrays.asList(gi1AndAnonymousUser, gi2AndRegisteredUser)));

		dashboardManager.updateFinishedMultiplayerGame(TENANT_ID, MGI_ID);

		assertLastDuel(GAME_ID_1, ANONYMOUS_USER_ID, REGISTERED_USER_ID);
		// For the second user this is an invalid last duel, since 1st user is the creator
		ArgumentCaptor<PlayedGameInfo> gameInfo = ArgumentCaptor.forClass(PlayedGameInfo.class);
		verify(dashboardDao, times(0)).updateLastPlayedGame(eq(TENANT_ID), eq(REGISTERED_USER_ID), gameInfo.capture());
	}
	
	private void assertLastDuel(String gameId, String userId, String expectedOpponentId) {
		PlayedGameInfo gameInfo = verifyAndGetLastPlayedGame(userId);
		
		assertThat(gameInfo.getGameId(), equalTo(gameId));
		assertThat(gameInfo.getType(), equalTo(GameType.DUEL));
		assertThat(gameInfo.getDuelInfo().getOpponentUserId(), equalTo(expectedOpponentId));
	}
	
	@Test
	public void testUpdateFinishedTournament() {
		Pair<String, String> gi1AndAnonymousUser = new Pair<String, String>(GAME_INSTANCE_1, ANONYMOUS_USER_ID);
		Pair<String, String> gi2AndRegisteredUser = new Pair<String, String>(GAME_INSTANCE_2, REGISTERED_USER_ID);
		Pair<String, String> gi3AndUser1 = new Pair<String, String>(GAME_INSTANCE_3, USER_ID_1);

		when(mgiManager.getMultiplayerGameConfig(MGI_ID)).thenReturn(new CustomGameConfig(GAME_ID_1));
		when(gameSelector.getGame(GAME_ID_1)).thenReturn(getMockedGame(GAME_ID_1, GameType.USER_TOURNAMENT));
		when(mgiManager.getMultiplayerUserGameInstances(MGI_ID, MultiplayerGameInstanceState.CALCULATED))
				.thenReturn(getMockedUserGameInstances(Arrays.asList(gi1AndAnonymousUser, gi2AndRegisteredUser, gi3AndUser1)));

		dashboardManager.updateFinishedMultiplayerGame(TENANT_ID, MGI_ID);

		assertLastUserTournament(ANONYMOUS_USER_ID, GAME_ID_1, MGI_ID);
		assertLastUserTournament(REGISTERED_USER_ID, GAME_ID_1, MGI_ID);
		assertLastUserTournament(USER_ID_1, GAME_ID_1, MGI_ID);
	}
	
	private void assertLastUserTournament(String userId, String gameId, String mgiId) {
		PlayedGameInfo gameInfo = verifyAndGetLastPlayedGame(userId);
		
		assertThat(gameInfo.getGameId(), equalTo(gameId));
		assertThat(gameInfo.getType(), equalTo(GameType.USER_TOURNAMENT));
		assertThat(gameInfo.getTournamentInfo().getMgiId(), equalTo(mgiId));
	}

	private PlayedGameInfo verifyAndGetLastPlayedGame(String userId) {
		ArgumentCaptor<PlayedGameInfo> gameInfo = ArgumentCaptor.forClass(PlayedGameInfo.class);
		verify(dashboardDao).updateLastPlayedGame(eq(TENANT_ID), eq(userId), gameInfo.capture());
		return gameInfo.getValue();
	}

	private List<MultiplayerUserGameInstance> getMockedUserGameInstances(List<Pair<String, String>> gameInstancesAndUsers) {
		return gameInstancesAndUsers.stream()
				.map(pair -> {
					String gameInstanceId = pair.getKey();
					ClientInfo clientInfo = new ClientInfo("t1", "a1", pair.getValue(), "ip", null);
					return new MultiplayerUserGameInstance(gameInstanceId, clientInfo);
				})
				.collect(Collectors.toList());
	}
	
	private Game getMockedGame(String gameId, GameType type) {
		Game game = new Game();
		game.setGameId(gameId);
		game.setType(type);
		return game;
	}
}
