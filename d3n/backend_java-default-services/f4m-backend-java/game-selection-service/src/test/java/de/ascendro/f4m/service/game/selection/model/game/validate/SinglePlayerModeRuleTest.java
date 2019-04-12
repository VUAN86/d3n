package de.ascendro.f4m.service.game.selection.model.game.validate;

import static org.junit.Assert.fail;

import org.junit.Test;

import de.ascendro.f4m.service.game.selection.builder.GameBuilder;
import de.ascendro.f4m.service.game.selection.exception.F4MInvalidGameModeException;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.singleplayer.SinglePlayerGameParameters;

public class SinglePlayerModeRuleTest {
	private static final Integer DUEL_PLAYER_READINESS = 30;
	private static final Integer TOURNAMENT_PLAYER_READINESS = 45;

	private static final String GAME_ID = "game_id_1";

	private SinglePlayerModeRule rule = new SinglePlayerModeRule();

	@Test
	public void testIsValidGameMode() throws Exception {
		SinglePlayerGameParameters params = new SinglePlayerGameParameters(true);
		GameBuilder gameBuilder = GameBuilder.createGame(GAME_ID, GameType.QUIZ24)
				.withPlayerReadiness(TOURNAMENT_PLAYER_READINESS, DUEL_PLAYER_READINESS);

		// free game
		gameBuilder.withFree(true);
		try {
			rule.validate(params, gameBuilder.build());
		} catch (Exception e) {
			fail(String.format("Unexpected exception [%s]", e.getMessage()));
		}

		// paid game
		gameBuilder.withFree(false);
		try {
			rule.validate(params, gameBuilder.build());
			fail("F4MInvalidGameModeException must be thrown");
		} catch (F4MInvalidGameModeException e) {
		}
	}
}
