package de.ascendro.f4m.service.game.engine.model.answer;

import com.google.gson.annotations.SerializedName;

import de.ascendro.f4m.service.game.engine.model.start.game.GameInstanceRequest;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class AnswerQuestionRequest implements JsonMessageContent, GameInstanceRequest {
	private String gameInstanceId;
	private int question;
	private int step;
	private String[] answers;
	
	@SerializedName(value = "tClientMs")
	private Long clientMsT;

	public String getGameId() {
		return gameInstanceId;
	}

	@Override
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

	public String[] getAnswers() {
		return answers;
	}

	public void setAnswers(String[] answers) {
		this.answers = answers;
	}

	public Long getClientMsT() {
		return clientMsT;
	}

	public void setClientMsT(Long clientMsT) {
		this.clientMsT = clientMsT;
	}
	
	public void setStep(int step) {
		this.step = step;
	}
	
	public int getStep() {
		return step;
	}

}
