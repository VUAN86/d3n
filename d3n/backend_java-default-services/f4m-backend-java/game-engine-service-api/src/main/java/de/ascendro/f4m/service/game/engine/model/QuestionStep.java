package de.ascendro.f4m.service.game.engine.model;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

public class QuestionStep extends JsonObjectWrapper {
	private static final String STEP_PROPERTY = "step";
	private static final String QUESTION_PROPERTY = "question";
	private static final String STEP_COUNT = "stepCount";

	public QuestionStep(int question, int step, int questionStepCount) {
		this.jsonObject = new JsonObject();
		jsonObject.addProperty(QUESTION_PROPERTY, question);
		jsonObject.addProperty(STEP_PROPERTY, step);
		jsonObject.addProperty(STEP_COUNT, questionStepCount);
	}

	public QuestionStep(JsonObject questionStepObject) {
		this.jsonObject = questionStepObject;
	}	

	public int getStep() {
		return getPropertyAsInt(STEP_PROPERTY);
	}

	public int getQuestion() {
		return getPropertyAsInt(QUESTION_PROPERTY);
	}

	public int getStepCount() {
		return getPropertyAsInt(STEP_COUNT);
	}

	public int nextStep() {
		final int newStep = getStep() + 1;
		jsonObject.addProperty(STEP_PROPERTY, newStep);
		return newStep;
	}

	public int nextQuestion(int nextQuestionStepCount) {
		final int newQuestionIndex = getQuestion() + 1;

		jsonObject.addProperty(QUESTION_PROPERTY, newQuestionIndex);
		jsonObject.addProperty(STEP_PROPERTY, 0);
		jsonObject.addProperty(STEP_COUNT, nextQuestionStepCount);

		return newQuestionIndex;
	}

	public boolean hasNextStep() {
		return getStep() + 1 < getStepCount();
	}
}
