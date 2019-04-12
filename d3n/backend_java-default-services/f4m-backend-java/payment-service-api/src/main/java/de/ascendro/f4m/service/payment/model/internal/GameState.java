package de.ascendro.f4m.service.payment.model.internal;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum GameState {
	@JsonProperty("0") OPEN, @JsonProperty("1") CLOSED;
}
