package de.ascendro.f4m.service.payment.rest.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GameRestPayout {
	@JsonProperty("GameId")
	private String gameId;
	@JsonProperty("Reference")
	private String reference;
	@JsonProperty("Payouts")
	private List<GameRestPayoutItem> payouts;

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

	public List<GameRestPayoutItem> getPayouts() {
		return payouts;
	}

	public void setPayouts(List<GameRestPayoutItem> payouts) {
		this.payouts = payouts;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GameRestPayout [gameId=");
		builder.append(gameId);
		builder.append(", reference=");
		builder.append(reference);
		builder.append(", payouts=");
		builder.append(payouts);
		builder.append("]");
		return builder.toString();
	}
}
