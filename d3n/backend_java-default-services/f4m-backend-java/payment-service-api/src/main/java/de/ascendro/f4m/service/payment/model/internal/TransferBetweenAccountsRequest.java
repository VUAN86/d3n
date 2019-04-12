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
		return "TransferBetweenAccountsRequest{" +
				"currency=" + currency +
				", tenantId='" + tenantId + '\'' +
				", fromProfileId='" + fromProfileId + '\'' +
				", toProfileId='" + toProfileId + '\'' +
				", amount=" + amount +
				", paymentDetails=" + paymentDetails +
				"} " + super.toString();
	}
}
