package de.ascendro.f4m.service.payment.rest.model;

import java.math.BigDecimal;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.ascendro.f4m.service.payment.model.external.PaymentTransactionType;

/**
 * Describes object in Paydent/Expolio API.
 */
@XmlRootElement
public class PaymentTransactionInitializationRest {
	@JsonProperty("AccountId")
	protected String accountId;
	@JsonProperty("Value")
	protected BigDecimal value;
	@JsonProperty("Rate")
	protected BigDecimal rate;
	@JsonProperty("Reference")
	protected String reference;
	@JsonProperty("Type")
	protected PaymentTransactionType type;
	@JsonProperty("CallbackUrlSuccess")
	protected String callbackUrlSuccess;
	@JsonProperty("CallbackUrlError")
	protected String callbackUrlError;
	@JsonProperty("RedirectUrlSuccess")
	private String redirectUrlSuccess;
	@JsonProperty("RedirectUrlError")
	private String redirectUrlError;
	@JsonProperty("CashoutData")
	private CashoutDataRequest cashoutData;

	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public BigDecimal getValue() {
		return value;
	}

	public void setValue(BigDecimal value) {
		this.value = value;
	}

	public BigDecimal getRate() {
		return rate;
	}

	public void setRate(BigDecimal rate) {
		this.rate = rate;
	}

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	public PaymentTransactionType getType() {
		return type;
	}

	public void setType(PaymentTransactionType type) {
		this.type = type;
	}

	public String getCallbackUrlSuccess() {
		return callbackUrlSuccess;
	}

	public void setCallbackUrlSuccess(String callbackUrlSuccess) {
		this.callbackUrlSuccess = callbackUrlSuccess;
	}

	public String getCallbackUrlError() {
		return callbackUrlError;
	}

	public void setCallbackUrlError(String callbackUrlError) {
		this.callbackUrlError = callbackUrlError;
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

	public CashoutDataRequest getCashoutData() {
		return cashoutData;
	}

	public void setCashoutData(CashoutDataRequest cashoutData) {
		this.cashoutData = cashoutData;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PaymentTransactionInitializationRest [accountId=");
		builder.append(accountId);
		builder.append(", value=");
		builder.append(value);
		builder.append(", rate=");
		builder.append(rate);
		builder.append(", reference=");
		builder.append(reference);
		builder.append(", type=");
		builder.append(type);
		builder.append(", callbackUrlSuccess=");
		builder.append(callbackUrlSuccess);
		builder.append(", callbackUrlError=");
		builder.append(callbackUrlError);
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
