package de.ascendro.f4m.service.payment.model.internal;

public class GetAccountHistoryResponse extends GetUserAccountHistoryResponse {

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
