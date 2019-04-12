package de.ascendro.f4m.service.payment.manager.impl;

import static de.ascendro.f4m.service.payment.manager.PaymentTestUtil.prepareAccounts;
import static de.ascendro.f4m.service.payment.manager.PaymentTestUtil.prepareCurrencyRest;
import static de.ascendro.f4m.service.payment.manager.impl.UserAccountManagerImpl.DEFAULT_FIRST_NAME;
import static de.ascendro.f4m.service.payment.manager.impl.UserAccountManagerImpl.DEFAULT_LAST_NAME;
import static de.ascendro.f4m.service.payment.manager.impl.UserAccountManagerImpl.DEFAULT_STREET;
import static de.ascendro.f4m.service.payment.manager.impl.UserAccountManagerImpl.UNKNOWN_ZIP;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.payment.callback.CallbackAnswer;
import de.ascendro.f4m.service.payment.callback.TestCallback;
import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.dao.TenantDao;
import de.ascendro.f4m.service.payment.dao.TenantInfo;
import de.ascendro.f4m.service.payment.exception.F4MPaymentClientException;
import de.ascendro.f4m.service.payment.exception.PaymentServiceExceptionCodes;
import de.ascendro.f4m.service.payment.manager.CurrencyManager;
import de.ascendro.f4m.service.payment.manager.ExternalTestCurrency;
import de.ascendro.f4m.service.payment.manager.PaymentTestUtil;
import de.ascendro.f4m.service.payment.manager.PaymentUserIdCalculator;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.PaymentClientInfo;
import de.ascendro.f4m.service.payment.model.config.InsertOrUpdateUserRequest;
import de.ascendro.f4m.service.payment.model.config.InsertOrUpdateUserRequestBuilder;
import de.ascendro.f4m.service.payment.model.internal.GetAccountBalanceRequest;
import de.ascendro.f4m.service.payment.model.internal.GetAccountBalanceResponse;
import de.ascendro.f4m.service.payment.model.internal.GetUserAccountBalanceResponse;
import de.ascendro.f4m.service.payment.rest.model.AccountRest;
import de.ascendro.f4m.service.payment.rest.model.ErrorInfoRest;
import de.ascendro.f4m.service.payment.rest.model.UserRest;
import de.ascendro.f4m.service.payment.rest.model.UserRestInsert;
import de.ascendro.f4m.service.payment.rest.wrapper.AccountRestWrapper;
import de.ascendro.f4m.service.payment.rest.wrapper.CurrencyRestWrapper;
import de.ascendro.f4m.service.payment.rest.wrapper.ExchangeRateRestWrapper;
import de.ascendro.f4m.service.payment.rest.wrapper.UserRestWrapper;
import de.ascendro.f4m.service.payment.session.PaymentClientInfoImpl;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileAddress;
import de.ascendro.f4m.service.profile.model.ProfileUser;
import io.github.benas.randombeans.api.EnhancedRandom;

public class UserAccountManagerImplTest {
	@Mock
	private UserRestWrapper userRestWrapper;
	@Mock
	private AccountRestWrapper accountRestWrapper;
	@Mock
	private ExchangeRateRestWrapper exchangeRateRestWrapper;
	@Mock
	private CurrencyManager currencyManager;
	@Mock
	private TenantDao tenantAerospikeDao;
	@Mock
	private CurrencyRestWrapper currencyRestWrapper;
	@Mock
	private CommonProfileAerospikeDao commonProfileAerospikeDao;

	private UserAccountManagerImpl userAccountManager;
	private TenantInfo tenantInfo;

	private final String tenantId = "tenant";
	private final String profileId = "profile";
	private final String appId = "app";
	private final String paymentUserId = PaymentUserIdCalculator.calcPaymentUserId(tenantId, profileId);

	@Before
	public void setUp() {
		//necessity to inject real currencyManager within test shows too tight coupling between PaymentManagerImpl and CurrencyManager
		MockitoAnnotations.initMocks(this);
		tenantInfo = new TenantInfo();
		tenantInfo.setMainCurrency("EUR");
		when(tenantAerospikeDao.getTenantInfo(anyString())).thenReturn(tenantInfo);
		when(currencyRestWrapper.getCurrencies()).thenReturn(PaymentTestUtil.createCurrencyRestsForVirtualCurrencies());
		when(accountRestWrapper.getTenantMoneyAccount()).thenReturn(PaymentTestUtil.prepareEurAccount());
		CurrencyManager currencyManager = new CurrencyManager((id) -> currencyRestWrapper, (id) -> accountRestWrapper,
				tenantAerospikeDao, (id) -> exchangeRateRestWrapper, new PaymentConfig());
		userAccountManager = new UserAccountManagerImpl(currencyManager, (id) -> userRestWrapper,
				(id) -> accountRestWrapper, new PaymentConfig(), commonProfileAerospikeDao);
	}

	@Test
	public void testUpdateUser() throws Exception {
		InsertOrUpdateUserRequest insertOrUpdateUserRequest = prepareInsertOrUpdateRequestData();
		when(userRestWrapper.getUserActiveAccounts("tenantId_profileId")).thenReturn(Collections.emptyList());
		doAnswer(new CallbackAnswer<>(new UserRest())).when(userRestWrapper).updateUser(any(), any());
		try (TestCallback<EmptyJsonMessageContent> callback = new TestCallback.WithIgnoringConsumer<>()) {
			userAccountManager.insertOrUpdateUser(insertOrUpdateUserRequest, callback);
		}
		verify(userRestWrapper, times(0)).insertUser(any(), any());
		verifyUserUpdateCall();
	}

	@Test
	public void testInsertUser() throws Exception {
		InsertOrUpdateUserRequest insertOrUpdateUserRequest = prepareInsertOrUpdateRequestData();
		insertOrUpdateUserRequest.setInsertNewUser(true);
		ErrorInfoRest errorInfo = new ErrorInfoRest();
		errorInfo.setMessage(ErrorInfoRest.USER_NOT_FOUND);
		String tenantPaymentSystemId = "tenantId_";
		when(userRestWrapper.getUserActiveAccounts(tenantPaymentSystemId))
				.thenThrow(new F4MPaymentClientException(PaymentServiceExceptionCodes.ERR_USER_NOT_FOUND, "no user"))
				.thenReturn(Collections.emptyList()); //assuming previous insert created the tenant
		String profilePaymentSystemId = "tenantId_profileId";
		when(userRestWrapper.getUserActiveAccounts(profilePaymentSystemId))
				.thenThrow(new F4MPaymentClientException(PaymentServiceExceptionCodes.ERR_USER_NOT_FOUND, "no user"))
				.thenReturn(Collections.emptyList()); //assuming previous insert created a user
		doAnswer(new CallbackAnswer<>(new UserRest())).when(userRestWrapper).insertUser(any(), any());
		doAnswer(new CallbackAnswer<>(new UserRest())).when(userRestWrapper).updateUser(any(), any());
		try (TestCallback<EmptyJsonMessageContent> callback = new TestCallback.WithIgnoringConsumer<>()) {
			userAccountManager.insertOrUpdateUser(insertOrUpdateUserRequest, callback);
		}
		ArgumentCaptor<UserRest> tenantInsertArg = ArgumentCaptor.forClass(UserRest.class);
		ArgumentCaptor<UserRest> userInsertArg = ArgumentCaptor.forClass(UserRest.class);
		ArgumentCaptor<UserRest> updateArg = ArgumentCaptor.forClass(UserRest.class);
		ArgumentCaptor<UserRest> updateArg2 = ArgumentCaptor.forClass(UserRest.class);
		verify(userRestWrapper, times(1)).insertUserSynchonously(tenantInsertArg.capture());
		verify(userRestWrapper, times(1)).insertUser(userInsertArg.capture(), any());
		verify(userRestWrapper, times(1)).updateUser(updateArg.capture(), any()); //verify, that after insert also other remaining fields are updated
		verify(currencyRestWrapper, times(1)).getCurrencies(); //check that currency initialisation was called before update

		updateAddressZip(insertOrUpdateUserRequest, "LV-1050"); //not a number, should not fail but produce UNPARSEABLE_ZIP
		insertOrUpdateUserRequest.setInsertNewUser(false);
		try (TestCallback<EmptyJsonMessageContent> callback = new TestCallback.WithIgnoringConsumer<>()) {
			userAccountManager.insertOrUpdateUser(insertOrUpdateUserRequest, callback);
		}
		verify(userRestWrapper, times(2)).updateUser(updateArg2.capture(), any());
		verify(currencyRestWrapper, times(1)).getCurrencies(); //check that currency initialisation was not called before repeated update

		UserRestInsert tenantInsertParam = tenantInsertArg.getValue();
		UserRest userInsertParam = userInsertArg.getValue();
		UserRest userUpdateParam = updateArg.getValue();
		assertEquals(userInsertParam, userUpdateParam);
		UserRest userUpdateParam2 = updateArg2.getAllValues().get(1);
		assertEquals(tenantPaymentSystemId, tenantInsertParam.getUserId());
		assertEquals(profilePaymentSystemId, userInsertParam.getUserId());
		assertEquals(profilePaymentSystemId, userUpdateParam2.getUserId());

		assertEquals("Wall Street", userInsertParam.getStreet()); //default value for required field
		assertNull(userUpdateParam2.getStreet()); //street is not changed, so no need to update it
		assertEquals(UserAccountManagerImpl.UNKNOWN_ZIP, userInsertParam.getZip());
		assertEquals(UserAccountManagerImpl.UNPARSEABLE_ZIP, userUpdateParam2.getZip());
	}

	private void updateAddressZip(InsertOrUpdateUserRequest insertOrUpdateUserRequest, String zip) {
		JsonObject address = new JsonObject();
		address.addProperty(ProfileAddress.ADDRESS_POSTAL_CODE_PROPERTY, zip);
		JsonObject profile = (JsonObject) insertOrUpdateUserRequest.getProfile();
		profile.add(Profile.ADDRESS_PROPERTY, address);
	}

	@Test
	public void testDisableUsers() throws Exception {
		InsertOrUpdateUserRequest insertOrUpdateUserRequest = prepareInsertOrUpdateRequestData();
		String profileId = "toDeleteProfileId";
		insertOrUpdateUserRequest.setProfileId(profileId);
		doAnswer(new CallbackAnswer<>(new UserRest())).when(userRestWrapper).updateUser(any(), any());
		try (TestCallback<EmptyJsonMessageContent> callback = new TestCallback.WithIgnoringConsumer<>()) {
			userAccountManager.insertOrUpdateUser(insertOrUpdateUserRequest, callback);
		}

		userAccountManager.disableUser(tenantId, PaymentUserIdCalculator.calcPaymentUserId(tenantId, profileId));
		ArgumentCaptor<String> userId = ArgumentCaptor.forClass(String.class);
		verify(userRestWrapper).disableUser(userId.capture());
		assertEquals(PaymentUserIdCalculator.calcPaymentUserId(tenantId, profileId), userId.getValue());
	}

	private void verifyUserUpdateCall() {
		ArgumentCaptor<UserRest> argument = ArgumentCaptor.forClass(UserRest.class);
		verify(userRestWrapper, times(1)).updateUser(argument.capture(), any());
		assertThat(argument.getValue().getUserId(), equalTo("tenantId_profileId"));
		assertThat(argument.getValue().getFirstName(), equalTo("First"));
		assertThat(argument.getValue().getName(), equalTo("Name"));
	}

	private InsertOrUpdateUserRequest prepareInsertOrUpdateRequestData() {
		JsonObject profile = prepareProfileData();
		InsertOrUpdateUserRequest insertOrUpdateUserRequest = new InsertOrUpdateUserRequestBuilder()
				.profileId("profileId").tenantId("tenantId").profile(profile).build();
		return insertOrUpdateUserRequest;
	}

	private JsonObject prepareProfileData() {
		return prepareProfileData("First", "Name");
	}
	
	public static JsonObject prepareProfileData(String firstName, String lastName) {
		JsonObject person = new JsonObject();
		person.addProperty(ProfileUser.PERSON_FIRST_NAME_PROPERTY, firstName);
		person.addProperty(ProfileUser.PERSON_LAST_NAME_PROPERTY, lastName);
		JsonObject profile = new JsonObject();
		profile.add(Profile.PERSON_PROPERTY, person);
		return profile;
	}

	@Test
	public void testZipConvertion() throws Exception {

	}

	@Test
	public void testFindActiveAccountIdForNormalProfile() throws Exception {
		String eurAccountId = "eurAccountId";
		String bonusAccountId = "bonusAccountId";
		List<AccountRest> accounts = prepareAccounts(eurAccountId, bonusAccountId);
		when(userRestWrapper.getUserActiveAccounts(paymentUserId)).thenReturn(accounts);

		assertEquals(eurAccountId, userAccountManager.findActiveAccountId(tenantId, profileId, Currency.MONEY));
		assertEquals(bonusAccountId, userAccountManager.findActiveAccountId(tenantId, profileId, Currency.BONUS));
	}

	@Test
	public void testFindActiveAccountIdForTenant() throws Exception {
		String eurAccountId = "eurAccountId";
		String bonusAccountId = "bonusAccountId";
		List<AccountRest> accounts = prepareAccounts(eurAccountId, bonusAccountId);
		when(userRestWrapper.getUserActiveAccounts("tenant_")).thenReturn(accounts);
		when(accountRestWrapper.getTenantMoneyAccount()).thenReturn(PaymentTestUtil.prepareEurAccount());
		PaymentConfig config = new PaymentConfig();
		assertEquals(config.getProperty(PaymentConfig.TENANT_MONEY_ACCOUNT_ID), userAccountManager.findActiveAccountId(tenantId, null, Currency.MONEY));
		assertEquals(bonusAccountId, userAccountManager.findActiveAccountId(tenantId, null, Currency.BONUS));
	}

	@Test
	public void testGetAccountBalance() throws Exception {
		GetAccountBalanceRequest request = new GetAccountBalanceRequest();
		request.setTenantId(tenantId);
		request.setProfileId(profileId);
		request.setCurrency(Currency.MONEY);
		PaymentClientInfo paymentClientInfo = new PaymentClientInfoImpl(tenantId, profileId, appId);

		AccountRest account = EnhancedRandom.random(AccountRest.class);
		account.setUserId(paymentUserId);
		account.setCurrency(prepareCurrencyRest(ExternalTestCurrency.EUR));
		when(userRestWrapper.getUserActiveAccounts(paymentUserId)).thenReturn(Arrays.asList(account));

		GetAccountBalanceResponse response = userAccountManager.getAccountBalance(paymentClientInfo, request);

		ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
		verify(userRestWrapper).getUserActiveAccounts(argument.capture());
		assertThat(argument.getValue(), equalTo(paymentUserId));

		assertThat(response.getAmount(), equalTo(account.getBalance()));
		assertThat(response.getCurrency(), equalTo(request.getCurrency()));
	}

	@Test
	public void testGetAccountBalances() throws Exception {
		PaymentClientInfo paymentClientInfo = new PaymentClientInfoImpl(tenantId, profileId, appId);
		AccountRest account = EnhancedRandom.random(AccountRest.class);
		account.setUserId(paymentUserId);
		account.setCurrency(prepareCurrencyRest(ExternalTestCurrency.EUR));
		UserRest user = new UserRest();
		user.setAccounts(Arrays.asList(account));
		when(userRestWrapper.getUser(paymentUserId)).thenReturn(user);

		GetUserAccountBalanceResponse response = userAccountManager.getAccountBalances(paymentClientInfo);

		assertThat(response.getBalances(), hasSize(1));
		GetAccountBalanceResponse balance = response.getBalances().get(0);
		assertThat(balance.getAmount(), equalTo(account.getBalance()));
		assertThat(balance.getCurrency(), equalTo(Currency.MONEY));
	}

	@Test
	public void prepareUserChangesWithEmptyStreetAndLastNameFilledFromDefaults() throws Exception {
		Profile profile = new Profile(prepareProfileData());
		profile.setAddress(new ProfileAddress());
		profile.getAddress().setStreet("");
		profile.getAddress().setPostalCode(null);
		profile.getPersonWrapper().setFirstName("");
		profile.getPersonWrapper().setLastName(null);
		UserRest userRest = userAccountManager.prepareUserChanges("x", profile, false);
		assertThat(userRest.getFirstName(), equalTo(DEFAULT_FIRST_NAME));
		assertThat(userRest.getName(), equalTo(DEFAULT_LAST_NAME));
		assertThat(userRest.getStreet(), equalTo(DEFAULT_STREET));
		assertThat(userRest.getZip(), equalTo(UNKNOWN_ZIP));
	}
}
