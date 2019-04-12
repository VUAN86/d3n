package de.ascendro.f4m.service.game.selection.model.game;

import java.math.BigDecimal;

import de.ascendro.f4m.service.payment.model.Currency;

public class GameWinningComponentListItem implements Comparable<GameWinningComponentListItem>{

	private String winningComponentId;
	private Boolean isPaid;
	private BigDecimal amount;
	private Currency currency;
	private Integer rightAnswerPercentage;

	public GameWinningComponentListItem() {
	}

	public GameWinningComponentListItem(String winningComponentId, boolean isPaid, BigDecimal amount, Currency currency, int rightAnswerPercentage) {
		this.winningComponentId = winningComponentId;
		this.isPaid = isPaid;
		this.amount = amount;
		this.currency = currency;
		this.rightAnswerPercentage = rightAnswerPercentage;
	}
	
	public String getWinningComponentId() {
		return winningComponentId;
	}

	public boolean isPaid() {
		return isPaid == null ? false : isPaid;
	}

	public BigDecimal getAmount() {
		return amount == null ? BigDecimal.ZERO : amount;
	}

	public Currency getCurrency() {
		return currency;
	}

	public int getRightAnswerPercentage() {
		return rightAnswerPercentage == null ? 100 : rightAnswerPercentage;
	}

	@Override
	public int compareTo(GameWinningComponentListItem o) {
		int paidCompare = this.isPaid.compareTo(o.isPaid);
		if (paidCompare == 0) {
			return this.winningComponentId.compareTo(o.getWinningComponentId());
		} else {
			return paidCompare * -1;
		}
	}

	@Override
	public String toString() {
		return "GameWinningComponentListItem{" +
				"winningComponentId='" + winningComponentId + '\'' +
				", isPaid=" + isPaid +
				", amount=" + amount +
				", currency=" + currency +
				", rightAnswerPercentage=" + rightAnswerPercentage +
				'}';
	}
}
