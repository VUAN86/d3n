package de.ascendro.f4m.service.game.selection.model.game.validate;


import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class CustomEntryFeeValues {

	private List<BigDecimal> values;

	public CustomEntryFeeValues(List<BigDecimal> values) {
		this.values = values;
	}

	public List<BigDecimal> getValues() {
		return values;
	}

	public void setValues(List<BigDecimal> values) {
		this.values = values;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CustomEntryFeeValues [values=");
		builder.append(values.stream().map(Object::toString)
				.collect(Collectors.joining(", ")));
		builder.append("]");
		return builder.toString();
	}

}
