package de.ascendro.f4m.service.game.selection.model.game;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class GameEntryFeeValues {

	@SerializedName(value = "money")
	private List<BigDecimal> money;
	@SerializedName(value = "bonusPoints")
	private List<BigDecimal> bonusPoints;
	@SerializedName(value = "credits")
	private List<BigDecimal> credits;

	public GameEntryFeeValues() {
	}

	public List<BigDecimal> getMoney() {
		return money;
	}

	public void setMoney(List<BigDecimal> money) {
		this.money = money;
	}

	public List<BigDecimal> getBonusPoints() {
		return bonusPoints;
	}

	public void setBonusPoints(List<BigDecimal> bonusPoints) {
		this.bonusPoints = bonusPoints;
	}

	public List<BigDecimal> getCredits() {
		return credits;
	}

	public void setCredits(List<BigDecimal> credits) {
		this.credits = credits;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GameEntryFeeValues [money=");
		builder.append(money.stream().map(Object::toString)
				.collect(Collectors.joining(", ")));
		builder.append(", bonusPoints=");
		builder.append(bonusPoints.stream().map(Object::toString)
				.collect(Collectors.joining(", ")));
		builder.append(", credits=");
		builder.append(credits.stream().map(Object::toString)
				.collect(Collectors.joining(", ")));
		builder.append("]");
		return builder.toString();
	}

}
