package de.ascendro.f4m.service.payment.manager.impl;

import static de.ascendro.f4m.service.payment.manager.PaymentTestUtil.mapToList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.server.analytics.AnalyticsDao;
import de.ascendro.f4m.server.analytics.tracker.TrackerImpl;
import de.ascendro.f4m.server.session.GlobalClientSessionInfo;
import de.ascendro.f4m.service.di.GsonProvider;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.dao.TenantDao;
import de.ascendro.f4m.service.payment.dao.TenantInfo;
import de.ascendro.f4m.service.payment.manager.AnalyticsEventManager;
import de.ascendro.f4m.service.payment.manager.CurrencyManager;
import de.ascendro.f4m.service.payment.manager.PaymentTestUtil;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.PaymentDetailsBuilder;
import de.ascendro.f4m.service.payment.model.TransactionId;
import de.ascendro.f4m.service.payment.model.internal.CloseJackpotRequest;
import de.ascendro.f4m.service.payment.model.internal.CloseJackpotRequest.PayoutItem;
import de.ascendro.f4m.service.payment.model.internal.CloseJackpotResponse;
import de.ascendro.f4m.service.payment.model.internal.CreateJackpotRequest;
import de.ascendro.f4m.service.payment.model.internal.GameState;
import de.ascendro.f4m.service.payment.model.internal.GetJackpotRequest;
import de.ascendro.f4m.service.payment.model.internal.GetJackpotResponse;
import de.ascendro.f4m.service.payment.model.internal.TransferJackpotRequest;
import de.ascendro.f4m.service.payment.rest.model.CurrencyRest;
import de.ascendro.f4m.service.payment.rest.model.GameRest;
import de.ascendro.f4m.service.payment.rest.model.GameRestBuyIn;
import de.ascendro.f4m.service.payment.rest.model.GameRestInsert;
import de.ascendro.f4m.service.payment.rest.model.GameRestPayout;
import de.ascendro.f4m.service.payment.rest.model.TransactionRest;
import de.ascendro.f4m.service.payment.rest.wrapper.AccountRestWrapper;
import de.ascendro.f4m.service.payment.rest.wrapper.CurrencyRestWrapper;
import de.ascendro.f4m.service.payment.rest.wrapper.ExchangeRateRestWrapper;
import de.ascendro.f4m.service.payment.rest.wrapper.GameRestWrapper;
import de.ascendro.f4m.service.session.pool.SessionPool;
import de.ascendro.f4m.service.util.TestGsonProvider;
import io.github.benas.randombeans.api.EnhancedRandom;

public class GameManagerImplTest {
	@Mock
	private GameRestWrapper gameRestWrapper;
	@Mock
	private CurrencyRestWrapper currencyRestWrapper;
	@Mock
	private TenantDao tenantAerospikeDao;
	@Mock
	private AccountRestWrapper accountRestWrapper;
	@Mock
	private ExchangeRateRestWrapper exchangeRateRestWrapper;
	@Mock
	private SessionPool sessionPool;
	@Mock
	private AnalyticsDao analyticsDao;
	@Mock
	private GlobalClientSessionInfo globalClientSessionInfo;

	private CurrencyManager currencyManager;
	private GameManagerImpl gameManager;
	private TenantInfo tenantInfo;


	private final static String TENANT_ID = "tenantId";
	private final static String GAME_ID = "new_game";
	private final static String CURRENCY_ID = "2";

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		GsonProvider gsonProvider = new TestGsonProvider();
		TrackerImpl tracker = new TrackerImpl(analyticsDao);
		AnalyticsEventManager analyticsEventManager = new AnalyticsEventManager(gsonProvider, tracker);
		tenantInfo = new TenantInfo();
		tenantInfo.setMainCurrency("EUR");
		when(tenantAerospikeDao.getTenantInfo(anyString())).thenReturn(tenantInfo);
		when(currencyRestWrapper.getCurrencies()).thenReturn(PaymentTestUtil.createCurrencyRestsForVirtualCurrencies());
		when(accountRestWrapper.getTenantMoneyAccount()).thenReturn(PaymentTestUtil.prepareEurAccount());
		currencyManager = new CurrencyManager((id) -> currencyRestWrapper, (id) -> accountRestWrapper,
				tenantAerospikeDao, (id) -> exchangeRateRestWrapper, new PaymentConfig());
		gameManager = new GameManagerImpl(currencyManager, (id) -> gameRestWrapper, gsonProvider, analyticsEventManager);
	}

	@Test
	public void createGameTest() throws Exception {
		CreateJackpotRequest request = new CreateJackpotRequest();
		request.setMultiplayerGameInstanceId(GAME_ID);
		request.setCurrency(Currency.CREDIT);
		request.setTenantId(TENANT_ID);

		GameRest gameRest = new GameRest();
		gameRest.setState(GameState.OPEN);
		when(gameRestWrapper.createGame(any())).thenReturn(gameRest);
		gameManager.createJackpot(request);

		ArgumentCaptor<GameRestInsert> argument = ArgumentCaptor.forClass(GameRestInsert.class);
		verify(gameRestWrapper).createGame(argument.capture());

		assertEquals(request.getMultiplayerGameInstanceId(), argument.getValue().getGameId());
		assertEquals(CURRENCY_ID, argument.getValue().getCurrencyId());
	}

	@Test
	public void getGameTest() throws Exception {
		CurrencyRest currencyRest = new CurrencyRest();
		currencyRest.setId(CURRENCY_ID);
		currencyRest.setShortName(Currency.CREDIT.toString());

		GameRest gameRest = new GameRest();
		gameRest.setState(GameState.OPEN);
		gameRest.setBalance(BigDecimal.ONE);
		gameRest.setCurrency(currencyRest);
		when(gameRestWrapper.getGame(any())).thenReturn(gameRest);

		GetJackpotRequest request = new GetJackpotRequest();
		request.setTenantId(TENANT_ID);
		request.setMultiplayerGameInstanceId(GAME_ID);
		GetJackpotResponse response = gameManager.getJackpot(request);

		assertTrue(BigDecimal.ONE.compareTo(response.getBalance()) == 0);
		assertEquals(GameState.OPEN, response.getState());
	}
	
	@Test
	public void transferJackpotTest() throws Exception {
		String expectedTransactionId = "expectedTransactionId";
		TransactionRest rest = new TransactionRest();
		rest.setId(expectedTransactionId);
		when(gameRestWrapper.buyInGame(any())).thenReturn(rest);
		
		TransferJackpotRequest request = EnhancedRandom.random(TransferJackpotRequest.class, "paymentDetails");
		request.setPaymentDetails(new PaymentDetailsBuilder().additionalInfo("info").gameId("1").appId("appId").build());
		request.setFromProfileId("profile");
		request.setTenantId("tenant");
		ClientInfo clientInfo = new ClientInfo();
		clientInfo.setAppId("1");
//		clientInfo.setCountryCode(ISOCountry.DE);
		
		TransactionId transactionId = gameManager.transferJackpot(request,clientInfo);
		assertEquals(expectedTransactionId, transactionId.getTransactionId());
		
		ArgumentCaptor<GameRestBuyIn> arg = ArgumentCaptor.forClass(GameRestBuyIn.class);
		verify(gameRestWrapper).buyInGame(arg.capture());
		assertThat(arg.getValue().getGameId(), equalTo(request.getMultiplayerGameInstanceId()));
		assertThat(arg.getValue().getAmount(), equalTo(request.getAmount()));
		assertThat(arg.getValue().getUserId(), equalTo("tenant_profile"));
		assertThat(arg.getValue().getReference(), equalTo("{\"appId\":\"appId\",\"gameId\":\"1\",\"additionalInfo\":\"info\"}"));
	}

	@Test
	public void closeJackpotWithSuccess() throws Exception {
		CloseJackpotRequest request = new CloseJackpotRequest();
		request.setTenantId("t1");
		request.setPaymentDetails(new PaymentDetailsBuilder().gameId("game").build());
		request.setMultiplayerGameInstanceId("multiplayerGameInstance");
		request.setPayouts(new ArrayList<>(2));
		request.getPayouts().add(new PayoutItem("prof1", BigDecimal.ONE));
		request.getPayouts().add(new PayoutItem("prof2", BigDecimal.TEN));

		when(gameRestWrapper.getGame(any())).thenReturn(new GameRest());
		CloseJackpotResponse response = gameManager.closeJackpot(request);
		assertNotNull(response);

		ArgumentCaptor<GameRestPayout> arg = ArgumentCaptor.forClass(GameRestPayout.class);
		verify(gameRestWrapper).endGame(arg.capture());

		assertThat(arg.getValue().getGameId(), equalTo(request.getMultiplayerGameInstanceId()));
		assertThat(arg.getValue().getReference(), equalTo("{\"gameId\":\"game\"}"));
		assertThat(mapToList(arg.getValue().getPayouts(), p -> p.getUserId()), contains("t1_prof1", "t1_prof2"));
		assertThat(mapToList(arg.getValue().getPayouts(), p -> p.getAmount()),
				contains(BigDecimal.ONE, BigDecimal.TEN));
	}

	@Test
	public void closeJackpotWithNoPayouts() throws Exception {
		CloseJackpotRequest request = EnhancedRandom.random(CloseJackpotRequest.class);
		request.setPayouts(null);
		gameManager.closeJackpot(request);
		ArgumentCaptor<GameRestPayout> arg = ArgumentCaptor.forClass(GameRestPayout.class);
		verify(gameRestWrapper).endGame(arg.capture());
		//verify that empty list (but not null) used in call to Paydent, since they have NPE in such case
		assertThat(arg.getValue().getPayouts(), hasSize(0));
	}
}