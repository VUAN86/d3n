package de.ascendro.f4m.service.voucher.model.uservoucher;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.voucher.model.UserVoucherResponseModel;

public class UserVoucherGetResponse implements JsonMessageContent {

	private UserVoucherResponseModel userVoucher;

	public UserVoucherGetResponse(UserVoucherResponseModel userVoucher) {
		this.userVoucher = userVoucher;
	}

	public UserVoucherResponseModel getVoucher() {
		return userVoucher;
	}

	public void setVoucher(UserVoucherResponseModel voucher) {
		this.userVoucher = voucher;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UserVoucherGetResponse [");
		builder.append("userVoucher=").append(userVoucher.toString());
		builder.append("]");
		return builder.toString();
	}

}
