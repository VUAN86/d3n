package de.ascendro.f4m.server.analytics.tracker;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.query.RecordSet;
import com.aerospike.client.query.Statement;

import de.ascendro.f4m.server.analytics.AnalyticsDaoImpl;
import de.ascendro.f4m.server.analytics.AnalyticsDaoImplTest;
import de.ascendro.f4m.server.analytics.AnalyticsPrimaryKeyUtil;
import de.ascendro.f4m.server.analytics.exception.F4MAnalyticsFatalErrorException;
import de.ascendro.f4m.server.analytics.model.AdEvent;
import de.ascendro.f4m.server.analytics.model.PaymentEvent;
import de.ascendro.f4m.server.analytics.model.TombolaEndEvent;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.dao.aerospike.AerospikeClientMockProvider;
import de.ascendro.f4m.service.dao.aerospike.RealAerospikeTestBase;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.session.pool.SessionPool;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;

public class TrackerImplTest extends RealAerospikeTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsDaoImplTest.class);

    private static final String NAMESPACE = "test";
    private final JsonUtil jsonUtil = new JsonUtil();
    private AnalyticsDaoImpl analyticsDaoImpl;

    @Mock
    private SessionPool sessionPool;

    @Before
    @Override
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        super.setUp();

        if (aerospikeClientProvider != null && !(aerospikeClientProvider instanceof AerospikeClientMockProvider)) {
            clearTestAnalyticsInstanceSet();
        }
    }

    @Override
    protected void setUpAerospike() {
        AnalyticsPrimaryKeyUtil analyticsPrimaryKeyUtil = new AnalyticsPrimaryKeyUtil(config);
        analyticsDaoImpl = new AnalyticsDaoImpl(config,
                analyticsPrimaryKeyUtil, aerospikeClientProvider, jsonUtil);
    }


    @Override
    protected Config createConfig() {
        final Config config = super.createConfig();
        config.setProperty(AerospikeConfigImpl.AEROSPIKE_NAMESPACE, NAMESPACE);

        return config;
    }

    @After
    @Override
    public void tearDown() {
        if (aerospikeClientProvider != null && !(aerospikeClientProvider instanceof AerospikeClientMockProvider)) {
            try {
                clearTestAnalyticsInstanceSet();
            } finally {
                super.tearDown();
            }
        }
    }

    private void clearTestAnalyticsInstanceSet() {
        LOGGER.info("Clearing Aerospike set {} within namespace {} at exit", AnalyticsDaoImpl.ANALYTICS_SET_NAME, NAMESPACE);
        clearSet(NAMESPACE, AnalyticsDaoImpl.ANALYTICS_SET_NAME);
    }


    protected IAerospikeClient getAerospikeClient() {
        return aerospikeClientProvider.get();
    }

    @Test
    public void testAddEvent() throws Exception {
        TrackerImpl tracker = new TrackerImpl(analyticsDaoImpl);

        AdEvent adEvent = new AdEvent();
        adEvent.setGameId(101L);
        adEvent.setEarnCredits(5L);
        adEvent.setAdId(500L);

        tracker.addEvent(KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO, adEvent);

        Statement statement = new Statement();
        statement.setNamespace(NAMESPACE);
        statement.setSetName(AnalyticsDaoImpl.ANALYTICS_SET_NAME);
        try(RecordSet recordSet = getAerospikeClient().query(null, statement)){
	        assertNotNull(recordSet);
	        assertTrue(recordSet.next());
        }
    }

    @Test(expected = F4MAnalyticsFatalErrorException.class)
    public void testEventRequireClientInfo() throws Exception {
        TrackerImpl tracker = new TrackerImpl(analyticsDaoImpl);

        AdEvent adEvent = new AdEvent();
        adEvent.setGameId(101L);
        adEvent.setEarnCredits(5L);
        adEvent.setAdId(500L);

        tracker.addEvent(new ClientInfo(KeyStoreTestUtil.ANONYMOUS_USER_ID), adEvent);
    }

    @Test
    public void testEventNotRequireClientInfo() throws Exception {
        TrackerImpl tracker = new TrackerImpl(analyticsDaoImpl);

        PaymentEvent paymentEvent = new PaymentEvent();
        paymentEvent.setCreditPaid(100L);

        tracker.addEvent(new ClientInfo(KeyStoreTestUtil.ANONYMOUS_USER_ID), paymentEvent);

        Statement statement = new Statement();
        statement.setNamespace(NAMESPACE);
        statement.setSetName(AnalyticsDaoImpl.ANALYTICS_SET_NAME);
        try(RecordSet recordSet = getAerospikeClient().query(null, statement)){
            assertNotNull(recordSet);
            assertTrue(recordSet.next());
        }
    }


    @Test
    public void testTombolaEndEvent() throws Exception {
        TrackerImpl tracker = new TrackerImpl(analyticsDaoImpl);

        TombolaEndEvent tombolaEndEvent = new TombolaEndEvent();
        tombolaEndEvent.setTombolaId(1L);

        tracker.addAnonymousEvent(tombolaEndEvent);

        Statement statement = new Statement();
        statement.setNamespace(NAMESPACE);
        statement.setSetName(AnalyticsDaoImpl.ANALYTICS_SET_NAME);
        try(RecordSet recordSet = getAerospikeClient().query(null, statement)){
            assertNotNull(recordSet);
            assertTrue(recordSet.next());
        }
    }

}