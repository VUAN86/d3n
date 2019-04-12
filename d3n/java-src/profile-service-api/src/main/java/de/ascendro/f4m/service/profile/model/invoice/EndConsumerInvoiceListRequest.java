package de.ascendro.f4m.service.profile.model.invoice;

import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

/**
 * End Consumer Invoice List request.
 */
public class EndConsumerInvoiceListRequest extends FilterCriteria implements JsonMessageContent {

	public static final int MAX_LIST_LIMIT = 100;

	public EndConsumerInvoiceListRequest() {
		this(MAX_LIST_LIMIT, 0);
	}

	public EndConsumerInvoiceListRequest(int limit, long offset) {
		setLimit(limit);
		setOffset(offset);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("EndConsumerInvoiceListRequest [");
		builder.append("limit=").append(Integer.toString(getLimit()));
		builder.append(", offset=").append(Long.toString(getOffset()));
		builder.append(", orderBy=").append(getOrderBy());
		builder.append(", searchBy=").append(getSearchBy());
		builder.append("]");
		return builder.toString();
	}

}
