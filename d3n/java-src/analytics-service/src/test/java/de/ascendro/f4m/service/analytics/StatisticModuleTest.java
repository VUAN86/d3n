package de.ascendro.f4m.service.analytics;

import static de.ascendro.f4m.service.analytics.util.F4MAssertions.assertDoesNotThrow;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Module;

import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import de.ascendro.f4m.server.analytics.AnalyticsDaoImpl;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.util.ProfilePrimaryKeyUtil;
import de.ascendro.f4m.service.analytics.activemq.IQueueBrokerManager;
import de.ascendro.f4m.service.analytics.activemq.router.AnalyticMessageListener;
import de.ascendro.f4m.service.analytics.config.AnalyticsConfig;
import de.ascendro.f4m.service.analytics.data.EventTestData;
import de.ascendro.f4m.service.analytics.di.AchievementModule;
import de.ascendro.f4m.service.analytics.di.AnalyticsServiceModule;
import de.ascendro.f4m.service.analytics.di.StatisticModule;
import de.ascendro.f4m.service.analytics.module.statistic.StatisticWatcher;
import de.ascendro.f4m.service.analytics.module.statistic.TestStatisticListener;
import de.ascendro.f4m.service.analytics.notification.di.NotificationWebSocketModule;
import de.ascendro.f4m.service.analytics.util.NetworkUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.dao.aerospike.AerospikeClientMockProvider;
import de.ascendro.f4m.service.dao.aerospike.RealAerospikeTestBase;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.integration.test.F4MIntegrationTestBase;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileAddress;
import de.ascendro.f4m.service.profile.model.ProfileUser;


public class StatisticModuleTest extends RealAerospikeTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatisticModuleTest.class);
    private static AnalyticsServiceStartup analyticsServiceStartup;
    private static DB db;
    private String profileSet;
    private ProfilePrimaryKeyUtil profilePrimaryKeyUtil;

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

    @Override
	@Before
    public void setUp() {
        super.setUp();

        profileSet = config.getProperty(AerospikeConfigImpl.AEROSPIKE_PROFILE_SET);
        profilePrimaryKeyUtil = new ProfilePrimaryKeyUtil(config);
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
        FileUtils.deleteDirectory(new File("target/activemq-data/"));
        final AnalyticsScan analyticsScan = analyticsServiceStartup.getInjector().getInstance(AnalyticsScan.class);
        startActiveMQ();
        initConsumers();
        analyticsScan.execute();
    }

    public void initConsumers() throws Exception {
        IQueueBrokerManager embeddedActiveMQ = analyticsServiceStartup.getInjector().getInstance(IQueueBrokerManager.class);
        embeddedActiveMQ.initConsumers();
    }


    public void startActiveMQ() throws Exception {
        IQueueBrokerManager embeddedActiveMQ = analyticsServiceStartup.getInjector().getInstance(IQueueBrokerManager.class);
        embeddedActiveMQ.start();
    }

    public void stopActiveMQ() throws Exception {
        IQueueBrokerManager embeddedActiveMQ = analyticsServiceStartup.getInjector().getInstance(IQueueBrokerManager.class);
        if (embeddedActiveMQ != null) {
            embeddedActiveMQ.stopSubscribers();
            embeddedActiveMQ.stop();
        }

        StatisticWatcher statisticWatcher = analyticsServiceStartup.getInjector().getInstance(StatisticWatcher.class);
        statisticWatcher.stopWatcher();
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
                    return Arrays.asList(new AnalyticsServiceModule(),
                            new StatisticModule(), new NotificationWebSocketModule(),
                            new AchievementModule());
                }

                @Override
                protected List<Class<? extends AnalyticMessageListener>> getTestListeners() {
                    return Arrays.asList(TestStatisticListener.class);
                }
            };
        } else {
            return new AnalyticsServiceStartupUsingAerospikeReal(F4MIntegrationTestBase.DEFAULT_TEST_STAGE){
                @Override
                protected Iterable<? extends Module> getTestedModules() {
                    return Arrays.asList(new AnalyticsServiceModule(),
                            new StatisticModule(), new NotificationWebSocketModule());
                }

                @Override
                protected List<Class<? extends AnalyticMessageListener>> getTestListeners() {
                	return Arrays.asList(TestStatisticListener.class);
                }
            };
        }
    }


    @Test
    public void testStart() throws Exception {
        AnalyticsDaoImpl analyticsDaoImpl = analyticsServiceStartup.getInjector().getInstance(AnalyticsDaoImpl.class);

        EventTestData eventTestData = new EventTestData(analyticsDaoImpl);
        eventTestData.createInitialData();

        TestStatisticListener.initLock(eventTestData.getEventCounter().getStatisticEntriesCounter());

        final int timeout = 2 * RetriedAssert.DEFAULT_TIMEOUT_MS;
        assumeNotNull(config.getProperty(AnalyticsConfig.MYSQL_DATABASE_URL));

        RetriedAssert.assertWithWait(() -> assertTrue(NetworkUtil.portAvailable(AnalyticsTestConfig.MARIADB_PORT)), timeout);

        URL url = this.getClass().getClassLoader().getResource("mysql/db");

        assertNotNull(url);

        String dbFolder = url.getFile();
        FileUtils.deleteQuietly(new File(dbFolder));

        DBConfigurationBuilder configBuilder = DBConfigurationBuilder.newBuilder();
        configBuilder.setPort(AnalyticsTestConfig.MARIADB_PORT);
        configBuilder.setDataDir(dbFolder);

        db = DB.newEmbeddedDB(configBuilder.build());
        LOGGER.info("Starting database");
        db.start();

        db.source("mysql/schema.sql");

        this.start();

        RetriedAssert.assertWithWait(() ->assertEquals(TestStatisticListener.lock.getCount(),0), 60000, 2000);

        LOGGER.info("Stopping database");
    }

    @Test
    public void testStop() throws Exception {
        assumeNotNull(config.getProperty(AnalyticsConfig.MYSQL_DATABASE_URL));
        assumeNotNull(analyticsServiceStartup);

        assertDoesNotThrow(()-> this.stop());
    }


    public Profile createProfile(AnalyticsDaoImpl analyticsDaoImpl, String userId) {
        Profile profile = new Profile();
        profile.setUserId(userId);
        ProfileAddress address = new ProfileAddress();
        address.setCity("City");
        address.setCountry("DE");
        profile.setAddress(address);
        ProfileUser person = new ProfileUser();
        person.setFirstName("Firstname");
        person.setLastName("Lastname");
        profile.setPersonWrapper(person);
        analyticsDaoImpl.createJson(profileSet, profilePrimaryKeyUtil
                .createPrimaryKey(userId), CommonProfileAerospikeDao.BLOB_BIN_NAME, profile.getAsString());
        return profile;
    }

}
