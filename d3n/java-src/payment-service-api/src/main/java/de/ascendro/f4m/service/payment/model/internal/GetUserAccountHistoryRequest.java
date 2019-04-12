package de.ascendro.f4m.service.payment.model.internal;

import java.time.ZonedDateTime;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.TransactionFilterType;

public class GetUserAccountHistoryRequest implements JsonMessageContent {
	//everything else is copy-paste from REST and also partially from FilterCriteria!
	protected Currency currency;
	protected Integer offset;
	protected Integer limit;
	protected ZonedDateTime startDate;
	protected ZonedDateTime endDate;
	protected TransactionFilterType typeFilter;

	public Currency getCurrency() {
		return currency;
	}

	public void setCurrency(Currency currency) {
		this.currency = currency;
	}

	public Integer getOffset() {
		return offset;
	}

	public void setOffset(Integer offset) {
		this.offset = offset;
	}

	public Integer getLimit() {
		return limit;
	}

	public void setLimit(Integer limit) {
		this.limit = limit;
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
		builder.append("GetUserAccountHistoryRequest [currency=");
		builder.append(currency);
		builder.append(", offset=");
		builder.append(offset);
		builder.append(", limit=");
		builder.append(limit);
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
