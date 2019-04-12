package de.ascendro.f4m.service.payment.rest.wrapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.di.RestClientProvider;
import de.ascendro.f4m.service.payment.exception.F4MPaymentClientException;
import de.ascendro.f4m.service.payment.rest.model.GameRestBuyIn;
import de.ascendro.f4m.service.payment.rest.model.TransactionRest;

public class GameRestWrapperTest {
	@Mock
	private PaymentConfig config;
	@Mock
	private RestClientProvider restClientProvider;
	@Mock
	private LoggingUtil loggingUtil;
	@InjectMocks
	private PaymentWrapperUtils utils;

	private GameRestWrapper gameRestWrapper;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		gameRestWrapper = spy(new GameRestWrapper("tenantId", restClientProvider, config, loggingUtil, utils));
		when(config.getPropertyAsInteger(PaymentConfig.TRANSACTION_RETRY_TIMES)).thenReturn(1);
	}

	@Test
	public void testIfBuyInRestIsRetriedOnFailure() {
		TransactionRest transaction = new TransactionRest();
		doThrow(new F4MPaymentClientException(
				PaymentExternalErrorCodes.ACCOUNTTRANSACTION_FAILED.getF4MCode(), "any")).
		doReturn(transaction).when(gameRestWrapper).callPut(any(), any(), any());
		
		GameRestBuyIn buyIn = new GameRestBuyIn();
		gameRestWrapper.buyInGame(buyIn);
	}
}
