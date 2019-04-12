package de.ascendro.f4m.service.payment.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum TransactionType {
	@JsonProperty("0")
	DEBIT, 
	@JsonProperty("1")
	CREDIT, 
	@JsonProperty("2")
	TRANSFER;
}
