package de.ascendro.f4m.service.payment.model.internal;

import java.math.BigDecimal;
import java.util.List;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class CloseJackpotRequest implements JsonMessageContent {

	private String tenantId;
	private String multiplayerGameInstanceId;
	private PaymentDetails paymentDetails;
	private List<PayoutItem> payouts;
	private JsonMessageContent responseToForward;

	public String getMultiplayerGameInstanceId() {
		return multiplayerGameInstanceId;
	}

	public void setMultiplayerGameInstanceId(String multiplayerGameInstanceId) {
		this.multiplayerGameInstanceId = multiplayerGameInstanceId;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public PaymentDetails getPaymentDetails() {
		return paymentDetails;
	}

	public void setPaymentDetails(PaymentDetails paymentDetails) {
		this.paymentDetails = paymentDetails;
	}

	public List<PayoutItem> getPayouts() {
		return payouts;
	}

	public void setPayouts(List<PayoutItem> payouts) {
		this.payouts = payouts;
	}

	public JsonMessageContent getResponseToForward() {
		return responseToForward;
	}

	public void setResponseToForward(JsonMessageContent responseToForward) {
		this.responseToForward = responseToForward;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CloseJackpotRequest [tenantId=");
		builder.append(tenantId);
		builder.append(", multiplayerGameInstanceId=");
		builder.append(multiplayerGameInstanceId);
		builder.append(", paymentDetails=");
		builder.append(paymentDetails);
		builder.append(", payouts=");
		builder.append(payouts);
		builder.append("]");
		return builder.toString();
	}

	public static class PayoutItem {
		private String profileId;
		private BigDecimal amount;
		
		public PayoutItem() {
			//empty constructor
		}

		public PayoutItem(String profileId, BigDecimal amount) {
			this.profileId = profileId;
			this.amount = amount;
		}

		public String getProfileId() {
			return profileId;
		}

		public void setProfileId(String profileId) {
			this.profileId = profileId;
		}

		public BigDecimal getAmount() {
			return amount;
		}

		public void setAmount(BigDecimal amount) {
			this.amount = amount;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("PayoutItem [profileId=");
			builder.append(profileId);
			builder.append(", amount=");
			builder.append(amount);
			builder.append("]");
			return builder.toString();
		}
	}
}
