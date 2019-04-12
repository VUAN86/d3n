package de.ascendro.f4m.service.payment.rest.model;

public class AccountHistoryRest extends ListResponseRest<TransactionRest> {

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AccountHistoryRest [limit=");
		builder.append(limit);
		builder.append(", offset=");
		builder.append(offset);
		builder.append(", totalCount=");
		builder.append(total);
		builder.append(", data=");
		builder.append(data);
		builder.append("]");
		return builder.toString();
	}

}
