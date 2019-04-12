package de.ascendro.f4m.server.scheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.util.DateTimeUtil;

public abstract class ExpiredRecordCleaner {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExpiredRecordCleaner.class);

	private final Config config;

	private final ScheduledExecutorService scheduledExecutorService;

	@Inject
	public ExpiredRecordCleaner(Config config) {
		this.config = config;

		scheduledExecutorService = Executors.newScheduledThreadPool(3, 
				new ThreadFactoryBuilder().setNameFormat("ExpiredRecordCleaner-task-%d").build());
	}

	@PostConstruct
	public void initializeExpiredRecordCleaner() {
		Boolean start = config.getPropertyAsBoolean(AerospikeConfigImpl.AEROSPIKE_EXPIRED_RECORD_CLEANER_SCHEDULE);
		if (Boolean.TRUE.equals(start)) {
			startScheduler();
		}
	}

	@PreDestroy
	public void stopScheduler() {
		if (scheduledExecutorService != null) {
			LOGGER.info("Stopping ExpiredRecordCleaner task scheduler");
			scheduledExecutorService.shutdownNow();
		}
	}

	public void startScheduler() {
		Long initialDelay = getInitialDelay();
		Long period = config.getPropertyAsLong(AerospikeConfigImpl.AEROSPIKE_EXPIRED_RECORD_CLEANER_PERIOD);
		if (initialDelay != null && period != null) {
			scheduledExecutorService.scheduleAtFixedRate(() -> executeRecordCleanerTask(), initialDelay, period,
					TimeUnit.MILLISECONDS);
			LOGGER.info("ExpiredRecordCleaner task scheduled with initial delay [{}] and period [{}]", initialDelay,
					period);
		} else {
			LOGGER.error("Failed to schedule ExpiredRecordCleaner task with initial delay [{}] and period [{}]",
					initialDelay, period);
		}
	}

	protected void executeRecordCleanerTask() {
		boolean initResult = initializeTask();
		if (initResult) {
			try {
				executeTask();
			} finally {
				finializeTask();
			}
		}
	}

	protected abstract boolean initializeTask();

	protected abstract void finializeTask();

	protected abstract void executeTask();

	private Long getInitialDelay() {
		final Long initialDelay;

		Long delayFromMidnight = config.getPropertyAsLong(AerospikeConfigImpl.AEROSPIKE_EXPIRED_RECORD_CLEANER_START);
		if (delayFromMidnight != null) {
			long lastMidnight = DateTimeUtil.getUTCDateTimestamp();
			long nextMidnight = lastMidnight + DateTimeUtil.getOneDayInMillis();
			long now = DateTimeUtil.getUTCTimestamp();
			initialDelay = lastMidnight + delayFromMidnight > now ? delayFromMidnight - (now - lastMidnight)
					: nextMidnight + delayFromMidnight - now;
		} else {
			initialDelay = null;
		}

		return initialDelay;
	}

}
