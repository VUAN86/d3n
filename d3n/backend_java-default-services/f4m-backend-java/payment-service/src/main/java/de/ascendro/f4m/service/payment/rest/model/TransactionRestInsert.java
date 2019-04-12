package de.ascendro.f4m.service.payment.rest.model;

import java.math.BigDecimal;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Describes object in Paydent/Expolio API.
 */
@XmlRootElement
@JsonInclude(JsonInclude.Include.NON_NULL)
//annotation necessary to serialise only existing attributes (leave out DebitorAccountId for credit operations)
public class TransactionRestInsert {
	@JsonProperty("DebitorAccountId")
	protected String debitorAccountId;
	@JsonProperty("CreditorAccountId")
	protected String creditorAccountId;
	@JsonProperty("Value")
	protected BigDecimal value;
	@JsonProperty("UsedExchangeRate")
	protected BigDecimal usedExchangeRate;
	@JsonProperty("Type")
	protected TransactionType type;
	@JsonProperty("Reference")
	protected String reference;

	public String getDebitorAccountId() {
		return debitorAccountId;
	}

	public void setDebitorAccountId(String debitorAccountId) {
		this.debitorAccountId = debitorAccountId;
	}

	public String getCreditorAccountId() {
		return creditorAccountId;
	}

	public void setCreditorAccountId(String creditorAccountId) {
		this.creditorAccountId = creditorAccountId;
	}

	public BigDecimal getValue() {
		return value;
	}

	public void setValue(BigDecimal value) {
		this.value = value;
	}

	public BigDecimal getUsedExchangeRate() {
		return usedExchangeRate;
	}

	public void setUsedExchangeRate(BigDecimal usedExchangeRate) {
		this.usedExchangeRate = usedExchangeRate;
	}

	public TransactionType getType() {
		return type;
	}

	public void setType(TransactionType type) {
		this.type = type;
	}

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TransactionRestInsert [debitorAccountId=");
		builder.append(debitorAccountId);
		builder.append(", creditorAccountId=");
		builder.append(creditorAccountId);
		builder.append(", value=");
		builder.append(value);
		builder.append(", usedExchangeRate=");
		builder.append(usedExchangeRate);
		builder.append(", type=");
		builder.append(type);
		builder.append(", reference=");
		builder.append(reference);
		builder.append("]");
		return builder.toString();
	}
}
