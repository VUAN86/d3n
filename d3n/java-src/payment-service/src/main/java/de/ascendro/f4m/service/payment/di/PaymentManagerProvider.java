package de.ascendro.f4m.service.payment.di;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Provider;

import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.manager.PaymentManager;
import de.ascendro.f4m.service.payment.manager.impl.PaymentManagerImpl;
import de.ascendro.f4m.service.payment.manager.impl.PaymentManagerMockImpl;

public class PaymentManagerProvider implements Provider<PaymentManager> {
	private static final Logger LOGGER = LoggerFactory.getLogger(PaymentManagerProvider.class);

	private PaymentManager paymentManager;
	private PaymentConfig config;
	private PaymentManagerFactory factory;
	
	public PaymentManagerProvider() {
		//non-injectable empty constructor for easier override in test to provide spy/mock instance  
	}

	@Inject
	public PaymentManagerProvider(PaymentConfig config, PaymentManagerFactory factory) {
		this.config = config;
		this.factory = factory;
	}

	@Override
	public PaymentManager get() {
		if (paymentManager == null) {
			if (config.getPropertyAsBoolean(PaymentConfig.MOCK_MODE)) {
				LOGGER.warn("Using PaymentManager in mock mode - no calls to real Payment API will be made");
				paymentManager = factory.createMock();
			} else {
				LOGGER.debug("Using PaymentManager in operational mode");
				paymentManager = factory.createImpl();
			}
		}
		return paymentManager;
	}

	public interface PaymentManagerFactory {
		public PaymentManagerImpl createImpl();
		public PaymentManagerMockImpl createMock();
	}
}
