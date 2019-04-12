package de.ascendro.f4m.service.payment.model;

import java.math.BigDecimal;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.payment.model.internal.PaymentDetails;

public interface TransferFundsRequest extends JsonMessageContent {
	public String getTenantId();

	public String getFromProfileId();

	public String getToProfileId();

	public BigDecimal getAmount();

	public PaymentDetails getPaymentDetails();
}
