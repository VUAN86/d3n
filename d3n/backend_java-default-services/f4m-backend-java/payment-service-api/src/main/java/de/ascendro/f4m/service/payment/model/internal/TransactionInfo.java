package de.ascendro.f4m.service.payment.model.internal;

import java.math.BigDecimal;

import de.ascendro.f4m.service.payment.model.TransferFundsRequest;

/**
 * Basic information about transaction.
 */
public class TransactionInfo implements TransferFundsRequest {
	protected String tenantId;
	protected String fromProfileId;
	protected String toProfileId;
	protected BigDecimal amount;
	protected PaymentDetails paymentDetails;

	public TransactionInfo() { }
	
	public TransactionInfo(String tenantId, String fromProfileId, String toProfileId, BigDecimal amount) {
		this.tenantId = tenantId;
		this.fromProfileId = fromProfileId;
		this.toProfileId = toProfileId;
		this.amount = amount;
	}
	
	@Override
	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	@Override
	public String getFromProfileId() {
		return fromProfileId;
	}

	public void setFromProfileId(String fromProfileId) {
		this.fromProfileId = fromProfileId;
	}

	@Override
	public String getToProfileId() {
		return toProfileId;
	}

	public void setToProfileId(String toProfileId) {
		this.toProfileId = toProfileId;
	}

	@Override
	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	@Override
	public PaymentDetails getPaymentDetails() {
		return paymentDetails;
	}

	public void setPaymentDetails(PaymentDetails paymentDetails) {
		this.paymentDetails = paymentDetails;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TransactionInfo [tenantId=");
		builder.append(tenantId);
		builder.append(", fromProfileId=");
		builder.append(fromProfileId);
		builder.append(", toProfileId=");
		builder.append(toProfileId);
		builder.append(", amount=");
		builder.append(amount);
		builder.append(", paymentDetails=");
		builder.append(paymentDetails);
		builder.append("]");
		return builder.toString();
	}
}
