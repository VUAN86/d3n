package de.ascendro.f4m.service.game.engine.model.answer;

import java.util.Arrays;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class AnswerQuestionResponse implements JsonMessageContent {

	private String[] correctAnswers;
	private boolean immediateAnswerPurchased;
	private String serverMsT;

	public AnswerQuestionResponse(String[] correctAnswers) {
		this.correctAnswers = correctAnswers;
	}

	public String[] getCorrectAnswers() {
		return correctAnswers;
	}

	public void setCorrectAnswers(String[] correctAnswers) {
		this.correctAnswers = correctAnswers;
	}

	public boolean isImmediateAnswerPurchased() {
		return immediateAnswerPurchased;
	}
	
	public void setImmediateAnswerPurchased(boolean immediateAnswerPurchased) {
		this.immediateAnswerPurchased = immediateAnswerPurchased;
	}

	public String getServerMsT() {
		return serverMsT;
	}

	public void setServerMsT(String serverMsT) {
		this.serverMsT = serverMsT;
	}

	@Override
	public String toString() {
		return "AnswerQuestionResponse [" +
				"correctAnswers=" + Arrays.toString(correctAnswers) +
				",immediateAnswerPurchased=" + immediateAnswerPurchased +
				"]";
	}

}
