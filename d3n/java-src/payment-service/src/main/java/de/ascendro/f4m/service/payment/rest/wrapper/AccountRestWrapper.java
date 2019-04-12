package de.ascendro.f4m.service.payment.rest.wrapper;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.di.RestClientProvider;
import de.ascendro.f4m.service.payment.rest.model.AccountHistoryRest;
import de.ascendro.f4m.service.payment.rest.model.AccountHistoryRestSearchParams;
import de.ascendro.f4m.service.payment.rest.model.AccountRest;
import de.ascendro.f4m.service.payment.rest.model.MergeAccountsRest;
import de.ascendro.f4m.service.util.DateTimeUtil;

/**
 * 	Account erstellen: POST /accounts - insertAccount not implemented since it is already created when creating a user
 * 	Account deaktivieren: PUT /accounts - disableAccount not needed currently
 * 	Account reaktivieren: PUT /accounts - reenableAccount not needed currently
 * 	Account auflosen: PUT /accounts - closing of an account not needed currently
 * 	Account konvertieren: PUT /accounts - switching of account currency not needed currently
 */
public class AccountRestWrapper extends RestWrapper {
	private static final Logger LOGGER = LoggerFactory.getLogger(AccountRestWrapper.class);

	public static final String URI_PATH = "accounts";
	public static final String TRANSACTIONS_SUBPATH = "transactions";
	public static final String CLIENTACCOUNT_SUBPATH = "clientaccount";

	@Inject
	public AccountRestWrapper(@Assisted String tenantId, RestClientProvider restClientProvider,
			PaymentConfig paymentConfig, LoggingUtil loggingUtil) {
		super(tenantId, restClientProvider, paymentConfig, loggingUtil);
	}

	public AccountRest getAccountById(String accountId) {
		AccountRest account = callGet(AccountRest.class, null, accountId);
		LOGGER.info("Returned account {}", account);
		return account;
	}
	
	public AccountRest getTenantMoneyAccount() {
		AccountRest account = callGet(AccountRest.class, null, CLIENTACCOUNT_SUBPATH);
		LOGGER.info("Returned tenant account {}", account);
		return account;
	}

	public AccountHistoryRest getAccountHistory(AccountHistoryRestSearchParams search) {
		//change all lists to respond with ListResponse containing totalCount etc?
		Map<String, Object> params = new HashMap<>();
		addToSearchParams(params, "Limit", search.getLimit());
		addToSearchParams(params, "Offset", search.getOffset());
		addDateToSearchParams(params, "StartDate", search.getStartDate());
		addDateToSearchParams(params, "EndDate", search.getEndDate());
		addToSearchParams(params, "TypeFilter", search.getTypeFilter().name());
		AccountHistoryRest transactions = callGet(AccountHistoryRest.class, params, search.getAccountId(),
				TRANSACTIONS_SUBPATH);
		LOGGER.info("Account history for {} was {}", search, transactions);
		return transactions;
	}
	
	public AccountRest mergeAccounts(MergeAccountsRest request) {
		AccountRest account = callPut(request, AccountRest.class);
		LOGGER.info("Account merge for {} was {}", request, account);
		return account;
	}
	
	private void addDateToSearchParams(Map<String, Object> params, String paramName, ZonedDateTime date) {
		if (date != null) {
			params.put(paramName, date.format(DateTimeFormatter.ofPattern(DateTimeUtil.JSON_DATE_FORMAT)));
		}
	}

	@Override
	protected String getUriPath() {
		return URI_PATH;
	}
}