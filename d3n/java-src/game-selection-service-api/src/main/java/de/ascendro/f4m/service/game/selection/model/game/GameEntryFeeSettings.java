package de.ascendro.f4m.service.game.selection.model.game;

import java.math.BigDecimal;

import com.google.gson.annotations.SerializedName;

public class GameEntryFeeSettings {

	private BigDecimal moneyMin;
	private BigDecimal moneyMax;
	private BigDecimal moneyIncrement;

	@SerializedName(value = "bonuspointsMin")
	private BigDecimal bonusMin;
	@SerializedName(value = "bonuspointsMax")
	private BigDecimal bonusMax;
	@SerializedName(value = "bonuspointsIncrement")
	private BigDecimal bonusIncrement;

	private BigDecimal creditsMin;
	private BigDecimal creditsMax;
	private BigDecimal creditsIncrement;

	public GameEntryFeeSettings() {
	}

	public BigDecimal getMoneyMin() {
		return moneyMin;
	}

	public void setMoneyMin(BigDecimal moneyMin) {
		this.moneyMin = moneyMin;
	}

	public BigDecimal getMoneyMax() {
		return moneyMax;
	}

	public void setMoneyMax(BigDecimal moneyMax) {
		this.moneyMax = moneyMax;
	}

	public BigDecimal getMoneyIncrement() {
		return moneyIncrement;
	}

	public void setMoneyIncrement(BigDecimal moneyIncrement) {
		this.moneyIncrement = moneyIncrement;
	}

	public BigDecimal getBonusMin() {
		return bonusMin;
	}

	public void setBonusMin(BigDecimal bonusMin) {
		this.bonusMin = bonusMin;
	}

	public BigDecimal getBonusMax() {
		return bonusMax;
	}

	public void setBonusMax(BigDecimal bonusMax) {
		this.bonusMax = bonusMax;
	}

	public BigDecimal getBonusIncrement() {
		return bonusIncrement;
	}

	public void setBonusIncrement(BigDecimal bonusIncrement) {
		this.bonusIncrement = bonusIncrement;
	}

	public BigDecimal getCreditsMin() {
		return creditsMin;
	}

	public void setCreditsMin(BigDecimal creditsMin) {
		this.creditsMin = creditsMin;
	}

	public BigDecimal getCreditsMax() {
		return creditsMax;
	}

	public void setCreditsMax(BigDecimal creditsMax) {
		this.creditsMax = creditsMax;
	}

	public BigDecimal getCreditsIncrement() {
		return creditsIncrement;
	}

	public void setCreditsIncrement(BigDecimal creditsIncrement) {
		this.creditsIncrement = creditsIncrement;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GameEntryFeeSettings [moneyMin=");
		builder.append(moneyMin);
		builder.append(", moneyMax=");
		builder.append(moneyMax);
		builder.append(", moneyIncrement=");
		builder.append(moneyIncrement);
		builder.append(", bonusMin=");
		builder.append(bonusMin);
		builder.append(", bonusMax=");
		builder.append(bonusMax);
		builder.append(", bonusIncrement=");
		builder.append(bonusIncrement);
		builder.append(", creditsMin=");
		builder.append(creditsMin);
		builder.append(", creditsMax=");
		builder.append(creditsMax);
		builder.append(", creditsIncrement=");
		builder.append(creditsIncrement);
		builder.append("]");
		return builder.toString();
	}

}
