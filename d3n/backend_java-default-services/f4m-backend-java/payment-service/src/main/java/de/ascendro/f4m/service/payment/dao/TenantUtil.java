package de.ascendro.f4m.service.payment.dao;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.payment.model.external.ExchangeRate;

public class TenantUtil {

	private final static Logger LOGGER = LoggerFactory.getLogger(TenantUtil.class);

	public static List<ExchangeRate> filterExchangeRates(TenantInfo tenantInfo) {
		return tenantInfo.getExchangeRates().stream().filter(rate -> validateExchangeRate(tenantInfo, rate))
				.collect(Collectors.toList());
	}

	private static boolean validateExchangeRate(TenantInfo tenantInfo, ExchangeRate rate) {
		boolean isValid = true;
		if (rate.getToCurrency().equals(tenantInfo.getMainCurrency())) {
			LOGGER.error("Wrong Exchange Rate {} to {}. Did not load to Tenant info", rate.getFromCurrency(),
					rate.getToCurrency());
			isValid = false;
		}
		return isValid;
	}
}
