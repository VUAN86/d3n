package de.ascendro.f4m.service.game.engine.feeder;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.game.engine.model.Question;
import de.ascendro.f4m.service.game.engine.model.QuestionStep;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.util.EventServiceClient;

public abstract class StepEventPublisher {

	protected LoggingUtil loggingUtil;
	protected EventServiceClient eventServiceClient;
	protected QuestionStep currentQuestionStep;
	protected QuestionFeeder questionFeeder;
	protected String mgiId;
	protected Question[] questions;

	protected void publishStartStepEvent() {
		final String startStepEventTopic = Game.getLiveTournamentStartStepTopic(mgiId);
		eventServiceClient.publish(startStepEventTopic, new JsonObject());
		// schedule only after really message is sent using future?
		if (hasNextAction(currentQuestionStep)) {
			nextAction(currentQuestionStep);
			questionFeeder.scheduleNextAction(mgiId, currentQuestionStep, questions);
		} else {
			questionFeeder.scheduleGameEnd(mgiId, questions);			
		}
	}

	private boolean hasNextAction(QuestionStep currentQuestionStep) {
		return hasNextQuestion(currentQuestionStep) || currentQuestionStep.hasNextStep();
	}

	private boolean hasNextQuestion(QuestionStep currentQuestionStep) {
		return currentQuestionStep.getQuestion() < getNumberOfQuestions() - 1;
	}

	private int getNumberOfQuestions() {
		return questions.length;
	}

	private void nextAction(QuestionStep currentQuestionStep) {
		if (currentQuestionStep.hasNextStep()) {
			currentQuestionStep.nextStep();
		} else {
			final int nextQuestionStepCount = questions[currentQuestionStep.getQuestion() + 1].getStepCount();
			currentQuestionStep.nextQuestion(nextQuestionStepCount);
		}
	}

}
