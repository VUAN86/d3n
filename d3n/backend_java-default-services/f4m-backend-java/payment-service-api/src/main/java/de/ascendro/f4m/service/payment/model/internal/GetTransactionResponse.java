package de.ascendro.f4m.service.payment.model.internal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.payment.model.Currency;

import static de.ascendro.f4m.service.util.DateTimeUtil.TIMEZONE;

public class GetTransactionResponse extends TransactionInfo implements JsonMessageContent {
	protected String id;
	protected Currency fromCurrency;
	protected Currency toCurrency;
	protected String type; //enum? How to link it with de.ascendro.f4m.service.payment.rest.model.TransactionType ?
	protected ZonedDateTime creationDate;


	public GetTransactionResponse amount(BigDecimal amount) {
		this.amount = amount;
		return this;
	}

	public GetTransactionResponse creationDate(LocalDateTime localDateTime) {
		this.creationDate = localDateTime.atZone(TIMEZONE);
		return this;
	}

	public GetTransactionResponse type(String type) {
		this.type = type;
		return this;
	}

	public GetTransactionResponse transactionId(String transactionId) {
		this.id = transactionId;
		return this;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Currency getFromCurrency() {
		return fromCurrency;
	}

	public void setFromCurrency(Currency fromCurrency) {
		this.fromCurrency = fromCurrency;
	}

	public Currency getToCurrency() {
		return toCurrency;
	}

	public void setToCurrency(Currency toCurrency) {
		this.toCurrency = toCurrency;
	}

	public ZonedDateTime getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(ZonedDateTime creationDate) {
		this.creationDate = creationDate;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GetTransactionResponse [type=");
		builder.append(type);
		builder.append(", id=");
		builder.append(id);
		builder.append(", tenantId=");
		builder.append(tenantId);
		builder.append(", fromProfileId=");
		builder.append(fromProfileId);
		builder.append(", toProfileId=");
		builder.append(toProfileId);
		builder.append(", fromCurrency=");
		builder.append(fromCurrency);
		builder.append(", toCurrency=");
		builder.append(toCurrency);
		builder.append(", amount=");
		builder.append(amount);
		builder.append(", creationDate=");
		builder.append(creationDate);
		builder.append(", paymentDetails=");
		builder.append(paymentDetails);
		builder.append("]");
		return builder.toString();
	}

}
