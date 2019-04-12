package de.ascendro.f4m.service.payment.rest.model;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.ascendro.f4m.service.payment.json.JacksonDateDeserializer;
import de.ascendro.f4m.service.payment.json.JacksonDateSerializer;
import de.ascendro.f4m.service.payment.model.TransactionFilterType;

public class AccountHistoryRestSearchParams {
	private String accountId;
	private Integer limit;
	private Integer offset;
	@JsonDeserialize(using = JacksonDateDeserializer.class)
	@JsonSerialize(using = JacksonDateSerializer.class)
	private ZonedDateTime startDate;
	@JsonDeserialize(using = JacksonDateDeserializer.class)
	@JsonSerialize(using = JacksonDateSerializer.class)
	private ZonedDateTime endDate;
	private TransactionFilterType typeFilter;

	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public Integer getLimit() {
		return limit;
	}

	public void setLimit(Integer limit) {
		this.limit = limit;
	}

	public Integer getOffset() {
		return offset;
	}

	public void setOffset(Integer offset) {
		this.offset = offset;
	}

	public ZonedDateTime getStartDate() {
		return startDate;
	}

	public void setStartDate(ZonedDateTime startDate) {
		this.startDate = startDate;
	}

	public ZonedDateTime getEndDate() {
		return endDate;
	}

	public void setEndDate(ZonedDateTime endDate) {
		this.endDate = endDate;
	}

	public TransactionFilterType getTypeFilter() {
		return typeFilter;
	}

	public void setTypeFilter(TransactionFilterType typeFilter) {
		this.typeFilter = typeFilter;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AccountHistorySearchParams [accountId=");
		builder.append(accountId);
		builder.append(", limit=");
		builder.append(limit);
		builder.append(", offset=");
		builder.append(offset);
		builder.append(", startDate=");
		builder.append(startDate);
		builder.append(", endDate=");
		builder.append(endDate);
		builder.append(", typeFilter=");
		builder.append(typeFilter);
		builder.append("]");
		return builder.toString();
	}
}
