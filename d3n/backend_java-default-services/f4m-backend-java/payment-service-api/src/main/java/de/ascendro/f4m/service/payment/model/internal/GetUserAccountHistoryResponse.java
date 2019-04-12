package de.ascendro.f4m.service.payment.model.internal;

import java.util.List;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class GetUserAccountHistoryResponse implements JsonMessageContent {
	protected List<GetTransactionResponse> items;
	protected Integer offset;
	protected Integer limit;
	protected long total;

	public List<GetTransactionResponse> getItems() {
		return items;
	}

	public void setItems(List<GetTransactionResponse> items) {
		this.items = items;
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

	public long getTotal() {
		return total;
	}

	public void setTotal(long total) {
		this.total = total;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GetUserAccountHistoryResponse [items=");
		builder.append(items);
		builder.append(", offset=");
		builder.append(offset);
		builder.append(", limit=");
		builder.append(limit);
		builder.append(", total=");
		builder.append(total);
		builder.append("]");
		return builder.toString();
	}
}
