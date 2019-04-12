package de.ascendro.f4m.server.result;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.json.model.JsonObjectWrapper;
import de.ascendro.f4m.service.util.F4MEnumUtils;

/**
 * Results of a single game.
 */
public class MultiplayerResults extends JsonObjectWrapper {

	public static final String PROPERTY_MULTIPLAYER_GAME_INSTANCE_ID = "multiplayerGameInstanceId";
	public static final String PROPERTY_GAME_TYPE = "gameType";
	
	public MultiplayerResults() {
		// Initialize empty results
	}
	
	public MultiplayerResults(JsonObject jsonObject) {
		super(jsonObject);
	}
	
	public MultiplayerResults(String mutliplayerGameInstanceId, GameType gameType) {
		setProperty(PROPERTY_MULTIPLAYER_GAME_INSTANCE_ID, mutliplayerGameInstanceId);
		setProperty(PROPERTY_GAME_TYPE, gameType == null ? null : gameType.name());
	}
	
	public String getMultiplayerGameInstanceId() {
		return getPropertyAsString(PROPERTY_MULTIPLAYER_GAME_INSTANCE_ID);
	}
	
	public GameType getGameType() {
		return F4MEnumUtils.getEnum(GameType.class, getPropertyAsString(PROPERTY_GAME_TYPE));
	}
	
}
