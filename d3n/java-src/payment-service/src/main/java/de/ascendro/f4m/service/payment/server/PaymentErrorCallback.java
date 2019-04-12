package de.ascendro.f4m.service.payment.server;

import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.request.jackpot.PaymentServiceCommunicator;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.service.payment.client.AdminEmailForwarder;
import de.ascendro.f4m.service.payment.client.DependencyServicesCommunicator;
import de.ascendro.f4m.service.payment.manager.CurrencyManager;
import de.ascendro.f4m.service.payment.manager.TransactionLogCacheManager;
import de.ascendro.f4m.service.payment.manager.UserAccountManager;
import de.ascendro.f4m.service.payment.model.TransactionLog;
import de.ascendro.f4m.service.payment.model.TransactionStatus;
import de.ascendro.f4m.service.usermessage.model.WebsocketMessageType;
import de.ascendro.f4m.service.usermessage.translation.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class PaymentErrorCallback extends PaymentSuccessCallback {
	private static final long serialVersionUID = -2983407233949399055L;
	private static final Logger LOGGER = LoggerFactory.getLogger(PaymentErrorCallback.class);

	@Inject
	public PaymentErrorCallback(TransactionLogCacheManager transactionLogCache, TransactionLogAerospikeDao transactionLogAerospikeDao,
								DependencyServicesCommunicator dependencyServicesCommunicator, CurrencyManager currencyManager,
								UserAccountManager userAccountManager, Tracker tracker, AdminEmailForwarder adminEmailForwarder,
								PaymentServiceCommunicator paymentServiceCommunicator)
	{
		super(transactionLogCache, transactionLogAerospikeDao, dependencyServicesCommunicator, currencyManager,userAccountManager,
			  tracker, adminEmailForwarder, paymentServiceCommunicator);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String transactionId = readTransactionId(req);
		LOGGER.info("Error callback received from payment system for transaction {}", transactionId);
		String logId = getLogId(transactionId, LOGGER, TransactionStatus.ERROR);
		try {
			if (logId != null) {
				TransactionLog transactionLog = getTransactionLog(logId);
				String userId = transactionLog.getUserFromIdOrUserToId();
				String appId = transactionLog.getAppId();

				//processCallback(logId, transactionId, TransactionStatus.ERROR);
				onPaymentError(logId, transactionId);
				if(appId != null) {
					pushMessageToUser(transactionLog, transactionId, userId, Messages.PAYMENT_SYSTEM_PAYMENT_ERROR_PUSH,
							WebsocketMessageType.PAYMENT_FAILURE, appId);
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error processing error payment callback", e);
		}
	}
	
	public void onPaymentError(String logId, String transactionId) {
		processCallback(logId, transactionId, TransactionStatus.ERROR);
	}
}
