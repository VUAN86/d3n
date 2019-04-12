package de.ascendro.f4m.service.payment.rest.model;

import java.time.ZonedDateTime;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.ascendro.f4m.service.payment.json.JacksonDateDeserializer;
import de.ascendro.f4m.service.payment.json.JacksonDateSerializer;
import de.ascendro.f4m.service.payment.json.JsonSerializerParameter;
import de.ascendro.f4m.service.util.DateTimeUtil;

/**
 * Describes object in Paydent/Expolio API.
 */
@XmlRootElement
public class TransactionRest extends TransactionRestInsert {
	@JsonProperty("CreditorAccount")
	private AccountRest creditorAccount;
	@JsonProperty("DebitorAccount")
	private AccountRest debitorAccount;
	@JsonProperty("Rate")
	private ExchangeRateRest rate;
	@JsonProperty("Id")
	private String id;
	@JsonProperty("Created")
	@JsonDeserialize(using = JacksonDateDeserializer.class)
	@JsonSerialize(using = JacksonDateSerializer.class)
	@JsonSerializerParameter(format = DateTimeUtil.JSON_DATETIME_FORMAT)
	private ZonedDateTime created;

	public AccountRest getCreditorAccount() {
		return creditorAccount;
	}

	public void setCreditorAccount(AccountRest creditorAccount) {
		this.creditorAccount = creditorAccount;
	}

	public AccountRest getDebitorAccount() {
		return debitorAccount;
	}

	public void setDebitorAccount(AccountRest debitorAccount) {
		this.debitorAccount = debitorAccount;
	}

	public ExchangeRateRest getRate() {
		return rate;
	}

	public void setRate(ExchangeRateRest rate) {
		this.rate = rate;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public ZonedDateTime getCreated() {
		return created;
	}

	public void setCreated(ZonedDateTime created) {
		this.created = created;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TransactionRest [debitorAccountId=");
		builder.append(debitorAccountId);
		builder.append(", creditorAccount=");
		builder.append(creditorAccount);
		builder.append(", creditorAccountId=");
		builder.append(creditorAccountId);
		builder.append(", debitorAccount=");
		builder.append(debitorAccount);
		builder.append(", rate=");
		builder.append(rate);
		builder.append(", id=");
		builder.append(id);
		builder.append(", created=");
		builder.append(created);
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
