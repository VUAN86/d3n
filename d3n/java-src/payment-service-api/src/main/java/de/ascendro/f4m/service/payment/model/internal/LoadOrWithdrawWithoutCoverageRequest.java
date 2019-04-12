package de.ascendro.f4m.service.payment.model.internal;

import java.math.BigDecimal;

import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.TransferFundsRequest;

public class LoadOrWithdrawWithoutCoverageRequest implements TransferFundsRequest {

	protected String tenantId;
	protected String profileId;
	protected Currency currency;
	protected BigDecimal amount;
	protected PaymentDetails paymentDetails;

	@Override
	public String getFromProfileId() {
		if (amount.signum() < 0) {
			return profileId;
		} else {
			return null; 
		}
	}

	@Override
	public String getToProfileId() {
		if (amount.signum() > 0) {
			return profileId;
		} else {
			return null; 
		}
	}

	@Override
	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getProfileId() {
		return profileId;
	}

	public void setProfileId(String profileId) {
		this.profileId = profileId;
	}

	public Currency getCurrency() {
		return currency;
	}

	public void setCurrency(Currency currency) {
		this.currency = currency;
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
		builder.append("LoadOrWithdrawWithoutCoverageRequest [tenantId=");
		builder.append(tenantId);
		builder.append(", profileId=");
		builder.append(profileId);
		builder.append(", currency=");
		builder.append(currency);
		builder.append(", amount=");
		builder.append(amount);
		builder.append(", paymentDetails=");
		builder.append(paymentDetails);
		builder.append("]");
		return builder.toString();
	}
}
