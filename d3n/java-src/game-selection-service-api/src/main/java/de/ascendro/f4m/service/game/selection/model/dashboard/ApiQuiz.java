package de.ascendro.f4m.service.game.selection.model.dashboard;

public class ApiQuiz {

	private int numberOfGamesAvailable;
	private PlayedGameInfo lastPlayedGame;
	private MostPlayedGame mostPlayedGame;
	private int numberOfSpecialGamesAvailable;
	private SpecialGameInfo latestSpecialGame;

	public int getNumberOfGamesAvailable() {
		return numberOfGamesAvailable;
	}

	public void setNumberOfGamesAvailable(int numberOfGamesAvailable) {
		this.numberOfGamesAvailable = numberOfGamesAvailable;
	}

	public PlayedGameInfo getLastPlayedGame() {
		return lastPlayedGame;
	}

	public void setLastPlayedGame(PlayedGameInfo lastPlayedGame) {
		this.lastPlayedGame = lastPlayedGame;
	}

	public MostPlayedGame getMostPlayedGame() {
		return mostPlayedGame;
	}

	public void setMostPlayedGame(MostPlayedGame mostPlayedGame) {
		this.mostPlayedGame = mostPlayedGame;
	}

	public int getNumberOfSpecialGamesAvailable() {
		return numberOfSpecialGamesAvailable;
	}

	public void setNumberOfSpecialGamesAvailable(int numberOfSpecialGamesAvailable) {
		this.numberOfSpecialGamesAvailable = numberOfSpecialGamesAvailable;
	}

	public SpecialGameInfo getLatestSpecialGame() {
		return latestSpecialGame;
	}

	public void setLatestSpecialGame(SpecialGameInfo gameInfo) {
		this.latestSpecialGame = gameInfo;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ApiQuiz [numberOfGamesAvailable=");
		builder.append(numberOfGamesAvailable);
		builder.append(", lastPlayedGame=");
		builder.append(lastPlayedGame);
		builder.append(", mostPlayedGame=");
		builder.append(mostPlayedGame);
		builder.append(", numberOfSpecialGamesAvailable=");
		builder.append(numberOfSpecialGamesAvailable);
		builder.append(", latestSpecialGame=");
		builder.append(latestSpecialGame);
		builder.append("]");
		return builder.toString();
	}

}
