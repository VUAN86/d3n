package de.ascendro.f4m.service.cache;

import java.util.Map;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.logging.LoggingUtil;

public class CacheCleanUpTask extends TimerTask {
	private static final Logger LOGGER = LoggerFactory.getLogger(CacheCleanUpTask.class);
	
	private final Map<?, ? extends Cached> cacheMap;
	private final LoggingUtil loggingUtil;

	public CacheCleanUpTask(Map<?, ? extends Cached> cacheMap, LoggingUtil loggingUtil) {
		this.cacheMap = cacheMap;
		this.loggingUtil = loggingUtil;
	}

	@Override
	public void run() {
		loggingUtil.saveBasicInformationInThreadContext();
		LOGGER.trace("Starting scheduled cache clean up task");
		for (Map.Entry<?, ? extends Cached> entry : cacheMap.entrySet()) {
			if (entry.getValue().isExpired()) {
				cacheMap.remove(entry.getKey());
			}
		}
	}

}
