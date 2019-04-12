package de.ascendro.f4m.service.game.gameaccess.dao;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.ascendro.f4m.server.config.GameConfigImpl;
import de.ascendro.f4m.server.game.util.GameEnginePrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.dao.aerospike.RealAerospikeTestBase;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.game.GameTypeConfiguration;
import de.ascendro.f4m.service.game.selection.model.game.SingleplayerGameTypeConfigurationData;
import de.ascendro.f4m.service.game.usergameaccess.UserGameAccessDaoImpl;

public class UserGameAccessDaoImplTest extends RealAerospikeTestBase{

	private static final String USER_DUMMY_ID = "3345";
	private final GameConfigImpl gameConfig = new GameConfigImpl();
	private final GameEnginePrimaryKeyUtil gameEngineKeyUtil = new GameEnginePrimaryKeyUtil(gameConfig);
	private final JsonUtil jsonUtil = new JsonUtil();
	
	private UserGameAccessDaoImpl userGameAccessDaoImpl;
	private Game game;
	
	@Override
	protected void setUpAerospike() {
		userGameAccessDaoImpl = new UserGameAccessDaoImpl(new F4MConfigImpl(config, gameConfig), gameEngineKeyUtil,
				aerospikeClientProvider,
				jsonUtil);
	}

	@Override
	public void setUp() {
		super.setUp();
		
		game = new Game();
		game.setType(GameType.QUIZ24);
		game.setGameId("-1");
		game.setEntryFeeBatchSize(3);
		game.setMultiplePurchaseAllowed(false);
		SingleplayerGameTypeConfigurationData spc = new SingleplayerGameTypeConfigurationData();
		spc.setSpecial(true);
		
		GameTypeConfiguration typeConfiguration = new GameTypeConfiguration(null, null, spc);
		game.setTypeConfiguration(typeConfiguration);		
		config.setProperty(GameConfigImpl.AEROSPIKE_USER_GAME_ACCESS_SET, "userGameAccess");
	}
	
	@Test
	public void testCreateUserGameAccessPlay() {
		userGameAccessDaoImpl.create(game, USER_DUMMY_ID);
		Long specialGame = userGameAccessDaoImpl.getUserGameCountLeft(game, USER_DUMMY_ID);
		assertEquals(3, specialGame.intValue());
		
		userGameAccessDaoImpl.decrementGamePlayedCount(game, USER_DUMMY_ID);
		Long specialGame2 = userGameAccessDaoImpl.getUserGameCountLeft(game, USER_DUMMY_ID);
		assertEquals(2, specialGame2.intValue());
		
		userGameAccessDaoImpl.decrementGamePlayedCount(game, USER_DUMMY_ID);
		Long specialGame3 = userGameAccessDaoImpl.getUserGameCountLeft(game, USER_DUMMY_ID);
		assertEquals(1, specialGame3.intValue());
		
		userGameAccessDaoImpl.decrementGamePlayedCount(game, USER_DUMMY_ID);
		Long specialGame4 = userGameAccessDaoImpl.getUserGameCountLeft(game, USER_DUMMY_ID);
		assertEquals(0, specialGame4.intValue());
	}	

	/**
	 * Tests what happens if you try to access decrement gameLeft count too much
	 * */
	@Test(expected = F4MFatalErrorException.class)
	public void testCreateUserGameAccessPlayTooMuch() {
		userGameAccessDaoImpl.create(game, USER_DUMMY_ID);
		Long specialGame = userGameAccessDaoImpl.getUserGameCountLeft(game, USER_DUMMY_ID);
		assertEquals(3, specialGame.intValue());
		
		userGameAccessDaoImpl.decrementGamePlayedCount(game, USER_DUMMY_ID);
		Long specialGame2 = userGameAccessDaoImpl.getUserGameCountLeft(game, USER_DUMMY_ID);
		assertEquals(2, specialGame2.intValue());
		
		userGameAccessDaoImpl.decrementGamePlayedCount(game, USER_DUMMY_ID);
		Long specialGame3 = userGameAccessDaoImpl.getUserGameCountLeft(game, USER_DUMMY_ID);
		assertEquals(1, specialGame3.intValue());
		
		userGameAccessDaoImpl.decrementGamePlayedCount(game, USER_DUMMY_ID);
		Long specialGame4 = userGameAccessDaoImpl.getUserGameCountLeft(game, USER_DUMMY_ID);
		assertEquals(0, specialGame4.intValue());
		
		userGameAccessDaoImpl.decrementGamePlayedCount(game, USER_DUMMY_ID);
		Long specialGame5 = userGameAccessDaoImpl.getUserGameCountLeft(game, USER_DUMMY_ID);
		assertEquals(0, specialGame5.intValue());
		
	}	

	
}
