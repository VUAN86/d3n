package de.ascendro.f4m.service.game.selection.model.game.validate;

import java.time.ZonedDateTime;

import org.junit.Before;
import org.junit.Test;

import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.game.GameTypeConfiguration;
import de.ascendro.f4m.service.game.selection.model.game.MultiplayerGameTypeConfigurationData;
import de.ascendro.f4m.service.game.selection.model.multiplayer.MultiplayerGameParameters;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class MultiplayerDatesRuleTest {

	Game game;
	MultiplayerDatesRule rule = new MultiplayerDatesRule();
	MultiplayerGameParameters params;

	@Before
	public void setUp() {
		game = new Game();
		game.setGameId("game_ulv");
		GameTypeConfiguration gtc = new GameTypeConfiguration();
		MultiplayerGameTypeConfigurationData mt = new MultiplayerGameTypeConfigurationData();
		mt.setMinimumPlayerNeeded(2);
		gtc.setGameTournament(mt);
		game.setTypeConfiguration(gtc);
		game.setType(GameType.USER_LIVE_TOURNAMENT);

		ZonedDateTime playDateTime = DateTimeUtil.getCurrentDateTime().plusHours(9);
		params = buildMultiplayerGameParameters("game_ulv", playDateTime);
	}

	@Test
	public void testSuccessfull() {
		rule.validate(params, game);
	}

	/**
	 * Test for failed validation if game is set before 2h from now
	 * */
	
	//FIXME: removed, as right now this validation is removed
	
//	@Test(expected = F4MCustomDatesNotValidException.class)
//	public void testTooFast() {
//		ZonedDateTime playDateTime = DateTimeUtil.getCurrentDateTime();
//		params.setPlayDateTime(playDateTime);
//		rule.validate(params, game);
//	}
//
//	/**
//	 * Test for failed validation if game is set after 7 days from now
//	 * */
//	@Test(expected = F4MCustomDatesNotValidException.class)
//	public void testAfterAWeek() {
//		ZonedDateTime playDateTime = DateTimeUtil.getCurrentDateTime().plusDays(10);
//		params.setPlayDateTime(playDateTime);
//		rule.validate(params, game);
//	}

	private MultiplayerGameParameters buildMultiplayerGameParameters(String gameId, ZonedDateTime playDateTime) {
		MultiplayerGameParameters params = new MultiplayerGameParameters(gameId);
		params.setGameId(gameId);
		params.setPlayDateTime(playDateTime);
		return params;
	}
}
