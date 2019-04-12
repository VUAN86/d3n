package de.ascendro.f4m.service.game.engine.model.joker;

import de.ascendro.f4m.service.game.engine.model.start.game.GameInstanceRequest;

public class JokersAvailableRequest implements GameInstanceRequest {

    private String gameInstanceId;
    private int question;

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

}
