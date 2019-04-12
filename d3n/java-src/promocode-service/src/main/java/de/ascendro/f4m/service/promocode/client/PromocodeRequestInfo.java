package de.ascendro.f4m.service.promocode.client;

import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.request.RequestInfoImpl;

public class PromocodeRequestInfo extends RequestInfoImpl {
	private String promocodeId;
	private Currency currency;
	private String transactionLogId;

	public PromocodeRequestInfo() {
		this.promocodeId = null;
	}

	public String getTransactionLogId() {
		return transactionLogId;
	}

	public void setTransactionLogId(String transactionLogId) {
		this.transactionLogId = transactionLogId;
	}

	public String getPromocodeId() {
		return promocodeId;
	}

	public void setPromocodeId(String promocodeId) {
		this.promocodeId = promocodeId;
	}

	public Currency getCurrency() {
		return currency;
	}

	public void setCurrency(Currency currency) {
		this.currency = currency;
	}
}
