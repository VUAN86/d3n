package de.ascendro.f4m.service.game.selection.model.subscription;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class ReceiveGameStartNotificationsRequest implements JsonMessageContent {

	private String multiplayerGameInstanceId;
	private boolean receive;

	public ReceiveGameStartNotificationsRequest() {
		// Empty constructor
	}

	public boolean isReceive() {
		return receive;
	}

	public void setReceive(boolean receive) {
		this.receive = receive;
	}
	
	public String getMultiplayerGameInstanceId() {
		return multiplayerGameInstanceId;
	}
	
	public void setMultiplayerGameInstanceId(String multiplayerGameInstanceId) {
		this.multiplayerGameInstanceId = multiplayerGameInstanceId;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder("ReceiveGameStartNotificationsRequest [multiplayerGameInstanceId=");
		builder.append(multiplayerGameInstanceId);
		builder.append(", receive=");
		builder.append(receive);
		builder.append("]");
		return builder.toString();
	}

	

}
