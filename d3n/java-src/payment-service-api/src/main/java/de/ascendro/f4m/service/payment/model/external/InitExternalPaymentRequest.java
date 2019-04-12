package de.ascendro.f4m.service.payment.model.external;

import java.math.BigDecimal;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.payment.model.Currency;

public class InitExternalPaymentRequest implements JsonMessageContent {
	private Currency currency;
	private BigDecimal amount;
	private String description;
	private String redirectUrlSuccess;
	private String redirectUrlError;
	private CashoutData cashoutData;

	public Currency getCurrency() {
		return currency;
	}

	public void setCurrency(Currency currency) {
		this.currency = currency;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getRedirectUrlSuccess() {
		return redirectUrlSuccess;
	}

	public void setRedirectUrlSuccess(String redirectUrlSuccess) {
		this.redirectUrlSuccess = redirectUrlSuccess;
	}

	public String getRedirectUrlError() {
		return redirectUrlError;
	}

	public void setRedirectUrlError(String redirectUrlError) {
		this.redirectUrlError = redirectUrlError;
	}

	public CashoutData getCashoutData() {
		return cashoutData;
	}

	public void setCashoutData(CashoutData cashoutData) {
		this.cashoutData = cashoutData;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("InitExternalPaymentRequest [currency=");
		builder.append(currency);
		builder.append(", amount=");
		builder.append(amount);
		builder.append(", description=");
		builder.append(description);
		builder.append(", redirectUrlSuccess=");
		builder.append(redirectUrlSuccess);
		builder.append(", redirectUrlError=");
		builder.append(redirectUrlError);
		builder.append(", cashoutData=");
		builder.append(cashoutData);
		builder.append("]");
		return builder.toString();
	}

}
