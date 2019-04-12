package de.ascendro.f4m.service.analytics;


import static de.ascendro.f4m.service.analytics.data.GameTestData.DUEL_INSTANCE_ID;
import static de.ascendro.f4m.service.analytics.data.GameTestData.TOURNAMENT_INSTANCE_ID;
import static de.ascendro.f4m.service.integration.F4MAssert.assertReceivedMessagesAnyOrderWithWait;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import de.ascendro.f4m.server.analytics.AnalyticsDaoImpl;
import de.ascendro.f4m.server.friend.CommonBuddyElasticDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.service.analytics.activemq.router.AnalyticMessageListener;
import de.ascendro.f4m.service.analytics.config.AnalyticsConfig;
import de.ascendro.f4m.service.analytics.data.EventTestData;
import de.ascendro.f4m.service.analytics.data.GameTestData;
import de.ascendro.f4m.service.analytics.data.TombolaTestData;
import de.ascendro.f4m.service.analytics.di.AchievementModule;
import de.ascendro.f4m.service.analytics.di.AnalyticsServiceModule;
import de.ascendro.f4m.service.analytics.di.NotificationModule;
import de.ascendro.f4m.service.analytics.di.StatisticModule;
import de.ascendro.f4m.service.analytics.module.notification.TestNotificationListener;
import de.ascendro.f4m.service.analytics.module.notification.processor.PushNotificationProcessor;
import de.ascendro.f4m.service.analytics.notification.di.NotificationWebSocketModule;
import de.ascendro.f4m.service.analytics.util.NetworkUtil;
import de.ascendro.f4m.service.exception.UnexpectedTestException;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.integration.test.F4MIntegrationTestBase;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileAddress;
import de.ascendro.f4m.service.profile.model.ProfileUser;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypes;
import de.ascendro.f4m.service.registry.model.monitor.MonitoringConnectionStatus;
import de.ascendro.f4m.service.registry.model.monitor.PushServiceStatistics;
import de.ascendro.f4m.service.registry.model.monitor.ServiceStatistics;
import de.ascendro.f4m.service.tombola.model.Tombola;
import de.ascendro.f4m.service.tombola.model.UserTombolaInfo;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.usermessage.model.SendEmailWrapperRequest;
import de.ascendro.f4m.service.usermessage.model.SendEmailWrapperResponse;
import de.ascendro.f4m.service.usermessage.model.SendUserPushResponse;
import de.ascendro.f4m.service.usermessage.model.SendWebsocketMessageRequest;
import de.ascendro.f4m.service.usermessage.translation.Messages;

public class NotificationModuleTest extends BaseServiceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationModuleTest.class);

	private Map<String, SendEmailWrapperRequest> emailsContentToCheck;
	private Map<String, SendWebsocketMessageRequest> pushContentToCheck;
	private List<JsonMessage<? extends JsonMessageContent>> receivedMessageList = new CopyOnWriteArrayList<>();

    private int emailCount;
    private CountDownLatch lock;
    private DB db;
    private DBConfigurationBuilder configBuilder;
    
    @Rule
    public ErrorCollector errorCollector = new ErrorCollector();

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        
        assertServiceStartup(UserMessageMessageTypes.SERVICE_NAME);

        emailsContentToCheck = new HashMap<>();
        pushContentToCheck = new HashMap<>();
        emailCount = 0;
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();

        AnalyticsServiceStartup analyticsServiceStartup = ((AnalyticsServiceStartup)jettyServerRule.getServerStartup());
        analyticsServiceStartup.stopScan();
        analyticsServiceStartup.stopActiveMQSubscribers();
        analyticsServiceStartup.stopActiveMQ();
        if (db!=null) {
            LOGGER.info("Stopping database");
            db.stop();
            db = null;
            configBuilder = null;
        }
    }

    @Override
    protected JsonMessageContent onReceivedMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded)
            throws Exception {
        if (UserMessageMessageTypes.SEND_EMAIL == originalMessageDecoded.getType(UserMessageMessageTypes.class)) {
            return onSendEmailReceived(originalMessageDecoded);
        } else if (UserMessageMessageTypes.SEND_WEBSOCKET_MESSAGE == originalMessageDecoded.getType(UserMessageMessageTypes.class)){
            return onSendPushReceived(originalMessageDecoded);
		} else if (ServiceRegistryMessageTypes.PUSH_SERVICE_STATISTICS == originalMessageDecoded.getType(ServiceRegistryMessageTypes.class)) {
        	receivedMessageList.add(originalMessageDecoded);
        	return null;
        }  else {
            throw new UnexpectedTestException("Unexpected message");
        }
    }

    @Override
    protected AnalyticsServiceStartupUsingAerospikeMock getAnalyticsServiceStartupUsingAerospikeMock() {
        return new AnalyticsServiceStartupUsingAerospikeMock(F4MIntegrationTestBase.DEFAULT_TEST_STAGE) {
            @Override
            protected Iterable<? extends Module> getTestedModules() {
                return Arrays.asList(new AnalyticsServiceModule(),
                        new NotificationModule(), new StatisticModule(), new NotificationWebSocketModule(),
                        new AchievementModule());
            }

            @Override
            protected List<Class<? extends AnalyticMessageListener>> getTestListeners() {
                return Arrays.asList(TestNotificationListener.class);
            }
            
			@Override
			protected Iterable<? extends Module> getModules() {
				return Arrays.asList(getAnalyticsServiceAerospikeOverrideModule(), new AbstractModule() {
					@Override
					protected void configure() {
						bind(CommonBuddyElasticDao.class).toInstance(mock(CommonBuddyElasticDao.class));
					}
				});
			}
        };
    }

    @Override
    protected AnalyticsServiceStartupUsingAerospikeReal getAnalyticsServiceStartupUsingAerospikeReal() {
        return new AnalyticsServiceStartupUsingAerospikeReal(F4MIntegrationTestBase.DEFAULT_TEST_STAGE) {
            @Override
            protected Iterable<? extends Module> getTestedModules() {
                return Arrays.asList(new AnalyticsServiceModule(),
                        new NotificationModule(), new StatisticModule(), new NotificationWebSocketModule());
            }

            @Override
            protected List<Class<? extends AnalyticMessageListener>> getTestListeners() {
                return Arrays.asList(TestNotificationListener.class);
            }
        };
    }

    private JsonMessageContent onSendPushReceived(JsonMessage<? extends JsonMessageContent> originalMessageDecoded) {
        verifyPush((SendWebsocketMessageRequest) originalMessageDecoded.getContent());
        return new SendUserPushResponse();
    }

    private void verifyPush(SendWebsocketMessageRequest request) {
    	SendWebsocketMessageRequest checkMessage = pushContentToCheck.get(request.getMessage());
        if (checkMessage!=null) {
        	errorCollector.checkThat(request.getUserId(), equalTo(checkMessage.getUserId()));
        	errorCollector.checkThat(request.getMessage(), equalTo(checkMessage.getMessage()));
            errorCollector.checkThat(request.isLanguageAuto(), is(true));
            if (checkMessage.getParameters()!=null) {
            	errorCollector.checkThat(request.getParameters(), arrayContaining(checkMessage.getParameters()));
            } else {
                errorCollector.checkThat(request.getParameters(), nullValue());
            }

            lock.countDown();
        }
    }

    private JsonMessageContent onSendEmailReceived(JsonMessage<? extends JsonMessageContent> originalMessageDecoded) {
        verifyEmail(emailCount, (SendEmailWrapperRequest) originalMessageDecoded.getContent());
        emailCount++;
        return new SendEmailWrapperResponse();
    }

    private void verifyEmail(int emailIndex, SendEmailWrapperRequest request) {
        SendEmailWrapperRequest checkMessage = emailsContentToCheck.get(request.getMessage());
        if (checkMessage!=null) {
            SendEmailWrapperRequest expected = emailsContentToCheck.get(emailIndex);
            errorCollector.checkThat(request.getUserId(), equalTo(expected.getUserId()));
            errorCollector.checkThat(request.getMessage(), equalTo(expected.getMessage()));
            errorCollector.checkThat(request.getParameters(), arrayContaining(expected.getParameters()));
            lock.countDown();
        }
    }

    public Profile createProfile(AnalyticsDaoImpl analyticsDaoImpl, String userId) {
        Profile profile = new Profile();
        profile.setUserId(userId);
        profile.setShowFullName(false);
        ProfileAddress address = new ProfileAddress();
        address.setCity("City");
        address.setCountry("DE");
        profile.setAddress(address);
        ProfileUser person = new ProfileUser();
        person.setFirstName("Firstname" + userId);
        person.setLastName("Lastname" + userId);
        person.setNickname("Nick" + userId);
        profile.setPersonWrapper(person);
        analyticsDaoImpl.createJson(profileSet, profilePrimaryKeyUtil
                .createPrimaryKey(userId), CommonProfileAerospikeDao.BLOB_BIN_NAME, profile.getAsString());
        return profile;
    }

    @Test
    public void testStart() throws Exception {
        //init analytics DAO
        AnalyticsDaoImpl analyticsDaoImpl = injector.getInstance(AnalyticsDaoImpl.class);

        //init event test data
        EventTestData eventTestData = new EventTestData(analyticsDaoImpl);

        //Create user profile
        Profile profile = createProfile(analyticsDaoImpl, "2");
        createProfile(analyticsDaoImpl, "1");
        pushContentToCheck.put(Messages.GAME_DUEL_WON_PUSH, getPushRequestWrapper(Messages.GAME_DUEL_WON_PUSH,
				String.valueOf(EventTestData.USER_ID), new String[] { "Nick2" }));
        eventTestData.createInitialData();
        eventTestData.createMultiplayerEvent(profile, TOURNAMENT_INSTANCE_ID);
        eventTestData.createMultiplayerEvent(profile, DUEL_INSTANCE_ID);

        //Prepare a game instance
        GameTestData gameTestData = new GameTestData(jsonLoader, analyticsDaoImpl, config);
        gameTestData.createGameInstanceTournament(TOURNAMENT_INSTANCE_ID);
        gameTestData.createGameInstanceDuel(DUEL_INSTANCE_ID);
        pushContentToCheck.put(Messages.GAME_TOURNAMENT_LOST_PUSH, getPushRequestWrapper(Messages.GAME_TOURNAMENT_LOST_PUSH,
                String.valueOf(EventTestData.USER_ID), null));

        //Create tombola data
        TombolaTestData tombolaTestData = new TombolaTestData(jsonLoader, jsonUtil, analyticsDaoImpl, config);
        List<Tombola> tombolaList = tombolaTestData.createTombolaTestData("tombolaData.json");
        List<UserTombolaInfo> expectedTombolas = tombolaTestData.createUserTombolaTestData("userTombolaData.json");
        assertEquals(1, expectedTombolas.size());

        tombolaTestData.createPurchasedTickets(injector, tombolaList.get(0));

        //Create tombola event
        eventTestData.createTombolaEvents();

        TestNotificationListener.initLock(eventTestData.getEventCounter().getNotificationEntriesCounter());

        assumeNotNull(config.getProperty(AnalyticsConfig.MYSQL_DATABASE_URL));
        final int timeout = 2 * RetriedAssert.DEFAULT_TIMEOUT_MS;

        RetriedAssert.assertWithWait(() -> assertTrue(NetworkUtil.portAvailable(AnalyticsTestConfig.MARIADB_PORT)), timeout);

        URL url = this.getClass().getClassLoader().getResource("mysql/db");

        assertNotNull(url);

        String dbFolder = url.getFile();
        FileUtils.deleteQuietly(new File(dbFolder));

        configBuilder = DBConfigurationBuilder.newBuilder();
        configBuilder.setPort(AnalyticsTestConfig.MARIADB_PORT);
        configBuilder.setDataDir(dbFolder);
        db = DB.newEmbeddedDB(configBuilder.build());
        LOGGER.info("Starting database");
        db.start();

        db.source("mysql/schema.sql");

        pushContentToCheck.put(Messages.TOMBOLA_DRAW_WIN_PUSH, getPushRequestWrapper(Messages.TOMBOLA_DRAW_WIN_PUSH,
                String.valueOf(EventTestData.USER_ID), new String[] {expectedTombolas.get(0).getPrizes().get(0).getName()}));
        lock = new CountDownLatch(pushContentToCheck.size());

        analyticsServiceStartup.startActiveMQ(false);
        analyticsServiceStartup.startScan();

        assertLocksReleased(TestNotificationListener.lock);

        assertTrue(lock.await(10, TimeUnit.SECONDS));

        verifyMonitoring();
    }

	private void assertLocksReleased(CountDownLatch latch) throws InterruptedException {
		latch.await(4000, TimeUnit.MILLISECONDS);
        assertEquals(0, latch.getCount());
	}

	private void verifyMonitoring() {
		jettyServerRule.getServerStartup().startMonitoring();
		assertReceivedMessagesAnyOrderWithWait(receivedMessageList,
				ServiceRegistryMessageTypes.PUSH_SERVICE_STATISTICS);
		JsonMessageContent content = receivedMessageList.get(0).getContent();
		assertThat(content, instanceOf(PushServiceStatistics.class));
		ServiceStatistics statistics = ((PushServiceStatistics)content).getStatistics();
		LOGGER.debug("Received statistics {}", statistics);
		assertEquals(MonitoringConnectionStatus.OK, statistics.getConnectionsToDb().getAerospike());
		assertEquals(MonitoringConnectionStatus.NC, statistics.getConnectionsToDb().getElastic());
		assertEquals(MonitoringConnectionStatus.OK, statistics.getConnectionsToDb().getMysql());
		assertEquals(MonitoringConnectionStatus.NA, statistics.getConnectionsToDb().getSpark());
	}

    @Test
    public void tournamentNotificationTestStart() throws Exception {
        TestNotificationListener.initLock(1);
        AnalyticsServiceStartup analyticsServiceStartup = ((AnalyticsServiceStartup)jettyServerRule.getServerStartup());
         //init analytics DAO
        AnalyticsDaoImpl analyticsDaoImpl = analyticsServiceStartup.getInjector().getInstance(AnalyticsDaoImpl.class);

        //Prepare a game instance
        GameTestData gameTestData = new GameTestData(jsonLoader, analyticsDaoImpl, config);
        gameTestData.createGameInstanceTournament("1");

        //init event test data provider
        EventTestData eventTestData = new EventTestData(analyticsDaoImpl);

        pushContentToCheck.put(Messages.GAME_TOURNAMENT_LOST_PUSH, getPushRequestWrapper(Messages.GAME_TOURNAMENT_LOST_PUSH,
                String.valueOf(EventTestData.USER_ID), null));
        lock = new CountDownLatch(pushContentToCheck.size());

        PushNotificationProcessor pushNotificationProcessor = analyticsServiceStartup.getInjector().getInstance(PushNotificationProcessor.class);
        Profile profile = createProfile(analyticsDaoImpl, TOURNAMENT_INSTANCE_ID);

		pushNotificationProcessor.handleEvent(
				eventTestData.createMultiplayerEventRecord(profile, TOURNAMENT_INSTANCE_ID).getAnalyticsContent());
		assertTrue(lock.await(10, TimeUnit.SECONDS));
    }

}
