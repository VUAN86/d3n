package de.ascendro.f4m.service.payment.rest.model;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.ascendro.f4m.service.payment.json.JacksonDateDeserializer;
import de.ascendro.f4m.service.payment.json.JacksonDateSerializer;
import de.ascendro.f4m.service.payment.model.external.PaymentTransactionState;
import de.ascendro.f4m.service.payment.model.external.PaymentTransactionType;

/**
 * Describes object in Paydent/Expolio API.
 */
@XmlRootElement
public class PaymentTransactionRest {
	@JsonProperty("CashoutData")
	protected CashoutDataResponse cashoutData;
	@JsonProperty("Id")
	protected String id;
	@JsonProperty("AccountId")
	protected String accountId;
	@JsonProperty("Value")
	protected BigDecimal value;
	@JsonProperty("Rate")
	protected BigDecimal rate;
	@JsonProperty("Reference")
	protected String reference;
	@JsonProperty("CallbackUrlSuccess")
	protected String callbackUrlSuccess;
	@JsonProperty("CallbackUrlError")
	protected String callbackUrlError;
	@JsonProperty("RedirectUrlSuccess")
	private String redirectUrlSuccess;
	@JsonProperty("RedirectUrlError")
	private String redirectUrlError;
	@JsonProperty("AccountTransactionId")
	protected String accountTransactionId;
	@JsonProperty("PaymentToken")
	protected String paymentToken;
	@JsonProperty("Module")
	protected String module;
	@JsonProperty("Type")
	protected PaymentTransactionType type;
	@JsonProperty("State")
	protected PaymentTransactionState state;
	@JsonProperty("Created")
	@JsonDeserialize(using = JacksonDateDeserializer.class)
	@JsonSerialize(using = JacksonDateSerializer.class)
	protected ZonedDateTime created;
	@JsonProperty("Processed")
	@JsonDeserialize(using = JacksonDateDeserializer.class)
	@JsonSerialize(using = JacksonDateSerializer.class)
	protected ZonedDateTime processed;

	public CashoutDataResponse getCashoutData() {
		return cashoutData;
	}

	public void setCashoutData(CashoutDataResponse cashoutData) {
		this.cashoutData = cashoutData;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

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

	public String getAccountTransactionId() {
		return accountTransactionId;
	}

	public void setAccountTransactionId(String accountTransactionId) {
		this.accountTransactionId = accountTransactionId;
	}

	public String getPaymentToken() {
		return paymentToken;
	}

	public void setPaymentToken(String paymentToken) {
		this.paymentToken = paymentToken;
	}

	public String getModule() {
		return module;
	}

	public void setModule(String module) {
		this.module = module;
	}

	public PaymentTransactionType getType() {
		return type;
	}

	public void setType(PaymentTransactionType type) {
		this.type = type;
	}

	public PaymentTransactionState getState() {
		return state;
	}

	public void setState(PaymentTransactionState state) {
		this.state = state;
	}

	public ZonedDateTime getCreated() {
		return created;
	}

	public void setCreated(ZonedDateTime created) {
		this.created = created;
	}

	public ZonedDateTime getProcessed() {
		return processed;
	}

	public void setProcessed(ZonedDateTime processed) {
		this.processed = processed;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PaymentTransactionRest [cashoutData=");
		builder.append(cashoutData);
		builder.append(", id=");
		builder.append(id);
		builder.append(", accountId=");
		builder.append(accountId);
		builder.append(", value=");
		builder.append(value);
		builder.append(", rate=");
		builder.append(rate);
		builder.append(", reference=");
		builder.append(reference);
		builder.append(", callbackUrlSuccess=");
		builder.append(callbackUrlSuccess);
		builder.append(", callbackUrlError=");
		builder.append(callbackUrlError);
		builder.append(", redirectUrlSuccess=");
		builder.append(redirectUrlSuccess);
		builder.append(", redirectUrlError=");
		builder.append(redirectUrlError);
		builder.append(", accountTransactionId=");
		builder.append(accountTransactionId);
		builder.append(", paymentToken=");
		builder.append(paymentToken);
		builder.append(", module=");
		builder.append(module);
		builder.append(", type=");
		builder.append(type);
		builder.append(", state=");
		builder.append(state);
		builder.append(", created=");
		builder.append(created);
		builder.append(", processed=");
		builder.append(processed);
		builder.append("]");
		return builder.toString();
	}
}
