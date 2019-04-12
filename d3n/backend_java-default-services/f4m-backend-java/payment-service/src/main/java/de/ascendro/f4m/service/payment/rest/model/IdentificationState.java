package de.ascendro.f4m.service.payment.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum IdentificationState {
	@JsonProperty("0")
	PENDING, 
	@JsonProperty("1")
	CANCELLED, 
	@JsonProperty("2")
	APPROVED, 
	@JsonProperty("3")
	REJECTED;
}
