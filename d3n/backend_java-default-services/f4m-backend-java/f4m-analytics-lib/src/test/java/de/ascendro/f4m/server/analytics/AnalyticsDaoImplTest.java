package de.ascendro.f4m.server.analytics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;

import java.math.BigDecimal;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Record;
import com.aerospike.client.query.RecordSet;
import com.aerospike.client.query.Statement;

import de.ascendro.f4m.server.analytics.exception.F4MAnalyticsFatalErrorException;
import de.ascendro.f4m.server.analytics.model.AdEvent;
import de.ascendro.f4m.server.analytics.model.InviteEvent;
import de.ascendro.f4m.server.analytics.model.PaymentEvent;
import de.ascendro.f4m.server.analytics.model.PlayerGameEndEvent;
import de.ascendro.f4m.server.analytics.tracker.TrackerBuilders;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.dao.aerospike.AerospikeClientMockProvider;
import de.ascendro.f4m.service.dao.aerospike.RealAerospikeTestBase;
import de.ascendro.f4m.service.json.model.ISOCountry;


public class AnalyticsDaoImplTest extends RealAerospikeTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsDaoImplTest.class);

    private static final String NAMESPACE = "test";
    private final JsonUtil jsonUtil = new JsonUtil();
    private AnalyticsDaoImpl analyticsDaoImpl;

    @Before
    @Override
    public void setUp() {
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

        //config.setProperty(AerospikeConfigImpl.AEROSPIKE_SERVER_HOST, "192.168.33.10");
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
        LOGGER.info("Clearing Aerospike set {} within namespace {} at exit", PlayerGameEndEvent.ANALYTICS_USER2APP_SET_NAME, NAMESPACE);
        clearSet(NAMESPACE, PlayerGameEndEvent.ANALYTICS_USER2APP_SET_NAME);
        LOGGER.info("Clearing Aerospike set {} within namespace {} at exit", PlayerGameEndEvent.ANALYTICS_USER2GAME_SET_NAME, NAMESPACE);
        clearSet(NAMESPACE, PlayerGameEndEvent.ANALYTICS_USER2GAME_SET_NAME);
        LOGGER.info("Clearing Aerospike set {} within namespace {} at exit", AdEvent.ANALYTICS_ADD2APP_SET_NAME, NAMESPACE);
        clearSet(NAMESPACE, AdEvent.ANALYTICS_ADD2APP_SET_NAME);
        LOGGER.info("Clearing Aerospike set {} within namespace {} at exit", AdEvent.ANALYTICS_ADD2GAME_SET_NAME, NAMESPACE);
        clearSet(NAMESPACE, AdEvent.ANALYTICS_ADD2GAME_SET_NAME);
    }

    @Test(expected = F4MAnalyticsFatalErrorException.class)
    public void testEmptyEvent() throws Exception {
        new TrackerBuilders.EventBuilder()
                .setAnalyticsContent(new TrackerBuilders.ContentBuilder()
                        .setUserId("1")
                        .setApplicationId("100")
                        .setCountryCode(ISOCountry.DE)
                        .setSessionIP("0.0.0.0")
                        .setEventData(null))
                .build();
    }

    @Test
    public void testEvent() throws Exception {
        AdEvent adEvent = new AdEvent();
        adEvent.setGameId(101L);
        adEvent.setEarnCredits(5L);
        adEvent.setAdId(500L);

        EventRecord testEventRecord = new TrackerBuilders.EventBuilder()
                .setAnalyticsContent(new TrackerBuilders.ContentBuilder()
                        .setUserId("1")
                        .setApplicationId("100")
                        .setCountryCode(ISOCountry.DE)
                        .setSessionIP("0.0.0.0")
                        .setEventData(adEvent))
                .build();

        //Register event 1
        analyticsDaoImpl.createAnalyticsEvent(testEventRecord);
        analyticsDaoImpl.createFirstAppUsageRecord(testEventRecord.getAnalyticsContent());
        analyticsDaoImpl.createFirstGameUsageRecord(testEventRecord.getAnalyticsContent());

        Statement statement = new Statement();
        statement.setNamespace(NAMESPACE);
        statement.setSetName(AnalyticsDaoImpl.ANALYTICS_SET_NAME);
        RecordSet recordSet = getAerospikeClient().query(null, statement);
        assertNotNull(recordSet);
        assertTrue(recordSet.next());
        Record record = recordSet.getRecord();

        assertTrue(record.bins.get(AnalyticsDaoImpl.CONTENT_BIN_NAME) instanceof byte[]);
        byte[] value = (byte[]) record.bins.get(AnalyticsDaoImpl.CONTENT_BIN_NAME);
        EventContent content = jsonUtil.fromJson(new String(value), EventContent.class);
        adEvent = new AdEvent(content.getEventData().getJsonObject());
        assertEquals(adEvent.getAdId().longValue(), 500L);
        assertEquals(adEvent.getEarnCredits().longValue(), 5L);

        recordSet.close();

        statement = new Statement();
        statement.setNamespace(NAMESPACE);
        statement.setSetName(AdEvent.ANALYTICS_ADD2APP_SET_NAME);
        recordSet = getAerospikeClient().query(null, statement);
        assertNotNull(recordSet);
        assertTrue(recordSet.next());

        clearTestAnalyticsInstanceSet();

        PaymentEvent paymentEvent = new PaymentEvent();
        paymentEvent.setGameId(101L);
        paymentEvent.setCreditPaid(1000L);
        paymentEvent.setMoneyCharged(new BigDecimal(50.5));

        testEventRecord = new TrackerBuilders.EventBuilder()
                .setAnalyticsContent(new TrackerBuilders.ContentBuilder()
                        .setUserId("1")
                        .setApplicationId("100")
                        .setCountryCode(ISOCountry.DE)
                        .setSessionIP("0.0.0.0")
                        .setEventData(paymentEvent))
                .build();


        //Register event 1
        analyticsDaoImpl.createAnalyticsEvent(testEventRecord);

        statement = new Statement();
        statement.setNamespace(NAMESPACE);
        statement.setSetName(AnalyticsDaoImpl.ANALYTICS_SET_NAME);
        recordSet = getAerospikeClient().query(null, statement);
        assertNotNull(recordSet);
        assertTrue(recordSet.next());
        record = recordSet.getRecord();

        assertTrue(record.bins.get(AnalyticsDaoImpl.CONTENT_BIN_NAME) instanceof byte[]);
        value = (byte[]) record.bins.get(AnalyticsDaoImpl.CONTENT_BIN_NAME);
        content = jsonUtil.fromJson(new String(value), EventContent.class);
        paymentEvent = new PaymentEvent(content.getEventData().getJsonObject());
        assertEquals(paymentEvent.getCreditPaid().longValue(), 1000);
        assertEquals(paymentEvent.getMoneyCharged(), new BigDecimal(50.5));
		assertEquals(content.getCountryCode(), ISOCountry.DE.name());

        clearTestAnalyticsInstanceSet();

        //Register event 3
        InviteEvent inviteEvent = new InviteEvent();
        inviteEvent.setGameId(101L);
        inviteEvent.setFriendsInvitedToo(true);
        inviteEvent.setInvitedFromFriends(true);

        testEventRecord = new TrackerBuilders.EventBuilder()
                .setAnalyticsContent(new TrackerBuilders.ContentBuilder()
                        .setUserId("1")
                        .setApplicationId("100")
                        .setCountryCode(ISOCountry.DE)
                        .setSessionIP("0.0.0.0")
                        .setEventData(inviteEvent))
                .build();


        //Register event 1
        analyticsDaoImpl.createAnalyticsEvent(testEventRecord);

        statement = new Statement();
        statement.setNamespace(NAMESPACE);
        statement.setSetName(AnalyticsDaoImpl.ANALYTICS_SET_NAME);
        recordSet = getAerospikeClient().query(null, statement);
        assertNotNull(recordSet);
        assertTrue(recordSet.next());
        record = recordSet.getRecord();

        assertTrue(record.bins.get(AnalyticsDaoImpl.CONTENT_BIN_NAME) instanceof byte[]);
        value = (byte[]) record.bins.get(AnalyticsDaoImpl.CONTENT_BIN_NAME);
        content = jsonUtil.fromJson(new String(value), EventContent.class);
        inviteEvent = new InviteEvent(content.getEventData().getJsonObject());
        assertTrue(inviteEvent.isFriendsInvitedToo());
        assertTrue(inviteEvent.isInvitedFromFriends());
    }


    @Test
    public void testEventWithProcessing() throws Exception {
        assumeNotNull(config.getProperty(AerospikeConfigImpl.AEROSPIKE_SERVER_HOST));

        AdEvent adEvent = new AdEvent();
        adEvent.setGameId(101L);
        adEvent.setEarnCredits(5L);
        adEvent.setAdId(500L);

        EventRecord testEventRecord = new TrackerBuilders.EventBuilder()
                .setAnalyticsContent(new TrackerBuilders.ContentBuilder()
                        .setUserId("1")
                        .setApplicationId("100")
                        .setCountryCode(ISOCountry.DE)
                        .setSessionIP("0.0.0.0")
                        .setEventData(adEvent))
                .build();


        //Register event 1
        String key = analyticsDaoImpl.createAnalyticsEvent(testEventRecord);

        EventContent content = jsonUtil.fromJson(analyticsDaoImpl.readJson(AnalyticsDaoImpl.ANALYTICS_SET_NAME, key, AnalyticsDaoImpl.CONTENT_BIN_NAME), EventContent.class);
        adEvent = new AdEvent(content.getEventData().getJsonObject());
        LOGGER.info("Game ID = " + adEvent.getGameId());
        LOGGER.info("Fisrt app usage = " + adEvent.isFirstAppUsage());
        assertTrue(adEvent.isFirstAppUsage());
        assertEquals(adEvent.getAdId().longValue(), 500);
        assertEquals(adEvent.getEarnCredits().longValue(), 5);

        //Register event 2
        adEvent = new AdEvent();
        adEvent.setGameId(102L);
        adEvent.setEarnCredits(5L);
        adEvent.setAdId(500L);

        testEventRecord = new TrackerBuilders.EventBuilder()
                .setAnalyticsContent(new TrackerBuilders.ContentBuilder()
                        .setUserId("1")
                        .setApplicationId("100")
                        .setCountryCode(ISOCountry.DE)
                        .setSessionIP("0.0.0.0")
                        .setEventData(adEvent))
                .build();


        //Register event 2
        key = analyticsDaoImpl.createAnalyticsEvent(testEventRecord);

        content = jsonUtil.fromJson(analyticsDaoImpl.readJson(AnalyticsDaoImpl.ANALYTICS_SET_NAME, key, AnalyticsDaoImpl.CONTENT_BIN_NAME), EventContent.class);
        adEvent = new AdEvent(content.getEventData().getJsonObject());
        LOGGER.info("Game ID = " + adEvent.getGameId());
        LOGGER.info("Fisrt app usage = " + adEvent.isFirstAppUsage());
        assertFalse(adEvent.isFirstAppUsage());
    }

    protected IAerospikeClient getAerospikeClient() {
        return aerospikeClientProvider.get();
    }
}

