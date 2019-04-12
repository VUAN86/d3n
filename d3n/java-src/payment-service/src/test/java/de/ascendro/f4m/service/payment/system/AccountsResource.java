package de.ascendro.f4m.service.payment.system;

import java.math.BigDecimal;
import java.util.Arrays;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.payment.manager.ExternalTestCurrency;
import de.ascendro.f4m.service.payment.manager.PaymentTestUtil;
import de.ascendro.f4m.service.payment.rest.model.AccountHistoryRest;
import de.ascendro.f4m.service.payment.rest.model.AccountRest;
import de.ascendro.f4m.service.payment.rest.wrapper.AccountRestWrapper;

@Path(AccountRestWrapper.URI_PATH)
public class AccountsResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(AccountsResource.class);

	@GET
	@Path(AccountRestWrapper.CLIENTACCOUNT_SUBPATH)
	@Produces(MediaType.APPLICATION_JSON)
	public AccountRest getClientAccount() {
		AccountRest tenantAccount = PaymentTestUtil.createAccount(ExternalTestCurrency.EUR, "0");
		tenantAccount.setBalance(new BigDecimal("12.34"));
		return tenantAccount;
	}

	@GET
	@Path("/{id}/" + AccountRestWrapper.TRANSACTIONS_SUBPATH)
	@Produces(MediaType.APPLICATION_JSON)
	public AccountHistoryRest searchTransactions(@PathParam("id") String accountId, @Context UriInfo uriInfo) {
		MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
		LOGGER.info("Received queryParams {}", queryParams);
		AccountHistoryRest history = new AccountHistoryRest();
		history.setData(Arrays.asList(AccountTransactionsResource.createTransaction()));
		history.setOffset(0);
		history.setLimit(10);
		history.setTotal(history.getData().size());
		return history;
	}
}
