package de.ascendro.f4m.service.game.engine.model.answer;

import com.google.gson.annotations.SerializedName;

import de.ascendro.f4m.service.game.engine.model.start.game.GameInstanceRequest;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class NextStepRequest implements JsonMessageContent, GameInstanceRequest {
	private String gameInstanceId;
	
	private int question;
	
	private int step;

	@SerializedName(value = "tClientMs")
	private Long clientMsT;

	@Override
	public String getGameInstanceId() {
		return gameInstanceId;
	}

	public void setGameInstanceId(String gameInstanceId) {
		this.gameInstanceId = gameInstanceId;
	}

	public Long getClientMsT() {
		return clientMsT;
	}

	public void setClientMsT(Long clientMsT) {
		this.clientMsT = clientMsT;
	}

	public int getQuestion() {
		return question;
	}

	public void setQuestion(int question) {
		this.question = question;
	}

	public int getStep() {
		return step;
	}

	public void setStep(int step) {
		this.step = step;
	}
}
