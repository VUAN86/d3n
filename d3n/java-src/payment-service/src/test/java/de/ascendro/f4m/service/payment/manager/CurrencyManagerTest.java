package de.ascendro.f4m.service.payment.manager;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.dao.TenantAerospikeDaoImpl;
import de.ascendro.f4m.service.payment.dao.TenantInfo;
import de.ascendro.f4m.service.payment.exception.F4MNoCurrencyRateException;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.external.ExchangeRate;
import de.ascendro.f4m.service.payment.rest.model.ExchangeRateRestInsert;
import de.ascendro.f4m.service.payment.rest.wrapper.AccountRestWrapper;
import de.ascendro.f4m.service.payment.rest.wrapper.CurrencyRestWrapper;
import de.ascendro.f4m.service.payment.rest.wrapper.ExchangeRateRestWrapper;
import de.ascendro.f4m.service.payment.rest.wrapper.RestWrapperFactory;

public class CurrencyManagerTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(CurrencyManagerTest.class);
	@Spy
	private PaymentConfig config = new PaymentConfig();
	@Mock
	private CurrencyRestWrapper currencyRest;
	@Mock
	private AccountRestWrapper accountRest;
	@Mock
	private ExchangeRateRestWrapper exchangeRateRestWrapper;
	@Mock
	private TenantAerospikeDaoImpl tenantAerospikeDao;
	@Spy
	private RestWrapperFactory<CurrencyRestWrapper> currencyRestWrapperFactory = new RestWrapperFactory<CurrencyRestWrapper>() {
		@Override
		public CurrencyRestWrapper create(String tenantId) {
			return currencyRest;
		}
	};
	@InjectMocks
	private CurrencyManager currencyManager = new CurrencyManager(currencyRestWrapperFactory, (id) -> accountRest,
			tenantAerospikeDao, (id) -> exchangeRateRestWrapper, new PaymentConfig());
	
	private String tenantId = "tenantId";

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		when(accountRest.getTenantMoneyAccount()).thenReturn(PaymentTestUtil.prepareEurAccount());
	}

	@Test
	public void testCurrencyInitialisation() throws Exception {
		when(tenantAerospikeDao.getTenantInfo("tenant")).thenReturn(getTenantInfo());
		when(currencyRest.getCurrencies()).thenReturn(Arrays.asList(PaymentTestUtil.prepareCurrencyRest(ExternalTestCurrency.BONUS)));
		when(currencyRest.insertCurrency(ExternalTestCurrency.CREDIT.toString(),
				ExternalTestCurrency.CREDIT.toString()))
						.thenReturn(PaymentTestUtil.prepareCurrencyRest(ExternalTestCurrency.CREDIT));
		//tests with default SUPPORTED_CURRENCIES_DEFAULT_VALUE
		currencyManager.initSystemCurrencies("tenant");
    	verify(currencyRest, times(1)).insertCurrency(ExternalTestCurrency.CREDIT.toString(), ExternalTestCurrency.CREDIT.toString());
    	
		verifyExchangeRates();
	}

	private void verifyExchangeRates() {
		//EUR->CREDIT, EUR->BONUS, BONUS<->CREDIT = 4 times, skip CREDIT->EUR, BONUS->EUR who shouldn't be allowed
		ArgumentCaptor<ExchangeRateRestInsert> arg = ArgumentCaptor.forClass(ExchangeRateRestInsert.class);
		verify(exchangeRateRestWrapper, times(4)).insertExchangeRate(arg.capture());
		arg.getAllValues().forEach(r -> LOGGER.debug("Inserted rate {}", r));
		String eurId = PaymentTestUtil.getTestCurrencyId(ExternalTestCurrency.EUR);
		String bonusId = PaymentTestUtil.getTestCurrencyId(ExternalTestCurrency.BONUS);
		String creditId = PaymentTestUtil.getTestCurrencyId(ExternalTestCurrency.CREDIT);
		verifyExpectedRate(arg.getAllValues(), eurId, bonusId, PaymentConfig.DUMMY_EXCHANGE_RATE_DEFAULT);
		verifyExpectedRate(arg.getAllValues(), eurId, creditId, PaymentConfig.DUMMY_EXCHANGE_RATE_DEFAULT);
		verifyExpectedRate(arg.getAllValues(), creditId, bonusId, new BigDecimal("0.01"));
		verifyExpectedRate(arg.getAllValues(), bonusId, creditId, PaymentConfig.DUMMY_EXCHANGE_RATE_DEFAULT);
	}

	private void verifyExpectedRate(List<ExchangeRateRestInsert> list, String eurId, String bonusId, BigDecimal value) {
		//AssertJ might help to verify this with more explanatory errors...
		assertTrue(list.stream()
				.anyMatch(r -> r.getFromCurrencyId().equals(eurId) && r.getToCurrencyId().equals(bonusId)
						&& r.getRate().equals(value)));
	}
	
	@Test
	public void testCurrencyInitialisationCaching() throws Exception {
		when(tenantAerospikeDao.getTenantInfo("tenant")).thenReturn(getTenantInfo());
		when(tenantAerospikeDao.getTenantInfo("another")).thenReturn(getTenantInfo());
		
		initCurrenciesAndResetMock("tenant", 1);
		initCurrenciesAndResetMock("tenant", 0);
		initCurrenciesAndResetMock("another", 1);
	}

	private void initCurrenciesAndResetMock(String tenantId, int wantedNumberOfInvocations) {
		reset(currencyRest);
		when(currencyRest.getCurrencies()).thenReturn(PaymentTestUtil.createCurrencyRestsForVirtualCurrencies());
		currencyManager.initSystemCurrencies(tenantId);
		verify(currencyRest, times(wantedNumberOfInvocations)).getCurrencies();
	}
	
	@Test
	public void testGetTenantExchangeRateNoRates() throws Exception {
		TenantInfo tenantInfo = new TenantInfo();
		when(tenantAerospikeDao.getTenantInfo(tenantId)).thenReturn(tenantInfo);
		try {
			currencyManager.getTenantExchangeRateByFromAmount(tenantId, BigDecimal.ONE, Currency.BONUS, Currency.CREDIT);
			fail();
		} catch (F4MNoCurrencyRateException e) {
			assertEquals("No exchange rate from 1 BONUS to CREDIT", e.getMessage());
		}
		tenantInfo.setExchangeRates(Arrays.asList(
				createRate("2", ExternalTestCurrency.BONUS, "3", ExternalTestCurrency.CREDIT)));
		try {
			currencyManager.getTenantExchangeRateByFromAmount(tenantId, BigDecimal.ONE, Currency.BONUS, Currency.CREDIT);
			fail();
		} catch (F4MNoCurrencyRateException e) {
			assertEquals("No exchange rate from 1 BONUS to CREDIT", e.getMessage());
		}
	}
	
	@Test
	public void testGetTenantExchangeRateWithRounding() throws Exception {
		TenantInfo tenantInfo = new TenantInfo();
		List<ExchangeRate> exchangeRates = Arrays.asList(
				createRate("1", ExternalTestCurrency.EUR, "3", ExternalTestCurrency.BONUS),
				createRate("3", ExternalTestCurrency.EUR, "1", ExternalTestCurrency.BONUS));
		tenantInfo.setExchangeRates(exchangeRates);
		tenantInfo.setMainCurrency(ExternalTestCurrency.EUR.toString());
		when(tenantAerospikeDao.getTenantInfo(tenantId)).thenReturn(tenantInfo);
		assertThat(
				currencyManager.getRate(currencyManager.getTenantExchangeRateByFromAmount(tenantId, new BigDecimal("1"), Currency.MONEY, Currency.BONUS)),
				comparesEqualTo(new BigDecimal("3")));
		assertThat(
				currencyManager.getRate(currencyManager.getTenantExchangeRateByFromAmount(tenantId, new BigDecimal("3"), Currency.MONEY, Currency.BONUS)),
				comparesEqualTo(new BigDecimal("0.3333333")));
	}
	
	@Test
	public void testGetTenantExchangeRateWithSameDataAsInExplorationTest() throws Exception {
		TenantInfo tenantInfo = new TenantInfo();
		List<ExchangeRate> exchangeRates = Arrays
				.asList(createRate("2.5", ExternalTestCurrency.EUR, "50", ExternalTestCurrency.BONUS));
		tenantInfo.setExchangeRates(exchangeRates);
		tenantInfo.setMainCurrency(ExternalTestCurrency.EUR.toString());
		when(tenantAerospikeDao.getTenantInfo(tenantId)).thenReturn(tenantInfo);
		// 2.5 EUR = 50 BONUS, eurToBonusRateValue = 20.00
		assertThat(
				currencyManager.getRate(currencyManager.getTenantExchangeRateByFromAmount(tenantId, new BigDecimal("2.5"), Currency.MONEY, Currency.BONUS)),
				comparesEqualTo(new BigDecimal("20")));
	}
	
	@Test
	public void testGetExchangeRatesTooSmall(){
		ExchangeRate exchangeRate = createRate("100000000000000.00", ExternalTestCurrency.BONUS, "1",
				ExternalTestCurrency.CREDIT);
		BigDecimal rate = currencyManager.getRate(exchangeRate);
		
		assertEquals(CurrencyManager.SMALLEST_EXCHANGE_RATE, rate);
	}
	
	public static ExchangeRate createRate(String fromAmount, ExternalTestCurrency fromCurrency, String toAmount,
			ExternalTestCurrency toCurrency) {
		ExchangeRate rate = new ExchangeRate();
		rate.setFromAmount(new BigDecimal(fromAmount));
		rate.setFromCurrency(fromCurrency.toString());
		rate.setToAmount(new BigDecimal(toAmount));
		rate.setToCurrency(toCurrency.toString());
		return rate;
	}
	
	private TenantInfo getTenantInfo() {
		TenantInfo tenantInfo = new TenantInfo();
		tenantInfo.setMainCurrency(ExternalTestCurrency.EUR.toString());
		return tenantInfo;
	} 
}
