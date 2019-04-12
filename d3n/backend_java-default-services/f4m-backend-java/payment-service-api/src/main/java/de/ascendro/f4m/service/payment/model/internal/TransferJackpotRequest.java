package de.ascendro.f4m.service.payment.model.internal;

import java.math.BigDecimal;

import de.ascendro.f4m.service.payment.model.TransferFundsRequest;

public class TransferJackpotRequest implements TransferFundsRequest {
	private String tenantId;
	private String multiplayerGameInstanceId;
	private String fromProfileId;
	private BigDecimal amount;
	private PaymentDetails paymentDetails;

	@Override
	public String getToProfileId() {
		return null; //no profile id for TransferJackpotRequest
	}

	@Override
	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getMultiplayerGameInstanceId() {
		return multiplayerGameInstanceId;
	}

	public void setMultiplayerGameInstanceId(String multiplayerGameInstanceId) {
		this.multiplayerGameInstanceId = multiplayerGameInstanceId;
	}

	@Override
	public String getFromProfileId() {
		return fromProfileId;
	}

	public void setFromProfileId(String fromProfileId) {
		this.fromProfileId = fromProfileId;
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
		builder.append("TransferJackpotRequest [tenantId=");
		builder.append(tenantId);
		builder.append(", multiplayerGameInstanceId=");
		builder.append(multiplayerGameInstanceId);
		builder.append(", fromProfileId=");
		builder.append(fromProfileId);
		builder.append(", amount=");
		builder.append(amount);
		builder.append(", paymentDetails=");
		builder.append(paymentDetails);
		builder.append("]");
		return builder.toString();
	}
}