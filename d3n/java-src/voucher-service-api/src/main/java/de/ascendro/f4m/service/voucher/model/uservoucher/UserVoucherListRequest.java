package de.ascendro.f4m.service.voucher.model.uservoucher;

import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.UserIdentifier;

public class UserVoucherListRequest extends FilterCriteria implements JsonMessageContent, UserIdentifier {

	public static final int MAX_LIST_LIMIT=200;

	public UserVoucherListRequest() {
		// Empty User Voucher List Request constructor
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UserVoucherListRequest [");
		builder.append("]");
		return builder.toString();
	}

	@Override
	public String getUserId() {
		return null;
	}
}
