package de.ascendro.f4m.service.voucher.model.uservoucher;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class UserVoucherAssignResponse implements JsonMessageContent {

	private String userVoucherId;

	private String voucherId;

	public UserVoucherAssignResponse(String userVoucherId, String voucherId) {
		this.userVoucherId = userVoucherId;
		this.voucherId = voucherId;
	}

	public String getUserVoucherId() {
		return userVoucherId;
	}

	public void setVoucherId(String voucherId) {
		this.voucherId = voucherId;
	}

	public String getVoucherId() {

		return voucherId;
	}

	public void setUserVoucherId(String voucherId) {
		this.userVoucherId = voucherId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UserVoucherAssignResponse [");
		builder.append("userVoucherId=").append(userVoucherId);
		builder.append("]");
		return builder.toString();
	}
}
