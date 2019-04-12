package de.ascendro.f4m.service.analytics;

import java.util.Arrays;
import java.util.List;

import javax.jms.JMSException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.util.Modules;

import de.ascendro.f4m.server.analytics.exception.F4MAnalyticsFatalErrorException;
import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.analytics.activemq.AMQStatistics;
import de.ascendro.f4m.service.analytics.activemq.AMQStatisticsImpl;
import de.ascendro.f4m.service.analytics.activemq.IQueueBrokerManager;
import de.ascendro.f4m.service.analytics.di.AchievementModule;
import de.ascendro.f4m.service.analytics.di.AnalyticsServiceModule;
import de.ascendro.f4m.service.analytics.di.JobModule;
import de.ascendro.f4m.service.analytics.di.NotificationModule;
import de.ascendro.f4m.service.analytics.di.SparkModule;
import de.ascendro.f4m.service.analytics.di.StatisticModule;
import de.ascendro.f4m.service.analytics.module.achievement.processor.AchievementsLoader;
import de.ascendro.f4m.service.analytics.module.achievement.updater.AchievementsUpdaterScheduler;
import de.ascendro.f4m.service.analytics.module.statistic.StatisticWatcher;
import de.ascendro.f4m.service.analytics.notification.di.NotificationWebSocketModule;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;

public class AnalyticsServiceStartup extends ServiceStartup {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsServiceStartup.class);
    private boolean started = false;

    public AnalyticsServiceStartup(Stage stage) {
        super(stage);
    }

    public static void main(String... args) throws Exception {
        new AnalyticsServiceStartup(Stage.PRODUCTION).start();
    }

    @Override
    public Injector createInjector(Stage stage) {
        return Guice.createInjector(stage, Modules.override(super.getModules()).with(getModules()));
    }

    @Override
    protected List<String> getDependentServiceNames() {
        return Arrays.asList(UserMessageMessageTypes.SERVICE_NAME, PaymentMessageTypes.SERVICE_NAME);
    }

    @Override
    protected Iterable<? extends Module> getModules() {
        return Arrays.asList(new AnalyticsServiceModule(),
                new StatisticModule(), new SparkModule(), new JobModule(),
                new NotificationModule(), new NotificationWebSocketModule(), new AchievementModule());
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        stopScan();
        stopActiveMQ();
    }

    @Override
    public void start() throws Exception {
        LOGGER.info("Starting Analytics Service");
        super.start();
        LOGGER.info("Build Achievements rules");
        loadAchievements();
        LOGGER.info("Schedule Job to update Achievemement rules");
        scheduleAchievementUpdaterJob();
        LOGGER.info("Starting ActiveMQ for Analytics Service");
        startActiveMQ();
        LOGGER.info("Starting event scan for Analytics Service");
        startScan();

        started = true;
    }

    private void scheduleAchievementUpdaterJob() {
    	getInjector().getInstance(AchievementsUpdaterScheduler.class).schedule();
	}

	private void loadAchievements() {
		getInjector().getInstance(AchievementsLoader.class).loadTenants();
	}

	public boolean isStarted() {
        return started;
    }

    public void stopScan() {
        AMQStatisticsImpl amqStats =  getInjector().getInstance(AMQStatisticsImpl.class);
        amqStats.stopAMQStatistics();

        final AnalyticsScan analyticsScan = getInjector().getInstance(AnalyticsScan.class);
        analyticsScan.shutdown();
    }

    public void startScan() throws JMSException {
        IQueueBrokerManager embeddedActiveMQ = getInjector().getInstance(IQueueBrokerManager.class);

        if (embeddedActiveMQ.isStarted()) {
            final AnalyticsScan analyticsScan = getInjector().getInstance(AnalyticsScan.class);
            initConsumers();
            AMQStatisticsImpl amqStats =  getInjector().getInstance(AMQStatisticsImpl.class);
            amqStats.startAMQStatistics();
            analyticsScan.execute();
        } else {
            LOGGER.error("Scan could not start. ActiveMQ is not running.");
            throw new F4MAnalyticsFatalErrorException("ActiveMQ is not running");
        }
    }




    public void initConsumers() throws JMSException {
        IQueueBrokerManager embeddedActiveMQ = getInjector().getInstance(IQueueBrokerManager.class);
        embeddedActiveMQ.initConsumers();
    }

    public void startActiveMQ() throws Exception {
        startActiveMQ(true);
    }


    public void startActiveMQ(boolean enableWatchers) throws Exception {
        IQueueBrokerManager embeddedActiveMQ = getInjector().getInstance(IQueueBrokerManager.class);
        embeddedActiveMQ.start();
        if (embeddedActiveMQ.isStarted()) {
            LOGGER.info("ActiveMQ for Analytics Service started.");

            if (enableWatchers) {
                StatisticWatcher statisticWatcher = getInjector().getInstance(StatisticWatcher.class);
                statisticWatcher.startWatcher();

                AMQStatistics amqStats = getInjector().getInstance(AMQStatistics.class);
                amqStats.startAMQStatistics();
            }
        }
    }

    public void stopActiveMQ() throws JMSException {
        IQueueBrokerManager embeddedActiveMQ = getInjector().getInstance(IQueueBrokerManager.class);
        if (embeddedActiveMQ != null) {
            AMQStatistics amqStats = getInjector().getInstance(AMQStatistics.class);
            amqStats.stopAMQStatistics();

            embeddedActiveMQ.disposeConnection();
            embeddedActiveMQ.stop();
        }
        StatisticWatcher statisticWatcher = getInjector().getInstance(StatisticWatcher.class);
        statisticWatcher.stopWatcher();
    }

    public void stopActiveMQSubscribers() {
        IQueueBrokerManager embeddedActiveMQ = getInjector().getInstance(IQueueBrokerManager.class);
        if (embeddedActiveMQ != null) {
            embeddedActiveMQ.stopSubscribers();
        }
    }

    @Override
	protected String getServiceName() {
        return "analytics";
    }
}
