package de.ascendro.f4m.service.voucher.model.voucher;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.voucher.model.VoucherResponseModel;

public class VoucherGetResponse implements JsonMessageContent {

	private VoucherResponseModel voucher;

	public VoucherGetResponse(VoucherResponseModel voucher) {
		this.voucher = voucher;
	}

	public VoucherResponseModel getVoucher() {
		return voucher;
	}

	public void setVoucher(VoucherResponseModel voucher) {
		this.voucher = voucher;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("VoucherGetResponse [");
		builder.append("voucher=").append(voucher.toString());
		builder.append("]");
		return builder.toString();
	}

}
