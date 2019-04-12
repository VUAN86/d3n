package de.ascendro.f4m.server.vat;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.common.base.Ticker;

import de.ascendro.f4m.server.exception.F4MVatNotDefinedException;
import de.ascendro.f4m.service.json.model.ISOCountry;
import de.ascendro.f4m.service.logging.LoggingUtil;

public class CountryVatCacheTest {
	private static final long ONE_HOUR_IN_MILLIS = 60 * 60 * 1000;
	private long OFFSET_FROM_BEGINNING_OF_HOUR = 1L;

	@Mock
	private CountryVatAerospikeDao countryVatAerospikeDao;
	@Mock
	private LoggingUtil loggingUtil;
	@Mock
	private Ticker ticker;
	private CountryVatCache countryVatCache;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		countryVatCache = new CountryVatCache(countryVatAerospikeDao, loggingUtil, ticker);
		when(ticker.read()).thenReturn(OFFSET_FROM_BEGINNING_OF_HOUR);
	}

	@Test
	public void testCacheUseage() throws Exception {
		BigDecimal expected = new BigDecimal("0.15");

		when(countryVatAerospikeDao.getActiveVat(ISOCountry.DE)).thenReturn(expected);
		assertThat(countryVatCache.getActiveVat(ISOCountry.DE), comparesEqualTo(expected));
		assertThat(countryVatCache.getActiveVat(ISOCountry.DE), comparesEqualTo(expected));
		verify(countryVatAerospikeDao, times(1)).getActiveVat(any());

		when(ticker.read()).thenReturn(ONE_HOUR_IN_MILLIS - 1);
		assertThat(countryVatCache.getActiveVat(ISOCountry.DE), comparesEqualTo(expected));
		verify(countryVatAerospikeDao, times(1)).getActiveVat(any());

		when(ticker.read()).thenReturn(ONE_HOUR_IN_MILLIS);
		assertThat(countryVatCache.getActiveVat(ISOCountry.DE), comparesEqualTo(expected));
		verify(countryVatAerospikeDao, times(2)).getActiveVat(any());

		when(ticker.read()).thenReturn(OFFSET_FROM_BEGINNING_OF_HOUR + ONE_HOUR_IN_MILLIS);
		assertThat(countryVatCache.getActiveVat(ISOCountry.DE), comparesEqualTo(expected));
		verify(countryVatAerospikeDao, times(2)).getActiveVat(any());
	}

	@Test
	public void testNullCountry() throws Exception {
		try {
			countryVatCache.getActiveVat((ISOCountry) null);
			fail();
		} catch (F4MVatNotDefinedException e) {
			assertEquals("No country specified", e.getMessage());
		}
	}

	@Test
	public void testUnknownCountry() throws Exception {
		try {
			countryVatCache.getActiveVat("Wadiya");
			fail();
		} catch (F4MVatNotDefinedException e) {
			assertEquals("Unknown country Wadiya", e.getMessage());
		}
	}
}
