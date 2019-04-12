package de.ascendro.f4m.server.vat;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.server.vat.CountryVatAerospikeDao.VatAerospikeValue;
import de.ascendro.f4m.service.json.model.ISOCountry;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.JsonLoader;

public class CountryVatAerospikeDaoTest {

	@Spy
	private JsonUtil jsonUtil = new JsonUtil();
	@Mock
	private AerospikeDao aerospikeDao;
	@InjectMocks
	private CountryVatAerospikeDao dao;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		dao.setUseMock(false);
	}

	@Test
	public void testGetActiveVat() throws Exception {
		String json = JsonLoader.getTextFromResources("vat_de.json", getClass());
		when(aerospikeDao.readJson(CountryVatAerospikeDao.VAT_SET_NAME, "vat:DE", CountryVatAerospikeDao.VAT_BIN_NAME))
				.thenReturn(json);
		assertThat(dao.getActiveVat(ISOCountry.DE), comparesEqualTo(new BigDecimal("0.15")));
	}

	@Test
	public void testFindingActiveVat() throws Exception {
		VatAerospikeValue vatValue = jsonUtil.fromJson(JsonLoader.getTextFromResources("vat_ro.json", getClass()),
				VatAerospikeValue.class); //contains two VAT values:
		//10% from February 1, 2017 to December 1, 2017
		//15% from January 1, 2018 to eternity
		assertThat(dao.findActiveVat(vatValue, ofDate(2017, 01, 31)), nullValue());
		Matcher<BigDecimal> ten = comparesEqualTo(new BigDecimal("0.10"));
		Matcher<BigDecimal> fifteen = comparesEqualTo(new BigDecimal("0.15"));
		assertThat(dao.findActiveVat(vatValue, ofDate(2017, 02, 01)), ten);
		assertThat(dao.findActiveVat(vatValue, ofDate(2017, 12, 01)), ten);
		assertThat(dao.findActiveVat(vatValue, ofDate(2017, 12, 02)), nullValue());
		assertThat(dao.findActiveVat(vatValue, ZonedDateTime.of(2017, 12, 31, 23, 59, 59, 0, DateTimeUtil.TIMEZONE)),
				nullValue());
		assertThat(dao.findActiveVat(vatValue, ofDate(2018, 01, 01)), fifteen);
		assertThat(dao.findActiveVat(vatValue, ofDate(2045, 01, 01)), fifteen);
	}

	private ZonedDateTime ofDate(int year, int month, int dayOfMonth) {
		return ZonedDateTime.of(year, month, dayOfMonth, 0, 0, 0, 0, DateTimeUtil.TIMEZONE);
	}
}
