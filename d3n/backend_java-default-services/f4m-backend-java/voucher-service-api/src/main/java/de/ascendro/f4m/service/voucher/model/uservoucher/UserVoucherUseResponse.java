package de.ascendro.f4m.service.voucher.model.uservoucher;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class UserVoucherUseResponse implements JsonMessageContent {

	private String userVoucherId;

	public UserVoucherUseResponse(String userVoucher) {
		this.userVoucherId = userVoucher;
	}

	public String getUserVoucherId() {
		return userVoucherId;
	}

	public void setVoucher(String userVoucherId) {
		this.userVoucherId = userVoucherId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UserVoucherUseResponse [");
		builder.append("userVoucherId=").append(userVoucherId);
		builder.append("]");
		return builder.toString();
	}

}
