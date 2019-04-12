package de.ascendro.f4m.service.game.selection.model.dashboard;

public class PlayedTournamentInfo {

	private String mgiId;
	private String gameInstanceId;
	private int opponentsNumber;
	private int placement;

	public PlayedTournamentInfo(String mgiId, String gameInstanceId) {
		this.mgiId = mgiId;
		this.gameInstanceId = gameInstanceId;
	}

	public String getMgiId() {
		return mgiId;
	}

	public void setMgiId(String mgiId) {
		this.mgiId = mgiId;
	}

	public String getGameInstanceId() {
		return gameInstanceId;
	}

	public void setGameInstanceId(String gameInstanceId) {
		this.gameInstanceId = gameInstanceId;
	}

	public int getOpponentsNumber() {
		return opponentsNumber;
	}

	public void setOpponentsNumber(int opponentsNumber) {
		this.opponentsNumber = opponentsNumber;
	}

	public int getPlacement() {
		return placement;
	}

	public void setPlacement(int placement) {
		this.placement = placement;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PlayedTournamentInfo [mgiId=");
		builder.append(mgiId);
		builder.append(", gameInstanceId=");
		builder.append(gameInstanceId);
		builder.append(", opponentsNumber=");
		builder.append(opponentsNumber);
		builder.append(", placement=");
		builder.append(placement);
		builder.append("]");
		return builder.toString();
	}

}
