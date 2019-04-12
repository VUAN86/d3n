package de.ascendro.f4m.service.payment.server;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.payment.client.DependencyServicesCommunicator;
import de.ascendro.f4m.service.payment.dao.PendingIdentification;
import de.ascendro.f4m.service.payment.dao.PendingIdentificationAerospikeDao;
import de.ascendro.f4m.service.payment.manager.PaymentUserIdCalculator;
import de.ascendro.f4m.service.payment.notification.IdentificationCallbackMessagePayload;
import de.ascendro.f4m.service.usermessage.model.WebsocketMessageType;
import de.ascendro.f4m.service.usermessage.translation.Messages;

public class IdentificationErrorCallback extends HttpServlet {
	private static final long serialVersionUID = -2983407233949399055L;
	private static final Logger LOGGER = LoggerFactory.getLogger(IdentificationErrorCallback.class);
	
	private final DependencyServicesCommunicator dependencyServicesCommunicator;
	private PendingIdentificationAerospikeDao pendingIdentificationAerospikeDao;
	
	@Inject
	public IdentificationErrorCallback(DependencyServicesCommunicator dependencyServicesCommunicator, PendingIdentificationAerospikeDao pendingIdentificationAerospikeDao) {
		this.dependencyServicesCommunicator = dependencyServicesCommunicator;
		this.pendingIdentificationAerospikeDao = pendingIdentificationAerospikeDao;
	}
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String userId = req.getParameter(IdentificationSuccessCallback.USER_ID);
		//identification success should be registered in event log?
		LOGGER.info("Error callback received from payment system for user {}", userId);
		try {
			String profileId = PaymentUserIdCalculator.calcProfileIdFromUserId(userId);
			String tenantId = PaymentUserIdCalculator.calcTenantIdFromUserId(userId);

			PendingIdentification pendingIdentification = pendingIdentificationAerospikeDao.getPendingIdentification(tenantId, profileId);

			dependencyServicesCommunicator.sendPushAndDirectMessages(profileId, pendingIdentification.getAppId(),
					Messages.PAYMENT_SYSTEM_IDENTIFICATION_ERROR_PUSH, null,
					new IdentificationCallbackMessagePayload(WebsocketMessageType.IDENTIFICATION_FAILURE));

			pendingIdentificationAerospikeDao.deletePendingIdentification(tenantId, profileId);
		} catch (Exception e) {
			LOGGER.error("Error processing error identification callback", e);
		}
	}
}
