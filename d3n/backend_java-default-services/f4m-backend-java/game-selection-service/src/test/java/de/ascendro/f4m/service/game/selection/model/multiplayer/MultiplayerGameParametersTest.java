package de.ascendro.f4m.service.game.selection.model.multiplayer;

import static de.ascendro.f4m.service.game.selection.builder.GameBuilder.createGame;
import static de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfigBuilder.create;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.Test;

import de.ascendro.f4m.service.game.selection.builder.GameBuilder;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.game.validate.NumberOfQuestionsRule;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class MultiplayerGameParametersTest {
	
	private static final Integer DUEL_PLAYER_READINESS = 30;
	private static final Integer TOURNAMENT_PLAYER_READINESS = 45;
	private static final String GAME_ID = "game_id_1";
	private static final String TOURNAMENT_GAME_ID = "game_id_2";
	private static final String TENANT_ID = "tenant_id_1";
	private static final String USER_ID = "user_id_1";
	private static final String APP_ID = "app_id_1";

	private static GameBuilder GAME_BUILDER;
	private static GameBuilder TOURNAMENT_GAME_BUILDER;
	private static CustomGameConfigBuilder CONFIG_BUILDER;

	@Before
	public void setUp() {
		GAME_BUILDER = createGame(GAME_ID, GameType.DUEL);
		TOURNAMENT_GAME_BUILDER = createGame(TOURNAMENT_GAME_ID, GameType.LIVE_TOURNAMENT);
		
		CONFIG_BUILDER = create(TENANT_ID, APP_ID, GAME_ID, USER_ID);
	}

	@Test
	public void testValidatePools() throws Exception {
		String[] pools = { "pool1", "pool2" };

		GAME_BUILDER.withAssignedPools(pools).withUserCanOverridePools(false)
			.withPlayerReadiness(TOURNAMENT_PLAYER_READINESS, DUEL_PLAYER_READINESS);
		CONFIG_BUILDER.withPools(ArrayUtils.EMPTY_STRING_ARRAY);
		assertTrue(validate());

		GAME_BUILDER.withAssignedPools(pools).withUserCanOverridePools(true);
		CONFIG_BUILDER.withPools(ArrayUtils.EMPTY_STRING_ARRAY);
		assertTrue(validate());

		GAME_BUILDER.withAssignedPools(pools).withUserCanOverridePools(true);
		CONFIG_BUILDER.withPools(pools[0]);
		assertTrue(validate());

		GAME_BUILDER.withAssignedPools(pools).withUserCanOverridePools(false);
		CONFIG_BUILDER.withPools(pools[0]);
		assertFalse(validate());

		GAME_BUILDER.withAssignedPools(pools).withUserCanOverridePools(true);
		CONFIG_BUILDER.withPools("some_other_pool");
		assertFalse(validate());
	}

	@Test
	public void testValidateNumberOfQuestions() throws Exception {
		numberOfQuestions(GAME_BUILDER, NumberOfQuestionsRule.VALID_NUMBER_OF_QUESTIONS_DEFAULT);
	}
	
	@Test
	public void testValidateNumberOfQuestionsTournament() throws Exception {
		numberOfQuestions(TOURNAMENT_GAME_BUILDER, NumberOfQuestionsRule.VALID_NUMBER_OF_QUESTIONS_TOURNAMENT);
	}
	
	private void numberOfQuestions(GameBuilder gameBuilder,List<Integer> numberOfQuestionsList) throws Exception{
		gameBuilder.withNumberOfQuestions(0)
			.withPlayerReadiness(TOURNAMENT_PLAYER_READINESS, DUEL_PLAYER_READINESS);
		
		for (Integer noq : numberOfQuestionsList) {
			gameBuilder.withNumberOfQuestions(noq);
			assertTrue(validate(gameBuilder));
		}
		
		gameBuilder.withNumberOfQuestions(numberOfQuestionsList.get(0));
		CONFIG_BUILDER.withNumberOfQuestions(numberOfQuestionsList.get(0));
		assertTrue(validate(gameBuilder));
		
		gameBuilder.withNumberOfQuestions(numberOfQuestionsList.get(0));
		CONFIG_BUILDER.withNumberOfQuestions(0);
		assertTrue(validate(gameBuilder));

		gameBuilder.withNumberOfQuestions(numberOfQuestionsList.get(0));
		CONFIG_BUILDER.withNumberOfQuestions(13);
		assertFalse(validate(gameBuilder));

		gameBuilder.withNumberOfQuestions(0);
		CONFIG_BUILDER.withNumberOfQuestions(13);
		assertFalse(validate(gameBuilder));

		gameBuilder.withNumberOfQuestions(0);
		CONFIG_BUILDER.withNumberOfQuestions(0);
		assertFalse(validate(gameBuilder));
		
		gameBuilder.withNumberOfQuestions(null);
		CONFIG_BUILDER.withNumberOfQuestions(3);
		assertFalse(validate(gameBuilder));
		
	}
	
	@Test
	public void testValidateMaxNumberOfParticipants() throws Exception {
		GAME_BUILDER.withType(GameType.USER_TOURNAMENT)
		.withPlayerReadiness(TOURNAMENT_PLAYER_READINESS, DUEL_PLAYER_READINESS);
		CONFIG_BUILDER.withMaxNumberOfParticipants(13);
		assertTrue(validate());
		
		GAME_BUILDER.withType(GameType.DUEL);
		CONFIG_BUILDER.withMaxNumberOfParticipants(Game.DUEL_PARTICIPANT_COUNT);
		assertTrue(validate());
		
		GAME_BUILDER.withType(GameType.DUEL);
		CONFIG_BUILDER.withMaxNumberOfParticipants(Game.DUEL_PARTICIPANT_COUNT + 1);
		assertFalse(validate());
	}
	
	@Test
	public void testValidateEntryFee() throws Exception {
		GAME_BUILDER.withFree(true)
			.withPlayerReadiness(TOURNAMENT_PLAYER_READINESS, DUEL_PLAYER_READINESS);
		CONFIG_BUILDER.withEntryFee(null, null);
		assertTrue(validate());
		
		GAME_BUILDER.withFree(false).withEntryFeeDecidedByPlayer(false);
		CONFIG_BUILDER.withEntryFee(null, null);
		assertTrue(validate());
		
		GAME_BUILDER.withFree(false).withEntryFeeDecidedByPlayer(true);
		CONFIG_BUILDER.withEntryFee(null, null);
		assertTrue(validate());
		
		GAME_BUILDER.withFree(false).withEntryFeeDecidedByPlayer(true).withEntryFee("0.50", "0.50", "1.55", "0.25", Currency.CREDIT);
		CONFIG_BUILDER.withEntryFee(new BigDecimal("1.55"), Currency.CREDIT);
		assertTrue(validate());
		
		GAME_BUILDER.withFree(false).withEntryFeeDecidedByPlayer(true).withEntryFee("0.50", "0.50", "1.50", "0.25", Currency.CREDIT);
		CONFIG_BUILDER.withEntryFee(new BigDecimal("1.00"), Currency.CREDIT);
		assertTrue(validate());
		
		GAME_BUILDER.withFree(false).withEntryFeeDecidedByPlayer(true).withEntryFee("0.50", "0.50", "1.50", "0.25", Currency.BONUS);
		CONFIG_BUILDER.withEntryFee(new BigDecimal("1.00"), Currency.CREDIT);
		assertTrue(validate());
		
		GAME_BUILDER.withFree(true).withEntryFeeDecidedByPlayer(true);
		CONFIG_BUILDER.withEntryFee(new BigDecimal("1.00"), Currency.CREDIT);
		assertTrue(validate());
		
		GAME_BUILDER.withFree(true).withEntryFeeDecidedByPlayer(false);
		CONFIG_BUILDER.withEntryFee(new BigDecimal("1.00"), Currency.CREDIT);
		assertFalse(validate());
		
		GAME_BUILDER.withFree(false).withEntryFeeDecidedByPlayer(true);
		CONFIG_BUILDER.withEntryFee(null, Currency.CREDIT);
		assertFalse(validate());
		
		GAME_BUILDER.withFree(false).withEntryFeeDecidedByPlayer(true);
		CONFIG_BUILDER.withEntryFee(new BigDecimal("1.00"), null);
		assertFalse(validate());
	}
	
	@Test
	public void testValidateDates() throws Exception {
		ZonedDateTime nowDate = DateTimeUtil.getCurrentDateTime().withNano(0);
		ZonedDateTime yesterdayDate = nowDate.minusDays(1);
		ZonedDateTime tomorrowDate = nowDate.plusDays(1);
		
		GAME_BUILDER.withType(GameType.USER_TOURNAMENT).withStartDateTime(yesterdayDate).withEndDateTime(tomorrowDate)
			.withPlayerReadiness(TOURNAMENT_PLAYER_READINESS, DUEL_PLAYER_READINESS);
		
		CONFIG_BUILDER.withStartDateTime(nowDate).withEndDateTime(tomorrowDate);
		assertTrue(validate());
		
		CONFIG_BUILDER.withStartDateTime(yesterdayDate).withEndDateTime(nowDate);
		assertTrue(validate());
		
		CONFIG_BUILDER.withStartDateTime(nowDate).withEndDateTime(nowDate);
		assertFalse(validate());
		
		CONFIG_BUILDER.withStartDateTime(tomorrowDate).withEndDateTime(nowDate);
		assertFalse(validate());
		
		GAME_BUILDER.withType(GameType.USER_TOURNAMENT).withStartDateTime(yesterdayDate).withEndDateTime(nowDate);
		CONFIG_BUILDER.withStartDateTime(nowDate).withEndDateTime(tomorrowDate);
		assertFalse(validate());
		
		GAME_BUILDER.withType(GameType.USER_TOURNAMENT).withStartDateTime(nowDate).withEndDateTime(tomorrowDate);
		CONFIG_BUILDER.withStartDateTime(yesterdayDate).withEndDateTime(tomorrowDate);
		assertFalse(validate());
	}

	private boolean validate() throws Exception {
		return validate(GAME_BUILDER);
	}
	
	
	private boolean validate(GameBuilder gameBuilder) throws Exception {
		MultiplayerGameParameters gameParams = CONFIG_BUILDER.buildMultiplayerGameParameters();
		Game game = gameBuilder.build();
		try {
			gameParams.validate(game);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

}
