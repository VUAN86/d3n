package de.ascendro.f4m.service.payment.system;

import static de.ascendro.f4m.service.payment.manager.ExternalTestCurrency.BONUS;
import static de.ascendro.f4m.service.payment.manager.ExternalTestCurrency.CREDIT;
import static de.ascendro.f4m.service.payment.manager.ExternalTestCurrency.EUR;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.manager.ExternalTestCurrency;
import de.ascendro.f4m.service.payment.manager.PaymentTestUtil;
import de.ascendro.f4m.service.payment.rest.model.ExchangeRateRest;
import de.ascendro.f4m.service.payment.rest.wrapper.ExchangeRateRestWrapper;

@Path(ExchangeRateRestWrapper.URI_PATH)
public class ExchangeRatesResource {
	private static final BigDecimal INVERTED_RATE = new BigDecimal("0.01");
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<ExchangeRateRest> getExchangeRates() {
		BigDecimal rateDefault = PaymentConfig.DUMMY_EXCHANGE_RATE_DEFAULT;
		return Arrays.asList(create(EUR, BONUS, rateDefault),
				create(EUR, CREDIT, rateDefault),
				create(BONUS, CREDIT, rateDefault),
				create(CREDIT, BONUS, INVERTED_RATE));
	}

	private ExchangeRateRest create(ExternalTestCurrency from, ExternalTestCurrency to, BigDecimal value) {
		ExchangeRateRest rate = new ExchangeRateRest();
		rate.setFromCurrency(PaymentTestUtil.prepareCurrencyRest(from));
		rate.setToCurrency(PaymentTestUtil.prepareCurrencyRest(to));
		rate.setRate(value);
		return rate;
	}
}
