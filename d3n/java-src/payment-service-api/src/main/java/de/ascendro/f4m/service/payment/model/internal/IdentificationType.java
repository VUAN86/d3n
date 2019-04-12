package de.ascendro.f4m.service.payment.model.internal;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum IdentificationType {
	@JsonProperty("0")
	LIGHT, 
	@JsonProperty("1")
	FULL;
}
