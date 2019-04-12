package de.ascendro.f4m.service.analytics;


import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.policy.ConsistencyLevel;
import com.aerospike.client.policy.ScanPolicy;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.analytics.AnalyticsDaoImpl;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.util.ProfilePrimaryKeyUtil;
import de.ascendro.f4m.service.analytics.config.AnalyticsConfig;
import de.ascendro.f4m.service.analytics.data.EventTestData;
import de.ascendro.f4m.service.analytics.module.achievement.processor.AchievementsLoader;
import de.ascendro.f4m.service.analytics.module.achievement.updater.AchievementsUpdaterScheduler;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.dao.aerospike.AerospikeClientMockProvider;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.integration.test.F4MIntegrationTestBase;
import de.ascendro.f4m.service.integration.test.ServiceStartupTest;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileAddress;
import de.ascendro.f4m.service.profile.model.ProfileUser;
import de.ascendro.f4m.service.registry.model.monitor.MonitoringConnectionStatus;
import de.ascendro.f4m.service.registry.model.monitor.MonitoringDbConnectionInfo;
import de.ascendro.f4m.service.util.MonitoringDbConnectionInfoBuilder;
import de.ascendro.f4m.service.util.register.ServiceMonitoringRegister;

public class AnalyticsServiceStartupTest extends ServiceStartupTest {
        //extends RealAerospikeTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsServiceStartupTest.class);

    private ServiceMonitoringRegister serviceMonitoringRegister;
    protected Config config;
    private AnalyticsServiceStartup analyticsServiceStartup;
    protected AerospikeClientProvider aerospikeClientProvider;
    private String profileSet;
    private ProfilePrimaryKeyUtil profilePrimaryKeyUtil;


    @Before
    @Override
    public void setUp() throws Exception {
        profileSet = config.getProperty(AerospikeConfigImpl.AEROSPIKE_PROFILE_SET);
        profilePrimaryKeyUtil = new ProfilePrimaryKeyUtil(config);
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
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

        analyticsServiceStartup.stopActiveMQ();
    }

	@Override
	public Class<? extends F4MConfigImpl> getServiceConfigClass() {
		return AnalyticsConfig.class;
	}
	
    private void setUpRealAerospikeClientProvider() {
    	initServiceMonitoringRegister();
    	aerospikeClientProvider = new AerospikeClientProvider(config, serviceMonitoringRegister);
    }

    private void setUpMockAerospikeClientProvider() {
    	initServiceMonitoringRegister();
        aerospikeClientProvider = new AerospikeClientMockProvider(config, serviceMonitoringRegister);
    }
    
    private void initServiceMonitoringRegister() {
    	serviceMonitoringRegister = new ServiceMonitoringRegister();
    }
    
	@Override
	protected MonitoringDbConnectionInfo getExpectedDbStatus() {
    	//due to manually (not via injection) started aerospike, monitoring won't return correct status.
    	//correct way would be binding AerospikeClientProvider or AerospikeClientMockProvider in AnalyticsServiceStartupUsingAerospikeMock
		return new MonitoringDbConnectionInfoBuilder(super.getExpectedDbStatus())
				.aerospike(MonitoringConnectionStatus.NC)
				.mysql(MonitoringConnectionStatus.NC) //mysql is not started in this test
				.elastic(MonitoringConnectionStatus.NC).build();
	}

	private void clearTestAnalyticsInstanceSet() {
        LOGGER.info("Clearing Aerospike set {} within namespace {} at exit", AnalyticsDaoImpl.ANALYTICS_SET_NAME, AnalyticsTestConfig.NAMESPACE);
        clearSet(AnalyticsTestConfig.NAMESPACE, AnalyticsDaoImpl.ANALYTICS_SET_NAME);
        clearSet(AnalyticsTestConfig.NAMESPACE, profileSet);
    }

    protected void clearSet(String namespace, String set) {
        LOGGER.info("Clearing set[{}] within namesapce[{}]", set, namespace);
        final IAerospikeClient iAerospikeClient = aerospikeClientProvider.get();
        final ScanPolicy scanPolicy = new ScanPolicy();
        scanPolicy.consistencyLevel = ConsistencyLevel.CONSISTENCY_ALL;
        iAerospikeClient.scanAll(scanPolicy, namespace, set, (key, record) -> iAerospikeClient.delete(null, key));
        LOGGER.info("Done clearing set[{}] within namesapce[{}]", set, namespace);
    }


    protected Config createConfig() {
        return new AnalyticsTestConfig();
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

    @Test
    public void testStart() throws Exception {
        assumeNotNull(config.getProperty(AerospikeConfigImpl.AEROSPIKE_SERVER_HOST));
        assumeNotNull(config.getProperty(AnalyticsConfig.MYSQL_DATABASE_URL));

        AnalyticsDaoImpl analyticsDaoImpl = analyticsServiceStartup.getInjector().getInstance(AnalyticsDaoImpl.class);
        EventTestData eventTestData = new EventTestData(analyticsDaoImpl);
        eventTestData.createInitialData();
        AchievementsUpdaterScheduler scheduler = spy(analyticsServiceStartup.getInjector().getInstance(AchievementsUpdaterScheduler.class));
        AchievementsLoader loader = spy(analyticsServiceStartup.getInjector().getInstance(AchievementsLoader.class));

        analyticsServiceStartup.start();
        RetriedAssert.assertWithWait(() -> assertTrue(analyticsServiceStartup.isStarted()));

        verify(analyticsServiceStartup, times(1)).startActiveMQ();
        verify(loader, times(1)).loadTenants();
        verify(scheduler, times(1)).schedule();
    }

    @Test
    public void testStop() throws Exception {
        //doNothing().when(analyticsServiceStartup).stopActiveMQ();

        analyticsServiceStartup.stop();

        verify(analyticsServiceStartup, times(1)).stopActiveMQ();
    }

    @Override
    protected AnalyticsServiceStartup getServiceStartup() {
        config = createConfig();

        if(!StringUtils.isEmpty(config.getProperty(AerospikeConfigImpl.AEROSPIKE_SERVER_HOST))){
            setUpRealAerospikeClientProvider();
        }else{
            setUpMockAerospikeClientProvider();
        }

        assertTrue(aerospikeClientProvider.get()
                .isConnected());

        if (StringUtils.isEmpty(config.getProperty(AerospikeConfigImpl.AEROSPIKE_SERVER_HOST))) {
            analyticsServiceStartup = spy(new AnalyticsServiceStartupUsingAerospikeMock(F4MIntegrationTestBase.DEFAULT_TEST_STAGE));
        } else {
            analyticsServiceStartup = spy(new AnalyticsServiceStartupUsingAerospikeReal(F4MIntegrationTestBase.DEFAULT_TEST_STAGE));
        }

        return analyticsServiceStartup;
    }
}
