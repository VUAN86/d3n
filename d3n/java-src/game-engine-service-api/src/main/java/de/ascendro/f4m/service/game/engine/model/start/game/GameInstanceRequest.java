package de.ascendro.f4m.service.game.engine.model.start.game;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public interface GameInstanceRequest extends JsonMessageContent {
	String getGameInstanceId();
}
