package de.ascendro.f4m.service.payment.model.external;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class InitExternalPaymentResponse implements JsonMessageContent {
	private String forwardUrl;
	private String transactionId;
	private String paymentToken;

	public String getForwardUrl() {
		return forwardUrl;
	}

	public void setForwardUrl(String forwardUrl) {
		this.forwardUrl = forwardUrl;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	public String getPaymentToken() {
		return paymentToken;
	}

	public void setPaymentToken(String paymentToken) {
		this.paymentToken = paymentToken;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("InitExternalPaymentResponse [forwardUrl=");
		builder.append(forwardUrl);
		builder.append(", transactionId=");
		builder.append(transactionId);
		builder.append(", paymentToken=");
		builder.append(paymentToken);
		builder.append("]");
		return builder.toString();
	}
}
