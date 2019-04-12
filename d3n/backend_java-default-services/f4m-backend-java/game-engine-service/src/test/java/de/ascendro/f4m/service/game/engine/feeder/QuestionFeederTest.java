package de.ascendro.f4m.service.game.engine.feeder;

import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_USER_ID;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.game.engine.config.GameEngineConfig;
import de.ascendro.f4m.service.game.engine.dao.preselected.PreselectedQuestionsDao;
import de.ascendro.f4m.service.game.engine.integration.TestDataLoader;
import de.ascendro.f4m.service.game.engine.model.Question;
import de.ascendro.f4m.service.game.engine.multiplayer.MultiplayerGameManager;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.EventServiceClient;

public class QuestionFeederTest {
	private static final String LIVE_GAME_TOPIC = Game.getLiveTournamentStartStepTopic(TestDataLoader.MGI_ID);
	private static final int answerMaxTime = 100;//0.1s

	@Mock
	private Config config;
	@Mock
	private EventServiceClient eventServiceClient;
	@Mock
	private PreselectedQuestionsDao preselectedQuestionsDao;
	@Mock
	private MultiplayerGameManager multiplayerGameManager;
	@Mock
	private LoggingUtil loggingUtil;
	
	private QuestionFeeder questionFeeder;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		when(config.getPropertyAsInteger(GameEngineConfig.QUESTION_FEEDER_THREAD_POOL_CORE_SIZE)).thenReturn(1);
		when(config.getPropertyAsInteger(GameEngineConfig.QUESTION_FEEDER_DISTRIBUTION_DELAY_IN_BETWEEN_QUESTIONS)).thenReturn(0);
		when(config.getPropertyAsInteger(GameEngineConfig.QUESTION_FEEDER_GAME_START_MIN_DELAY)).thenReturn(0);
		when(preselectedQuestionsDao.getCreatorId(TestDataLoader.MGI_ID)).thenReturn(ANONYMOUS_USER_ID);
		
		questionFeeder = new QuestionFeeder(config, eventServiceClient, preselectedQuestionsDao,
				multiplayerGameManager, loggingUtil);
	}

	@After
	public void tearDown() throws Exception {
		questionFeeder.finialize();
	}

	@Test
	public void testScheduleGameStart() {
		final ZonedDateTime playDateTime = DateTimeUtil.getCurrentDateTime();
		
		final Game game = mock(Game.class);
		when(game.getNumberOfQuestions()).thenReturn(3);
		when(game.getGameId()).thenReturn(TestDataLoader.GAME_ID);
		when(game.getStartDateTime()).thenReturn(DateTimeUtil.getCurrentDateTime().minusDays(2));//open registration one day in advance
		
		final Question[] questions = new Question[game.getNumberOfQuestions()];
		
		final AtomicInteger totalStepCount = new AtomicInteger(0);
		for(int i = 0; i < questions.length; i++){
			questions[i] = mock(Question.class);
			
			when(questions[i].getStepCount()).thenReturn(i % 2 + 1);
			totalStepCount.set(totalStepCount.get() + questions[i].getStepCount());
			when(questions[i].getAnswerMaxTime(anyInt())).thenReturn((long)answerMaxTime);
		}
		
		assertWithEnoughPlayers(playDateTime, game, questions, totalStepCount);
		assertWithNotEnoughPlayers(playDateTime, game, questions);
	}

	private void assertWithNotEnoughPlayers(final ZonedDateTime playDateTime, final Game game,
			final Question[] questions) {
		when(multiplayerGameManager.hasEnoughPlayersToPlay(TestDataLoader.MGI_ID)).thenReturn(false);

		questionFeeder.scheduleGameStart(game, ANONYMOUS_USER_ID, TestDataLoader.MGI_ID, questions, playDateTime);

		RetriedAssert.assertWithWait(() -> verify(multiplayerGameManager).cancelLiveTournament(TestDataLoader.MGI_ID));
	}

	private void assertWithEnoughPlayers(final ZonedDateTime playDateTime, final Game game,
			final Question[] questions, final AtomicInteger totalStepCount) {
		when(multiplayerGameManager.hasEnoughPlayersToPlay(TestDataLoader.MGI_ID)).thenReturn(true);
		
		questionFeeder.scheduleGameStart(game, ANONYMOUS_USER_ID, TestDataLoader.MGI_ID, questions, playDateTime);
		
		RetriedAssert.assertWithWait(() -> 
			verify(eventServiceClient, times(totalStepCount.get())).publish(LIVE_GAME_TOPIC, new JsonObject()),
			totalStepCount.get() * answerMaxTime);
	}

}
