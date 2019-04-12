package de.ascendro.f4m.service.payment.rest.wrapper;

import com.google.inject.assistedinject.Assisted;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.payment.callback.Callback;
import de.ascendro.f4m.service.payment.callback.WrapperCallback;
import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.di.RestClientProvider;
import de.ascendro.f4m.service.payment.rest.model.TransactionRest;
import de.ascendro.f4m.service.payment.rest.model.TransactionRestInsert;
import de.ascendro.f4m.service.payment.rest.model.TransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Wrapper for REST calls to manage external transaction under /accountTransactions
 */
public class TransactionRestWrapper extends RestWrapper {
	private static final Logger LOGGER = LoggerFactory.getLogger(TransactionRestWrapper.class);
	
	public static final String URI_PATH = "accountTransactions";

	private PaymentWrapperUtils utils;

	@Inject
	public TransactionRestWrapper(@Assisted String tenantId, RestClientProvider restClientProvider,
			PaymentConfig paymentConfig, LoggingUtil loggingUtil, PaymentWrapperUtils utils) {
		super(tenantId, restClientProvider, paymentConfig, loggingUtil);
		this.utils = utils;
	}
	
	public void getTransaction(String transactionId, Callback<TransactionRest> callback) {
		callGetAsync(TransactionRest.class, callback, null, transactionId);
	}

	public void loadOntoOrWithdrawFromAccount(String accountId, BigDecimal value, String reference, Callback<TransactionRest> callback) {
		TransactionRestInsert insert = new TransactionRestInsert();
		insert.setReference(reference);
		insert.setValue(value);
		if (value.signum() > 0) {
			insert.setCreditorAccountId(accountId);
			insert.setType(TransactionType.CREDIT);
		} else if (value.signum() < 0) {
			insert.setDebitorAccountId(accountId);
			insert.setType(TransactionType.DEBIT);
		} else {
			throw new F4MValidationFailedException("Incorrect amount for transaction " + value.toPlainString());
		}
		callPostAsync(insert, TransactionRest.class, callback);		
	}
	
	public void  transferBetweenAccounts(TransactionRestInsert insert, Callback<TransactionRest> callback) {
		if (insert.getType() == null) {
			throw new F4MFatalErrorException("Transaction type not specified");
		}
		AtomicInteger retryCounter = new AtomicInteger(0);
		WrapperCallback<TransactionRest, TransactionRest> retryCallback = new WrapperCallback<TransactionRest, TransactionRest>(callback) {
			@Override
			protected void executeOnCompleted(TransactionRest response) {
				originalCallback.completed(response);
			}
			
			@Override
			protected void executeOnFailed(Throwable throwable) {
				int retryNr = retryCounter.incrementAndGet();
				if (utils.shouldRetryTransaction(throwable, retryNr)) {
					LOGGER.warn("Payment system failed, retrying transaction", throwable);
					callPostAsync(insert, TransactionRest.class, this);
				} else {
					super.executeOnFailed(throwable);
				}
			}

		};
		LOGGER.debug("transferBetweenAccounts insert {} ", insert);
		callPostAsync(insert, TransactionRest.class, retryCallback);

	}

	public void transferBetweenAccounts(TransactionRestInsert insert) {
		if (insert.getType() == null) {
			throw new F4MFatalErrorException("Transaction type not specified");
		}
		TransactionRest entity = callPost(insert, TransactionRest.class);
		LOGGER.info("Transfer between accounts {} result {}", insert, entity);
	}

	@Override
	protected String getUriPath() {
		return URI_PATH;
	}

}
