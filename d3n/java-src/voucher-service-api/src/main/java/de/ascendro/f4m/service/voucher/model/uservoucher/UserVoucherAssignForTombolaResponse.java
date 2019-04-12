package de.ascendro.f4m.service.voucher.model.uservoucher;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class UserVoucherAssignForTombolaResponse implements JsonMessageContent {

	private String userVoucherId;

	public UserVoucherAssignForTombolaResponse(String userVoucherId) {
		this.userVoucherId = userVoucherId;
	}

	public String getUserVoucherId() {
		return userVoucherId;
	}

	public void setUserVoucherId(String voucherId) {
		this.userVoucherId = voucherId;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("UserVoucherAssignForTombolaResponse{");
		sb.append("userVoucherId='").append(userVoucherId).append('\'');
		sb.append('}');
		return sb.toString();
	}
}
