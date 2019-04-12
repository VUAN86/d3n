package de.ascendro.f4m.service.voucher.model.uservoucher;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class UserVoucherReleaseRequest implements JsonMessageContent {

	private String voucherId;

	public UserVoucherReleaseRequest(String voucherId) {
		this.voucherId = voucherId;
	}

	public String getVoucherId() {
		return voucherId;
	}

	public void setVoucherId(String voucherId) {
		this.voucherId = voucherId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UserVoucherReleaseRequest [");
		builder.append("voucherId=").append(voucherId);
		builder.append("]");
		return builder.toString();
	}

}
