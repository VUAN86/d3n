package de.ascendro.f4m.service.payment.model.internal;

import java.util.List;

public class GetAccountHistoryResponse extends GetUserAccountHistoryResponse {


	public GetAccountHistoryResponse items(List<GetTransactionResponse> items) {
		this.setItems(items);
		return this;
	}

	public GetAccountHistoryResponse offset(Integer offset) {
		this.offset = offset;
		return this;
	}

	public GetAccountHistoryResponse limit(Integer limit) {
		this.limit = limit;
		return this;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GetAccountHistoryResponse [items=");
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
