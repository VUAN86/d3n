package de.ascendro.f4m.service.payment.rest.wrapper;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.service.payment.config.PaymentConfig;

public class CurrencyRestWrapperTest {
	
	@Mock
	private PaymentConfig config;
	@InjectMocks
	private CurrencyRestWrapper currencyRest;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void testUriCalculation() throws Exception {
		verifyUri("http://144.76.94.164/currencies", "http://144.76.94.164/");
		verifyUri("http://144.76.94.164/currencies", "http://144.76.94.164");
		verifyUri("http://144.76.94.164/api/currencies", "http://144.76.94.164/api");
		verifyUri("http://144.76.94.164/api/currencies", "http://144.76.94.164/api/");
	}

	private void verifyUri(String expected, String baseUri) {
		when(config.getProperty(PaymentConfig.PAYMENT_SYSTEM_REST_URL)).thenReturn(baseUri);
		assertEquals(expected, currencyRest.getURI().toString());
	}
}
