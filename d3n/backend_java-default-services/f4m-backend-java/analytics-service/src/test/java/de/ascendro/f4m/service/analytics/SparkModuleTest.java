package de.ascendro.f4m.service.analytics;

import static de.ascendro.f4m.service.analytics.util.F4MAssertions.assertDoesNotThrow;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeNotNull;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Module;

import ch.vorburger.mariadb4j.DB;
import de.ascendro.f4m.server.analytics.AnalyticsDaoImpl;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.service.analytics.activemq.AMQStatistics;
import de.ascendro.f4m.service.analytics.activemq.IQueueBrokerManager;
import de.ascendro.f4m.service.analytics.activemq.router.AnalyticMessageListener;
import de.ascendro.f4m.service.analytics.config.AnalyticsConfig;
import de.ascendro.f4m.service.analytics.data.EventTestData;
import de.ascendro.f4m.service.analytics.di.AchievementModule;
import de.ascendro.f4m.service.analytics.di.AnalyticsServiceModule;
import de.ascendro.f4m.service.analytics.di.SparkModule;
import de.ascendro.f4m.service.analytics.listeners.TestMQTTListener;
import de.ascendro.f4m.service.analytics.listeners.TestSparkListener;
import de.ascendro.f4m.service.analytics.notification.di.NotificationWebSocketModule;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.dao.aerospike.AerospikeClientMockProvider;
import de.ascendro.f4m.service.dao.aerospike.RealAerospikeTestBase;
import de.ascendro.f4m.service.integration.test.F4MIntegrationTestBase;

public class SparkModuleTest extends RealAerospikeTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatisticModuleTest.class);
    private static AnalyticsServiceStartup analyticsServiceStartup;
    private static DB db;

    @BeforeClass
    public static void startClass() {
        analyticsServiceStartup = getServiceStartup();
    }

    @AfterClass
    public static void stopClass() {
        analyticsServiceStartup = null;
    }

    @Override
    @After
    public void tearDown() {
        if (aerospikeClientProvider != null && !(aerospikeClientProvider instanceof AerospikeClientMockProvider)) {
            try {
                clearTestAnalyticsInstanceSet();
            } finally {
                if (aerospikeClientProvider != null) {
                    aerospikeClientProvider.get()
                            .close();
                }
            }
        }
    }

    private void clearTestAnalyticsInstanceSet() {
        LOGGER.info("Clearing Aerospike set {} within namespace {} at exit", AnalyticsDaoImpl.ANALYTICS_SET_NAME, AnalyticsTestConfig.NAMESPACE);
        clearSet(AnalyticsTestConfig.NAMESPACE, AnalyticsDaoImpl.ANALYTICS_SET_NAME);
    }

    public void stop() throws Exception {
        stopActiveMQ();
        final AnalyticsScan analyticsScan = analyticsServiceStartup.getInjector().getInstance(AnalyticsScan.class);
        analyticsScan.shutdown();
        if (db!=null) {
            db.stop();
            db = null;
        }
    }

    public void start() throws Exception {
        final AnalyticsScan analyticsScan = analyticsServiceStartup.getInjector().getInstance(AnalyticsScan.class);
        FileUtils.deleteDirectory(new File("target/activemq-data/"));
        startActiveMQ();
        initConsumers();
        analyticsScan.execute();
    }

    public void initConsumers() throws Exception {
        IQueueBrokerManager embeddedActiveMQ = analyticsServiceStartup.getInjector().getInstance(IQueueBrokerManager.class);
        embeddedActiveMQ.initConsumers();

        AMQStatistics amqStats =  analyticsServiceStartup.getInjector().getInstance(AMQStatistics.class);
        amqStats.startAMQStatistics();
    }


    public void startActiveMQ() throws Exception {
        IQueueBrokerManager embeddedActiveMQ = analyticsServiceStartup.getInjector().getInstance(IQueueBrokerManager.class);
        embeddedActiveMQ.start();
    }

    public void stopActiveMQ() throws Exception {
        IQueueBrokerManager embeddedActiveMQ = analyticsServiceStartup.getInjector().getInstance(IQueueBrokerManager.class);
        if (embeddedActiveMQ != null) {
            AMQStatistics amqStats =  analyticsServiceStartup.getInjector().getInstance(AMQStatistics.class);
            amqStats.stopAMQStatistics();

            embeddedActiveMQ.stopSubscribers();
            embeddedActiveMQ.stop();
        }
    }

    @Override
    protected Config createConfig() {
        return new AnalyticsTestConfig();
    }

    @Override
    protected void setUpAerospike() {

    }

    protected static AnalyticsServiceStartup getServiceStartup() {
        if (StringUtils.isEmpty(new AnalyticsTestConfig().getProperty(AerospikeConfigImpl.AEROSPIKE_SERVER_HOST))) {
            return new AnalyticsServiceStartupUsingAerospikeMock(F4MIntegrationTestBase.DEFAULT_TEST_STAGE) {
                @Override
                protected Iterable<? extends Module> getTestedModules() {
                    return Arrays.asList(new AnalyticsServiceModule(), new NotificationWebSocketModule(),
                            new SparkModule(), new AchievementModule());
                }

                @Override
                protected List<Class<? extends AnalyticMessageListener>> getTestListeners() {
                    return Arrays.asList(TestSparkListener.class, TestMQTTListener.class);
                }
            };
        } else {
            return new AnalyticsServiceStartupUsingAerospikeReal(F4MIntegrationTestBase.DEFAULT_TEST_STAGE){
                @Override
                protected Iterable<? extends Module> getTestedModules() {
                    return Arrays.asList(new AnalyticsServiceModule(), new NotificationWebSocketModule(),
                            new SparkModule());
                }

                @Override
                protected List<Class<? extends AnalyticMessageListener>> getTestListeners() {
                    return Arrays.asList(TestSparkListener.class, TestMQTTListener.class);
                }
            };
        }
    }


    @Test
    public void testStart() throws Exception {
        AnalyticsDaoImpl analyticsDaoImpl = analyticsServiceStartup.getInjector().getInstance(AnalyticsDaoImpl.class);

        EventTestData eventTestData = new EventTestData(analyticsDaoImpl);
        IntStream.range(0,3).forEach((i) -> eventTestData.createInvoiceEvents());


        TestSparkListener.initLock(eventTestData.getEventCounter().getSparkEntriesCounter());
        TestMQTTListener.initLock(eventTestData.getEventCounter().getSparkEntriesCounter());

        this.start();

        TestMQTTListener.lock.await(4000, TimeUnit.MILLISECONDS);
        TestSparkListener.lock.await(4000, TimeUnit.MILLISECONDS);

        assertEquals(TestSparkListener.lock.getCount(),0);

        LOGGER.info("Stopping database");
    }


    @Test
    public void testStop() throws Exception {
        assumeNotNull(config.getProperty(AnalyticsConfig.MYSQL_DATABASE_URL));
        assumeNotNull(analyticsServiceStartup);

        assertDoesNotThrow(()-> this.stop());
    }
}
