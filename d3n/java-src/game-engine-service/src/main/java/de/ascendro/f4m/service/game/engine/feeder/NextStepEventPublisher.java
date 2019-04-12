package de.ascendro.f4m.service.game.engine.feeder;

import de.ascendro.f4m.service.game.engine.model.Question;
import de.ascendro.f4m.service.game.engine.model.QuestionStep;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.util.EventServiceClient;

public class NextStepEventPublisher extends StepEventPublisher implements Runnable {

	public NextStepEventPublisher(QuestionFeeder questionFeeder, EventServiceClient eventServiceClient, String mgiId,
			QuestionStep currentQuestionStep, Question[] questions, LoggingUtil loggingUtil) {
		super.eventServiceClient = eventServiceClient;
		super.mgiId = mgiId;
		super.currentQuestionStep = currentQuestionStep;
		super.questionFeeder = questionFeeder;
		super.questions = questions;
		super.loggingUtil = loggingUtil;
	}

	@Override
	public void run() {
		loggingUtil.saveBasicInformationInThreadContext();
		publishStartStepEvent();
	}

}
