package de.ascendro.f4m.service.payment.rest.wrapper;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.di.RestClientProvider;
import de.ascendro.f4m.service.payment.rest.model.GameRest;
import de.ascendro.f4m.service.payment.rest.model.GameRestBuyIn;
import de.ascendro.f4m.service.payment.rest.model.GameRestInsert;
import de.ascendro.f4m.service.payment.rest.model.GameRestPayout;
import de.ascendro.f4m.service.payment.rest.model.TransactionRest;

public class GameRestWrapper extends RestWrapper {
	private static final Logger LOGGER = LoggerFactory.getLogger(GameRestWrapper.class);
	private static final String URI_PATH = "games";
	private static final String BUYIN_SUBPATH = "buyin";
	private static final String CLOSE_SUBPATH = "close";
	private PaymentWrapperUtils utils;

	@Inject
	public GameRestWrapper(@Assisted String tenantId, RestClientProvider restClientProvider,
			PaymentConfig paymentConfig, LoggingUtil loggingUtil, PaymentWrapperUtils utils) {
		super(tenantId, restClientProvider, paymentConfig, loggingUtil);
		this.utils = utils;
	}
	
	public GameRest createGame(GameRestInsert gameRestInsert) {
		GameRest response = callPost(gameRestInsert, GameRest.class);
		LOGGER.info("Game inserted {}", response);
		return response;
	}

	public GameRest getGame(String gameId) {
		GameRest game = callGet(GameRest.class, null, gameId);
		LOGGER.info("Game got {}", game);
		return game;
	}
	
	public TransactionRest buyInGame(GameRestBuyIn buyIn) {
		int retryCounter = 0;
		TransactionRest transaction = null;
		while (transaction == null) {
			try {
				transaction = callPut(buyIn, TransactionRest.class, BUYIN_SUBPATH);
			} catch (F4MException e) {
				retryCounter++;
				if (utils.shouldRetryTransaction(e, retryCounter)) {
					LOGGER.warn("Payment system failed, retrying transaction", e);
				} else {
					throw e;
				}
			}
		}
		LOGGER.info("Received transaction response on buyin {}", transaction);
		return transaction;
	}
	
	public GameRest endGame(GameRestPayout gameRestPayout) {
		GameRest game = callPut(gameRestPayout, GameRest.class, CLOSE_SUBPATH);
		LOGGER.info("Received game on ending {}", game);
		return game;
	}

	@Override
	protected String getUriPath() {
		return URI_PATH;
	}
}
