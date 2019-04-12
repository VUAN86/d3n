package de.ascendro.f4m.service.payment.exploration;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.integration.F4MAssert;
import de.ascendro.f4m.service.payment.F4MPaymentException;
import de.ascendro.f4m.service.payment.callback.WaitingCallback;
import de.ascendro.f4m.service.payment.manager.ExternalTestCurrency;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.PaymentClientInfo;
import de.ascendro.f4m.service.payment.model.internal.GetAccountHistoryResponse;
import de.ascendro.f4m.service.payment.model.internal.GetUserAccountHistoryRequest;
import de.ascendro.f4m.service.payment.rest.model.AccountHistoryRest;
import de.ascendro.f4m.service.payment.rest.model.AccountHistoryRestSearchParams;
import de.ascendro.f4m.service.payment.rest.model.AccountRest;
import de.ascendro.f4m.service.payment.rest.model.CurrencyRest;
import de.ascendro.f4m.service.payment.rest.model.ExchangeRateRest;
import de.ascendro.f4m.service.payment.rest.model.ExchangeRateRestInsert;
import de.ascendro.f4m.service.payment.rest.model.TransactionRest;
import de.ascendro.f4m.service.payment.rest.model.TransactionRestInsert;
import de.ascendro.f4m.service.payment.rest.model.TransactionType;
import de.ascendro.f4m.service.payment.rest.model.UserRest;
import de.ascendro.f4m.service.payment.session.PaymentClientInfoImpl;

public class PaydentTransactionExplorationTest extends PaydentExplorationTestBase {
	private static final Logger LOGGER = LoggerFactory.getLogger(PaydentTransactionExplorationTest.class);
	
	//accidentally created MONEY currency in test system should be ignored (event better - it should be deleted, when there is a chance to do so).
	private static final int UNUSED_CURRECY_COUNT = 0;
	private final BigDecimal eurToBonusRateValue = new BigDecimal("20.00");

	@Test
	public void mainTestInitializingCurrencyRatesUsersAndVerifyingInternalPayments() throws Exception {
		//actually other tests are depending on correctly set up currencies, so they might fail in clear environment
		currencyManager.initSystemCurrencies(tenantId);
		List<CurrencyRest> currencies = currencyRestWrapper.getCurrencies();
		LOGGER.info("Currency data in Paydent system - {}", currencies);
		F4MAssert.assertSize(Currency.values().length + UNUSED_CURRECY_COUNT, currencies);
		
		List<ExchangeRateRest> exchangeRates = exchangeRateRestWrapper.getExchangeRates();
		assertNotNull(exchangeRates);
		assertThat(exchangeRates.size(), greaterThanOrEqualTo(3));
		
		UserRest user = createAndVerifyTestUser();
		verifyTransactionCalls(user.getAccounts());
		//List<AccountRest> userActiveAccounts = userRestWrapper.getUserActiveAccounts(userId);
		//verifyTransctionCalls(userActiveAccounts);
	}

	private UserRest createAndVerifyTestUser() throws Exception {
		List<UserRest> users = userRestWrapper.getUsers(null, null);
		assertNotNull(users);
		boolean userAlreadyExists = userAccountManager.userExists(userRestWrapper, userId);
		UserRest userChanges = new UserRest();
		userChanges.setUserId(userId);
		userAccountManager.fillDefaultUserRestValues(userChanges);
		userChanges.setFirstName(FIRST_NAME);
		userChanges.setName(LAST_NAME);
		if (!userAlreadyExists) {
			WaitingCallback<UserRest> insertCallback = new WaitingCallback<>();
			userRestWrapper.insertUser(userChanges, insertCallback);
			insertCallback.getResult();
			//List<UserRest> newList = userRestWrapper.getUsers(null, null);
			//assertEquals(users.size() + 1, newList.size()); <-- fails, when offset of 10 is reached
		} else {
			WaitingCallback<UserRest> updateCallback = new WaitingCallback<>();
			userRestWrapper.updateUser(userChanges, updateCallback);
			updateCallback.getResult();
		}
		//UserRest insertUser = userRestWrapper.insertUser(userId); this throws de.ascendro.f4m.service.payment.manager.F4MPaymentException: UNDEFINED_ERROR - An error occurred while updating the entries. See the inner exception for details.
		List<UserRest> newList = userRestWrapper.getUsers(5, 0);
		LOGGER.info("After update user list is {}", newList);
		UserRest updated = userRestWrapper.getUser(userId);
		assertNotNull(updated);
		//assertEquals(firstName, updated.getFirstName()); //FIXME: issue to Paydent, that non-ASCII is not working!
		//for new user with accounts in all necessary currencies next 2 lines can be uncommented
		//int accountCount = Currency.values().length + 1 + UNUSED_CURRECY_COUNT;
		//assertEquals(accountCount, updated.get().getAccounts().size());

		List<AccountRest> userActiveAccounts = userRestWrapper.getUserActiveAccounts(userId);
		//for new user with accounts in all necessary currencies can next line be uncommented
		//assertEquals(Currency.values().length + UNUSED_CURRECY_COUNT, userActiveAccounts.size());
		Optional<AccountRest> eurAccountFromUserData = updated.getAccounts().stream()
				.filter(account -> REAL_EUR_CURRENCY_ID.equals(account.getCurrencyId())).findFirst();
		assertTrue(eurAccountFromUserData.isPresent());
		Optional<AccountRest> eurAccountFromActiveAccounts = userActiveAccounts.stream()
				.filter(account -> REAL_EUR_CURRENCY_ID.equals(account.getCurrencyId())).findFirst();
		//if getUserActiveAccounts starts to give us uninitialized EUR accounts:
		// - remove unnecessary ERR_MONEY_ACCOUNT_NOT_INITIALIZED
		// - revert back to normal findAccountId instead of updating and retrieving EUR account from updated user info
		assertTrue(eurAccountFromActiveAccounts.isPresent());
		
		String accountId = userActiveAccounts.get(0).getId();
		AccountRest accountById = accountRestWrapper.getAccountById(accountId);
		assertNotNull(accountById);
		//verifyUserIdentityCall();
		
		AccountHistoryRestSearchParams search = new AccountHistoryRestSearchParams();
		search.setAccountId(accountId);
		search.setOffset(0);
		search.setLimit(20);
		search.setStartDate(ZonedDateTime.of(1900, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
		//Beware! new Date() will basically set endDate to beginning of this day, which means that todays transactions will be missing!
		//search.setEndDate(new Date());
		search.setEndDate(ZonedDateTime.of(2060, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
		AccountHistoryRest accountHistory = accountRestWrapper.getAccountHistory(search);
		assertNotNull(accountHistory);
		return updated;
	}
	
	private void verifyTransactionCalls(List<AccountRest> userActiveAccounts) {
		AccountRest creditAccount = findAccount(userActiveAccounts, ExternalTestCurrency.CREDIT).get();
		BigDecimal creditBalanceInitial = creditAccount.getBalance();
		AccountRest bonusAccount = findAccount(userActiveAccounts, ExternalTestCurrency.BONUS).get();
		BigDecimal bonusBalanceInitial = bonusAccount.getBalance();
		
		BigDecimal loadedEurAmount = new BigDecimal("7");
		LOGGER.info("Loading {} on EUR account", loadedEurAmount);
		WaitingCallback<TransactionRest> loadCallback = new WaitingCallback<>();
		transactionRestWrapper.loadOntoOrWithdrawFromAccount(creditAccount.getId(),
				loadedEurAmount, "Test load onto account", loadCallback);
		TransactionRest loadTransaction = loadCallback.getResult();
		assertNotNull(loadTransaction);
		verifyAccountBalance(creditAccount, creditBalanceInitial.add(loadedEurAmount));
		
		BigDecimal transferedAmount = new BigDecimal("2.5");
		TransactionRestInsert transferTransactionRequest = new TransactionRestInsert();
		transferTransactionRequest.setDebitorAccountId(creditAccount.getId());
		transferTransactionRequest.setCreditorAccountId(bonusAccount.getId());
		//transferTransactionRequest.setCreditorAccountId("0"); does not work
		transferTransactionRequest.setValue(transferedAmount);
		transferTransactionRequest.setReference("Test transfer between accounts");
		transferTransactionRequest.setType(TransactionType.TRANSFER);
		transferTransactionRequest.setUsedExchangeRate(eurToBonusRateValue);
		LOGGER.info("Transfering {} from {} to {} account with rate {}", transferedAmount,
				creditAccount.getCurrency().getShortName(), bonusAccount.getCurrency().getShortName(), eurToBonusRateValue);
		WaitingCallback<TransactionRest> betweenCallback = new WaitingCallback<>();
		transactionRestWrapper.transferBetweenAccounts(transferTransactionRequest, betweenCallback);
		TransactionRest betweenTransaction = betweenCallback.getResult();
		assertNotNull(betweenTransaction);
		assertEquals(creditAccount.getId(), betweenTransaction.getDebitorAccountId());
		assertEquals(bonusAccount.getId(), betweenTransaction.getCreditorAccountId());
		BigDecimal expectedEurBalance = creditBalanceInitial.add(loadedEurAmount).subtract(transferedAmount);
		verifyAccountBalance(creditAccount, expectedEurBalance);
		BigDecimal expectedBonusBalance = bonusBalanceInitial.add(transferedAmount.multiply(eurToBonusRateValue));
		verifyAccountBalance(bonusAccount, expectedBonusBalance);
		
		BigDecimal withdrawEur = loadedEurAmount.subtract(transferedAmount).negate();
		LOGGER.info("Withdrawing {} from {} account", withdrawEur, creditAccount.getCurrency().getShortName());
		WaitingCallback<TransactionRest> eurWithdrawCallback = new WaitingCallback<>();
		transactionRestWrapper.loadOntoOrWithdrawFromAccount(
				creditAccount.getId(), withdrawEur, "Test withdraw from EUR account", eurWithdrawCallback);
		TransactionRest eurWithdrawTransaction = eurWithdrawCallback.getResult();
		
		assertNotNull(eurWithdrawTransaction);
		String withdrawBonusReference = "Test withdraw from BONUS account";
		WaitingCallback<TransactionRest> bonusWithdrawCallback = new WaitingCallback<>();
		transactionRestWrapper.loadOntoOrWithdrawFromAccount(bonusAccount.getId(),
				transferedAmount.multiply(eurToBonusRateValue).negate(), withdrawBonusReference, bonusWithdrawCallback);
		TransactionRest bonusWithdrawTransaction = bonusWithdrawCallback.getResult();
		assertNotNull(bonusWithdrawTransaction);
		verifyAccountBalance(creditAccount, creditBalanceInitial);
		verifyAccountBalance(bonusAccount, bonusBalanceInitial);
		
		WaitingCallback<TransactionRest> transactionCallback = new WaitingCallback<>();
		transactionRestWrapper.getTransaction(bonusWithdrawTransaction.getId(), transactionCallback);
		TransactionRest transaction = transactionCallback.getResult();
		assertEquals(withdrawBonusReference, transaction.getReference());
	}

	private void verifyAccountBalance(AccountRest eurAccount, BigDecimal expectedBalance) {
		AccountRest accountById = accountRestWrapper.getAccountById(eurAccount.getId());
		LOGGER.info("{} account balance now is {}", accountById.getCurrency().getShortName(), accountById.getBalance());
		assertThat(accountById.getBalance(), comparesEqualTo(expectedBalance));
	}
	
	//use PaymentManager instead of copying! -->
	private Optional<AccountRest> findAccount(List<AccountRest> userActiveAccounts, ExternalTestCurrency externalCurrency) {
		return userActiveAccounts.stream().filter(account -> isSameCurrency(externalCurrency, account.getCurrency())).findAny();
	}
    
	private boolean isSameCurrency(ExternalTestCurrency externalCurrency, CurrencyRest currencyRest) {
		return externalCurrency.toString().equals(currencyRest.getShortName());
	}
	//<-- use PaymentManager instead of copying!

    @Test
	public void testErrorMessageOnExchangeRateInsert() throws Exception {
		ExchangeRateRestInsert exchangeRate = new ExchangeRateRestInsert();
		exchangeRate.setFromCurrencyId("-1");
		exchangeRate.setToCurrencyId("-1");
		exchangeRate.setRate(new BigDecimal("-1"));
		try {
			exchangeRateRestWrapper.insertExchangeRate(exchangeRate);
			fail("Exception was expected");
		} catch (F4MPaymentException e) {
			LOGGER.info(e.getMessage());
			assertEquals("CURRENCY_NOT_FOUND - -1", e.getMessage());
		}
	}
    
    @Test
	public void testGetTenantAccounts() throws Exception {
		AccountRest tenantMoneyAccount = accountRestWrapper.getTenantMoneyAccount();
		LOGGER.info("Tenant money account {}", tenantMoneyAccount);
		assertNotNull(tenantMoneyAccount);
		assertEquals("EUR", tenantMoneyAccount.getCurrency().getShortName());
		assertEquals("Euro", tenantMoneyAccount.getCurrency().getName());
		String tenantPaymentSysUserId = tenantMoneyAccount.getUserId();
		assertEquals("F4M", tenantPaymentSysUserId);
		
		//List<AccountRest> userActiveAccounts = userRestWrapper.getUserActiveAccounts(tenantPaymentSysUserId);
		//Returns "{"Code":500,"Message":"USER_NOT_FOUND","AdditionalMessage":"F4M"}"
		
		/*
		AccountRest tenantAccountById0 = accountRestWrapper.getAccountById("0");
		//{"Code":200,"Message":"ACCOUNT_NOT_FOUND","AdditionalMessage":"0"}
		assertEquals("EUR", tenantAccountById0.getCurrency().getShortName());
		assertEquals("Euro", tenantAccountById0.getCurrency().getName());
		String tenantPaymentSysUserIdByAccount0 = tenantAccountById0.getUserId();
		assertEquals("F4M", tenantPaymentSysUserIdByAccount0);
		*/
	}
    
    @Test
	public void getAccountHistory() throws Exception {
    	PaymentClientInfo paymentClientInfo = new PaymentClientInfoImpl(tenantId, profileId, appId);
		GetUserAccountHistoryRequest request = new GetUserAccountHistoryRequest();
		request.setCurrency(Currency.BONUS);
		GetAccountHistoryResponse accountHistory = paymentManager.getAccountHistory(paymentClientInfo, request);
		LOGGER.info("Returned result {}", accountHistory);
	}
}
