package de.ascendro.f4m.service.game.selection.model.dashboard;

public class ApiTournament {

	protected NextInvitation nextInvitation;
	protected NextInvitation nextTournament;
	protected long numberOfInvitations;
	protected long numberOfTournaments;

	public NextInvitation getNextInvitation() {
		return nextInvitation;
	}

	public void setNextInvitation(NextInvitation nextInvitation) {
		this.nextInvitation = nextInvitation;
	}

	public NextInvitation getNextTournament() {
		return nextTournament;
	}

	public void setNextTournament(NextInvitation nextTournament) {
		this.nextTournament = nextTournament;
	}

	public long getNumberOfInvitations() {
		return numberOfInvitations;
	}

	public void setNumberOfInvitations(long numberOfInvitations) {
		this.numberOfInvitations = numberOfInvitations;
	}

	public long getNumberOfTournaments() {
		return numberOfTournaments;
	}

	public void setNumberOfTournaments(long numberOfTournaments) {
		this.numberOfTournaments = numberOfTournaments;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ApiTournament [nextInvitation=");
		builder.append(nextInvitation);
		builder.append(", nextTournament=");
		builder.append(nextTournament);
		builder.append(", numberOfInvitations=");
		builder.append(numberOfInvitations);
		builder.append(", numberOfTournaments=");
		builder.append(numberOfTournaments);
		builder.append("]");
		return builder.toString();
	}

}
