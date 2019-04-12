package de.ascendro.f4m.service.payment.model.external;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum PaymentTransactionState {
	@JsonProperty("0")
	PENDING, 
	@JsonProperty("1")
	PROCESSED;

}
