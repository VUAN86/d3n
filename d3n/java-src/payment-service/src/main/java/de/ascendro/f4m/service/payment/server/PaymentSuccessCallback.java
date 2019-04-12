package de.ascendro.f4m.service.payment.server;

import de.ascendro.f4m.server.analytics.model.InvoiceEvent;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.request.jackpot.PaymentServiceCommunicator;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.service.auth.model.register.SetUserRoleRequest;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.json.model.user.UserRole;
import de.ascendro.f4m.service.payment.client.AdminEmailForwarder;
import de.ascendro.f4m.service.payment.client.DependencyServicesCommunicator;
import de.ascendro.f4m.service.payment.manager.CurrencyManager;
import de.ascendro.f4m.service.payment.manager.TransactionLogCacheManager;
import de.ascendro.f4m.service.payment.manager.UserAccountManager;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.TransactionLog;
import de.ascendro.f4m.service.payment.model.TransactionStatus;
import de.ascendro.f4m.service.payment.model.external.ExchangeRate;
import de.ascendro.f4m.service.payment.notification.PaymentCallbackMessagePayload;
import de.ascendro.f4m.service.payment.rest.model.CurrencyRest;
import de.ascendro.f4m.service.usermessage.model.WebsocketMessageType;
import de.ascendro.f4m.service.usermessage.translation.Messages;
import de.ascendro.f4m.service.util.auth.UserRoleUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.RoundingMode;
import java.util.List;

public class PaymentSuccessCallback extends HttpServlet {
	private static final long serialVersionUID = -2983407233949399055L;
	private static final Logger LOGGER = LoggerFactory.getLogger(PaymentSuccessCallback.class);
	public static final String TRANSACTION_ID = "token";

	private final TransactionLogCacheManager transactionLogCache;
	private final TransactionLogAerospikeDao transactionLogAerospikeDao;
	private final DependencyServicesCommunicator dependencyServicesCommunicator;
	private final CurrencyManager currencyManager;
	private final UserAccountManager userAccountManager;
	private final Tracker tracker;
	private final AdminEmailForwarder adminEmailForwarder;
	private PaymentServiceCommunicator paymentServiceCommunicator;

	@Inject
	public PaymentSuccessCallback(TransactionLogCacheManager transactionLogCache, TransactionLogAerospikeDao transactionLogAerospikeDao,
								  DependencyServicesCommunicator dependencyServicesCommunicator, CurrencyManager currencyManager,
								  UserAccountManager userAccountManager, Tracker tracker, AdminEmailForwarder adminEmailForwarder,
								  PaymentServiceCommunicator paymentServiceCommunicator)
	{
		this.transactionLogCache = transactionLogCache;
		this.transactionLogAerospikeDao = transactionLogAerospikeDao;
		this.dependencyServicesCommunicator = dependencyServicesCommunicator;
		this.currencyManager = currencyManager;
		this.userAccountManager = userAccountManager;
		this.tracker = tracker;
		this.adminEmailForwarder = adminEmailForwarder;
		this.paymentServiceCommunicator = paymentServiceCommunicator;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String transactionId = readTransactionId(req);
		LOGGER.error("Success callback received from payment system for transaction {}", transactionId);
		try {
			onPaymentSuccess(transactionId, true);
		} catch (Exception e) {
			LOGGER.error("Error processing success payment callback", e);
		}
	}
	
	public void onPaymentSuccess(String transactionId, boolean pushMessageToUser) {
		String logId = getLogId(transactionId, LOGGER, TransactionStatus.COMPLETED);
		if (logId != null) {
			TransactionLog transactionLog = getTransactionLog(logId);
            LOGGER.error("onPaymentSuccess transactionLog {} ", transactionLog);
            String profileId = transactionLog.getUserFromIdOrUserToId();
			String appId = transactionLog.getAppId();

			processCallback(logId, transactionId, TransactionStatus.COMPLETED);
			ClientInfo clientInfo = new ClientInfo(transactionLog.getTenantId(), profileId);
			addInvoiceEvent(clientInfo, transactionLog);
			if (pushMessageToUser) {
				////////////////////////////////////////////////////////////
				// payment/initExternalPayment
                // For https://qd1wpromo.f4mworld.com/#/dashboard there will be only two currencies, credit and bonus. (appId=10)
                // Therefore, when you transfer money immediately need to convert into loans.
				LOGGER.error("onPaymentSuccess transactionLog {} ", transactionLog);
				if (appId != null && appId.equals("10"))
                {
                    // we charge loans relatively exchangeRateList.
					ExchangeRate exchangeRate = getExchangeRate(transactionLog);
					if (exchangeRate != null && exchangeRate.getFromAmount().equals(transactionLog.getAmount()))
					{
                        if (exchangeRate.getFromCurrency().equals("EUR"))
                        {
                            // We charge bonuses or credits.
                            paymentServiceCommunicator.transferFundsToUserAccount(transactionLog.getTenantId(), profileId, exchangeRate.getToAmount(),
																				  Currency.valueOf(exchangeRate.getToCurrency()), appId);
                            // We charge for credits or bonuses.
                            paymentServiceCommunicator.withdrawFromThePlayerAccount(transactionLog.getAppId(), transactionLog.getTenantId(),
                                                                                    exchangeRate.getFromAmount(), transactionLog.getUserFromId());
                        }
                    }
                }
                ////////////////////////////////////////////////////////////

                pushMessageToUser(transactionLog, transactionId, profileId,
                                  Messages.PAYMENT_SYSTEM_PAYMENT_SUCCESS_PUSH,
                                  WebsocketMessageType.PAYMENT_SUCCESS, appId);
			}
			updateUserRole(profileId, transactionLog.getTenantId());
		}
	}

	// Get the exchange rates.
    private ExchangeRate getExchangeRate(TransactionLog transactionLog) {
        List<ExchangeRate> exchangeRateList = currencyManager.getTenantExchangeRateList(transactionLog.getTenantId());
		return exchangeRateList.stream()
							   .filter(rate -> rate.getFromAmount().equals(transactionLog.getAmount()))
							   .findAny()
							   .orElse(null);
    }

	protected void processCallback(String logId, String transactionId, TransactionStatus transactionStatus) {
		transactionLogAerospikeDao.updateTransactionLog(logId, transactionId, transactionStatus);
	}

	protected String getLogId(String transactionId, Logger log, TransactionStatus status) {
		String logId = null;
		if (StringUtils.isNotBlank(transactionId)) {
			logId = transactionLogCache.popLogId(transactionId);
		}
		if (logId == null) {
			log.warn("Received callback on transactionId '{}' which cannot be found", transactionId);
			TransactionLog transactionLog = new TransactionLog();
			transactionLog.setTransactionId(transactionId);
			transactionLog.setStatus(status);
			transactionLog.setReason("Unrecognized payment transaction completed");
			transactionLogAerospikeDao.createTransactionLog(transactionLog);
		}
		return logId;
	}

	protected String readTransactionId(HttpServletRequest req) {
		return req.getParameter(TRANSACTION_ID);
	}

	protected void pushMessageToUser(TransactionLog transactionLog, String transactionId, String profileId,
									 String message, WebsocketMessageType type, String appId) {
		if (transactionLog != null) {
			String amount = transactionLog.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
			CurrencyRest currency = currencyManager.getCurrencyRestByCurrencyEnum(transactionLog.getTenantId(),
					transactionLog.getCurrency());
			String[] parameters = new String[] { transactionId, amount, currency.getShortName() };
			PaymentCallbackMessagePayload payload = new PaymentCallbackMessagePayload(type);
			payload.setTransactionId(transactionId);
			payload.setAmount(transactionLog.getAmount());
			payload.setCurrency(currency.getShortName());
			dependencyServicesCommunicator.sendPushAndDirectMessages(profileId, appId, message, parameters, payload);
		}
	}

	protected TransactionLog getTransactionLog(String logId) {
		return transactionLogAerospikeDao.getTransactionLog(logId);
	}

	private void updateUserRole(String profileId, String tenantId) {
		SetUserRoleRequest setUserRoleRequest = UserRoleUtil.createSetUserRoleRequest(profileId,
				userAccountManager.getUserRoles(profileId, tenantId), UserRole.FULLY_REGISTERED_BANK, UserRole.ANONYMOUS);
		if (setUserRoleRequest != null) {
			dependencyServicesCommunicator.updateUserRoles(setUserRoleRequest);
		}
	}

	private void addInvoiceEvent(ClientInfo clientInfo, TransactionLog transactionLog) {
		InvoiceEvent invoiceEvent = new InvoiceEvent();
		invoiceEvent.setPaymentType(InvoiceEvent.PaymentType.CREDIT_SALES);
		invoiceEvent.setPaymentAmount(transactionLog.getAmount());
		invoiceEvent.setCurrency(transactionLog.getCurrency());
		invoiceEvent.setCurrencyTo(transactionLog.getCurrencyTo());
		invoiceEvent.setPaymentAmountTo(transactionLog.getAmountTo());
		invoiceEvent.setExchangeRate(transactionLog.getRate());
		invoiceEvent.setPaymentAmountTo(transactionLog.getAmountTo());
		tracker.addEvent(clientInfo, invoiceEvent);
	}
}
