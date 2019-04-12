package de.ascendro.f4m.service.payment.manager;

import javax.inject.Inject;

import de.ascendro.f4m.service.cache.CacheManager;
import de.ascendro.f4m.service.cache.CachedObject;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.payment.config.PaymentConfig;

public class TransactionLogCacheManager {
	private final CacheManager<String, TransactionLogCache> logCacheManager;
	private long entryTTL;
	
	@Inject
	public TransactionLogCacheManager(PaymentConfig config, LoggingUtil loggingUtil) {
		final long cleanUpInterval = config.getPropertyAsLong(PaymentConfig.LOG_CACHE_CLEAN_UP_INTERVAL);
		logCacheManager = new CacheManager<>(cleanUpInterval, loggingUtil);
		entryTTL = config.getPropertyAsLong(PaymentConfig.LOG_CACHE_ENTRY_TIME_TO_LIVE);
	}
	
	public void put(String transactionId, String logId) {
		TransactionLogCache entry = new TransactionLogCache(logId, entryTTL);
		logCacheManager.put(transactionId, entry);
	}
	
	public String popLogId(String transactionId) {
		String logId = null;
		TransactionLogCache removed = logCacheManager.remove(transactionId);
		if (removed != null) {
			logId = removed.getLogId();
		}
		return logId;
	}
	
	public class TransactionLogCache extends CachedObject {
		private String logId;
		
		public TransactionLogCache(String logId, Long timeToLive) {
			this.logId = logId;
			this.timeToLive = timeToLive;
		}

		public String getLogId() {
			return logId;
		}
	}
}
