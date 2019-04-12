package de.ascendro.f4m.service.payment.model.internal;

import java.math.BigDecimal;

import de.ascendro.f4m.service.payment.model.Currency;

public class TransferBetweenAccountsRequestBuilder {
	private String tenantId;
	private String fromProfileId;
	private String toProfileId;
	private Currency currency;
	private BigDecimal amount;
	private PaymentDetails paymentDetails;

	public TransferBetweenAccountsRequestBuilder tenantId(String tenantId) {
		this.tenantId = tenantId;
		return this;
	}

	public TransferBetweenAccountsRequestBuilder fromProfileId(String fromProfileId) {
		this.fromProfileId = fromProfileId;
		return this;
	}

	public TransferBetweenAccountsRequestBuilder toProfileId(String toProfileId) {
		this.toProfileId = toProfileId;
		return this;
	}

	public TransferBetweenAccountsRequestBuilder currency(Currency currency) {
		this.currency = currency;
		return this;
	}

	public TransferBetweenAccountsRequestBuilder amount(BigDecimal amount) {
		this.amount = amount;
		return this;
	}

	public TransferBetweenAccountsRequestBuilder paymentDetails(PaymentDetails paymentDetails) {
		this.paymentDetails = paymentDetails;
		return this;
	}

	public TransferBetweenAccountsRequest build() {
		TransferBetweenAccountsRequest transferBetweenAccountsRequest = new TransferBetweenAccountsRequest();
		transferBetweenAccountsRequest.tenantId = tenantId;
		transferBetweenAccountsRequest.fromProfileId = fromProfileId;
		transferBetweenAccountsRequest.toProfileId = toProfileId;
		transferBetweenAccountsRequest.currency = currency;
		transferBetweenAccountsRequest.amount = amount;
		transferBetweenAccountsRequest.paymentDetails = paymentDetails;
		return transferBetweenAccountsRequest;
	}

}
