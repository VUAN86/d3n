package de.ascendro.f4m.service.game.engine.feeder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.game.engine.model.Question;
import de.ascendro.f4m.service.game.engine.model.QuestionStep;
import de.ascendro.f4m.service.game.engine.multiplayer.MultiplayerGameManager;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.util.EventServiceClient;

public class FirstStepEventPublisher extends StepEventPublisher implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(FirstStepEventPublisher.class);

	private final MultiplayerGameManager multiplayerGameManager;

	public FirstStepEventPublisher(QuestionFeeder questionFeeder, EventServiceClient eventServiceClient, String mgiId,
			QuestionStep currentQuestionStep, Question[] questions, LoggingUtil loggingUtil, 
			MultiplayerGameManager multiplayerGameManager) {
		super.eventServiceClient = eventServiceClient;
		super.mgiId = mgiId;
		super.currentQuestionStep = currentQuestionStep;
		super.questionFeeder = questionFeeder;
		super.questions = questions;
		super.loggingUtil = loggingUtil;
		this.multiplayerGameManager = multiplayerGameManager;
	}

	@Override
	public void run() {
		loggingUtil.saveBasicInformationInThreadContext();
		if (multiplayerGameManager.hasEnoughPlayersToPlay(mgiId)) {
			LOGGER.info("Publishing first step of live tournament [%s]", mgiId);
			publishStartStepEvent();
		} else {
			LOGGER.info("Cancelling live tournament [%s] - not enough players", mgiId);
			cancelLiveTournament();
		}
	}

	private void cancelLiveTournament() {
		multiplayerGameManager.cancelLiveTournament(mgiId);
	}

}
