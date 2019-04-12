package de.ascendro.f4m.service.game.selection.model.subscription;

public class GameStartCountDownNotification extends GameStartNotification {

	private long time;

	public GameStartCountDownNotification(String multiplayerGameInstanceId, long time) {
		super(multiplayerGameInstanceId);
		this.time = time;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GameStartCountDownNotification");
		builder.append(" [multiplayerGameInstanceId=").append(getMultiplayerGameInstanceId());
		builder.append(", time=").append(time);
		builder.append("]");

		return builder.toString();
	}

}
