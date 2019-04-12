package de.ascendro.f4m.service.payment.model.external;

import de.ascendro.f4m.service.payment.model.TransactionId;

public class GetExternalPaymentRequest extends TransactionId {
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GetExternalPaymentRequest [transactionId=");
		builder.append(transactionId);
		builder.append("]");
		return builder.toString();
	}
}
