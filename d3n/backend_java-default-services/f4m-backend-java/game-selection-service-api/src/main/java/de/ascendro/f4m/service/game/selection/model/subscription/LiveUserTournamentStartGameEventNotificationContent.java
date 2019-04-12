package de.ascendro.f4m.service.game.selection.model.subscription;

public class LiveUserTournamentStartGameEventNotificationContent {

	private String gameId;
	private String mgiId;

	public LiveUserTournamentStartGameEventNotificationContent(String gameId, String mgiId) {
		this.setGameId(gameId);
		this.setMgiId(mgiId);
		
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("LiveUserTournamentStartGameEventNotificationContent [mgiId=");
		builder.append(mgiId);
		builder.append(", gameId=");
		builder.append(gameId);
		builder.append("]");
		return builder.toString();
	}

	public String getGameId() {
		return gameId;
	}

	public void setGameId(String gameId) {
		this.gameId = gameId;
	}

	public String getMgiId() {
		return mgiId;
	}

	public void setMgiId(String mgiId) {
		this.mgiId = mgiId;
	}


}
