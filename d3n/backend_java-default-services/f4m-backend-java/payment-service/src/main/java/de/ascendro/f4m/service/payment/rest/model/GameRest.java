package de.ascendro.f4m.service.payment.rest.model;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.ascendro.f4m.service.payment.model.internal.GameState;

public class GameRest {

	@JsonProperty("Id")
	private String id;
	@JsonProperty("Balance")
	private BigDecimal balance;
	@JsonProperty("State")
	private GameState state;
	@JsonProperty("Currency")
	private CurrencyRest currency;
	@JsonProperty("Participants")
	private List<String> participantIds;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	public GameState getState() {
		return state;
	}

	public void setState(GameState state) {
		this.state = state;
	}

	public List<String> getParticipantIds() {
		return participantIds;
	}

	public void setParticipantIds(List<String> participantIds) {
		this.participantIds = participantIds;
	}

	public CurrencyRest getCurrency() {
		return currency;
	}

	public void setCurrency(CurrencyRest currency) {
		this.currency = currency;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GameRest [id=");
		builder.append(id);
		builder.append(", balance=");
		builder.append(balance);
		builder.append(", state=");
		builder.append(state);
		builder.append(", currency=");
		builder.append(currency);
		builder.append(", participantIds=");
		builder.append(participantIds);
		builder.append("]");
		return builder.toString();
	}
}
