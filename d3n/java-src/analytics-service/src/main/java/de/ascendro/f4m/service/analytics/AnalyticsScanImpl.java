package de.ascendro.f4m.service.analytics;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.analytics.config.AnalyticsConfig;
import de.ascendro.f4m.service.analytics.dao.AnalyticsAerospikeDao;
import de.ascendro.f4m.service.analytics.logging.InjectLogger;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.logging.LoggingUtil;

public class AnalyticsScanImpl implements AnalyticsScan {
    @InjectLogger
    private static Logger LOGGER;
    protected final Config config;

    protected final JsonUtil jsonUtil;
    protected final AnalyticsAerospikeDao analyticsAerospikeDao;
    protected final LoggingUtil loggingUtil;


    private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(
    		new ThreadFactoryBuilder().setNameFormat("AnalyticsScan").build());

    @Inject
    public AnalyticsScanImpl(Config config, LoggingUtil loggingUtil,
                             JsonUtil jsonUtil, AnalyticsAerospikeDao analyticsAerospikeDao) {
        this.config = config;
        this.analyticsAerospikeDao = analyticsAerospikeDao;
        this.jsonUtil = jsonUtil;
        this.loggingUtil = loggingUtil;
    }

    @Override
    public void shutdown() {
        analyticsAerospikeDao.suspendScan();
        service.shutdown();
        LOGGER.info("Analytics scan stopped");
    }

    @Override
    public boolean isRunning() {
        return analyticsAerospikeDao.isRunning();
    }

    @Override
    public void execute() {
        resume();
        service.scheduleAtFixedRate(() -> {
        	loggingUtil.saveBasicInformationInThreadContext();
            LOGGER.debug("Analytics scan operation started");
            action();
            LOGGER.debug("Analytics scan operation finalized");
        }, 0, config.getPropertyAsLong(AnalyticsConfig.AEROSPIKE_SCAN_DELAY), TimeUnit.SECONDS);
    }

    @Override
    public void suspend() {
        analyticsAerospikeDao.suspendScan();
     }

    @Override
    public void resume() {
        analyticsAerospikeDao.resumeScan();
    }

    private void action() {
        analyticsAerospikeDao.loadAnalyticsData();
    }
}
