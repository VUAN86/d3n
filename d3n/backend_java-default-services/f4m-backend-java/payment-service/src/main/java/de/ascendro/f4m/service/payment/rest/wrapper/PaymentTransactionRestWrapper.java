package de.ascendro.f4m.service.payment.rest.wrapper;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.di.RestClientProvider;
import de.ascendro.f4m.service.payment.rest.model.PaymentTransactionInitializationRest;
import de.ascendro.f4m.service.payment.rest.model.PaymentTransactionRest;

/**
 * Wrapper for REST calls to manage external transaction under /paymentTransactions
 */
public class PaymentTransactionRestWrapper extends RestWrapper {
	private static final Logger LOGGER = LoggerFactory.getLogger(PaymentTransactionRestWrapper.class);
	
	public static final String URI_PATH = "paymentTransactions";

	@Inject
	public PaymentTransactionRestWrapper(@Assisted String tenantId, RestClientProvider restClientProvider,
			PaymentConfig paymentConfig, LoggingUtil loggingUtil) {
		super(tenantId, restClientProvider, paymentConfig, loggingUtil);
	}

	public PaymentTransactionRest startPaymentTransactionToGetForwardURL(
			PaymentTransactionInitializationRest initializationRequest) {
		PaymentTransactionRest entity = callPost(initializationRequest, PaymentTransactionRest.class);
		LOGGER.info("Payment transaction initialized {} result {}", initializationRequest, entity);
		return entity;
	}

	public PaymentTransactionRest getPaymentTransaction(String id) {
		PaymentTransactionRest entity = callGet(PaymentTransactionRest.class, null, id);
		LOGGER.info("Payment transaction {}", entity);
		return entity;
	}

	@Override
	protected String getUriPath() {
		return URI_PATH;
	}

}
