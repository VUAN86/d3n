package de.ascendro.f4m.service.game.engine.feeder;

import static de.ascendro.f4m.service.game.engine.config.GameEngineConfig.QUESTION_FEEDER_DISTRIBUTION_DELAY_IN_BETWEEN_QUESTIONS;
import static de.ascendro.f4m.service.game.engine.config.GameEngineConfig.QUESTION_FEEDER_THREAD_POOL_CORE_SIZE;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.game.engine.config.GameEngineConfig;
import de.ascendro.f4m.service.game.engine.dao.preselected.PreselectedQuestionsDao;
import de.ascendro.f4m.service.game.engine.model.Question;
import de.ascendro.f4m.service.game.engine.model.QuestionStep;
import de.ascendro.f4m.service.game.engine.multiplayer.MultiplayerGameManager;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.EventServiceClient;

public class QuestionFeeder {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(QuestionFeeder.class);
	
	private final ScheduledExecutorService scheduledExecutorService;
	
	private final Config config;
	private final EventServiceClient eventServiceClient;
	private final PreselectedQuestionsDao preselectedQuestionsDao;
	private final MultiplayerGameManager multiplayerGameManager;
	private final LoggingUtil loggingUtil;
	
	@Inject
	public QuestionFeeder(Config config, EventServiceClient eventServiceClient,
			PreselectedQuestionsDao preselectedQuestionsDao, MultiplayerGameManager multiplayerGameManager, LoggingUtil loggingUtil) {
		this.config = config;
		this.eventServiceClient = eventServiceClient;
		this.preselectedQuestionsDao = preselectedQuestionsDao;
		this.multiplayerGameManager = multiplayerGameManager;
		this.loggingUtil = loggingUtil;
		
		scheduledExecutorService = createScheduledExecutorService();
	}
	
	@PreDestroy
	public void finialize() {
		if (scheduledExecutorService != null) {
			try {
				final List<Runnable> scheduledRunnabled = scheduledExecutorService.shutdownNow();
				LOGGER.info("Stopping QuestionFeeder with {} scheduled tasks", scheduledRunnabled != null
						? scheduledRunnabled.size() : 0);
			} catch (Exception e) {
				LOGGER.error("Failed to shutdown QuestionFeeder scheduler", e);
			}
		}
	}
	
	/**
	 * Schedule Live tournament question feeder if starter's user id matches preselected question record creator id.
	 * @param game - Live tournament game config to be scheduled
	 * @param userId - starter's id as user id
	 * @param mgiId - multiplayer game instance id
	 * @param questions - questions for particular game (length of questions array must be same as game.numberOfQuestions)
	 * @param playDateTime - live tournament play date-time start
	 */
	public void scheduleGameStart(Game game, String userId, String mgiId, Question[] questions, ZonedDateTime playDateTime){
		if(checkIfAlreadyScheduled(mgiId, userId)){
			Validate.notNull(playDateTime, "Game play date-time is mandatory");
			Validate.isTrue(questions.length == game.getNumberOfQuestions(),
					String.format("Unexpected question step disribution details with length %d for game with %d",
							questions.length, game.getNumberOfQuestions()));
			
			final QuestionStep initialQuestionStep = new QuestionStep(0, 0, questions[0].getStepCount());
			final FirstStepEventPublisher publishTask = new FirstStepEventPublisher(this, eventServiceClient,
					mgiId, initialQuestionStep, questions, loggingUtil, multiplayerGameManager);
			
			final long delayUntilPlayDateTime = DateTimeUtil.getSecondsBetween(DateTimeUtil.getCurrentDateTime(), playDateTime);
			final long minGameStartDelay = config.getPropertyAsLong(GameEngineConfig.QUESTION_FEEDER_GAME_START_MIN_DELAY);
			final long actualDelay = Math.max(delayUntilPlayDateTime * 1000, minGameStartDelay);
			LOGGER.debug("Scheduling live tournament first question in {}ms: gameId [{}], startDateTime [{}], playDateTime[{}], endDateTime [{}]",
					actualDelay, game.getGameId(), game.getStartDateTime(), playDateTime, game.getEndDateTime());
			scheduledExecutorService.schedule(publishTask, actualDelay, TimeUnit.MILLISECONDS);
		}
	}

	public void scheduleGameEnd(String gameId, Question[] questions) {
		final GameEndEventPublisher publishTask = new GameEndEventPublisher(eventServiceClient, gameId, loggingUtil);
		final long delayBeforeStartStep = config.getPropertyAsLong(QUESTION_FEEDER_DISTRIBUTION_DELAY_IN_BETWEEN_QUESTIONS);
		
		LOGGER.debug("Scheduling live tournament end game in {}ms: gameId [{}]", delayBeforeStartStep, gameId);
		scheduledExecutorService.schedule(publishTask, delayBeforeStartStep, TimeUnit.MILLISECONDS);		
	}
	
	/**
	 * Schedule Live Tournament next step
	 * @param gameId
	 * @param currentQuestionStep
	 * @param questions
	 */
	protected void scheduleNextAction(String mgiId, QuestionStep currentQuestionStep, Question[] questions){
		final NextStepEventPublisher publishTask = new NextStepEventPublisher(this, eventServiceClient, mgiId,
				currentQuestionStep, questions, loggingUtil);
		final long answerMaxTime = questions[currentQuestionStep.getQuestion()].getAnswerMaxTime(currentQuestionStep.getStep());
		final long delayBeforeStartStep = config.getPropertyAsLong(QUESTION_FEEDER_DISTRIBUTION_DELAY_IN_BETWEEN_QUESTIONS);
		
		final long nextQuestionDelay = answerMaxTime + delayBeforeStartStep;
		LOGGER.debug("Scheduling live tournament next question in {}ms: mgiId [{}], currentQuestionStep [{}]",
				nextQuestionDelay, mgiId, currentQuestionStep);
		scheduledExecutorService.schedule(publishTask, nextQuestionDelay, TimeUnit.MILLISECONDS);
	}
	
	private boolean checkIfAlreadyScheduled(String mgiId, String userId){
		Validate.notNull(mgiId, "Live tournament game config should have multiplayer game instance reference set");
		Validate.notNull(userId, "Question scheduler starter's user id should be provided");
		
		final String creatorId = preselectedQuestionsDao.getCreatorId(mgiId);
		Validate.notNull(creatorId, "Preselected question record creator id should exist");
		return creatorId.equals(userId);
	}

	private ScheduledExecutorService createScheduledExecutorService() {
		final ThreadFactory threadFactory = new ThreadFactoryBuilder()
			.setNameFormat("QuestionFeeder-Scheduler-%d")
			.build();
		final int corePoolSize = config.getPropertyAsInteger(QUESTION_FEEDER_THREAD_POOL_CORE_SIZE);
		return Executors.newScheduledThreadPool(corePoolSize, threadFactory);
	}
}
