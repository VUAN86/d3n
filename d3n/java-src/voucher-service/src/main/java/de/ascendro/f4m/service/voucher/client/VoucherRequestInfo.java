package de.ascendro.f4m.service.voucher.client;

import de.ascendro.f4m.service.request.RequestInfoImpl;

public class VoucherRequestInfo extends RequestInfoImpl {
	private String voucherId;
	private String transactionLogId;

	public VoucherRequestInfo(String voucherId) {
		this.voucherId = voucherId;
	}
	
	public String getVoucherId() {
		return voucherId;
	}

	public String getTransactionLogId() {
		return transactionLogId;
	}

	public void setTransactionLogId(String transactionLogId) {
		this.transactionLogId = transactionLogId;
	}
}
