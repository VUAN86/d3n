package de.ascendro.f4m.service.payment.model.internal;

import de.ascendro.f4m.service.payment.model.TransactionId;

public class GetTransactionRequest extends TransactionId {
	private String tenantId;

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GetTransactionRequest [tenantId=");
		builder.append(tenantId);
		builder.append(", transactionId=");
		builder.append(transactionId);
		builder.append("]");
		return builder.toString();
	}
}
