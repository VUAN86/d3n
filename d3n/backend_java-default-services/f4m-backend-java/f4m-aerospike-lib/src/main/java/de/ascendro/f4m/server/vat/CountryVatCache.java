package de.ascendro.f4m.server.vat;

import java.math.BigDecimal;
import java.util.Calendar;

import javax.inject.Inject;

import org.apache.commons.lang3.EnumUtils;

import com.google.common.base.Ticker;

import de.ascendro.f4m.server.exception.F4MVatNotDefinedException;
import de.ascendro.f4m.service.cache.CacheManager;
import de.ascendro.f4m.service.cache.Cached;
import de.ascendro.f4m.service.json.model.ISOCountry;
import de.ascendro.f4m.service.logging.LoggingUtil;

public class CountryVatCache {
	
	private CountryVatAerospikeDao countryVatAerospikeDao;
	private CacheManager<ISOCountry, VatCacheEntry> cache;
	private Ticker ticker; //guava caches start to slowly creep in to replace CacheManager (only this time millisecond based)...

	@Inject
	public CountryVatCache(CountryVatAerospikeDao countryVatAerospikeDao, LoggingUtil loggingUtil) {
		this(countryVatAerospikeDao, loggingUtil, new SystemMillisTicker());
	}
	
	public CountryVatCache(CountryVatAerospikeDao countryVatAerospikeDao, LoggingUtil loggingUtil, Ticker ticker) {
		this.countryVatAerospikeDao = countryVatAerospikeDao;
		this.ticker = ticker;
		int cleanUpInterval = 60 * 60 * 1000; //max 1 hour is hard-coded because of getBeginningOfNextHour
		cache = new CacheManager<>(cleanUpInterval, loggingUtil);
	}
	
	public BigDecimal getActiveVat(String isoCountryAsString) {
		ISOCountry country = EnumUtils.getEnum(ISOCountry.class, isoCountryAsString);
		if (country == null) {
			throw new F4MVatNotDefinedException("Unknown country " + isoCountryAsString);
		}
		return getActiveVat(country);
	}
	
	public BigDecimal getActiveVat(ISOCountry country) {
		if (country == null) {
			throw new F4MVatNotDefinedException("No country specified");
		}
		VatCacheEntry cacheValue = cache.getAndRefresh(country);
		if (cacheValue == null || cacheValue.isExpired()) {
			long expiryDate = getBeginningOfNextHour();
			BigDecimal activeVat = countryVatAerospikeDao.getActiveVat(country);
			cacheValue = new VatCacheEntry(activeVat, expiryDate, ticker);
			cache.put(country, cacheValue);
		}
		return cacheValue.getVat();
	}

	private long getBeginningOfNextHour() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(ticker.read());
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		calendar.add(Calendar.HOUR, +1);
		return calendar.getTimeInMillis();
	}
	
	static class SystemMillisTicker extends Ticker {
		@Override
		public long read() {
			return System.currentTimeMillis();
		}
	}
	
	static class VatCacheEntry extends CacheEntryWithExpiryTime {
		private BigDecimal vat;

		public VatCacheEntry(BigDecimal vat, long expiryTimeNanos, Ticker ticker) {
			super(expiryTimeNanos, ticker);
			this.vat = vat;
		}

		public BigDecimal getVat() {
			return vat;
		}
	}
	
	static class CacheEntryWithExpiryTime implements Cached {
		private long expiryTime;
		private Ticker ticker;

		public CacheEntryWithExpiryTime(long expiryTime, Ticker ticker) {
			this.expiryTime = expiryTime;
			this.ticker = ticker;
		}
		
		@Override
		public boolean isExpired() {
			return expiryTime <= ticker.read();
		}

		@Override
		public void refreshLastAccess() {
			//expiration is based on set time, refresh does not extend expiration time in contrast to CachedObject
		}
	}
}
