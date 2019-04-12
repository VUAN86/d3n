package de.ascendro.f4m.service.friend.client;

import com.google.inject.Inject;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.model.PaymentDetailsBuilder;
import de.ascendro.f4m.service.payment.model.TransactionLog;
import de.ascendro.f4m.service.payment.model.TransferFundsRequest;
import de.ascendro.f4m.service.payment.model.TransferFundsRequestBuilder;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;

public class PaymentServiceCommunicator {

    private final ServiceRegistryClient serviceRegistryClient;
    private final JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;
    private final JsonMessageUtil jsonUtil;
    private final TransactionLogAerospikeDao transactionLogAerospikeDao;
    @Inject
    public PaymentServiceCommunicator(ServiceRegistryClient serviceRegistryClient,
                                          JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool, JsonMessageUtil jsonUtil,
                                          TransactionLogAerospikeDao transactionLogAerospikeDao) {
        this.serviceRegistryClient = serviceRegistryClient;
        this.jsonWebSocketClientSessionPool = jsonWebSocketClientSessionPool;
        this.jsonUtil = jsonUtil;
        this.transactionLogAerospikeDao = transactionLogAerospikeDao;
    }

    public void initiatePayment(UserActionPayoutRequestInfo requestInfo) {
        final TransferFundsRequestBuilder builder = new TransferFundsRequestBuilder()
                .fromTenantToProfile(requestInfo.getTenantId(), requestInfo.getUserId())
                .amount(requestInfo.getAmount(), requestInfo.getCurrency())
                .withPaymentDetails(new PaymentDetailsBuilder()
                        .additionalInfo(requestInfo.getReason())
                        .appId(requestInfo.getAppId())
                        .build());

        TransferFundsRequest transactionRequest = builder.buildSingleUserPaymentForGame();
        final JsonMessage<TransferFundsRequest> transactionRequestMessage = jsonUtil.createNewMessage(
                builder.getBuildSingleUserPaymentForGameRequestType(), transactionRequest);

        String transactionLogId = transactionLogAerospikeDao.createTransactionLog(new TransactionLog(transactionRequest,
                requestInfo.getCurrency(), requestInfo.getReason(), requestInfo.getAppId()));
        requestInfo.setTransactionLogId(transactionLogId);

        try {
            jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(
                    serviceRegistryClient.getServiceConnectionInformation(PaymentMessageTypes.SERVICE_NAME),
                    transactionRequestMessage, requestInfo);
        } catch (F4MValidationFailedException | F4MIOException e) {
            throw new F4MFatalErrorException("Unable to send transferFunds request to payment service", e);
        }
    }
}
