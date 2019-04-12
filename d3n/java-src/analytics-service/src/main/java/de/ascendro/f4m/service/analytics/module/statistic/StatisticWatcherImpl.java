package de.ascendro.f4m.service.analytics.module.statistic;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.ascendro.f4m.service.analytics.logging.InjectLogger;
import de.ascendro.f4m.service.analytics.module.statistic.query.base.ITableUpdater;
import de.ascendro.f4m.service.logging.LoggingUtil;

public class StatisticWatcherImpl implements StatisticWatcher {
    @InjectLogger
    private static Logger LOGGER;

    @SuppressWarnings("rawtypes")
	private final Map<String, ITableUpdater> queryHandlers;
    private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setNameFormat("StatisticWatcher").build());
	private LoggingUtil loggingUtil;

    @Inject
    @SuppressWarnings("rawtypes")
    public StatisticWatcherImpl(Map<String, ITableUpdater> queryHandlers, LoggingUtil loggingUtil) {
        this.queryHandlers = queryHandlers;
		this.loggingUtil = loggingUtil;
    }

    @Override
    public void startWatcher() {
        LOGGER.info("Statistic watcher started");
        service.scheduleAtFixedRate(() -> {
        	loggingUtil.saveBasicInformationInThreadContext();
            queryHandlers.entrySet().parallelStream().forEach(h->{
                try {
                    h.getValue().triggerBatchExecute();
                } catch (Exception e) {
                    LOGGER.debug("Error executing statistic watcher batch update", e);
                }
            });
        }, 60, 60, TimeUnit.SECONDS);
    }

    @Override
    public void stopWatcher() {
        if (!service.isShutdown()) {
            service.shutdownNow();
            LOGGER.info("Statistic watcher stopped");
        }
    }
}
