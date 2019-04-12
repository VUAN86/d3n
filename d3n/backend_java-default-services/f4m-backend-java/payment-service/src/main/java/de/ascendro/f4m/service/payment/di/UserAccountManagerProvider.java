package de.ascendro.f4m.service.payment.di;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Provider;

import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.manager.UserAccountManager;
import de.ascendro.f4m.service.payment.manager.impl.UserAccountManagerImpl;
import de.ascendro.f4m.service.payment.manager.impl.UserAccountManagerMockImpl;

public class UserAccountManagerProvider implements Provider<UserAccountManager> {
	private static final Logger LOGGER = LoggerFactory.getLogger(UserAccountManagerProvider.class);

	private UserAccountManager manager;
	private PaymentConfig config;
	private UserAccountManagerFactory factory;


	public UserAccountManagerProvider() {
		//non-injectable empty constructor for easier override in test to provide spy/mock instance  
	}

	@Inject
	public UserAccountManagerProvider(PaymentConfig config, UserAccountManagerFactory factory) {
		this.config = config;
		this.factory = factory;
	}

	@Override
	public UserAccountManager get() {
		if (manager == null) {
			if (config.getPropertyAsBoolean(PaymentConfig.MOCK_MODE)) {
				LOGGER.warn("Using UserAccountManager in mock mode - no calls to real Payment API will be made");
				manager = factory.createMock();
			} else {
				LOGGER.debug("Using UserAccountManager in operational mode");
				manager = factory.createImpl();
			}
		}
		return manager;
	}

	public interface UserAccountManagerFactory {
		public UserAccountManagerImpl createImpl();
		public UserAccountManagerMockImpl createMock();
	}
}
