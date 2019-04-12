package de.ascendro.f4m.service.payment.exploration;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.payment.callback.WaitingCallback;
import de.ascendro.f4m.service.payment.exception.F4MPaymentClientException;
import de.ascendro.f4m.service.payment.manager.impl.UserAccountManagerImpl;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.PaymentClientInfo;
import de.ascendro.f4m.service.payment.model.config.InsertOrUpdateUserRequest;
import de.ascendro.f4m.service.payment.model.config.InsertOrUpdateUserRequestBuilder;
import de.ascendro.f4m.service.payment.model.config.MergeUsersRequest;
import de.ascendro.f4m.service.payment.model.internal.GetAccountBalanceResponse;
import de.ascendro.f4m.service.payment.model.internal.GetUserAccountBalanceResponse;
import de.ascendro.f4m.service.payment.rest.model.AccountRest;
import de.ascendro.f4m.service.payment.rest.model.TransactionRest;
import de.ascendro.f4m.service.payment.rest.model.UserRest;
import de.ascendro.f4m.service.payment.session.PaymentClientInfoImpl;

public class PaydentUserExplorationTest extends PaydentExplorationTestBase {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(PaydentUserExplorationTest.class);
	
	@Test
	public void testMergeUsers() throws Exception {
		String fromProfileId = UUID.randomUUID().toString();
		String toProfileId = UUID.randomUUID().toString();
		fillUserAccountAndInsert(tenantId, fromProfileId);
		fillUserAccountAndInsert(tenantId, toProfileId);

		AccountRest fromBonusAccount = userAccountManager.findActiveAccount(tenantId, fromProfileId, Currency.BONUS);
		WaitingCallback<TransactionRest> plusOneBonus = new WaitingCallback<>();
		transactionRestWrapper.loadOntoOrWithdrawFromAccount(fromBonusAccount.getId(), BigDecimal.ONE,
				"Test charge to Bonus account", plusOneBonus);
		plusOneBonus.getResult();
		AccountRest fromCreditAccount = userAccountManager.findActiveAccount(tenantId, fromProfileId, Currency.CREDIT);
		
		WaitingCallback<TransactionRest> plusTenCredits = new WaitingCallback<>();
		transactionRestWrapper.loadOntoOrWithdrawFromAccount(fromCreditAccount.getId(), BigDecimal.TEN, "Test charge to Credit account", plusTenCredits);
		plusTenCredits.getResult();
		
		AccountRest toBonusAccount = userAccountManager.findActiveAccount(tenantId, toProfileId, Currency.BONUS);
		WaitingCallback<TransactionRest> plusTenBonus = new WaitingCallback<>();
		transactionRestWrapper.loadOntoOrWithdrawFromAccount(toBonusAccount.getId(), BigDecimal.TEN, "Test charge to Bonus account", plusTenBonus);
		plusTenBonus.getResult();

		MergeUsersRequest request = new MergeUsersRequest();
		request.setTenantId(tenantId);
		request.setFromProfileId(fromProfileId);
		request.setToProfileId(toProfileId);
		
		paymentManager.mergeUserAccounts(request);
		
		AccountRest resultCreditAccount = userAccountManager.findActiveAccount(tenantId, toProfileId, Currency.CREDIT);
		AccountRest resultBonusAccount = userAccountManager.findActiveAccount(tenantId, toProfileId, Currency.BONUS);
		
		assertTrue("Credit ammount should be 10.", BigDecimal.TEN.compareTo(resultCreditAccount.getBalance()) == 0);
		assertTrue("Bonus ammount should be 11.", (new BigDecimal("11")).compareTo(resultBonusAccount.getBalance()) == 0);
	}
	
	@Test
	public void createNewUser() throws Exception {
		long unique = System.currentTimeMillis();
		UserRest userRest = fillUserAccountAndInsert(tenantId, String.valueOf(unique));
		//UserRest userRest = fillUserAccountAndInsert(tenantId, profileId);
		Optional<AccountRest> realEurAccountWithWrongInternalFlag = userRest.getAccounts().stream()
				.filter(account -> !account.getCurrency().getIsInternal()
						&& REAL_EUR_CURRENCY_ID.equals(account.getCurrency().getId()))
				.findFirst();
		LOGGER.warn("IsInternal should be true for real EUR currency, but was {}", realEurAccountWithWrongInternalFlag.get());
		//assertFalse("IsInternal:true is expected to be set on real EUR account", realEurAccountWithWrongInternalFlag.isPresent());
		
		UserRest updateUser = new UserRest();
		updateUser.setUserId(userRest.getUserId());
		updateUser.setZip(UserAccountManagerImpl.UNPARSEABLE_ZIP);
		WaitingCallback<UserRest> updateCallback = new WaitingCallback<>();
		userRestWrapper.updateUser(updateUser, updateCallback);
		updateCallback.getResult();
	}
	
	private UserRest fillUserAccountAndInsert(String tenantId, String profileId) throws Exception {
		String userId = tenantId + "_" + profileId;

		UserRest user = new UserRest();
		user.setUserId(userId); //{"Code":100,"Message":"UNDEFINED_ERROR","AdditionalMessage":"Validation failed for one or more entities. See 'EntityValidationErrors' property for more details."}
		userAccountManager.fillDefaultUserRestValues(user);
		//other mandatory attributes:
		//{"Code":105,"Message":"REQUIRED_PROPERTY_MISSING","AdditionalMessage":"FirstName"}
		//{"Code":105,"Message":"REQUIRED_PROPERTY_MISSING","AdditionalMessage":"Name"}
		//{"Code":105,"Message":"REQUIRED_PROPERTY_MISSING","AdditionalMessage":"Zip"}
		//{"Code":105,"Message":"REQUIRED_PROPERTY_MISSING","AdditionalMessage":"Street"}
		//{"Code":105,"Message":"REQUIRED_PROPERTY_MISSING","AdditionalMessage":"DateOfBirth"}
		WaitingCallback<UserRest> insertCallback = new WaitingCallback<>();
		userRestWrapper.insertUser(user, insertCallback);
		user = insertCallback.getResult();
		
		for(AccountRest account : user.getAccounts()) {
			account.setBalance(new BigDecimal(new Random().nextDouble()));	
		}
		WaitingCallback<UserRest> updateCallback = new WaitingCallback<>();
		userRestWrapper.updateUser(user, updateCallback);
		return updateCallback.getResult();
	}
	
	@Test
	public void createNewUserWithFaultyAssumptionThatItAlreadyExistsInPaymentSystem() throws Exception {
		//Tests the case, when Profile service asked to insert user, but payment system was down.
		//And after some time there are more updates to user profile and payment service is called to update(not create!) user.
		//Payment service should not fail, but try to recover by creating the user.
		InsertOrUpdateUserRequest insertRequest = new InsertOrUpdateUserRequestBuilder()
				.tenantId(tenantId)
				.profileId(String.valueOf(System.currentTimeMillis()))
				.insertNewUser(false) //simulate that profile service did not know, that previous insert failed
				.profile(new JsonObject()).build();
		WaitingCallback<EmptyJsonMessageContent> insertCallback = new WaitingCallback<>();
		userAccountManager.insertOrUpdateUser(insertRequest, insertCallback);
		insertCallback.getResult();
	}
	
	@Test
	public void testFindingNotExistingUser() throws Exception {
		String userId = "NotExistingUserId";
		assertFalse(userAccountManager.userExists(userRestWrapper, userId));
        
        try {
            userRestWrapper.getUserActiveAccounts("Doesn't_exist");
            fail("Exception expected, when retrieving accounts for user which does not exist");
        } catch (F4MPaymentClientException e) {
            assertTrue(e.getMessage().contains("USER_NOT_FOUND"));
        }
	}
	
	@Test
	public void testNonEnglishCharactersInUserData() {
		String firstName = "Romanian-\u0219\u0103\021B\u00EE";
		String lastName = "Latvian-" + LAST_NAME + (int) (Math.random() * 10);
		String street = "German-\u00E4\u00F6\u00DF";
		
		WaitingCallback<UserRest> callback = new WaitingCallback<>();
		UserRest userChanges = new UserRest();
		userChanges.setUserId(userId);
		userChanges.setFirstName(firstName);
		userChanges.setName(lastName);
		userChanges.setStreet(street);
		LOGGER.info("Updating user data {}", userChanges);
		userRestWrapper.updateUser(userChanges , callback);
		UserRest userFromUpdate = callback.getResult();
		UserRest userFromGet = userRestWrapper.getUser(userId);
		try (AutoCloseableSoftAssertions test = new AutoCloseableSoftAssertions()) {
			assertUserData(userFromUpdate, firstName, lastName, street, test, "user from update");
			assertUserData(userFromGet, firstName, lastName, street, test, "user from get");
		}
	}

	private void assertUserData(UserRest userFromUpdate, String firstName, String lastName, String street,
			AutoCloseableSoftAssertions test, String source) {
		test.assertThat(userFromUpdate.getUserId()).as(source).isEqualTo(userId);
		test.assertThat(userFromUpdate.getFirstName()).as(source).isEqualTo(firstName);
		test.assertThat(userFromUpdate.getName()).as(source).isEqualTo(lastName);
		test.assertThat(userFromUpdate.getStreet()).as(source).isEqualTo(street);
	}

	@Test
	public void getUserAccountBalances() throws Exception {
		PaymentClientInfo paymentClientInfo = new PaymentClientInfoImpl(tenantId, profileId, appId);
		GetUserAccountBalanceResponse balances = userAccountManager.getAccountBalances(paymentClientInfo);
		for (GetAccountBalanceResponse balance : balances.getBalances()) {
			LOGGER.info("Returned balance {}", balance);
		}
		List<Currency> balanceCurrencies = balances.getBalances().stream().map(b -> b.getCurrency())
				.collect(Collectors.toList());
		assertThat(balanceCurrencies, containsInAnyOrder(Currency.values()));
	}
}
