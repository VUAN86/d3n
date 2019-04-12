package de.ascendro.f4m.service.payment.notification;

import java.io.Serializable;
import java.math.BigDecimal;

import de.ascendro.f4m.service.usermessage.model.WebsocketMessageType;
import de.ascendro.f4m.service.usermessage.notification.MobilePushJsonNotification;

/**
 * Contents of "payload" for {@link WebsocketMessageType#PAYMENT_SUCCESS} and {@link WebsocketMessageType#PAYMENT_FAILURE} 
 */
public class PaymentCallbackMessagePayload extends MobilePushJsonNotification implements Serializable {
	private static final long serialVersionUID = 1154194129329149186L;
	
	private String transactionId;
	private BigDecimal amount;
	private String currency;
	private transient WebsocketMessageType type; //won't be serialized to JSON
	
	public PaymentCallbackMessagePayload(WebsocketMessageType type) {
		this.type = type;
	}

	@Override
	public WebsocketMessageType getType() {
		return type;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((amount == null) ? 0 : amount.hashCode());
		result = prime * result + ((currency == null) ? 0 : currency.hashCode());
		result = prime * result + ((transactionId == null) ? 0 : transactionId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		PaymentCallbackMessagePayload other = (PaymentCallbackMessagePayload) obj;
		if (amount == null) {
			if (other.amount != null)
				return false;
		} else if (!amount.equals(other.amount))
			return false;
		if (currency == null) {
			if (other.currency != null)
				return false;
		} else if (!currency.equals(other.currency))
			return false;
		if (transactionId == null) {
			if (other.transactionId != null)
				return false;
		} else if (!transactionId.equals(other.transactionId))
			return false;
		return true;
	}
}
