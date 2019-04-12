package de.ascendro.f4m.service.payment.rest.wrapper;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.core.GenericType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.di.RestClientProvider;
import de.ascendro.f4m.service.payment.rest.model.CurrencyRest;
import de.ascendro.f4m.service.payment.rest.model.CurrencyRestInsert;

/**
 * Wraps currency related calls to Paydent API.
 * 
 * Keep in mind that there is a trick - API allows listing and updating only currencies defined from F4M.
 * But in the Payment System there always is a built-in currency EUR.
 */
public class CurrencyRestWrapper extends RestWrapper {
	private static final Logger LOGGER = LoggerFactory.getLogger(CurrencyRestWrapper.class);
	public static final String URI_PATH = "currencies";

	@Inject
	public CurrencyRestWrapper(@Assisted String tenantId, RestClientProvider restClientProvider,
			PaymentConfig paymentConfig, LoggingUtil loggingUtil) {
		super(tenantId, restClientProvider, paymentConfig, loggingUtil);
	}
	
	public List<CurrencyRest> getCurrencies() {
		List<CurrencyRest> currencies = callGet(new GenericType<List<CurrencyRest>>(){});
		LOGGER.debug("currencies {}", currencies);
		return currencies;
	}
	
	public CurrencyRest insertCurrency(String shortName, String name) {
		if ("EUR".equals(shortName)) {
			throw new F4MFatalErrorException("Creating EUR currency not allowed");
		}
		CurrencyRestInsert currency = new CurrencyRestInsert();
		currency.setShortName(shortName);
		currency.setName(name);
		LOGGER.info("Inserting currency {}", currency);
		CurrencyRest entity = callPost(currency, CurrencyRest.class);
		LOGGER.info("Currency inserted {}", entity);
		return entity;
	}
	
	public CurrencyRest updateCurrency(CurrencyRest currency) {
		LOGGER.info("Updating currency {}", currency);
		CurrencyRest entity = callPut(currency, CurrencyRest.class);
		LOGGER.info("Currency updated {}", entity);
		return entity;
	}
	
	@Override
	protected String getUriPath() {
		return URI_PATH;
	}
}
