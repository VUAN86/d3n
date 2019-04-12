package de.ascendro.f4m.service.payment.manager.impl;

import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.payment.callback.Callback;
import de.ascendro.f4m.service.payment.callback.ManagerCallback;
import de.ascendro.f4m.service.payment.callback.WaitingCallback;
import de.ascendro.f4m.service.payment.callback.WrapperCallback;
import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.exception.F4MMoneyAccountNotInitializedException;
import de.ascendro.f4m.service.payment.exception.F4MPaymentClientException;
import de.ascendro.f4m.service.payment.exception.PaymentServiceExceptionCodes;
import de.ascendro.f4m.service.payment.manager.CurrencyManager;
import de.ascendro.f4m.service.payment.manager.PaymentUserIdCalculator;
import de.ascendro.f4m.service.payment.manager.UserAccountManager;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.PaymentClientInfo;
import de.ascendro.f4m.service.payment.model.config.InsertOrUpdateUserRequest;
import de.ascendro.f4m.service.payment.model.internal.GetAccountBalanceRequest;
import de.ascendro.f4m.service.payment.model.internal.GetAccountBalanceResponse;
import de.ascendro.f4m.service.payment.model.internal.GetUserAccountBalanceResponse;
import de.ascendro.f4m.service.payment.payment.system.manager.AccountBalanceManager;
import de.ascendro.f4m.service.payment.payment.system.model.TenantBalance;
import de.ascendro.f4m.service.payment.payment.system.repository.TenantBalanceRepository;
import de.ascendro.f4m.service.payment.rest.model.AccountRest;
import de.ascendro.f4m.service.payment.rest.model.CurrencyRest;
import de.ascendro.f4m.service.payment.rest.model.UserRest;
import de.ascendro.f4m.service.payment.rest.model.UserRestInsert;
import de.ascendro.f4m.service.payment.rest.wrapper.AccountRestWrapper;
import de.ascendro.f4m.service.payment.rest.wrapper.PaymentExternalErrorCodes;
import de.ascendro.f4m.service.payment.rest.wrapper.RestWrapperFactory;
import de.ascendro.f4m.service.payment.rest.wrapper.UserRestWrapper;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileAddress;
import de.ascendro.f4m.service.profile.model.ProfileUser;
import de.ascendro.f4m.service.util.DateTimeUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class UserAccountManagerImpl implements UserAccountManager {
	public static final String UNKNOWN_ZIP = "1";
	public static final Long UNPARSEABLE_ZIP = 2L;
	protected static final Long ZIP_NOT_ALLOWED_VALUE = 0L;
	protected static final String DEFAULT_FIRST_NAME = "Ano";
	protected static final String DEFAULT_LAST_NAME = "Nymous";
	protected static final String DEFAULT_STREET = "Wall Street";
	
	private static final Logger LOGGER = LoggerFactory.getLogger(UserAccountManagerImpl.class);

	@Inject
	private AccountBalanceManager accountBalanceManager;

	private CurrencyManager currencyManager;
	private RestWrapperFactory<UserRestWrapper> userRestWrapperFactory;
	private RestWrapperFactory<AccountRestWrapper> accountRestWrapperFactory;
	private PaymentConfig paymentConfig;
	private CommonProfileAerospikeDao commonProfileAerospikeDao;
	
	@Inject
	public UserAccountManagerImpl(CurrencyManager currencyManager,
			RestWrapperFactory<UserRestWrapper> userRestWrapperFactory,
			RestWrapperFactory<AccountRestWrapper> accountRestWrapperFactory,
			PaymentConfig paymentConfig,
			CommonProfileAerospikeDao commonProfileAerospikeDao) {
		this.currencyManager = currencyManager;
		this.userRestWrapperFactory = userRestWrapperFactory;
		this.accountRestWrapperFactory = accountRestWrapperFactory;
		this.paymentConfig = paymentConfig;
		this.commonProfileAerospikeDao = commonProfileAerospikeDao;
	}
	
	private void initialize(String tenantId) {
		//This is disputable - should user related call initiate currencies.
		//But from the business point of view, this should be the first call - without users there are no transactions, no nothing.
		//Only possible exception - paying in money to tenant's account, but currently there is no info about that.
		boolean initializationExecuted = currencyManager.initSystemCurrencies(tenantId);
		if (initializationExecuted) {
			initTenantUserIfNecessary(tenantId);
		}
	}
	
	private void initTenantUserIfNecessary(String tenantId) {
		UserRestWrapper userRestWrapper = userRestWrapperFactory.create(tenantId);
		String userId = PaymentUserIdCalculator.calcPaymentUserId(tenantId, null);
		if (!userExists(userRestWrapper, userId)) {
			UserRestInsert tenant = new UserRestInsert();
			fillDefaultUserRestValues(tenant);
			tenant.setUserId(userId);
			tenant.setFirstName("Tenant");
			tenant.setName("Tenant");
			LOGGER.info("Creating user for representing tenant {}", tenant);
			userRestWrapper.insertUserSynchonously(tenant); //wait for call to finish - it is mandatory to have tenant user created!
		}
	}
	
	@Override
	public GetAccountBalanceResponse getTenantAccountBalance(String tenantId) {
		TenantBalance tenantBalance = accountBalanceManager.getTenantBalance(tenantId);
		return new GetAccountBalanceResponse(tenantBalance.getBalance(), Currency.MONEY);
	}

	@Override
	public GetAccountBalanceResponse getAccountBalance(PaymentClientInfo paymentClientInfo, GetAccountBalanceRequest request) {
		AccountRest account = findActiveAccount(paymentClientInfo.getTenantId(), paymentClientInfo.getProfileId(), request.getCurrency());
		return convertAccountRestToBalanceResponse(account, paymentClientInfo);
	}

	@Override
	public GetUserAccountBalanceResponse getAccountBalances(PaymentClientInfo paymentClientInfo) {
		List<AccountRest> accounts = findAllAccounts(paymentClientInfo.getTenantId(), paymentClientInfo.getProfileId());
		GetUserAccountBalanceResponse response = new GetUserAccountBalanceResponse();
		response.setBalances(new ArrayList<>(Currency.values().length));
		for (Currency currency : Currency.values()) {
			CurrencyRest currencyRest = currencyManager.getCurrencyRestByCurrencyEnum(paymentClientInfo.getTenantId(), currency);
			try {
				AccountRest account = findAccountFromListByExtenalCurrency(accounts, paymentClientInfo.getTenantId(),
						paymentClientInfo.getProfileId(), currencyRest);
				response.getBalances().add(convertAccountRestToBalanceResponse(account, paymentClientInfo));
			} catch (F4MEntryNotFoundException | F4MMoneyAccountNotInitializedException e) {
				LOGGER.warn("Account with currency {}/{} not found, not returning to balances list", currency,
						currencyRest, e);
			}
		}
		return response;
	}
	
	private GetAccountBalanceResponse convertAccountRestToBalanceResponse(AccountRest account, PaymentClientInfo paymentClientInfo) {
		GetAccountBalanceResponse balance = new GetAccountBalanceResponse();
		balance.setAmount(account.getBalance());
		balance.setSubcurrency(account.getCurrency().getShortName());
		balance.setCurrency(
				currencyManager.getCurrencyEnumByCurrencyRest(paymentClientInfo.getTenantId(), account.getCurrency()));
		return balance;
		
	}

	@Override
	public String findAccountIdFromAllAccounts(String tenantId, String profileId, Currency currency) {
		String accountId;
		try {
			accountId = findActiveAccount(tenantId, profileId, currency).getId();
		} catch (F4MMoneyAccountNotInitializedException e) {
			if (Currency.MONEY.equals(currency)) {
				String userId = PaymentUserIdCalculator.calcPaymentUserId(tenantId, profileId);
				LOGGER.warn("MONEY account not initialized for user {}, using workaround via userUpdate to retrieve it", userId);
				UserRest fakeUserChanges = new UserRest();
				fakeUserChanges.setUserId(userId);
				UserRest updateUser = userRestWrapperFactory.create(tenantId).getUser(userId);
				//TODO: previously updateUser was userRestWrapperFactory.create(tenantId).updateUser(fakeUserChanges), check GET user also returns also inactive EUR account
				accountId = findAccountFromListOrUseTenantEurAccountId(updateUser.getAccounts(), tenantId, profileId, currency);
			} else {
				throw e;
			}
		}
		return accountId;
	}
	
	@Override
	public String findActiveAccountId(String tenantId, String profileId, Currency currency) {
		String accountId;
		LOGGER.debug("findActiveAccountId tenantId {}  ", getTenantMoneyAccountId());
		if (PaymentUserIdCalculator.isDestinationTenantAccount(profileId) && Currency.MONEY == currency) {
			return getTenantMoneyAccountId();
		} else {
			accountId = findActiveAccount(tenantId, profileId, currency).getId();
		}
		return accountId;
	}
	
	@Override
	public AccountRest findActiveAccount(String tenantId, String profileId, Currency currency) {
		List<AccountRest> userActiveAccounts = userRestWrapperFactory.create(tenantId)
				.getUserActiveAccounts(PaymentUserIdCalculator.calcPaymentUserId(tenantId, profileId));
		CurrencyRest currencyRest = currencyManager.getCurrencyRestByCurrencyEnum(tenantId, currency);
		return findAccountFromListByExtenalCurrency(userActiveAccounts, tenantId, profileId, currencyRest);
	}

	@Override
	public String findActiveGameAccountId(String tenantId, String profileId, Currency currency) {
		String accountId;
		if (PaymentUserIdCalculator.isDestinationTenantAccount(profileId) && Currency.MONEY == currency) {
			return getTenantMoneyAccountId();
		} else {
			accountId = findActiveGameAccount(tenantId, profileId, currency).getId();
		}
		return accountId;
	}

	@Override
	public AccountRest findActiveGameAccount(String tenantId, String profileId, Currency currency) {
		List<AccountRest> userActiveAccounts = userRestWrapperFactory.create(tenantId)
				.getUserActiveAccounts(profileId);
		CurrencyRest currencyRest = currencyManager.getCurrencyRestByCurrencyEnum(tenantId, currency);
		return findAccountFromListByExtenalCurrency(userActiveAccounts, tenantId, profileId, currencyRest);
	}

	@Override
	public List<AccountRest> findActiveAccounts(String tenantId, String profileId) {
		return userRestWrapperFactory.create(tenantId)
				.getUserActiveAccounts(PaymentUserIdCalculator.calcPaymentUserId(tenantId, profileId));
	}

	@Override
	public String findAccountFromListOrUseTenantEurAccountId(List<AccountRest> userActiveAccounts, String tenantId,
			String profileId, Currency currency) {
		LOGGER.debug("findAccountFromListOrUseTenantEurAccountId 1");
		if (PaymentUserIdCalculator.isDestinationTenantAccount(profileId) && Currency.MONEY == currency) {
			LOGGER.debug("findAccountFromListOrUseTenantEurAccountId 2");
			return getTenantMoneyAccountId();
		} else {
			LOGGER.debug("findAccountFromListOrUseTenantEurAccountId 3");
			CurrencyRest currencyRest = currencyManager.getCurrencyRestByCurrencyEnum(tenantId, currency);
			LOGGER.debug("findAccountFromListOrUseTenantEurAccountId 4");
			return findAccountFromListByExtenalCurrency(userActiveAccounts, tenantId, profileId, currencyRest).getId();
		}
	}

	@Override
	public List<AccountRest> findAllAccounts(String tenantId, String profileId) {
		String userId = PaymentUserIdCalculator.calcPaymentUserId(tenantId, profileId);
		UserRest user = userRestWrapperFactory.create(tenantId).getUser(userId);
		if (user == null) {
			LOGGER.error("User not found {}", userId);
			throw new F4MPaymentClientException(PaymentExternalErrorCodes.USER_NOT_FOUND.getF4MCode(),
					"User not found " + userId);
		}
		return user.getAccounts();
	}

	@Override
	public AccountRest findAccountFromListByExtenalCurrency(List<AccountRest> userActiveAccounts, String tenantId,
			String profileId, CurrencyRest currencyRest) {
		Optional<AccountRest> first = userActiveAccounts.stream()
				.filter(account -> account.getCurrency().getId().equals(currencyRest.getId())).findFirst();
		//Could it happen that there are multiple accounts with the same currency? Should not.
		if (!first.isPresent()) {
			Currency currency = currencyManager.getCurrencyEnumByCurrencyRest(tenantId, currencyRest);
			if (Currency.MONEY.equals(currency)) {
				LOGGER.info(
						"No {} account found for {}_{}, please initialize the account by paying in money into account",
						currency, tenantId, profileId);
				throw new F4MMoneyAccountNotInitializedException("Account not initialized");
			} else {
				LOGGER.error("Could not find account for {}, {}, {}", tenantId, profileId, currencyRest);
				throw new F4MEntryNotFoundException("Account with given currency not found");
			}
		}
		return first.get();
	}

	@Override
	public AccountRest findAccountByExternalCurrency(String tenantId, String profileId, String currencyShortName) {
		UserRestWrapper userRestWrapper = userRestWrapperFactory.create(tenantId);
		List<AccountRest> activeAccounts = userRestWrapper
				.getUserActiveAccounts(PaymentUserIdCalculator.calcPaymentUserId(tenantId, profileId));

		AccountRest account = null;
		if (activeAccounts != null && !activeAccounts.isEmpty()) {
			Optional<AccountRest> first = activeAccounts.stream().filter(a -> currencyShortName.equals(a.getCurrency().getShortName()))
					.findFirst();
			if (first.isPresent()) {
				account = first.get();
			}
		}
		if (account == null) {
			throw new F4MEntryNotFoundException(
					String.format("User account with given currency %s not found", currencyShortName));
		}
		return account;
	}
	
	@Override
	public void createUserFromLatestAerospikeData(String tenantId, String profileId) {
		InsertOrUpdateUserRequest request = new InsertOrUpdateUserRequest();
		request.setInsertNewUser(true);
		request.setTenantId(tenantId);
		request.setProfileId(profileId);
		
		Profile profile = commonProfileAerospikeDao.getProfile(profileId);
		request.setProfile(profile.getJsonObject());
		
		WaitingCallback<EmptyJsonMessageContent> callback = new WaitingCallback<>();
		insertOrUpdateUser(request, callback);
		callback.getResult();
	}

	@Override
	public void insertOrUpdateUser(InsertOrUpdateUserRequest request, Callback<EmptyJsonMessageContent> originalCallback) {
		//initialize(request.getTenantId());
		
//		Profile service should have checked if all fields are set and if user is registered (UserRole.ANONYMOUS is not in his roles)
		String userId = PaymentUserIdCalculator.calcPaymentUserId(request.getTenantId(), request.getProfileId());
		UserRestWrapper userRestWrapper = userRestWrapperFactory.create(request.getTenantId());

		Profile profile = new Profile(request.getProfile());
		LOGGER.debug("insertOrUpdateUser profile {} ", profile);
		boolean userExists = !request.isInsertNewUser();
		UserRest userChanges = prepareUserChanges(userId, profile, userExists);
		LOGGER.debug("insertOrUpdateUser userChanges {} ", userChanges);

		if (!userExists) {
			LOGGER.debug("Inserting user {}", userChanges);
			accountBalanceManager.createUser(request);
//			userRestWrapper.insertUser(userChanges,
//					new WrapperCallback<UserRest, EmptyJsonMessageContent>(originalCallback) {
//						@Override
//						protected void executeOnCompleted(UserRest response) {
//							updateUserWithRetry(userRestWrapper, userChanges, profile, originalCallback);
//						}
//					});
		}
//		else {
//			updateUserWithRetry(userRestWrapper, userChanges, profile, originalCallback);
//		}
	}

	@Override
	public UserRest disableUser(String tenantId, String userId) {
		return userRestWrapperFactory.create(tenantId).disableUser(userId);
	}

	protected UserRest prepareUserChanges(String userId, Profile profile, boolean userExists) {
		UserRest userChanges = new UserRest();
		userChanges.setUserId(userId);
		if (!userExists) { //default values:
			fillDefaultUserRestValues(userChanges);
		}
		
		//update de.ascendro.f4m.service.profile.model.ProfileUpdateValidator if change this mapping!
		Optional<ProfileUser> profileUser = Optional.of(profile).map(Profile::getPersonWrapper);
		profileUser.map(ProfileUser::getFirstName).map(StringUtils::stripToNull).ifPresent(userChanges::setFirstName);
		profileUser.map(ProfileUser::getLastName).map(StringUtils::stripToNull).ifPresent(userChanges::setName);
		profileUser.map(ProfileUser::getBirthDate).ifPresent(userChanges::setDateOfBirth);

		Optional<ProfileAddress> address = Optional.of(profile).map(Profile::getAddress);
		address.map(ProfileAddress::getStreet).map(StringUtils::stripToNull).ifPresent(userChanges::setStreet);
		address.map(ProfileAddress::getPostalCode).ifPresent(userChanges::setZip);
		//probably some more fields might/should be synchronized to payment system...
		return userChanges;
	}

	private void updateUserWithRetry(UserRestWrapper userRestWrapper, UserRest userChanges, Profile profile,
			Callback<EmptyJsonMessageContent> originalCallback) {
		WrapperCallback<UserRest, EmptyJsonMessageContent> insertCallback = new WrapperCallback<UserRest, EmptyJsonMessageContent>(
				originalCallback) {
			@Override
			protected void executeOnCompleted(UserRest response) {
				LOGGER.warn("User insert was success after failed update for user, now updating additional data {}", userChanges);
				ManagerCallback<UserRest, ? extends JsonMessageContent> updateCallback2 = new ManagerCallback<>(
						originalCallback, updatedUser -> new EmptyJsonMessageContent());
				userRestWrapper.updateUser(userChanges, updateCallback2);
			}
		};
		userRestWrapper.updateUser(userChanges, new ManagerCallback<UserRest, EmptyJsonMessageContent>(originalCallback,
				updatedUser -> new EmptyJsonMessageContent()) {
			@Override
			protected void executeOnFailed(Throwable e) {
				LOGGER.warn("User update failed, trying to insert new user if it does not exist with data {}",
						userChanges, e);
				boolean userExists = userExists(userRestWrapper, userChanges.getUserId());
				if (!userExists) {
					LOGGER.debug("Verified that user doesn't exist, trying to insert new user if it does not exist with data {}",
							userChanges, e);
					UserRest userInsert = prepareUserChanges(userChanges.getUserId(), profile, false);
					userRestWrapper.insertUser(userInsert, insertCallback);
				} else {
					LOGGER.error("User already exists {}, restoring original error", userChanges, e);
					originalCallback.failed(e);
				}
			}
		});
	}
	
//	protected Long convertStringZipCodeToLong(String zip) {
//		Long result;
//		try {
//			result = new Long(zip);
//		} catch (NumberFormatException e) {
//			LOGGER.warn("Substituting user.zip to '{}' because '{}' is not a number value as requested by payment system",
//					UNPARSEABLE_ZIP, zip);
//			result = UNPARSEABLE_ZIP;
//		}
//		if (result.equals(ZIP_NOT_ALLOWED_VALUE)) {
//			LOGGER.warn("Substituting user.zip to '{}' because '{}' is not a valid zip value in payment system",
//					UNKNOWN_ZIP, result);
//			result = UNKNOWN_ZIP;
//		}
//		return result;
//	}

	public void fillDefaultUserRestValues(UserRestInsert user) {
		user.setFirstName(DEFAULT_FIRST_NAME);
		user.setName(DEFAULT_LAST_NAME);
		user.setDateOfBirth(DateTimeUtil.getCurrentDateTime());
		user.setStreet(DEFAULT_STREET);
		user.setZip(UNKNOWN_ZIP);
	}

	public boolean userExists(UserRestWrapper userRestWrapper, String userId) {
		try {
			//there is no call to get user - therefore we try to get user and catch exception if it fails
			userRestWrapper.getUserActiveAccounts(userId);
			return true;
		} catch (F4MPaymentClientException e) {
			if (PaymentServiceExceptionCodes.ERR_USER_NOT_FOUND.equals(e.getCode())) {
				return false;
			} else {
				throw e;
			}
		}
	}

	@Override
	public String[] getUserRoles(String userId, String tenantId) {
		String[] roleList;
		Profile profile = commonProfileAerospikeDao.getProfile(userId);
		if (profile != null) {
			roleList = profile.getRoles(tenantId);
		} else {
			roleList = new String[0];
		}
		return roleList;
	}

	@Override
	public Set<String> getProfileTenants(String profileId) {
		Profile profile = commonProfileAerospikeDao.getProfile(profileId);
		return profile == null ? null : profile.getTenants();
	}


	public String getTenantMoneyAccountId() {
		return paymentConfig.getProperty(PaymentConfig.TENANT_MONEY_ACCOUNT_ID);
	}
	

}
