package de.ascendro.f4m.service.payment.rest.wrapper;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.core.GenericType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.di.RestClientProvider;
import de.ascendro.f4m.service.payment.rest.model.ExchangeRateRest;
import de.ascendro.f4m.service.payment.rest.model.ExchangeRateRestInsert;
import de.ascendro.f4m.service.payment.rest.model.ExchangeRateRestUpdate;

public class ExchangeRateRestWrapper extends RestWrapper {
	private static final Logger LOGGER = LoggerFactory.getLogger(ExchangeRateRestWrapper.class);
	public static final String URI_PATH = "exchangeRates";
	
	@Inject
	public ExchangeRateRestWrapper(@Assisted String tenantId, RestClientProvider restClientProvider,
			PaymentConfig paymentConfig, LoggingUtil loggingUtil) {
		super(tenantId, restClientProvider, paymentConfig, loggingUtil);
	}

	public List<ExchangeRateRest> getExchangeRates() {
		List<ExchangeRateRest> exchangeRates = callGet(new GenericType<List<ExchangeRateRest>>(){});
		LOGGER.debug("exchangeRates {}", exchangeRates);
		return exchangeRates;
	}

	public ExchangeRateRest insertExchangeRate(ExchangeRateRestInsert exchangeRate) {
		ExchangeRateRest entity = callPost(exchangeRate, ExchangeRateRest.class);
		LOGGER.info("ExchangeRate inserted {}", entity);
		return entity;
	}

	public ExchangeRateRest updateExchangeRate(ExchangeRateRestUpdate exchangeRate) {
		ExchangeRateRest entity = callPut(exchangeRate, ExchangeRateRest.class);
		LOGGER.info("ExchangeRate updated {}", entity);
		return entity;
	}

	@Override
	protected String getUriPath() {
		return URI_PATH;
	}
}
