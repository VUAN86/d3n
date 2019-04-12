package de.ascendro.f4m.service.game.engine.model.joker;

import java.math.BigDecimal;

import de.ascendro.f4m.service.payment.model.Currency;

public class JokerInformation {

	private Integer availableCount;
	private boolean availableForCurrentQuestion;
	private boolean alreadyPurchasedForCurrentQuestion;
	private Currency currency;
	private BigDecimal price;

	public JokerInformation(Integer availableCount, boolean availableForCurrentQuestion, 
			boolean alreadyPurchasedForCurrentQuestion, Currency currency, BigDecimal price) {
		this.availableCount = availableCount;
		this.availableForCurrentQuestion = availableForCurrentQuestion;
		this.alreadyPurchasedForCurrentQuestion = alreadyPurchasedForCurrentQuestion;
		this.currency = currency;
		this.price = price;
	}

	/**
	 * <code>null</code> - unlimited.
	 */
	public Integer getAvailableCount() {
		return availableCount;
	}

	public boolean isAvailableForCurrentQuestion() {
		return availableForCurrentQuestion;
	}

	public boolean isAlreadyPurchasedForCurrentQuestion() {
		return alreadyPurchasedForCurrentQuestion;
	}
	
	public Currency getCurrency() {
		return currency;
	}

	public BigDecimal getPrice() {
		return price;
	}
	
}
