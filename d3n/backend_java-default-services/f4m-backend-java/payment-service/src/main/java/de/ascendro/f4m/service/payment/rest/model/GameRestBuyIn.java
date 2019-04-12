package de.ascendro.f4m.service.payment.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GameRestBuyIn extends GameRestPayoutItem {
	@JsonProperty("GameId")
	private String gameId;
	@JsonProperty("Reference")
	private String reference;

	public String getGameId() {
		return gameId;
	}

	public void setGameId(String gameId) {
		this.gameId = gameId;
	}

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GameRestBuyIn [gameId=");
		builder.append(gameId);
		builder.append(", reference=");
		builder.append(reference);
		builder.append(", userId=");
		builder.append(userId);
		builder.append(", amount=");
		builder.append(amount);
		builder.append("]");
		return builder.toString();
	}

}
