package de.ascendro.f4m.service.payment.model.external;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum PaymentTransactionType {
	@JsonProperty("0")
	DEBIT, 
	@JsonProperty("1")
	CREDIT
}
