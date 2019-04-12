package de.ascendro.f4m.service.payment.model;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class TransactionId implements JsonMessageContent {
	protected String transactionId;
	
	public TransactionId() {
	}

	public TransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TransactionId [transactionId=");
		builder.append(transactionId);
		builder.append("]");
		return builder.toString();
	}

}
