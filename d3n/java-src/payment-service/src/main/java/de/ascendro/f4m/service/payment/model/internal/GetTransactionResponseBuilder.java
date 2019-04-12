package de.ascendro.f4m.service.payment.model.internal;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import de.ascendro.f4m.service.payment.model.Currency;

public class GetTransactionResponseBuilder {
	//Some problems here:
	//class is not quite generated, TransactionInfo fields must be updated by hand (or copied from TransactionInfoBuilder)
	//not easy to manage inheritance of GetTransactionResponse
	//And - common fields are basically copied from TransferBetweenAccountsRequestBuilder, which has same problems
	private String id;
	private String type;
	private String tenantId;
	private String fromProfileId;
	private String toProfileId;
	private Currency fromCurrency;
	private Currency toCurrency;
	private BigDecimal amount;
	private ZonedDateTime creationDate;
	private PaymentDetails paymentDetails;

	public GetTransactionResponseBuilder tenantId(String tenantId) {
		this.tenantId = tenantId;
		return this;
	}

	public GetTransactionResponseBuilder fromProfileId(String fromProfileId) {
		this.fromProfileId = fromProfileId;
		return this;
	}

	public GetTransactionResponseBuilder toProfileId(String toProfileId) {
		this.toProfileId = toProfileId;
		return this;
	}

	public GetTransactionResponseBuilder fromCurrency(Currency fromCurrency) {
		this.fromCurrency = fromCurrency;
		return this;
	}

	public GetTransactionResponseBuilder toCurrency(Currency toCurrency) {
		this.toCurrency = toCurrency;
		return this;
	}

	public GetTransactionResponseBuilder amount(BigDecimal amount) {
		this.amount = amount;
		return this;
	}

	public GetTransactionResponseBuilder creationDate(ZonedDateTime creationDate) {
		this.creationDate = creationDate;
		return this;
	}

	public GetTransactionResponseBuilder paymentDetails(PaymentDetails paymentDetails) {
		this.paymentDetails = paymentDetails;
		return this;
	}
	
	public GetTransactionResponseBuilder id(String id) {
		this.id = id;
		return this;
	}

	public GetTransactionResponseBuilder type(String type) {
		this.type = type;
		return this;
	}

	public GetTransactionResponse build() {
		GetTransactionResponse getTransactionResponse = new GetTransactionResponse();
		getTransactionResponse.id = id;
		getTransactionResponse.type = type;
		getTransactionResponse.tenantId = tenantId;
		getTransactionResponse.fromProfileId = fromProfileId;
		getTransactionResponse.toProfileId = toProfileId;
		getTransactionResponse.fromCurrency = fromCurrency;
		getTransactionResponse.toCurrency = toCurrency;
		getTransactionResponse.amount = amount;
		getTransactionResponse.creationDate = creationDate;
		getTransactionResponse.paymentDetails = paymentDetails;
		return getTransactionResponse;
	}
}
