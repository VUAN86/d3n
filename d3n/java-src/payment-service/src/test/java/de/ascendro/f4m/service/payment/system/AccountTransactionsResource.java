package de.ascendro.f4m.service.payment.system;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import de.ascendro.f4m.service.payment.manager.ExternalTestCurrency;
import de.ascendro.f4m.service.payment.manager.PaymentTestUtil;
import de.ascendro.f4m.service.payment.rest.model.TransactionRest;
import de.ascendro.f4m.service.payment.rest.model.TransactionType;
import de.ascendro.f4m.service.payment.rest.wrapper.TransactionRestWrapper;

@Path(TransactionRestWrapper.URI_PATH)
public class AccountTransactionsResource {

	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public TransactionRest getTransation(@PathParam("id") String id) {
		return createTransaction();
	}
	
	public static TransactionRest createTransaction() {
		TransactionRest transaction = new TransactionRest();
		transaction.setId("transactionId");
		transaction.setType(TransactionType.TRANSFER);
		transaction.setCreated(ZonedDateTime.parse("2017-01-11T14:37:59+00:00[GMT]"));
		transaction.setValue(new BigDecimal("12.34"));
		transaction.setDebitorAccount(PaymentTestUtil.createAccountBuilder(ExternalTestCurrency.EUR, "123")
				.userId("tenant_fromProfile").build());
		transaction.setCreditorAccount(PaymentTestUtil.createAccountBuilder(ExternalTestCurrency.BONUS, "124")
				.userId("tenant_toProfile").build());
		transaction.setReference("{\"appId\":\"appId\",\"additionalInfo\":\"no\"}");
		return transaction;
	}
}
