package de.ascendro.f4m.service.payment.dao;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.ascendro.f4m.service.payment.model.external.ExchangeRate;

/**
 * Tenant data structure as expected to be stored in Aerospike
 */
public class TenantInfo {
	private String tenantId;
	private String apiId;
	private String mainCurrency;
	private List<ExchangeRate> exchangeRates;

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getApiId() {
		return apiId;
	}

	public void setApiId(String apiId) {
		this.apiId = apiId;
	}

	public String getMainCurrency() {
		return mainCurrency;
	}

	public void setMainCurrency(String mainCurrency) {
		this.mainCurrency = mainCurrency;
	}

	public List<ExchangeRate> getExchangeRates() {
		return exchangeRates;
	}

	public void setExchangeRates(List<ExchangeRate> exchangeRates) {
		this.exchangeRates = exchangeRates;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TenantInfo [tenantId=");
		builder.append(tenantId);
		builder.append(", apiId=");
		builder.append(apiId);
		builder.append(", mainCurrency=");
		builder.append(mainCurrency);
		builder.append(", exchangeRates=[");
		builder.append(StringUtils.join(exchangeRates, ", "));
		builder.append("]]");
		return builder.toString();
	}

}
