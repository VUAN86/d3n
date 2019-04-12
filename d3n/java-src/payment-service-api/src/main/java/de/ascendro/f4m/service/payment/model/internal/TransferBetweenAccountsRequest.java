package de.ascendro.f4m.service.payment.model.internal;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.payment.model.Currency;

public class TransferBetweenAccountsRequest extends TransactionInfo implements JsonMessageContent {
	protected Currency currency;

	public Currency getCurrency() {
		return currency;
	}

	public void setCurrency(Currency currency) {
		this.currency = currency;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TransferBetweenAccountsRequest [tenantId=");
		builder.append(tenantId);
		builder.append(", fromProfileId=");
		builder.append(fromProfileId);
		builder.append(", toProfileId=");
		builder.append(toProfileId);
		builder.append(", currency=");
		builder.append(currency);
		builder.append(", amount=");
		builder.append(amount);
		builder.append(", paymentDetails=");
		builder.append(paymentDetails);
		builder.append("]");
		return builder.toString();
	}
}
