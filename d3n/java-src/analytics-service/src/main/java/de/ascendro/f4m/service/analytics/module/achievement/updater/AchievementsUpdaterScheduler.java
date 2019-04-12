package de.ascendro.f4m.service.analytics.module.achievement.updater;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.ascendro.f4m.service.analytics.logging.InjectLogger;
import de.ascendro.f4m.service.analytics.module.achievement.processor.AchievementsLoader;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class AchievementsUpdaterScheduler {

	@InjectLogger
	private static Logger LOGGER;

    private static final long ONE_DAY_IN_MILLISECONDS = TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS);

    private AchievementsLoader achievementsLoader;

    @Inject
    public AchievementsUpdaterScheduler(AchievementsLoader achievementsUpdater) {
        this.achievementsLoader = achievementsUpdater;
    }

    public void schedule() {
        ScheduledExecutorService scheduler = getScheduler();
        scheduler.scheduleAtFixedRate(() -> achievementsLoader.loadTenants(), getInitialDelay(), ONE_DAY_IN_MILLISECONDS,
                TimeUnit.MILLISECONDS);
		LOGGER.info("achievement rule engine: scheduled next achievements refresh");
    }

	ScheduledExecutorService getScheduler() {
		final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("AchievementsUpdater-Scheduler-%d")
				.build();
		return Executors.newScheduledThreadPool(1, threadFactory);
	}

	private long getInitialDelay(){
		ZonedDateTime now = DateTimeUtil.getCurrentDateTime();
		ZonedDateTime midnight = DateTimeUtil.getCurrentDateStart().plusDays(1);
		Duration duration = Duration.between(now, midnight);
		return duration.get(ChronoUnit.SECONDS) * TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS);
	}

}
