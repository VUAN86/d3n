package de.ascendro.f4m.service.payment.model.external;

import de.ascendro.f4m.service.payment.model.TransactionId;

public class ConvertBetweenCurrenciesUserResponse extends TransactionId {

	public ConvertBetweenCurrenciesUserResponse(String transactionId) {
		super(transactionId);
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ConvertBetweenCurrenciesUserResponse [transactionId=");
		builder.append(transactionId);
		builder.append("]");
		return builder.toString();
	}

}
