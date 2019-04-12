package de.ascendro.f4m.server.vat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Function;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.server.exception.F4MVatNotDefinedException;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.json.model.ISOCountry;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class CountryVatAerospikeDao {
	public static final String VAT_SET_NAME = "vat";
	public static final String VAT_BIN_NAME = "value";
	private static final String VAT_KEY_PREFIX = "vat" +  PrimaryKeyUtil.KEY_ITEM_SEPARATOR;
	
	private JsonUtil jsonUtil;
	private AerospikeDao aerospikeDao;
	private boolean useMock;

	@Inject
	public CountryVatAerospikeDao(JsonUtil jsonUtil, AerospikeDao aerospikeDao) {
		this.jsonUtil = jsonUtil;
		this.aerospikeDao = aerospikeDao;
		//TODO: Change value or remove this parameter, after real VAT data are available in Aerospike (easier than changing config)
		this.setUseMock(true);
	}

	public BigDecimal getActiveVat(ISOCountry country) {
		if (country == null) {
			throw new F4MVatNotDefinedException("No country specified");
		}
		if (isUseMock()) {
			return getMockedVat(country);
		} else {
			return getVatFromAerospike(country);
		}
	}

	private BigDecimal getVatFromAerospike(ISOCountry country) {
		String json = aerospikeDao.readJson(VAT_SET_NAME, createPrimaryKey(country), VAT_BIN_NAME);
		if (StringUtils.isNotBlank(json)) {
			VatAerospikeValue vatValue = jsonUtil.fromJson(json, VatAerospikeValue.class);
			BigDecimal vat = findActiveVat(vatValue, DateTimeUtil.getCurrentDateTime());
			if (vat == null) {
				throw new F4MVatNotDefinedException("VAT not found for country " + country);
			} else {
				return vat;
			}
		} else {
			throw new F4MVatNotDefinedException("VAT not found for country " + country);
		}
	}

	protected BigDecimal findActiveVat(VatAerospikeValue vatValue, ZonedDateTime now) {
		BigDecimal vat = null;
		for (VatAerospikeValueEntry entry : vatValue.vats) {
			//assume that range is startDate >= now > endDate
			boolean compliesWithStart = isInRange(entry.startDate, date -> !now.isBefore(date));
			boolean compliesWithEnd = isInRange(entry.endDate, date -> now.isBefore(date));
			if (compliesWithStart && compliesWithEnd) {
				vat = entry.percent.multiply(new BigDecimal("0.01"));
				break;
			}
		}
		return vat;
	}
	
	private boolean isInRange(Long date, Function<ZonedDateTime, Boolean> comparison) {
		if (date == null) {
			return true;
		} else {
			ZonedDateTime d = ZonedDateTime.ofInstant(Instant.ofEpochSecond(date), DateTimeUtil.TIMEZONE);
			return comparison.apply(d);
		}
	}

	private BigDecimal getMockedVat(ISOCountry country) {
		BigDecimal value;
		if (ISOCountry.DE == country) {
			value = new BigDecimal("0.19");
		} else if (ISOCountry.RO == country) {
			value = new BigDecimal("0.24");
		} else if (ISOCountry.LV == country) {
			value = new BigDecimal("0.21");
		} else if (ISOCountry.GB == country) {
			value = new BigDecimal("0.20");
		} else if (ISOCountry.US == country) {
			value = new BigDecimal("0.0");
		} else {
			throw new F4MVatNotDefinedException("VAT not found for country " + country);
		}
		return value;
	}
	
	private String createPrimaryKey(ISOCountry country) {
		return VAT_KEY_PREFIX + country;
	}

	public boolean isUseMock() {
		return useMock;
	}

	public void setUseMock(boolean useMock) {
		this.useMock = useMock;
	}

	static class VatAerospikeValue {
		public String country;
		public List<VatAerospikeValueEntry> vats;
	}

	static class VatAerospikeValueEntry {
		public Long startDate;
		public Long endDate;
		public BigDecimal percent;
	}
}
