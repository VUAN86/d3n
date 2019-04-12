package de.ascendro.f4m.service.voucher.model.uservoucher;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class UserVoucherGetRequest implements JsonMessageContent {

	private String userVoucherId;

	public UserVoucherGetRequest(String voucherId) {
		this.userVoucherId = voucherId;
	}

	public String getUserVoucherId() {
		return userVoucherId;
	}

	public void setUserVoucherId(String userVoucherId) {
		this.userVoucherId = userVoucherId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UserVoucherGetRequest [");
		builder.append("userVoucherId=").append(userVoucherId);
		builder.append("]");
		return builder.toString();
	}

}
