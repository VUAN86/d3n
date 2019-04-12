package de.ascendro.f4m.service.payment.manager.impl;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.payment.manager.UserPaymentManager;
import de.ascendro.f4m.service.payment.model.PaymentClientInfo;
import de.ascendro.f4m.service.payment.model.external.GetExternalPaymentRequest;
import de.ascendro.f4m.service.payment.model.external.GetExternalPaymentResponse;
import de.ascendro.f4m.service.payment.model.external.GetIdentificationResponse;
import de.ascendro.f4m.service.payment.model.external.InitExternalPaymentRequest;
import de.ascendro.f4m.service.payment.model.external.InitExternalPaymentResponse;
import de.ascendro.f4m.service.payment.model.external.InitIdentificationRequest;
import de.ascendro.f4m.service.payment.model.external.InitIdentificationResponse;
import de.ascendro.f4m.service.payment.model.external.PaymentTransactionState;
import de.ascendro.f4m.service.payment.model.external.PaymentTransactionType;
import de.ascendro.f4m.service.payment.model.internal.IdentificationType;
import de.ascendro.f4m.service.util.DateTimeUtil;

/**
 * Mock implementation for testing without real payment system. 
 */
public class UserPaymentManagerMockImpl implements UserPaymentManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(UserPaymentManagerMockImpl.class);
	private static final String RECEIVE_INFO = "{} received call {} to mock from client info {}";

    @Override
	public InitIdentificationResponse initiateIdentification(PaymentClientInfo paymentClientInfo, InitIdentificationRequest request) {
    	LOGGER.info(RECEIVE_INFO, "initiateIdentification", request, paymentClientInfo);
        InitIdentificationResponse response = new InitIdentificationResponse();
        response.setForwardUrl("www.payments.com/identification/index/12345");
        response.setIdentificationId("12345");
        return response;
    }

	@Override
	public InitExternalPaymentResponse initiateExternalPayment(PaymentClientInfo paymentClientInfo, InitExternalPaymentRequest request) {
		LOGGER.info(RECEIVE_INFO, "initiateExternalPayment", request, paymentClientInfo);
		InitExternalPaymentResponse response = new InitExternalPaymentResponse();
		response.setTransactionId("54321");
		response.setPaymentToken("C3F10AD7CE4CC89213B0AFECEA296938.sbg-vm- fe02");
		return response;
	}

	@Override
	public GetIdentificationResponse getIdentification(PaymentClientInfo paymentClientInfo) {
		LOGGER.info(RECEIVE_INFO, "getIdentification", paymentClientInfo);
		GetIdentificationResponse response = new GetIdentificationResponse();
		response.setId("88");
		response.setFirstName("John");
		response.setName("Smit");
		response.setStreet("Brivibas");
		response.setZip("1010");
		response.setCity("Riga");
		response.setCountry("LV");
		response.setDateOfBirth(ZonedDateTime.of(1999, 9, 27, 0, 0, 0, 0, DateTimeUtil.TIMEZONE));
		response.setPlaceOfBirth("New York");
		response.setType(IdentificationType.LIGHT);
		return response;
	}

	@Override
	public GetExternalPaymentResponse getExternalPayment(PaymentClientInfo paymentClientInfo, GetExternalPaymentRequest request) {
		LOGGER.info(RECEIVE_INFO, "getExternalPayment", request, paymentClientInfo);
		GetExternalPaymentResponse response = new GetExternalPaymentResponse();
		response.setTransactionId("transactionIdReturn");
		response.setAmount(new BigDecimal("32.1"));
		response.setDescription("Short description");
		response.setPaymentToken("C3F10AD7CE4CC89213B0AFECEA296938.sbg-vm- fe02");
		response.setType(PaymentTransactionType.DEBIT);
		response.setState(PaymentTransactionState.PROCESSED);
		response.setCreated(ZonedDateTime.of(2008, 9, 27, 22, 9, 1, 0, DateTimeUtil.TIMEZONE));
		response.setProcessed(ZonedDateTime.of(2008, 9, 27, 22, 59, 58, 0, DateTimeUtil.TIMEZONE));
		return response;
	}

	@Override
	public void synchronizeIdentityInformation(String userId) {
		//
	}
}