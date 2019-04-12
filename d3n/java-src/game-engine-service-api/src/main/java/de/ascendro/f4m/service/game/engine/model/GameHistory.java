package de.ascendro.f4m.service.game.engine.model;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.game.engine.model.end.GameEndStatus;
import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

public class GameHistory extends JsonObjectWrapper {
	private static final String GAME_INSTANCE_ID_PROPERTY = "gameInstanceId";
	private static final String GAME_ID_PROPERTY = "gameId";
	private static final String MGI_ID_PROPERTY = "mgiId";
	private static final String STATUS_PROPERTY = "status";
	private static final String END_STATUS_PROPERTY = "endStatus";
	private static final String LAST_UPDATE_TIMESTAMP_PROPERTY = "timestamp";

	public GameHistory() {
	}

	public GameHistory(JsonObject jsonObject) throws IllegalArgumentException {
		super(jsonObject);
	}
	
	public GameHistory(GameStatus gameStatus) {
		setStatus(gameStatus);
	}

	public GameStatus getStatus() {
		final String status = getPropertyAsString(STATUS_PROPERTY);
		return status != null ? GameStatus.valueOf(status) : null;
	}
	
	public void setTimestamp(Long timestamp){
		setProperty(LAST_UPDATE_TIMESTAMP_PROPERTY, timestamp);
	}

	public void setStatus(GameStatus status) {
		setProperty(STATUS_PROPERTY, status != null ? status.name() : null);
	}

	public GameEndStatus getEndStatus() {
		final String endStatus = getPropertyAsString(END_STATUS_PROPERTY);
		return endStatus != null ? GameEndStatus.valueOf(endStatus) : null;
	}

	public void setEndStatus(GameEndStatus endStatus) {
		setProperty(END_STATUS_PROPERTY, endStatus != null ? endStatus.name() : null);
	}

	public void setGameId(String gameId) {
		setProperty(GAME_ID_PROPERTY, gameId);
	}

	public String getGameId() {
		return getPropertyAsString(GAME_INSTANCE_ID_PROPERTY);
	}
	
	public void setMgiId(String mgiId) {
		setProperty(MGI_ID_PROPERTY, mgiId);
	}

	public String getMgiId() {
		return getPropertyAsString(MGI_ID_PROPERTY);
	}
	
	public void setGameInstanceId(String gameInstanceId) {
		setProperty(GAME_INSTANCE_ID_PROPERTY, gameInstanceId);
	}

	public String getGameInstanceId() {
		return getPropertyAsString(GAME_INSTANCE_ID_PROPERTY);
	}

}
