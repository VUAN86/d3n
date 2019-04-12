package de.ascendro.f4m.service.game.engine.model.ready;

import de.ascendro.f4m.service.game.engine.model.start.game.GameInstanceRequest;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class ReadyToPlayRequest implements JsonMessageContent, GameInstanceRequest {
	private String gameInstanceId;

	@Override
	public String getGameInstanceId() {
		return gameInstanceId;
	}

	public void setGameInstanceId(String gameInstanceId) {
		this.gameInstanceId = gameInstanceId;
	}

}
