package de.ascendro.f4m.service.game.selection.model.dashboard;

public class MostPlayedGame {

	private long numberOfPlayers;
	private PlayedGameInfo gameInfo;

	public MostPlayedGame() {
		this.numberOfPlayers = 0L;
		this.gameInfo = new PlayedGameInfo();
	}

	public MostPlayedGame(Long numberOfPlayers, PlayedGameInfo gameInfo) {
		this.numberOfPlayers = numberOfPlayers;
		this.gameInfo = gameInfo;
	}

	public long getNumberOfPlayers() {
		return numberOfPlayers;
	}

	public void setNumberOfPlayers(long numberOfPlayers) {
		this.numberOfPlayers = numberOfPlayers;
	}

	public PlayedGameInfo getGameInfo() {
		return gameInfo;
	}

	public void setGameInfo(PlayedGameInfo gameInfo) {
		this.gameInfo = gameInfo;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MostPlayedGame [numberOfPlayers=");
		builder.append(numberOfPlayers);
		builder.append(", gameInfo=");
		builder.append(gameInfo);
		builder.append("]");
		return builder.toString();
	}

}
