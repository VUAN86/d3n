package de.ascendro.f4m.service.payment.rest.wrapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.core.GenericType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.payment.callback.Callback;
import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.di.RestClientProvider;
import de.ascendro.f4m.service.payment.rest.model.AccountRest;
import de.ascendro.f4m.service.payment.rest.model.IdentityRest;
import de.ascendro.f4m.service.payment.rest.model.UserListRestResponse;
import de.ascendro.f4m.service.payment.rest.model.UserRest;
import de.ascendro.f4m.service.payment.rest.model.UserRestInsert;

public class UserRestWrapper extends RestWrapper {
	private static final Logger LOGGER = LoggerFactory.getLogger(UserRestWrapper.class);
	
	public static final String URI_PATH = "users";
	public static final String ACCOUNTS_SUBPATH = "accounts";
	private static final String IDENTITY_SUBPATH = "identity";

	@Inject
	public UserRestWrapper(@Assisted String tenantId, RestClientProvider restClientProvider,
			PaymentConfig paymentConfig, LoggingUtil loggingUtil) {
		super(tenantId, restClientProvider, paymentConfig, loggingUtil);
	}
	
	//used only in exploration tests
	public List<UserRest> getUsers(Integer limit, Integer offset) {
		Map<String, Object> params = new HashMap<>();
		addToSearchParams(params, "Limit", limit);
		addToSearchParams(params, "Offset", offset);
		UserListRestResponse userResponse = callGet(UserListRestResponse.class, params);
		LOGGER.info("UserList response was {}", userResponse);
		return userResponse.getData();
	}
	
	public UserRest getUser(String userId) {
		UserRest user = callGet(UserRest.class, null, userId);
		LOGGER.info("User response was {}", user);
		return user;
	}
	
	public IdentityRest getUserIdentity(String userId) {
		IdentityRest identityResponse = callGet(IdentityRest.class, null, userId, IDENTITY_SUBPATH);
		LOGGER.info("User identity response was {}", identityResponse);
		return identityResponse;
	}
	
	public List<AccountRest> getUserActiveAccounts(String userId) {
		List<AccountRest> accounts = callGet(new GenericType<List<AccountRest>>(){}, null, userId, ACCOUNTS_SUBPATH);
		LOGGER.info("User {} accounts response was {}", userId, accounts);
		return accounts;
	}
	
	public void insertUser(UserRestInsert user, Callback<UserRest> callback) {
		callPostAsync(user, UserRest.class, callback);
	}	
	
	public UserRest insertUserSynchonously(UserRestInsert user) {
		UserRest entity = callPost(user, UserRest.class);
		LOGGER.info("User inserted {}", entity);
		return entity;
	}

	public UserRest disableUser(String userId) {
		UserRest userInfo = new UserRest();
		userInfo.setUserId(userId);
		userInfo.setDisabled(true);
		return updateUserSynchonously(userInfo);
	}

	private UserRest updateUserSynchonously(UserRest userChanges) {
		UserRest entity = callPut(userChanges, UserRest.class);
		LOGGER.info("User updated {}", entity);
		return entity;
	}

	public void updateUser(UserRest userChanges, Callback<UserRest> callback) {
		callPutAsync(userChanges, UserRest.class, callback);
	}

	@Override
	protected String getUriPath() {
		return URI_PATH;
	}

}