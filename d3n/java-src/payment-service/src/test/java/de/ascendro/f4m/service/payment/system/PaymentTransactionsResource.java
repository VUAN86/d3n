package de.ascendro.f4m.service.payment.system;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import de.ascendro.f4m.service.payment.rest.model.PaymentTransactionInitializationRest;
import de.ascendro.f4m.service.payment.rest.model.PaymentTransactionRest;
import de.ascendro.f4m.service.payment.rest.wrapper.PaymentTransactionRestWrapper;

@Path(PaymentTransactionRestWrapper.URI_PATH)
public class PaymentTransactionsResource {

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public PaymentTransactionRest initiatePaymentTransaction(PaymentTransactionInitializationRest request) {
		PaymentTransactionRest transaction = new PaymentTransactionRest();
		transaction.setId("54321");
		transaction.setPaymentToken("C3F10AD7CE4CC89213B0AFECEA296938.sbg-vm- fe02");
		return transaction;
	}
}
