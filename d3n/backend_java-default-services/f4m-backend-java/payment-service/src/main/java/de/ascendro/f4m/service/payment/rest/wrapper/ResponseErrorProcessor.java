package de.ascendro.f4m.service.payment.rest.wrapper;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.payment.F4MPaymentException;
import de.ascendro.f4m.service.payment.exception.F4MPaymentClientException;
import de.ascendro.f4m.service.payment.rest.model.ErrorInfoRest;

public class ResponseErrorProcessor {
	private static final Logger LOGGER = LoggerFactory.getLogger(ResponseErrorProcessor.class);
	
	public static <T> Response verifyError(String method, T entity, Response response) {
		if (!Response.Status.Family.SUCCESSFUL.equals(response.getStatusInfo().getFamily())) {
			LOGGER.error("Unsuccessful {} call with [{}] to Paydent API, status: {}", method, entity,
					response.getStatus());
			ErrorInfoRest errorInfo = null;
			Throwable cause = null;
			try {
				errorInfo = response.readEntity(ErrorInfoRest.class);
				LOGGER.error("Error received from Paydent API {}", errorInfo);
			} catch (ProcessingException e) {
				cause = e;
			}
			throw createPaymentException(errorInfo, cause, response.getStatusInfo().getStatusCode());
		}
		return response;
	}

	private static F4MException createPaymentException(ErrorInfoRest errorInfo, Throwable cause, int statusCode) {
		F4MException exception;
		if (errorInfo == null) {
			exception = new F4MPaymentException("Unknown error with status " + statusCode, cause);
		} else {
			PaymentExternalErrorCodes errCode = PaymentExternalErrorCodes.byCode(errorInfo.getCode());
			if (errCode != null && errCode.getF4MCode() != null) {
				exception = new F4MPaymentClientException(errCode.getF4MCode(), errorInfo.getMessage());
			} else {
				exception = new F4MPaymentException(errorInfo);
			}
		}
		return exception;
	}


}
