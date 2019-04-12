package de.ascendro.f4m.service.game.selection.server;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import de.ascendro.f4m.server.game.GameResponseSanitizer;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.game.selection.dao.GameSelectorAerospikeDao;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameFilter;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.game.GameTypeConfiguration;
import de.ascendro.f4m.service.game.selection.model.game.GetGameListRequest;
import de.ascendro.f4m.service.game.selection.model.game.GetGameListResponse;
import de.ascendro.f4m.service.game.selection.model.game.SingleplayerGameTypeConfigurationData;
import de.ascendro.f4m.service.game.usergameaccess.UserGameAccessService;
import de.ascendro.f4m.service.json.model.ISOCountry;

public class GameSelectorTest {
	
	@Spy
	JsonUtil jsonUtil = new JsonUtil();
	@Mock
	GameSelectorAerospikeDao gameSelectorAerospikeDao;
	@Mock
	GameResponseSanitizer gameResponseSanitizer;
	@Mock
	private UserGameAccessService userGameAccessService; 
	
	@InjectMocks
	private GameSelector gameSelector;
	private JsonArray gameList;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		gameList = new JsonArray();
		gameList.add(createGameWithWeight("a", 1d));
		gameList.add(createGameWithWeight("b", 1000d));
		gameList.add(createOtherGameWithoutConfig("e"));
		gameList.add(createGameWithWeight("d", null));
		gameList.add(createGameWithWeight("c", 0.9));
	}

	@Test
	public void getSpecialGameListSorted() throws Exception {
		when(gameSelectorAerospikeDao.getGameList("tenantId", "appId")).thenReturn(gameList);
		when(userGameAccessService.canPlay(Mockito.any(), Mockito.any())).thenReturn(true);
		
		GetGameListRequest getGameListRequest = new GetGameListRequest();
		getGameListRequest.setIsSpecialGame(true);
		JsonElement filterElement = jsonUtil.toJsonElement(getGameListRequest);
		GameFilter gameFilter = new GameFilter("tenantId", "appId", ISOCountry.TV.toString(), filterElement);
		GetGameListResponse response = gameSelector.getGameList("userId", gameFilter);
		assertThat(getIds(response.getGames()), contains("b", "a", "c", "d"));
	}
	
	
	/**
	 * Tests if games are filtered with all all games played and non special games are not affected
	 * */
	@Test
	public void getSpecialGameListWithNoPlays() throws Exception {
		
		JsonArray gameListWithNoPlaysLeftGame = new JsonArray();
		
		Game game = new Game();
		game.setGameId("ff");
		game.setMultiplePurchaseAllowed(false);
		game.setEntryFeeBatchSize(2);
		gameListWithNoPlaysLeftGame.add(jsonUtil.toJsonElement(game));
		
		// This is out of this task, but such thing need to be checked
		Game game2 = new Game();
		game2.setGameId("gg");
		game2.setMultiplePurchaseAllowed(true);
		game2.setEntryFeeBatchSize(2);
		gameListWithNoPlaysLeftGame.add(jsonUtil.toJsonElement(game2));
	
		gameListWithNoPlaysLeftGame.add(createOtherGameWithoutConfig("mm"));
		
		when(gameSelectorAerospikeDao.getGameList("tenantId", "appId")).thenReturn(gameListWithNoPlaysLeftGame);
		when(userGameAccessService.isVisible(argThat(hasProperty("gameId", equalTo("ff"))), Mockito.any()))
				.thenReturn(true);
		
		GetGameListRequest getGameListRequest = new GetGameListRequest();
		JsonElement filterElement = jsonUtil.toJsonElement(getGameListRequest);
		GameFilter gameFilter = new GameFilter("tenantId", "appId", ISOCountry.TV.toString(), filterElement);
		GetGameListResponse response = gameSelector.getGameList("userId", gameFilter);
		assertEquals(2, response.getGames().size());
		assertThat(getIds(response.getGames()), contains("ff", "mm"));
	}	
	
	@Test
	public void testSorting() throws Exception {
		JsonArray sorted = gameSelector.sortBySpecialGameWeight(gameList);
		assertThat(getIds(sorted), contains("b", "a", "c", "e", "d"));
	}
	
	private List<String> getIds(JsonArray gameList) {
		List<String> ids = new ArrayList<>(gameList.size());
		for (int i = 0; i < gameList.size(); i++) {
			ids.add(gameList.get(i).getAsJsonObject().get("gameId").getAsString());
		}
		return ids;
	}

	private JsonElement createGameWithWeight(String gameId, Double weight) {
		Game game = new Game();
		game.setType(GameType.QUIZ24);
		game.setGameId(gameId);
		SingleplayerGameTypeConfigurationData gameQuiz24Config = new SingleplayerGameTypeConfigurationData();
		gameQuiz24Config.setWeight(weight);
		gameQuiz24Config.setSpecial(true);
		GameTypeConfiguration typeConfiguration = new GameTypeConfiguration();
		typeConfiguration.setGameQuickQuiz(gameQuiz24Config);
		game.setTypeConfiguration(typeConfiguration);
		return jsonUtil.toJsonElement(game);
	}
	
	private JsonElement createOtherGameWithoutConfig(String gameId) {
		Game game = new Game();
		game.setGameId(gameId);
		return jsonUtil.toJsonElement(game);
	}
}
