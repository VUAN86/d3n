package de.ascendro.f4m.service.payment.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CashoutDataResponse extends CashoutDataRequest {
	@JsonProperty("PaymentTransactionId")
	protected String paymentTransactionId;
	@JsonProperty("ClientId")
	protected String clientId;
	@JsonProperty("Value")
	protected String value;

	public String getPaymentTransactionId() {
		return paymentTransactionId;
	}

	public void setPaymentTransactionId(String paymentTransactionId) {
		this.paymentTransactionId = paymentTransactionId;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CashoutDataResponse [paymentTransactionId=");
		builder.append(paymentTransactionId);
		builder.append(", clientId=");
		builder.append(clientId);
		builder.append(", value=");
		builder.append(value);
		builder.append(", beneficiary=");
		builder.append(beneficiary);
		builder.append(", iban=");
		builder.append(iban);
		builder.append(", bic=");
		builder.append(bic);
		builder.append("]");
		return builder.toString();
	}

}
