package de.ascendro.f4m.service.game.selection.model.dashboard;

public class ApiUserTournament extends ApiTournament {
	private PlayedGameInfo lastTournament;

	public PlayedGameInfo getLastTournament() {
		return lastTournament;
	}

	public void setLastTournament(PlayedGameInfo lastTournament) {
		this.lastTournament = lastTournament;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ApiUserTournament [nextInvitation=");
		builder.append(nextInvitation);
		builder.append(", nextTournament=");
		builder.append(nextTournament);
		builder.append(", lastTournament=");
		builder.append(lastTournament);
		builder.append(", numberOfInvitations=");
		builder.append(numberOfInvitations);
		builder.append(", numberOfTournaments=");
		builder.append(numberOfTournaments);
		builder.append("]");
		return builder.toString();
	}
}
