package de.ascendro.f4m.service.payment.client;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.server.market.CommonMarketInstanceAerospikeDao;
import de.ascendro.f4m.server.request.jackpot.PaymentServiceCommunicator;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.service.exception.client.F4MClientException;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.ISOLanguage;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.payment.F4MPaymentException;
import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.usermessage.model.SendEmailWrapperRequest;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.math.BigDecimal;

import static de.ascendro.f4m.service.usermessage.model.SendEmailWrapperRequest.NEW_LINE;

public class AdminEmailForwarder {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminEmailForwarder.class);
    private PaymentConfig config;
    private JsonMessageUtil jsonUtil;
    private JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;
    private ServiceRegistryClient serviceRegistryClient;
    private TransactionLogAerospikeDao transactionLogAerospikeDao;
    private CommonMarketInstanceAerospikeDao commonMarketInstanceAerospikeDao;
    private PaymentServiceCommunicator paymentServiceCommunicator;

    @Inject
    public AdminEmailForwarder(ServiceRegistryClient serviceRegistryClient, JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool,
                               JsonMessageUtil jsonUtil, PaymentConfig config, TransactionLogAerospikeDao transactionLogAerospikeDao,
                               CommonMarketInstanceAerospikeDao commonMarketInstanceAerospikeDao, PaymentServiceCommunicator paymentServiceCommunicator) {
        this.serviceRegistryClient = serviceRegistryClient;
        this.jsonWebSocketClientSessionPool = jsonWebSocketClientSessionPool;
        this.jsonUtil = jsonUtil;
        this.config = config;
        this.transactionLogAerospikeDao = transactionLogAerospikeDao;
        this.commonMarketInstanceAerospikeDao = commonMarketInstanceAerospikeDao;
        this.paymentServiceCommunicator = paymentServiceCommunicator;
    }

    private void forwardErrorToAdminEmail(String subjectPrefix, String messageContent) {
        String emailAddress = config.getProperty(PaymentConfig.ADMIN_EMAIL);
        if (StringUtils.isNotBlank(emailAddress)) {
            SendEmailWrapperRequest request = new SendEmailWrapperRequest();
            request.setAddress(emailAddress);
            //no params necessary since message is already prepared and no translation is necessary for emails to admin
            request.setSubject(subjectPrefix + " in payment service on " + config.getProperty(PaymentConfig.SERVICE_HOST) + ":" + config.getProperty(PaymentConfig.JETTY_SSL_PORT));
            request.setMessage(messageContent);
            request.setISOLanguage(ISOLanguage.EN); //use english to have email header/footer translated
            JsonMessage<SendEmailWrapperRequest> requestJson = jsonUtil.createNewMessage(UserMessageMessageTypes.SEND_EMAIL, request);
            try {
                jsonWebSocketClientSessionPool.sendAsyncMessage(serviceRegistryClient.getServiceConnectionInformation(UserMessageMessageTypes.SERVICE_NAME), requestJson);
            } catch (F4MValidationFailedException | F4MIOException e) {
                throw new F4MFatalErrorException("Unable to send sendEmail loadOrWithdrawWithoutCoverage to user message service", e);
            }
        }
    }

    public void forwardErrorToAdmin(JsonMessage<? extends JsonMessageContent> message, Throwable e, String details) {
        StringBuilder content = new StringBuilder();
        content.append("Processing of payment message ");
        content.append(NEW_LINE);
        if (StringUtils.isNotEmpty(details)) {
            content.append(details);
            content.append(NEW_LINE);
        }
        fillInMessageData(message, content);
        content.append(NEW_LINE);
        fillInExceptionData(e, content);
        this.forwardErrorToAdminEmail("An error has occurred", content.toString());
    }

    public void forwardWarningToAdmin(String messageContent, Throwable e) {
        StringBuilder content = new StringBuilder();
        content.append("Processing of payment message ");
        content.append(NEW_LINE);
        content.append(messageContent);
        content.append(NEW_LINE);
        fillInExceptionData(e, content);
        this.forwardErrorToAdminEmail("Warning about recoverable error", content.toString());
    }

    private void fillInExceptionData(Throwable e, StringBuilder content) {
        content.append(" failed with ");
        if (e instanceof F4MPaymentException) {
            F4MPaymentException paymentException = (F4MPaymentException) e;
            if (paymentException.getErrorInfo() != null) {
                content.append("error response from Payment System:");
                content.append(NEW_LINE);
                content.append(paymentException.getErrorInfo().toString());
            } else {
                content.append("payment exception:");
                fillInStacktrace(e, content);
            }
        } else {
            content.append("unexpected exception:");
            fillInStacktrace(e, content);
        }
    }

    private void fillInStacktrace(Throwable e, StringBuilder content) {
        content.append(NEW_LINE);
        String stacktrace = ExceptionUtils.getStackTrace(e);
        stacktrace = stacktrace.replace(System.lineSeparator(), NEW_LINE);
        content.append(stacktrace);
    }

    private void fillInMessageData(JsonMessage<? extends JsonMessageContent> message, StringBuilder content) {
        content.append(message.toString());
    }

    public boolean shouldForwardErrorToAdmin(Throwable e) {
        //ClientException indicates that there has been some error from user's side and info about it is forwarded to user so that he can fix it
        return !(e instanceof F4MClientException)
                //XXX: turn on validation reporting to admin again after front-end has finished testing the API
                && !(e instanceof F4MValidationFailedException);
    }

    public void createPurchaseCreditInstanceAndroid(String className, String orderId, String userId, String tenantId, String appId, String date, String packageName, String productId, String purchaseTime, String purchaseState, String purchaseToken) {
        commonMarketInstanceAerospikeDao.createPurchaseCreditInstanceAndroid(className, orderId, userId, tenantId, appId, date, packageName, productId, purchaseTime, purchaseState, purchaseToken);
    }

    public void createPurchaseCreditInstanceIos(String className, String transactionId, String userId, String tenantId, String appId, String date, String productId, String dateTime, String receipt) {
        commonMarketInstanceAerospikeDao.createPurchaseCreditInstanceIos(className, transactionId, userId, tenantId, appId, date, productId, dateTime, receipt);
    }

    public void createPurchaseCreditInstanceError(String className, String userId, String tenantId, String appId, String receipt, String date, Long timestamp, String error) {
        if (StringUtils.isNotEmpty(receipt)) {
            commonMarketInstanceAerospikeDao.createPurchaseCreditInstanceError(className, userId, tenantId, appId, receipt, date, timestamp, error);
        }

    }

    public boolean isPurchaseCreditInstance(String className, String id) {
        try {
            return commonMarketInstanceAerospikeDao.isPurchaseCreditInstance(className, id);
        } catch (Exception e) {
            return true;
        }
    }

    public PaymentConfig getConfig() {
        return config;
    }

    /**
     * @param tenantId  - admins
     * @param profileId - user bought goods
     * @param amount    - number of credits for admission
     * @param currency  - only credits
     * @param appId     - aplication
     */
    public void transferFundsToUserAccount(String tenantId, String profileId, BigDecimal amount, Currency currency, String appId) {
        paymentServiceCommunicator.transferFundsToUserAccount(tenantId, profileId, amount, currency, appId);
    }

    public String withdrawFromThePlayerAccount(String appId, String toTenantId, BigDecimal amount, String userFromId) {
        return paymentServiceCommunicator.withdrawFromThePlayerAccount(appId, toTenantId, amount, userFromId);
    }
}
