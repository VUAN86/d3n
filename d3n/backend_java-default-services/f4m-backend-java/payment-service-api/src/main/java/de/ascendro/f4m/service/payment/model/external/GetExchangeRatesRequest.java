package de.ascendro.f4m.service.payment.model.external;

import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;

public class GetExchangeRatesRequest extends EmptyJsonMessageContent {

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GetExchangeRatesRequest []");
		return builder.toString();
	}
}
