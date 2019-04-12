package de.ascendro.f4m.service.payment.system;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import de.ascendro.f4m.service.payment.manager.ExternalTestCurrency;
import de.ascendro.f4m.service.payment.manager.PaymentTestUtil;
import de.ascendro.f4m.service.payment.rest.model.AccountRest;
import de.ascendro.f4m.service.payment.rest.wrapper.UserRestWrapper;

@Path(UserRestWrapper.URI_PATH)
public class UsersResource {

	@GET
	@Path("/{id}/" + UserRestWrapper.ACCOUNTS_SUBPATH)
	@Produces(MediaType.APPLICATION_JSON)
	public List<AccountRest> getUserAccounts(@PathParam("id") String id) {
		return Arrays.asList(PaymentTestUtil.createAccountBuilder(ExternalTestCurrency.EUR, "1")
				.balance(new BigDecimal("12.34")).build());
	}
}
