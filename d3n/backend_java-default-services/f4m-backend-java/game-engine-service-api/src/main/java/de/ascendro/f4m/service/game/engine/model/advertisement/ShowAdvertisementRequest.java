package de.ascendro.f4m.service.game.engine.model.advertisement;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class ShowAdvertisementRequest implements JsonMessageContent {
	private String gameInstanceId;
	private String advertisementBlobKey;

	public ShowAdvertisementRequest() {
	}
	
	public ShowAdvertisementRequest(String gameInstanceId, String advertisementBlobKey) {
		this.gameInstanceId = gameInstanceId;
		this.advertisementBlobKey = advertisementBlobKey;
	}

	public String getGameInstanceId() {
		return gameInstanceId;
	}

	public void setGameInstanceId(String gameInstanceId) {
		this.gameInstanceId = gameInstanceId;
	}
	
	public String getAdvertisementBlobKey() {
		return advertisementBlobKey;
	}

	public void setAdvertisementBlobKey(String advertisementBlobKey) {
		this.advertisementBlobKey = advertisementBlobKey;
	}

}
