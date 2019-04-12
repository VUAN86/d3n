package de.ascendro.f4m.service.game.selection.model.dashboard;

import com.google.gson.JsonObject;

public class ApiQuizBattle {

	private JsonObject nextInvitation;
	private JsonObject nextQuizBattle;
	private int numberOfQuizBattles;

	public JsonObject getNextInvitation() {
		return nextInvitation;
	}

	public void setNextInvitation(JsonObject nextInvitation) {
		this.nextInvitation = nextInvitation;
	}

	public JsonObject getNextQuizBattle() {
		return nextQuizBattle;
	}

	public void setNextQuizBattle(JsonObject nextQuizBattle) {
		this.nextQuizBattle = nextQuizBattle;
	}

	public int getNumberOfQuizBattles() {
		return numberOfQuizBattles;
	}

	public void setNumberOfQuizBattles(int numberOfQuizBattles) {
		this.numberOfQuizBattles = numberOfQuizBattles;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ApiQuizBattle [nextInvitation=");
		builder.append(nextInvitation);
		builder.append(", nextQuizBattle=");
		builder.append(nextQuizBattle);
		builder.append(", numberOfQuizBattles=");
		builder.append(numberOfQuizBattles);
		builder.append("]");
		return builder.toString();
	}

}
