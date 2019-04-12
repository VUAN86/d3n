package de.ascendro.f4m.service.voucher.model.voucher;

import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class VoucherListRequest extends FilterCriteria implements JsonMessageContent {

	public static final int MAX_LIST_LIMIT = 200;
	private String tenantId;
	
	public VoucherListRequest() {
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("VoucherListRequest [");
		builder.append("limit=").append(Integer.toString(getLimit()));
		builder.append(", offset=").append(Long.toString(getOffset()));
		builder.append(", orderBy=").append(getOrderBy());
		builder.append(", searchBy=").append(getSearchBy());
		builder.append("]");
		return builder.toString();
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}
}
