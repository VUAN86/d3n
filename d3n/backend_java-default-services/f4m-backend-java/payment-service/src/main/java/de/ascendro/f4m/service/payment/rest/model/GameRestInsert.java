package de.ascendro.f4m.service.payment.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GameRestInsert {
	@JsonProperty("GameId")
	private String gameId;
	@JsonProperty("CurrencyId")
	private String currencyId;

	public String getCurrencyId() {
		return currencyId;
	}

	public void setCurrencyId(String currencyId) {
		this.currencyId = currencyId;
	}

	public String getGameId() {
		return gameId;
	}

	public void setGameId(String gameId) {
		this.gameId = gameId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GameRestInsert [gameId=");
		builder.append(gameId);
		builder.append(", currencyId=");
		builder.append(currencyId);
		builder.append("]");
		return builder.toString();
	}
}
