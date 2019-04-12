package de.ascendro.f4m.service.game.selection.model.dashboard;

public class ApiDuel {

	private NextInvitation nextInvitation;
	private NextInvitation nextPublicGame;
	private PlayedGameInfo lastDuel;
	private long numberOfInvitations;
	private long numberOfPublicGames;

	public NextInvitation getNextInvitation() {
		return nextInvitation;
	}

	public void setNextInvitation(NextInvitation nextInvitation) {
		this.nextInvitation = nextInvitation;
	}

	public NextInvitation getNextPublicGame() {
		return nextPublicGame;
	}

	public void setNextPublicGame(NextInvitation nextPublicGame) {
		this.nextPublicGame = nextPublicGame;
	}

	public PlayedGameInfo getLastDuel() {
		return lastDuel;
	}

	public void setLastDuel(PlayedGameInfo lastDuel) {
		this.lastDuel = lastDuel;
	}

	public long getNumberOfInvitations() {
		return numberOfInvitations;
	}

	public void setNumberOfInvitations(long numberOfInvitations) {
		this.numberOfInvitations = numberOfInvitations;
	}

	public long getNumberOfPublicGames() {
		return numberOfPublicGames;
	}

	public void setNumberOfPublicGames(long numberOfPublicGames) {
		this.numberOfPublicGames = numberOfPublicGames;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ApiDuel [nextInvitation=");
		builder.append(nextInvitation);
		builder.append(", nextPublicGame=");
		builder.append(nextPublicGame);
		builder.append(", lastDuel=");
		builder.append(lastDuel);
		builder.append(", numberOfInvitations=");
		builder.append(numberOfInvitations);
		builder.append(", numberOfPublicGames=");
		builder.append(numberOfPublicGames);
		builder.append("]");
		return builder.toString();
	}

}
