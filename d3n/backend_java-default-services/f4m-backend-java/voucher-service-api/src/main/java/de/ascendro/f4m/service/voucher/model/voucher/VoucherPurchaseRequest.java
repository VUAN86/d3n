package de.ascendro.f4m.service.voucher.model.voucher;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class VoucherPurchaseRequest implements JsonMessageContent {

	private String voucherId;

	public VoucherPurchaseRequest(String voucherId) {
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
		builder.append("VoucherPurchaseRequest [");
		builder.append("voucherId=").append(voucherId);
		builder.append("]");
		return builder.toString();
	}

}
