package de.ascendro.f4m.service.game.engine.model.start.step;

import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.engine.model.GameState;
import de.ascendro.f4m.service.game.engine.model.Question;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

/**
 * Game Engine request to Client. Request to start step
 */
public class StartStepRequest implements JsonMessageContent {
	private String gameInstanceId;
	private int question;
	private String questionStepBlobKey;
	private Integer step;
	private String decryptionKey;

	public StartStepRequest(GameInstance gameInstance) {
		this.gameInstanceId = gameInstance.getId();

		final GameState actualGameState = gameInstance.getGameState();
		this.question = actualGameState.getCurrentQuestionStep().getQuestion();
		this.step = actualGameState.getCurrentQuestionStep().getStep();
		this.step = step != null ? step : 0;

		final Question question = gameInstance.getQuestion(this.question);
		questionStepBlobKey = question.getQuestionBlobKey(this.step);
		decryptionKey = question.getDecryptionKey(this.step);
	}

	public String getGameInstanceId() {
		return gameInstanceId;
	}

	public void setGameInstanceId(String gameInstanceId) {
		this.gameInstanceId = gameInstanceId;
	}

	public int getQuestion() {
		return question;
	}

	public void setQuestion(int question) {
		this.question = question;
	}

	public Integer getStep() {
		return step;
	}

	public void setStep(Integer step) {
		this.step = step;
	}

	public String getQuestionStepBlobKey() {
		return questionStepBlobKey;
	}

	public void setQuestionStepBlobKey(String questionStepBlobKey) {
		this.questionStepBlobKey = questionStepBlobKey;
	}

	public String getDecryptionKey() {
		return decryptionKey;
	}

	public void setDecryptionKey(String decryptionKey) {
		this.decryptionKey = decryptionKey;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("StartStepRequest [gameInstanceId=").append(gameInstanceId);
		builder.append(", question=").append(question);
		builder.append(", questionStepBlobKey=").append(questionStepBlobKey);
		builder.append(", step=").append(step);
		builder.append(", decryptionKey=").append(decryptionKey).append("]");
		return builder.toString();
	}

}
