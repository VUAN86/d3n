package de.ascendro.f4m.service.payment.di;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Provider;

import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.manager.GameManager;
import de.ascendro.f4m.service.payment.manager.impl.GameManagerImpl;
import de.ascendro.f4m.service.payment.manager.impl.GameManagerMockImpl;

public class GameManagerProvider implements Provider<GameManager> {
	private static final Logger LOGGER = LoggerFactory.getLogger(GameManagerProvider.class);

	private GameManager gameManager;
	private PaymentConfig config;
	private GameManagerFactory factory;

	public GameManagerProvider() {
		//non-injectable empty constructor for easier override in test to provide spy/mock instance  
	}

	@Inject
	public GameManagerProvider(PaymentConfig config, GameManagerFactory factory) {
		this.config = config;
		this.factory = factory;
	}

	@Override
	public GameManager get() {
		if (gameManager == null) {
			if (config.getPropertyAsBoolean(PaymentConfig.MOCK_MODE)) {
				LOGGER.warn("Using GameManager in mock mode - no calls to real Game API will be made");
				gameManager = factory.createMock();
			} else {
				LOGGER.debug("Using GameManager in operational mode");
				gameManager = factory.createImpl();
			}
		}
		return gameManager;
	}

	public interface GameManagerFactory {
		public GameManagerImpl createImpl();
		public GameManagerMockImpl createMock();
	}
}
