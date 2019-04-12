package de.ascendro.f4m.service.payment.rest.wrapper;

import java.util.function.Function;
import java.util.function.Supplier;

import javax.inject.Inject;

import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.exception.F4MPaymentClientException;

public class PaymentWrapperUtils {
	private PaymentConfig config;

	@Inject
	public PaymentWrapperUtils(PaymentConfig config) {
		this.config = config;

	}

	public boolean shouldRetryTransaction(Throwable throwable, int retryNr) {
		boolean retry = false;
		if (throwable instanceof F4MPaymentClientException) {
			F4MPaymentClientException e = (F4MPaymentClientException) throwable;
			retry = PaymentExternalErrorCodes.ACCOUNTTRANSACTION_FAILED.getF4MCode().equals(e.getCode());
		}
		if (retryNr > config.getPropertyAsInteger(PaymentConfig.TRANSACTION_RETRY_TIMES)) {
			retry = false; //do fail in the end, if payment does not respond with success in the end
		}
		return retry;
	}
	
	public static <T> T executePaymentCall(Supplier<T> dataSupplier,
			Function<F4MPaymentClientException, T> userNotFoundHandler, PaymentExternalErrorCodes errorCodeToHandle) {
		T data;
		try {
			data = dataSupplier.get();
		} catch (F4MPaymentClientException e) {
			if (errorCodeToHandle.getF4MCode().equals(e.getCode())) {
				data = userNotFoundHandler.apply(e);
			} else {
				throw e;
			}
		}
		return data;
	}
}
