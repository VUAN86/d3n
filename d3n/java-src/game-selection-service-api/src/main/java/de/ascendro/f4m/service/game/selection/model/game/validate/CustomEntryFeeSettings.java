package de.ascendro.f4m.service.game.selection.model.game.validate;

import java.math.BigDecimal;

public class CustomEntryFeeSettings {

	private BigDecimal min;
	private BigDecimal max;
	private BigDecimal step;

	public CustomEntryFeeSettings(BigDecimal min, BigDecimal max, BigDecimal step) {
		this.min = min;
		this.max = max;
		this.step = step;
	}

	public BigDecimal getMin() {
		return min;
	}

	public void setMin(BigDecimal min) {
		this.min = min;
	}

	public BigDecimal getMax() {
		return max;
	}

	public void setMax(BigDecimal max) {
		this.max = max;
	}

	public BigDecimal getStep() {
		return step;
	}

	public void setStep(BigDecimal step) {
		this.step = step;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CustomEntryFeeSettings [min=");
		builder.append(min);
		builder.append(", max=");
		builder.append(max);
		builder.append(", step=");
		builder.append(step);
		builder.append("]");
		return builder.toString();
	}

}
