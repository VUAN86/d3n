package de.ascendro.f4m.service.payment.system;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import de.ascendro.f4m.service.payment.manager.ExternalTestCurrency;
import de.ascendro.f4m.service.payment.manager.PaymentTestUtil;
import de.ascendro.f4m.service.payment.rest.model.CurrencyRest;
import de.ascendro.f4m.service.payment.rest.wrapper.CurrencyRestWrapper;

@Path(CurrencyRestWrapper.URI_PATH)
public class CurrenciesResource {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<CurrencyRest> getCurrencies() {
		return Arrays.asList(PaymentTestUtil.prepareCurrencyRest(ExternalTestCurrency.EUR),
				PaymentTestUtil.prepareCurrencyRest(ExternalTestCurrency.BONUS),
				PaymentTestUtil.prepareCurrencyRest(ExternalTestCurrency.CREDIT));
	}
}
