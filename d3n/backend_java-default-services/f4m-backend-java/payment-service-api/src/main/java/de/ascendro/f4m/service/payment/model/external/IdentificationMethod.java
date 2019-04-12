package de.ascendro.f4m.service.payment.model.external;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum IdentificationMethod {
	@JsonProperty("0")
	ID, 
	@JsonProperty("1")
	POSTIDENT;
}
