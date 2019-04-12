package de.ascendro.f4m.service.game.engine.history;

import java.time.Instant;
import java.util.Date;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.server.config.GameConfigImpl;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class ActiveGameTimer extends Timer {
	private static final Logger LOGGER = LoggerFactory.getLogger(ActiveGameTimer.class);
	private static final long TWELVE_HOURS_IN_MILLIS = TimeUnit.HOURS.toMillis(12);
	private static final long TEN_MINUTES_IN_MILLIS = TimeUnit.MINUTES.toMillis(3);
	
	private final Config config;
	private final ActiveGameTimerTask task;

	@Inject
	public ActiveGameTimer(Config config, ActiveGameTimerTask task) {
		super("activeGame-cleanUp");
		this.config = config;
		this.task = task;
	}

	public void scheduleActiveGameCleanUp() {
		final String executionTime = config.getProperty(GameConfigImpl.GAME_CLEAN_UP_UTC_TIME);
		final String[] executionHoursAndMinutes = StringUtils.split(executionTime, ':');

		final Instant firstExecutionTime = DateTimeUtil.getCurrentDateTime()
				.withHour(Integer.valueOf(executionHoursAndMinutes[0]))
				.withMinute(Integer.valueOf(executionHoursAndMinutes[1]))
				.toInstant();
		LOGGER.info("Scheduling active game clean up timer at {} each {}ms", firstExecutionTime,
				TWELVE_HOURS_IN_MILLIS);
		scheduleAtFixedRate(task, Date.from(firstExecutionTime), TWELVE_HOURS_IN_MILLIS);
	}

	/**
	 * Scheduling cleanup every 10 minutes is a temporary solution
	 * until cleanup of tournaments through regular events is fixed (#10587)
	 */
	public void scheduleActiveGameCleanUpFrequently() {
		LOGGER.info("Scheduling active game clean up timer each {}ms", TEN_MINUTES_IN_MILLIS);
		scheduleAtFixedRate(task, TEN_MINUTES_IN_MILLIS, TEN_MINUTES_IN_MILLIS);
	}

	@PreDestroy
	public void stopTimer() {
		this.cancel();
	}

}
