package de.ascendro.f4m.service.payment.rest.wrapper;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.di.RestClientProvider;
import de.ascendro.f4m.service.payment.rest.model.IdentificationInitializationRest;
import de.ascendro.f4m.service.payment.rest.model.IdentificationResponseRest;

public class IdentificationRestWrapper extends RestWrapper {
	private static final Logger LOGGER = LoggerFactory.getLogger(IdentificationRestWrapper.class);
	private static final String IDENTIFICATION_PATH = "Identifications";
	private static final String APPROVE_SUBPATH = "approve";

	@Inject
	public IdentificationRestWrapper(@Assisted String tenantId, RestClientProvider restClientProvider,
			PaymentConfig paymentConfig, LoggingUtil loggingUtil) {
		super(tenantId, restClientProvider, paymentConfig, loggingUtil);
	}

	public IdentificationResponseRest startUserIdentificationToGetForwardURL(
			IdentificationInitializationRest identificationInitializationRest) {
		IdentificationResponseRest identificationResponse = callPost(identificationInitializationRest,
				IdentificationResponseRest.class);
		LOGGER.debug("User identity init response was {}", identificationResponse);
		return identificationResponse;
	}

	// Function designed for testing only. 
	public void approveUser(String userId){
		callGet(response -> {
			LOGGER.info("User approve response status was {}", response.getStatus());
			return Void.TYPE;
		}, null, APPROVE_SUBPATH, userId);
	}

	@Override
	protected String getUriPath() {
		return IDENTIFICATION_PATH;
	}

}
