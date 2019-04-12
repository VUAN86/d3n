package de.ascendro.f4m.service.advertisement.client;


import org.apache.commons.lang3.StringUtils;

import com.google.inject.Inject;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.service.advertisement.config.AdvertisementConfig;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.ISOLanguage;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.model.PaymentDetailsBuilder;
import de.ascendro.f4m.service.payment.model.TransactionLog;
import de.ascendro.f4m.service.payment.model.TransferFundsRequest;
import de.ascendro.f4m.service.payment.model.TransferFundsRequestBuilder;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.usermessage.model.SendEmailWrapperRequest;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;

public class DependencyServicesCommunicatorImpl implements DependencyServicesCommunicator {
    private static final String REWARD_PAYOUT_REASON = "Reward from Fyber";

    private final ServiceRegistryClient serviceRegistryClient;
    private final JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;
    private final TransactionLogAerospikeDao transactionLogAerospikeDao;
    private final JsonMessageUtil jsonMessageUtil;
    private final AdvertisementConfig advertisementConfig;

    @Inject
    public DependencyServicesCommunicatorImpl(ServiceRegistryClient serviceRegistryClient,
                                          JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool,
                                          TransactionLogAerospikeDao transactionLogAerospikeDao,
                                          JsonMessageUtil jsonMessageUtil,
                                          AdvertisementConfig advertisementConfig){

        this.serviceRegistryClient = serviceRegistryClient;
        this.jsonWebSocketClientSessionPool = jsonWebSocketClientSessionPool;
        this.transactionLogAerospikeDao = transactionLogAerospikeDao;
        this.jsonMessageUtil = jsonMessageUtil;
        this.advertisementConfig = advertisementConfig;
    }

    @Override
    public void initiateRewardPayment(RewardPayoutRequestInfo requestInfo) {
        final TransferFundsRequestBuilder builder = new TransferFundsRequestBuilder()
                .fromTenantToProfile(requestInfo.getTenantId(), requestInfo.getUserId())
                .amount(requestInfo.getAmount(), requestInfo.getCurrency())
                .withPaymentDetails(new PaymentDetailsBuilder()
                        .additionalInfo(REWARD_PAYOUT_REASON)
                        .fyberTransactionId(requestInfo.getFyberTransactionId())
                        .build());

        TransferFundsRequest transactionRequest = builder.buildSingleUserPaymentForGame();
        final JsonMessage<TransferFundsRequest> transactionRequestMessage = jsonMessageUtil.createNewMessage(
                builder.getBuildSingleUserPaymentForGameRequestType(), transactionRequest);

        String transactionLogId = transactionLogAerospikeDao.createTransactionLog(new TransactionLog(transactionRequest,
                requestInfo.getCurrency(), REWARD_PAYOUT_REASON, requestInfo.getAppId()));
        requestInfo.setTransactionLogId(transactionLogId);

        try {
            jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(
                    serviceRegistryClient.getServiceConnectionInformation(PaymentMessageTypes.SERVICE_NAME),
                    transactionRequestMessage, requestInfo);
        } catch (F4MValidationFailedException | F4MIOException e) {
            throw new F4MFatalErrorException("Unable to send transferFunds request to payment service", e);
        }
    }

    @Override
    public void sendEmailToAdmin(String subject, String[] subjectParameters, String body, String[] bodyParameters) {
        String emailAddress = advertisementConfig.getProperty(AdvertisementConfig.ADMIN_EMAIL);
        if (StringUtils.isNotBlank(emailAddress)) {
            SendEmailWrapperRequest request = new SendEmailWrapperRequest();
            request.setAddress(emailAddress);
            request.setSubject(subject);
            request.setSubjectParameters(subjectParameters);
            request.setMessage(body);
            request.setParameters(bodyParameters);
            request.setISOLanguage(ISOLanguage.EN);
            JsonMessage<SendEmailWrapperRequest> requestJson = jsonMessageUtil
                    .createNewMessage(UserMessageMessageTypes.SEND_EMAIL, request);
            try {
                jsonWebSocketClientSessionPool.sendAsyncMessage(
                        serviceRegistryClient.getServiceConnectionInformation(UserMessageMessageTypes.SERVICE_NAME),
                        requestJson);
            } catch (F4MValidationFailedException | F4MIOException e) {
                throw new F4MFatalErrorException("Unable to send sendEmail request to user message service", e);
            }
        }
    }
}
