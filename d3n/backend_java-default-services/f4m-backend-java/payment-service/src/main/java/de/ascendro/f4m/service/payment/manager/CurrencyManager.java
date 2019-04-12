package de.ascendro.f4m.service.payment.manager;

import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.payment.F4MPaymentException;
import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.dao.TenantDao;
import de.ascendro.f4m.service.payment.dao.TenantInfo;
import de.ascendro.f4m.service.payment.exception.F4MNoCurrencyRateException;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.external.ExchangeRate;
import de.ascendro.f4m.service.payment.rest.model.AccountRest;
import de.ascendro.f4m.service.payment.rest.model.CurrencyRest;
import de.ascendro.f4m.service.payment.rest.model.ExchangeRateRest;
import de.ascendro.f4m.service.payment.rest.model.ExchangeRateRestInsert;
import de.ascendro.f4m.service.payment.rest.wrapper.AccountRestWrapper;
import de.ascendro.f4m.service.payment.rest.wrapper.CurrencyRestWrapper;
import de.ascendro.f4m.service.payment.rest.wrapper.ExchangeRateRestWrapper;
import de.ascendro.f4m.service.payment.rest.wrapper.RestWrapperFactory;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CurrencyManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(CurrencyManager.class);

	private static final String EXCHANGE_RATE_PARAM_PREFIX = "exchange.rate.";
	private static final int NUMBER_OF_DIGITS_IN_CURRENCY = 7;
	public static final BigDecimal SMALLEST_EXCHANGE_RATE = BigDecimal.valueOf(Math.pow(10, -1 * (double)NUMBER_OF_DIGITS_IN_CURRENCY ));
	
	
	private RestWrapperFactory<CurrencyRestWrapper> currencyRestWrapperFactory;
	private RestWrapperFactory<AccountRestWrapper> accountRestWrapperFactory;
	private RestWrapperFactory<ExchangeRateRestWrapper> exchangeRateRestWrapperFactory;
	private TenantDao tenantAerospikeDao;
	private PaymentConfig paymentConfig;

	private Map<String, InitializedTenantData> initializedTenants = new HashMap<>();

	@Inject
	public CurrencyManager(RestWrapperFactory<CurrencyRestWrapper> currencyRestWrapperFactory,
			RestWrapperFactory<AccountRestWrapper> accountRestWrapperFactory, TenantDao tenantAerospikeDao,
			RestWrapperFactory<ExchangeRateRestWrapper> exchangeRateRestWrapperFactory, PaymentConfig paymentConfig) {
		this.currencyRestWrapperFactory = currencyRestWrapperFactory;
		this.accountRestWrapperFactory = accountRestWrapperFactory;
		this.tenantAerospikeDao = tenantAerospikeDao;
		this.exchangeRateRestWrapperFactory = exchangeRateRestWrapperFactory;
		this.paymentConfig = paymentConfig;
	}

	public synchronized boolean initSystemCurrencies(String tenantId) {
		//check on startup, if currencies have been initialized. Without them nothing works.
		boolean initializationExecuted = false;
		LOGGER.debug("initSystemCurrencies init tenant={}",tenantId);
		if (!initializedTenants.containsKey(tenantId)) {
//			if("F4M".equals(tenantId))
//				tenantAerospikeDao.cloneTenantInfo(tenantId); // TODO
			TenantInfo tenantInfo = tenantAerospikeDao.getTenantInfo(tenantId); //verify if tenant exists at all in out DB at first
			CurrencyRestWrapper currencyRestWrapper = currencyRestWrapperFactory.create(tenantId);
			List<CurrencyRest> virtualCurrencies = currencyRestWrapper.getCurrencies();
			List<CurrencyRest> allCurrencies = new ArrayList<>(virtualCurrencies.size() + 1);
			allCurrencies.addAll(virtualCurrencies); //virtualCurrencies might be read-only
			for (String supportedCurrency : getExternalCurrencies()) {
				Optional<CurrencyRest> existing = virtualCurrencies.stream()
						.filter(c -> supportedCurrency.equals(c.getShortName())).findFirst();
				if (!existing.isPresent()) {
					CurrencyRest insertedCurrency = currencyRestWrapper.insertCurrency(supportedCurrency,
							supportedCurrency);
					allCurrencies.add(insertedCurrency);
				}
			}
			AccountRest tenantMoneyAccount = getTenantMoneyAccount(tenantId);
			allCurrencies.add(tenantMoneyAccount.getCurrency());
			initializedTenants.put(tenantId,
					new InitializedTenantData(tenantMoneyAccount, createCurrencyAssociation(tenantInfo, allCurrencies)));
			initExchangeRates(tenantId);
			initializationExecuted = true;
		}
		return initializationExecuted;
	}

	protected synchronized void initExchangeRates(String tenantId) {
		List<String> externalCurrencyList = findTenantCurrencyAssociation(tenantId).values().stream()
				.map(CurrencyRest::getShortName).collect(Collectors.toList());
		List<ExchangeRateRest> exRateList = exchangeRateRestWrapperFactory.create(tenantId).getExchangeRates().stream()
				.filter(er -> externalCurrencyList.contains(er.getFromCurrency().getShortName()))
				.filter(er -> externalCurrencyList.contains(er.getToCurrency().getShortName()))
				.collect(Collectors.toList());
		for (Pair<Currency, Currency> currencyPair : getCurrencyPairsForExchangeRates()) {
			Currency fromCurrency = currencyPair.getLeft();
			Currency toCurrency = currencyPair.getRight();
			BigDecimal configRate = getConfigRate(fromCurrency, toCurrency);
			validateAndUpdateExchangeRate(exRateList, tenantId, fromCurrency, toCurrency, configRate );
		}
	}
	
	private BigDecimal getConfigRate(Currency fromCurrency, Currency toCurrency) {
		String configName = EXCHANGE_RATE_PARAM_PREFIX + fromCurrency + "." + toCurrency;
		BigDecimal configRate = paymentConfig.getPropertyAsBigDecimal(configName.toLowerCase());
		if (configRate == null) {
			configName = EXCHANGE_RATE_PARAM_PREFIX + toCurrency + "." + fromCurrency;
			BigDecimal reverseRate = paymentConfig.getPropertyAsBigDecimal(configName.toLowerCase());
			configRate = BigDecimal.ONE.divide(reverseRate, 2, BigDecimal.ROUND_HALF_UP);
		}
		return configRate;
	}
	
	protected Set<Pair<Currency, Currency>> getCurrencyPairsForExchangeRates() {
		Set<Pair<Currency, Currency>> pairs = new HashSet<>();
		pairs.add(new ImmutablePair<Currency, Currency>(Currency.MONEY, Currency.BONUS));
		pairs.add(new ImmutablePair<Currency, Currency>(Currency.MONEY, Currency.CREDIT));
		pairs.add(new ImmutablePair<Currency, Currency>(Currency.CREDIT, Currency.BONUS));
		pairs.add(new ImmutablePair<Currency, Currency>(Currency.BONUS, Currency.CREDIT));
		//don't create exchange rates to EUR - such conversions shouldn't be allowed!
		return pairs;
	}

	private void validateAndUpdateExchangeRate(List<ExchangeRateRest> exRateList, String tenantId,
			Currency fromCurrency, Currency toCurrency, BigDecimal configRate) {
		Optional<ExchangeRateRest> erOptional = exRateList.stream()
				.filter(er -> getCurrencyEnumByCurrencyRest(tenantId, er.getFromCurrency()) == fromCurrency)
				.filter(er -> getCurrencyEnumByCurrencyRest(tenantId, er.getToCurrency()) == toCurrency).findFirst();
		if (erOptional.isPresent()) {
			ExchangeRateRest exchangeRateUpdate = erOptional.get();
			if (exchangeRateUpdate.getRate().compareTo(configRate) != 0) {
				exchangeRateUpdate.setRate(configRate);
				exchangeRateRestWrapperFactory.create(tenantId).updateExchangeRate(exchangeRateUpdate);
			}
		} else {
			ExchangeRateRestInsert exchangeRateInsert = new ExchangeRateRestInsert();
			exchangeRateInsert.setFromCurrencyId(getCurrencyRestByCurrencyEnum(tenantId, fromCurrency).getId());
			exchangeRateInsert.setToCurrencyId(getCurrencyRestByCurrencyEnum(tenantId, toCurrency).getId());
			exchangeRateInsert.setRate(configRate);
			exchangeRateRestWrapperFactory.create(tenantId).insertExchangeRate(exchangeRateInsert);
		}
	}

	public AccountRest getTenantMoneyAccount(String tenantId) {
		AccountRestWrapper accountRestWrapper = accountRestWrapperFactory.create(tenantId);
		AccountRest accountRest = accountRestWrapper.getTenantMoneyAccount();
		if (accountRest != null) {
			return accountRest;
		} else {
			throw new F4MPaymentException("Tenant money account not initialized, cannot retrieve MONEY currency data");
		}
	}

	private List<String> getExternalCurrencies() {
		List<String> currencies = new ArrayList<>(2);
		//currencies.add(getTenantExternalCurrencyShortName(tenantAerospikeDao.getTenantInfo(tenantId)) + INTERNAL_CURRENCY_MARK); // For adding EUR_INTERNAL if necessary
		currencies.add(Currency.BONUS.toString());
		currencies.add(Currency.CREDIT.toString());
		return currencies;
	}

	private Map<Currency, CurrencyRest> findTenantCurrencyAssociation(String tenantId) {
		initSystemCurrencies(tenantId);
		return findTenantCurrencyAssociationWithoutInitialization(tenantId);
	}

	private Map<Currency, CurrencyRest> findTenantCurrencyAssociationWithoutInitialization(String tenantId) {
		LOGGER.debug("findTenantCurrencyAssociationWithoutInitialization tenantId={}",tenantId);
		return findOrInitializeTenant(tenantId).getCurrencyAssociations();
	}

	public boolean isTenantMoneyAccount(String tenantId, AccountRest account) {
		InitializedTenantData initializedTenantData = findOrInitializeTenant(tenantId);
		if (initializedTenantData == null) {
			throw new F4MPaymentException("Tenant '" + tenantId + "' not initialized");
		}
		return initializedTenantData.getMoneyAccount().getId().equals(account.getId());
	}

	public InitializedTenantData findOrInitializeTenant(String tenantId) {
		InitializedTenantData initializedTenantData = this.initializedTenants.get(tenantId);
		if (initializedTenantData == null) {
			initSystemCurrencies(tenantId);
			initializedTenantData = this.initializedTenants.get(tenantId);
		}
		return initializedTenantData;
	}
	
	public boolean isInitializedTenant(String tenantId) {
		InitializedTenantData initializedTenantData = this.initializedTenants.get(tenantId);
		return initializedTenantData != null;		
	}

	public CurrencyRest getCurrencyRestByCurrencyEnum(String tenantId, Currency currency) {
		Map<Currency, CurrencyRest> association = findTenantCurrencyAssociation(tenantId);
		if (association != null) {
			return association.get(currency);
		} else {
			throw new F4MPaymentException("Currency data not initialized for tentant " + tenantId);
		}
	}

	public Currency getCurrencyEnumByCurrencyRest(String tenantId, CurrencyRest currencyRest) {
		Map<Currency, CurrencyRest> association = findTenantCurrencyAssociation(tenantId);
		if (association != null) {
			Optional<Entry<Currency, CurrencyRest>> currencyFromCache = association.entrySet().stream()
					.filter(e -> e.getValue().getId().equals(currencyRest.getId())).findFirst();
			if (currencyFromCache.isPresent()) {
				return currencyFromCache.get().getKey();
			} else {
				throw new F4MPaymentException(String.format("Wrong currency: %s (%s) for tentant %s",
						currencyRest.getShortName(), currencyRest.getId(), tenantId));
			}
		} else {
			throw new F4MPaymentException("Currency data not initialized for tentant " + tenantId);
		}
	}

	private String getTenantExternalCurrencyShortName(TenantInfo tenantInfo) {
		String mainCurrency = tenantInfo.getMainCurrency();
		if (StringUtils.isEmpty(mainCurrency)) {
			throw new F4MValidationFailedException("Main currency not set for tenant " + tenantInfo.getTenantId());
		}
		return mainCurrency;
	}

	private String getExternalCurrencyShortName(TenantInfo tenantInfo, Currency currency) {
		String externalCurrencyShortName;
		if (Currency.MONEY.equals(currency)) {
			externalCurrencyShortName = getTenantExternalCurrencyShortName(tenantInfo);
		} else {
			externalCurrencyShortName = currency.toString();
		}
		return externalCurrencyShortName;
	}

	private Currency getF4MCurrencyFromExternalCurrency(String currencyName, TenantInfo tenantInfo) {
		Currency f4mInternalCurrency = EnumUtils.getEnum(Currency.class, currencyName);
		if (f4mInternalCurrency == null) {
			if (!currencyName.equals(getTenantExternalCurrencyShortName(tenantInfo))) {
				throw new F4MFatalErrorException("Unknown external currency '" + currencyName + "'");
			}
			f4mInternalCurrency = Currency.MONEY;
		}
		return f4mInternalCurrency;
	}

	public ExchangeRate getTenantExchangeRateByFromAmount(String tenantId, BigDecimal fromAmount, Currency fromCurrency,
			Currency toCurrency) {
		Predicate<? super ExchangeRate> amountFilter = rate -> rate.getFromAmount().compareTo(fromAmount) == 0;
		ExchangeRate exchangeRate = getTenantExchangeRate(tenantId, amountFilter, fromCurrency, toCurrency);
		if (exchangeRate == null) {
			throw new F4MNoCurrencyRateException(
					"No exchange rate from " + fromAmount + " " + fromCurrency + " to " + toCurrency);
		}
		return exchangeRate;
	}

	public BigDecimal getRate(ExchangeRate exchangeRate) {
		BigDecimal rate = exchangeRate.getToAmount().divide(exchangeRate.getFromAmount(), NUMBER_OF_DIGITS_IN_CURRENCY,
				RoundingMode.HALF_UP);
		if (rate.compareTo(SMALLEST_EXCHANGE_RATE)< 0) {
			rate = SMALLEST_EXCHANGE_RATE;
		}
		return rate;
	}

	private ExchangeRate getTenantExchangeRate(String tenantId, Predicate<? super ExchangeRate> amountFilter,
			Currency fromCurrency, Currency toCurrency) {
		ExchangeRate exchangeRate = null;
		TenantInfo tenantInfo = tenantAerospikeDao.getTenantInfo(tenantId);
		String from = getExternalCurrencyShortName(tenantInfo, fromCurrency);
		String to = getExternalCurrencyShortName(tenantInfo, toCurrency);
		Optional<ExchangeRate> optional = Optional.ofNullable(tenantInfo).map(TenantInfo::getExchangeRates)
				.orElse(Collections.emptyList()).stream()
				.filter(rate -> rate.getFromCurrency().equals(from) && rate.getToCurrency().equals(to))
				.filter(amountFilter).findFirst();
		if (optional.isPresent()) {
			exchangeRate = optional.get();
		}
		return exchangeRate;
	}

	public List<ExchangeRate> getTenantExchangeRateList(String tenantId) {
		TenantInfo tenantInfo = tenantAerospikeDao.getTenantInfo(tenantId);
		return tenantInfo.getExchangeRates();
	}

	private Map<Currency, CurrencyRest> createCurrencyAssociation(TenantInfo tenantInfo, List<CurrencyRest> currencies) {
		Map<Currency, CurrencyRest> currencyAssociationMap = new EnumMap<>(Currency.class);
		for (CurrencyRest currencyRest : currencies) {
			try {
				Currency currency = getF4MCurrencyFromExternalCurrency(currencyRest.getShortName(), tenantInfo);
				currencyAssociationMap.put(currency, currencyRest);
			} catch (F4MFatalErrorException e) {
				LOGGER.warn("Cannont associate currency {} with F4M currencies", currencyRest.getShortName(), e);
			}
		}
		return currencyAssociationMap;
	}
	
	public static class InitializedTenantData {
		private AccountRest moneyAccount;
		private Map<Currency, CurrencyRest> currencyAssociations;
		
		public InitializedTenantData(AccountRest moneyAccount, Map<Currency, CurrencyRest> currencyAssociations) {
			this.moneyAccount = moneyAccount;
			this.currencyAssociations = currencyAssociations;
		}

		public AccountRest getMoneyAccount() {
			return moneyAccount;
		}

		public Map<Currency, CurrencyRest> getCurrencyAssociations() {
			return currencyAssociations;
		}
	}
}
