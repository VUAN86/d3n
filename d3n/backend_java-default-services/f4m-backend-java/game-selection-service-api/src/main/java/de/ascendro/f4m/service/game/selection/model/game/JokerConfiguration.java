package de.ascendro.f4m.service.game.selection.model.game;

import java.math.BigDecimal;

import de.ascendro.f4m.service.payment.model.Currency;

public class JokerConfiguration {

	private JokerType type;
	private boolean enabled;
	private Integer availableCount;
	private BigDecimal price;
	private Currency currency;
	private Integer displayTime;
	
	/**
	 * if availableCount is null, it means unlimited uses of the joker type
	 */
	public Integer getAvailableCount() {
		if (! enabled) {
			return 0;
		}
		JokerType jokerType = getType();
		if (jokerType != null && jokerType.getMaxLimit() != null) {
			// There is a maximum limit for a joker
			return availableCount == null || availableCount > jokerType.getMaxLimit() 
					? jokerType.getMaxLimit() : availableCount;
		}
		return availableCount;
	}

	public void setAvailableCount(Integer availableCount) {
		this.availableCount = availableCount;
	}
	
	public JokerType getType() {
		return type;
	}

	public void setType(JokerType type) {
		this.type = type;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public Currency getCurrency() {
		return currency;
	}

	public void setCurrency(Currency currency) {
		this.currency = currency;
	}

	public Integer getDisplayTime() {
		return displayTime;
	}

	public void setDisplayTime(Integer displayTime) {
		this.displayTime = displayTime;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("JokerConfiguration [type=");
		builder.append(type);
		builder.append(", enabled=");
		builder.append(enabled);
		builder.append(", availableCount=");
		builder.append(availableCount);
		builder.append(", price=");
		builder.append(price);
		builder.append(", currency=");
		builder.append(currency);
		builder.append(", displayTime=");
		builder.append(displayTime);
		builder.append("]");
		return builder.toString();
	}
}
