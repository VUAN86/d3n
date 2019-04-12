package de.ascendro.f4m.service.payment.di;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Provider;

import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.manager.UserPaymentManager;
import de.ascendro.f4m.service.payment.manager.impl.UserPaymentManagerImpl;
import de.ascendro.f4m.service.payment.manager.impl.UserPaymentManagerMockImpl;

public class UserPaymentManagerProvider implements Provider<UserPaymentManager> {
	private static final Logger LOGGER = LoggerFactory.getLogger(UserPaymentManagerProvider.class);

	private UserPaymentManager paymentManager;
	private PaymentConfig config;
	private UserPaymentManagerFactory factory;


	public UserPaymentManagerProvider() {
		//non-injectable empty constructor for easier override in test to provide spy/mock instance  
	}

	@Inject
	public UserPaymentManagerProvider(PaymentConfig config, UserPaymentManagerFactory factory) {
		this.config = config;
		this.factory = factory;
	}

	@Override
	public UserPaymentManager get() {
		if (paymentManager == null) {
			if (config.getPropertyAsBoolean(PaymentConfig.MOCK_MODE)) {
				LOGGER.warn("Using UserAccountManager in mock mode - no calls to real Payment API will be made");
				paymentManager = factory.createMock();
			} else {
				LOGGER.debug("Using UserAccountManager in operational mode");
				paymentManager = factory.createImpl();
			}
		}
		return paymentManager;
	}

	public interface UserPaymentManagerFactory {
		public UserPaymentManagerImpl createImpl();
		public UserPaymentManagerMockImpl createMock();
	}
}
