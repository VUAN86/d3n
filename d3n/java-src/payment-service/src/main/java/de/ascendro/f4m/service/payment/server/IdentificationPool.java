package de.ascendro.f4m.service.payment.server;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.payment.client.DependencyServicesCommunicator;
import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.dao.PendingIdentification;
import de.ascendro.f4m.service.payment.dao.PendingIdentificationAerospikeDao;
import de.ascendro.f4m.service.payment.di.UserPaymentManagerProvider;
import de.ascendro.f4m.service.payment.manager.PaymentUserIdCalculator;
import de.ascendro.f4m.service.payment.manager.UserPaymentManager;
import de.ascendro.f4m.service.payment.notification.IdentificationCallbackMessagePayload;
import de.ascendro.f4m.service.usermessage.model.WebsocketMessageType;
import de.ascendro.f4m.service.usermessage.translation.Messages;

public class IdentificationPool {

	private static final Logger LOGGER = LoggerFactory.getLogger(IdentificationPool.class);

	private final DependencyServicesCommunicator dependencyServicesCommunicator;
	private final UserPaymentManager userPaymentManager;
	private final PaymentConfig config;
	private PendingIdentificationAerospikeDao pendingIdentificationAerospikeDao;

	private ScheduledExecutorService executor;

	@Inject
	public IdentificationPool(DependencyServicesCommunicator dependencyServicesCommunicator,
			UserPaymentManagerProvider userPaymentManagerProvider, PaymentConfig config, PendingIdentificationAerospikeDao pendingIdentificationAerospikeDao) {
		this.dependencyServicesCommunicator = dependencyServicesCommunicator;
		this.userPaymentManager = userPaymentManagerProvider.get();
		this.config = config;
		this.pendingIdentificationAerospikeDao = pendingIdentificationAerospikeDao;

		int scheduleCount = config.getPropertyAsInteger(PaymentConfig.IDENTIFICATION_POOL_SIZE);
		executor = Executors.newScheduledThreadPool(scheduleCount,
				new ThreadFactoryBuilder().setNameFormat("PaymentIdentificationPool-%d").build());
	}

	private void processUser(String userId) {
		try {
			LOGGER.debug("Execute Identity Information synchronize to user {}", userId);
			userPaymentManager.synchronizeIdentityInformation(userId);
			String profileId = PaymentUserIdCalculator.calcProfileIdFromUserId(userId);
			String tenantId = PaymentUserIdCalculator.calcTenantIdFromUserId(userId);

			PendingIdentification pendingIdentification = pendingIdentificationAerospikeDao.getPendingIdentification(tenantId, profileId);
			dependencyServicesCommunicator.sendPushAndDirectMessages(profileId, pendingIdentification.getAppId(),
					Messages.PAYMENT_SYSTEM_IDENTIFICATION_SUCCESS_PUSH, null,
					new IdentificationCallbackMessagePayload(WebsocketMessageType.IDENTIFICATION_SUCCESS));

			pendingIdentificationAerospikeDao.deletePendingIdentification(tenantId, profileId);
		} catch (F4MException e) {
			LOGGER.error("Failed to get Identity Information. ", e);
		}
	}

	public void add(String userId) {
		int scheduleTimeout = config.getPropertyAsInteger(PaymentConfig.IDENTIFICATION_SCHEDULE_TIMEOUT);
		executor.schedule(() -> processUser(userId), scheduleTimeout, TimeUnit.MILLISECONDS);
	}

	@PreDestroy
	public void finialize() {
		if (executor != null) {
			try {
				final List<Runnable> scheduledRunnabled = executor.shutdownNow();
				LOGGER.info("Stopping IdentificationPool with {} scheduled tasks",
						scheduledRunnabled != null ? scheduledRunnabled.size() : 0);
			} catch (Exception e) {
				LOGGER.error("Failed to shutdown IdentificationPool scheduler", e);
			}
		}
	}

}
