package de.ascendro.f4m.service.payment.model.external;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import de.ascendro.f4m.service.payment.model.TransactionId;
import sun.util.resources.cldr.ee.TimeZoneNames_ee;

import static de.ascendro.f4m.service.util.DateTimeUtil.TIMEZONE;

public class GetExternalPaymentResponse extends TransactionId {
	private BigDecimal amount;
	private String description;
	private String paymentToken;
	private PaymentTransactionType type;
	private PaymentTransactionState state;
	private ZonedDateTime created;
	private ZonedDateTime processed;


	public GetExternalPaymentResponse amount(BigDecimal amount) {
		this.amount = amount;
		return this;
	}

	public GetExternalPaymentResponse description(String description) {
		this.description = description;
		return this;
	}

	public GetExternalPaymentResponse created(LocalDateTime localDateTime) {
		this.created = localDateTime.atZone(TIMEZONE);
		return this;
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

	public String getPaymentToken() {
		return paymentToken;
	}

	public void setPaymentToken(String paymentToken) {
		this.paymentToken = paymentToken;
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
		builder.append("GetExternalPaymentResponse [amount=");
		builder.append(amount);
		builder.append(", description=");
		builder.append(description);
		builder.append(", paymentToken=");
		builder.append(paymentToken);
		builder.append(", type=");
		builder.append(type);
		builder.append(", state=");
		builder.append(state);
		builder.append(", created=");
		builder.append(created);
		builder.append(", processed=");
		builder.append(processed);
		builder.append(", transactionId=");
		builder.append(transactionId);
		builder.append("]");
		return builder.toString();
	}

}
