package de.ascendro.f4m.service.payment.model.external;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class GetExchangeRatesResponse implements JsonMessageContent {

	private List<ExchangeRate> exchangeRates;

	public List<ExchangeRate> getExchangeRates() {
		if (exchangeRates == null) {
			exchangeRates = new ArrayList<ExchangeRate>();
		}
		return exchangeRates;
	}

	public void setExchangeRates(List<ExchangeRate> exchangeRateList) {
		this.exchangeRates = exchangeRateList;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GetExchangeRatesResponse {" + "exchangeRates=[");
		builder.append(StringUtils.join(exchangeRates, ", "));
		builder.append("]");
		return builder.toString();
	}

}
