package de.ascendro.f4m.service.advertisement.integration;

import static de.ascendro.f4m.server.advertisement.dao.AdvertisementDaoImpl.COUNTER_BIN_NAME;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.google.inject.Injector;

import de.ascendro.f4m.advertisement.AdvertisementMessageTypes;
import de.ascendro.f4m.advertisement.model.GameAdvertisementsGetResponse;
import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.advertisement.dao.AdvertisementPrimaryKeyUtil;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDaoImpl;
import de.ascendro.f4m.server.profile.model.AppConfig;
import de.ascendro.f4m.server.profile.model.AppConfigBuilder;
import de.ascendro.f4m.server.profile.util.ProfilePrimaryKeyUtil;
import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.advertisement.di.AdvertisementDefaultMessageMapper;
import de.ascendro.f4m.service.advertisement.model.schema.AdvertisementMessageSchemaMapper;
import de.ascendro.f4m.service.annotation.ClientMessageHandler;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.exception.ExceptionCodes;
import de.ascendro.f4m.service.integration.collector.ReceivedMessageCollector;
import de.ascendro.f4m.service.integration.test.F4MServiceIntegrationTestBase;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;

public class AdvertisementServiceStartupTest extends F4MServiceIntegrationTestBase {

    private AerospikeDao aerospikeDao;
    private IAerospikeClient aerospikeClient;
    private String profileSet;
    private ProfilePrimaryKeyUtil profilePrimaryKeyUtil;
    private ReceivedMessageCollector receivedMessageCollector;
    private ClientInfo clientInfo;
    private static final long TEST_ADVERTISEMENT_PROVIDER_ID = 5;
    private static final int NUMBER_OF_ADVERTISEMENTS_EXPECTED = 5;
    private static final String AEROSPIKE_ADVERTISEMENT_SET = "advertisement";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        receivedMessageCollector = (ReceivedMessageCollector) clientInjector
                .getInstance(com.google.inject.Key.get(JsonMessageHandlerProvider.class, ClientMessageHandler.class))
                .get();

        this.profileSet = config.getProperty(AerospikeConfigImpl.AEROSPIKE_PROFILE_SET);
        this.profilePrimaryKeyUtil = new ProfilePrimaryKeyUtil(config);
        Injector injector = jettyServerRule.getServerStartup().getInjector();
        aerospikeDao = injector.getInstance(CommonProfileAerospikeDaoImpl.class);

        aerospikeClient = jettyServerRule.getServerStartup().getInjector()
                .getInstance(AerospikeClientProvider.class).get();

        ApplicationConfigurationAerospikeDao appConfigDao = injector.getInstance(ApplicationConfigurationAerospikeDao.class);
        when(appConfigDao.getAppConfiguration(anyString(), anyString())).thenReturn(AppConfigBuilder.buildDefaultAppConfigWithAdvertisement());
        setClientInfo(ANONYMOUS_CLIENT_INFO);
    }

    private void setClientInfo(ClientInfo clientInfo) {
        this.clientInfo = ClientInfo.cloneOf(clientInfo);
    }

    @Override
    protected TestClientInjectionClasses getTestClientInjectionClasses() {
        return new TestClientInjectionClasses(AdvertisementDefaultMessageMapper.class, AdvertisementMessageSchemaMapper.class);
    }

    @Override
    protected ServiceStartup getServiceStartup() {
        return new AdvertisementServiceStartupUsingAerospikeMock(DEFAULT_TEST_STAGE);
    }


    @Test
    public void testGameAdvertisementsGet() throws Exception {
        writeTestData();
        final String requestJson = getPlainTextJsonFromResources("GameAdvertisementsGetRequest.json", clientInfo);
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
        assertReceivedMessagesWithWait(AdvertisementMessageTypes.GAME_ADVERTISEMENTS_GET_RESPONSE);
        JsonMessage<GameAdvertisementsGetResponse> response = receivedMessageCollector.getMessageByType(AdvertisementMessageTypes.GAME_ADVERTISEMENTS_GET_RESPONSE);
        assertEquals(NUMBER_OF_ADVERTISEMENTS_EXPECTED, response.getContent().getAdvertisementBlobKeys().length);
    }

    @Test
    public void testAdvertisementHasBeenShown() throws Exception {
        createProfile(KeyStoreTestUtil.ANONYMOUS_USER_ID);
        final String requestJson = getPlainTextJsonFromResources("AdvertisementHasBeenShownRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO);
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
        assertReceivedMessagesWithWait(AdvertisementMessageTypes.ADVERTISEMENT_HAS_BEEN_SHOWN_RESPONSE);
        final JsonMessage<? extends JsonMessageContent> response =
                receivedMessageCollector.getMessageByType(AdvertisementMessageTypes.ADVERTISEMENT_HAS_BEEN_SHOWN_RESPONSE);

        assertNull(response.getError());
    }

    @Test
    public void testAdvertisementHasBeenShownWithGameId() throws Exception {
        createProfile(KeyStoreTestUtil.ANONYMOUS_USER_ID);
        final String requestJson = getPlainTextJsonFromResources("AdvertisementHasBeenShownRequestWithGameId.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO);
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
        assertReceivedMessagesWithWait(AdvertisementMessageTypes.ADVERTISEMENT_HAS_BEEN_SHOWN_RESPONSE);
        final JsonMessage<? extends JsonMessageContent> response =
                receivedMessageCollector.getMessageByType(AdvertisementMessageTypes.ADVERTISEMENT_HAS_BEEN_SHOWN_RESPONSE);

        assertNull(response.getError());
    }

    @Test
    public void testAdvertisementHasBeenShownSchemaValidation() throws Exception {
        createProfile(KeyStoreTestUtil.ANONYMOUS_USER_ID);
        final String requestJson = getPlainTextJsonFromResources("AdvertisementHasBeenShownRequestNoBlobKey.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO);
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
        assertReceivedMessagesWithWait(AdvertisementMessageTypes.ADVERTISEMENT_HAS_BEEN_SHOWN_RESPONSE);
        final JsonMessage<? extends JsonMessageContent> response =
                receivedMessageCollector.getMessageByType(AdvertisementMessageTypes.ADVERTISEMENT_HAS_BEEN_SHOWN_RESPONSE);

        assertEquals(ExceptionCodes.ERR_VALIDATION_FAILED, response.getError().getCode());
    }

    @Test
    public void testAdvertisementsGetNoProviderId() throws Exception {
        writeTestData();
        final String requestJson = getPlainTextJsonFromResources("GameAdvertisementsGetNoProviderIdRequest.json", clientInfo);
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
        assertReceivedMessagesWithWait(AdvertisementMessageTypes.GAME_ADVERTISEMENTS_GET_RESPONSE);
        JsonMessage<GameAdvertisementsGetResponse> response = receivedMessageCollector.getMessageByType(AdvertisementMessageTypes.GAME_ADVERTISEMENTS_GET_RESPONSE);
        assertEquals(ExceptionCodes.ERR_ENTRY_NOT_FOUND, response.getError().getCode());
    }

    @Test
    public void testAdvertisementsGetNoNumberOfAdvertisementsSet() throws Exception {
        Injector injector = jettyServerRule.getServerStartup().getInjector();
        ApplicationConfigurationAerospikeDao appConfigDao = injector.getInstance(ApplicationConfigurationAerospikeDao.class);
        AppConfig appConfig = AppConfigBuilder.buildDefaultAppConfigWithAdvertisement();
        appConfig.getApplication().getConfiguration().setNumberOfPreloadedAdvertisements(0);
        when(appConfigDao.getAppConfiguration(anyString(), anyString())).thenReturn(appConfig);
        final String requestJson = getPlainTextJsonFromResources("GameAdvertisementsGetRequest.json", clientInfo);
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
        assertReceivedMessagesWithWait(AdvertisementMessageTypes.GAME_ADVERTISEMENTS_GET_RESPONSE);
        JsonMessage<GameAdvertisementsGetResponse> response = receivedMessageCollector.getMessageByType(AdvertisementMessageTypes.GAME_ADVERTISEMENTS_GET_RESPONSE);
        // if no numberOfAdvertisements is set, we get the default 20, so error must be null :
        assertEquals(null, response.getError());
        assertEquals(0, response.getContent().getAdvertisementBlobKeys().length);

    }


    private void createProfile(String userId) {
        Profile profile = new Profile();
        profile.setUserId(userId);

        aerospikeDao.createJson(profileSet, profilePrimaryKeyUtil.createPrimaryKey(userId),
                CommonProfileAerospikeDao.BLOB_BIN_NAME, profile.getAsString());
    }
    private void writeTestData() {
        AdvertisementPrimaryKeyUtil primaryKeyUtil = new AdvertisementPrimaryKeyUtil(config);

        final String counterKey = primaryKeyUtil.createPrimaryKey(TEST_ADVERTISEMENT_PROVIDER_ID);
        Key advKey = new Key("test", AEROSPIKE_ADVERTISEMENT_SET, counterKey);
        Bin counterBin1 = new Bin(COUNTER_BIN_NAME, NUMBER_OF_ADVERTISEMENTS_EXPECTED);
        aerospikeClient.put(null, advKey, counterBin1);
    }
}
