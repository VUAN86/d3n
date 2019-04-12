package de.ascendro.f4m.service.promocode.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Injector;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.util.ProfilePrimaryKeyUtil;
import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.annotation.ClientMessageHandler;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.exception.ExceptionCodes;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.exception.UnexpectedTestException;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.integration.collector.ReceivedMessageCollector;
import de.ascendro.f4m.service.integration.rule.MockServiceRule.BindableAbstractModule;
import de.ascendro.f4m.service.integration.rule.MockServiceRule.ThrowingFunction;
import de.ascendro.f4m.service.integration.test.F4MServiceWithMockIntegrationTestBase;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.exception.F4MPaymentClientException;
import de.ascendro.f4m.service.payment.exception.PaymentServiceExceptionCodes;
import de.ascendro.f4m.service.payment.model.TransactionId;
import de.ascendro.f4m.service.profile.ProfileMessageTypes;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.promocode.PromocodeMessageTypes;
import de.ascendro.f4m.service.promocode.config.PromocodeConfig;
import de.ascendro.f4m.service.promocode.dao.PromocodeAerospikeDao;
import de.ascendro.f4m.service.promocode.dao.PromocodeAerospikeDaoImpl;
import de.ascendro.f4m.service.promocode.di.PromocodeDefaultMessageMapper;
import de.ascendro.f4m.service.promocode.exception.PromocodeServiceExceptionCodes;
import de.ascendro.f4m.service.promocode.model.schema.PromocodeMessageSchemaMapper;
import de.ascendro.f4m.service.promocode.util.PromocodePrimaryKeyUtil;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;

public class PromocodeServiceStartupTest extends F4MServiceWithMockIntegrationTestBase {

	private PromocodeAerospikeDao aerospikeDao;
	private IAerospikeClient aerospikeClient;
	private String profileSet;
	private PromocodePrimaryKeyUtil promocodePrimaryKeyUtil;
	private ProfilePrimaryKeyUtil profilePrimaryKeyUtil;

	private ReceivedMessageCollector receivedMessageCollector;

	private static final String PROMOCODE_BIN_NAME = "promocode";
	private static final String PROMOCODE_CAMPAIGN_BIN_NAME = "promoCampaign";
	private static final String DEFAULT_TEST_PROMOCODE_CODE = "MEGAPROMO";

	private static final String PROMOCODE_COUNTER_RECORD_NAME = "counter";
	private static final String PROMOCODE_CREATION_COUNTER_BIN_NAME = "creationCnt";
	private static final String PROMOCODE_CONSUMING_COUNTER_BIN_NAME = "consumingCnt";

	@Override
	protected void configureInjectionModuleMock(BindableAbstractModule module) {
		module.bindToModule(JsonMessageTypeMap.class).to(PromocodeDefaultMessageMapper.class);
		module.bindToModule(JsonMessageSchemaMap.class).to(PromocodeMessageSchemaMapper.class);
	}

	@Override
	protected ThrowingFunction<RequestContext, JsonMessageContent> getMessageHandlerMock() {
		return ctx -> onReceivedMessage(ctx.getMessage());
	}

	private int transferRequests = 0;
	private boolean paymentsShouldFail = false;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		MockitoAnnotations.initMocks(this);
		receivedMessageCollector = (ReceivedMessageCollector) clientInjector
				.getInstance(com.google.inject.Key.get(JsonMessageHandlerProvider.class, ClientMessageHandler.class))
				.get();

		assertServiceStartup(ProfileMessageTypes.SERVICE_NAME, PaymentMessageTypes.SERVICE_NAME);

		Injector injector = jettyServerRule.getServerStartup().getInjector();
		aerospikeDao = injector.getInstance(PromocodeAerospikeDaoImpl.class);

		aerospikeClient = jettyServerRule.getServerStartup().getInjector().getInstance(AerospikeClientProvider.class)
				.get();
		this.profileSet = config.getProperty(AerospikeConfigImpl.AEROSPIKE_PROFILE_SET);
		this.profilePrimaryKeyUtil = new ProfilePrimaryKeyUtil(config);
		this.promocodePrimaryKeyUtil = new PromocodePrimaryKeyUtil(config);
	}

	private JsonMessageContent onReceivedMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded)
			throws Exception {
		PaymentMessageTypes paymentType = originalMessageDecoded.getType(PaymentMessageTypes.class);
		if (PaymentMessageTypes.TRANSFER_BETWEEN_ACCOUNTS == paymentType
				|| PaymentMessageTypes.LOAD_OR_WITHDRAW_WITHOUT_COVERAGE == paymentType) {
				++transferRequests;

				if(paymentsShouldFail) {
					return onFailingTransferBetweenAccountsReceived(originalMessageDecoded);
				} else {
					return onTransferBetweenAccountsReceived(originalMessageDecoded);
				}

		} else {
			throw new UnexpectedTestException("Unexpected message " + originalMessageDecoded);
		}
	}

	private TransactionId onFailingTransferBetweenAccountsReceived(JsonMessage<? extends JsonMessageContent> originalMessageDecoded) {
		throw new F4MPaymentClientException(PaymentServiceExceptionCodes.ERR_ACCOUNT_STATE_NOT_CHANGEABLE,
				"Error occured");
	}

	private TransactionId onTransferBetweenAccountsReceived(JsonMessage<? extends JsonMessageContent> originalMessageDecoded) throws F4MException {
		return new TransactionId("123");
	}
	@Test
	public void testPromocodeUse() throws Exception {
		paymentsShouldFail = false;
		transferRequests = 0;

		String set = config.getProperty(PromocodeConfig.AEROSPIKE_PROMOCODE_SET);
		String campaignSet = config.getProperty(PromocodeConfig.AEROSPIKE_PROMOCODE_CAMPAIGN_SET);
		updateTestData("promocodeTestData.json", set, PROMOCODE_BIN_NAME);
		updateTestData("promocodeCampaignTestData.json", campaignSet, PROMOCODE_CAMPAIGN_BIN_NAME);
		// create user profile
		createProfile(KeyStoreTestUtil.ANONYMOUS_USER_ID);
		updateCounterTestData();

		final String promocodeUseRequest = getPlainTextJsonFromResources("promocodeUseRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO)
				.replace("<<CODE>>", "MEGAPROMO");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), promocodeUseRequest);

		RetriedAssert.assertWithWait(() ->
				assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> response =
				receivedMessageCollector.getMessageByType(PromocodeMessageTypes.PROMOCODE_USE_RESPONSE);
		assertNotNull(response);
		RetriedAssert.assertWithWait(() -> assertEquals(3, transferRequests));
	}

	@Test
	public void testPromocodeUseMultipleTimes() throws Exception {
		paymentsShouldFail = false;
		transferRequests = 0;
		String set = config.getProperty(PromocodeConfig.AEROSPIKE_PROMOCODE_SET);
		String campaignSet = config.getProperty(PromocodeConfig.AEROSPIKE_PROMOCODE_CAMPAIGN_SET);
		updateTestData("promocodeTestData.json", set, PROMOCODE_BIN_NAME);
		updateTestData("promocodeCampaignTestData.json", campaignSet, PROMOCODE_CAMPAIGN_BIN_NAME);
		// create user profile
		createProfile(KeyStoreTestUtil.ANONYMOUS_USER_ID);
		updateCounterTestData();

		final String promocodeUseRequest = getPlainTextJsonFromResources("promocodeUseRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO)
				.replace("<<CODE>>", "MEGAPROMO");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), promocodeUseRequest);

		RetriedAssert.assertWithWait(() ->
				assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> response =
				receivedMessageCollector.getMessageByType(PromocodeMessageTypes.PROMOCODE_USE_RESPONSE);
		assertNotNull(response);
		RetriedAssert.assertWithWait(() -> assertEquals(3, transferRequests));

		// send it second time :
		receivedMessageCollector.clearReceivedMessageList();
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), promocodeUseRequest);

		RetriedAssert.assertWithWait(() ->
				assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> responseSecondRequest =
				receivedMessageCollector.getMessageByType(PromocodeMessageTypes.PROMOCODE_USE_RESPONSE);
		assertNotNull(responseSecondRequest);
		RetriedAssert.assertWithWait(() -> assertEquals(6, transferRequests));

		// 3rd time should fail :
		receivedMessageCollector.clearReceivedMessageList();
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), promocodeUseRequest);
		RetriedAssert.assertWithWait(() ->
				assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> responseThirdRquest =
				receivedMessageCollector.getMessageByType(PromocodeMessageTypes.PROMOCODE_USE_RESPONSE);
		assertNotNull(responseThirdRquest);
		assertEquals(PromocodeServiceExceptionCodes.ERR_NO_LONGER_AVAILABLE, responseThirdRquest.getError().getCode());
		assertEquals(6, transferRequests);
	}

	@Test
	public void testInvalidPromocodeUse() throws Exception {
		paymentsShouldFail = false;
		transferRequests = 0;

		String set = config.getProperty(PromocodeConfig.AEROSPIKE_PROMOCODE_SET);
		String campaignSet = config.getProperty(PromocodeConfig.AEROSPIKE_PROMOCODE_CAMPAIGN_SET);
		updateTestData("promocodeTestData.json", set, PROMOCODE_BIN_NAME);
		updateTestData("promocodeCampaignTestData.json", campaignSet, PROMOCODE_CAMPAIGN_BIN_NAME);
		// create user profile
		String userId="1";
		createProfile(userId);
		updateCounterTestData();

		final String promocodeUseRequest = getPlainTextJsonFromResources("promocodeUseRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO)
				.replace("<<CODE>>", "INVALIDCODE");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), promocodeUseRequest);

		RetriedAssert.assertWithWait(() ->
				assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> response =
				receivedMessageCollector.getMessageByType(PromocodeMessageTypes.PROMOCODE_USE_RESPONSE);
		assertNotNull(response);
		assertEquals(ExceptionCodes.ERR_ENTRY_NOT_FOUND, response.getError().getCode());
		assertEquals(0, transferRequests);
	}

	@Test
	public void testNoLongerAvailablePromocodeUse() throws Exception {
		paymentsShouldFail = false;
		transferRequests = 0;

		String set = config.getProperty(PromocodeConfig.AEROSPIKE_PROMOCODE_SET);
		String campaignSet = config.getProperty(PromocodeConfig.AEROSPIKE_PROMOCODE_CAMPAIGN_SET);
		updateTestData("promocodeTestData.json", set, PROMOCODE_BIN_NAME);
		updateTestData("promocodeCampaignTestData.json", campaignSet, PROMOCODE_CAMPAIGN_BIN_NAME);
		// create user profile
		String userId="1";
		createProfile(userId);
		updateCounterTestDataNoLongerAvailable();

		final String promocodeUseRequest = getPlainTextJsonFromResources("promocodeUseRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO)
				.replace("<<CODE>>", "MEGAPROMO");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), promocodeUseRequest);

		RetriedAssert.assertWithWait(() ->
				assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> response =
				receivedMessageCollector.getMessageByType(PromocodeMessageTypes.PROMOCODE_USE_RESPONSE);
		assertNotNull(response);
		assertEquals(PromocodeServiceExceptionCodes.ERR_NO_LONGER_AVAILABLE, response.getError().getCode());
		assertEquals(0, transferRequests);
	}

	@Test
	public void testFailingTransfersPromocodeUse() throws Exception {
		paymentsShouldFail = true;
		transferRequests = 0;
		String set = config.getProperty(PromocodeConfig.AEROSPIKE_PROMOCODE_SET);
		String campaignSet = config.getProperty(PromocodeConfig.AEROSPIKE_PROMOCODE_CAMPAIGN_SET);
		updateTestData("promocodeTestData.json", set, PROMOCODE_BIN_NAME);
		updateTestData("promocodeCampaignTestData.json", campaignSet, PROMOCODE_CAMPAIGN_BIN_NAME);
		// create user profile
		String userId="1";
		createProfile(userId);
		updateCounterTestData();

		final String promocodeUseRequest = getPlainTextJsonFromResources("promocodeUseRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO)
				.replace("<<CODE>>", "MEGAPROMO");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), promocodeUseRequest);

		RetriedAssert.assertWithWait(() ->
				assertEquals(4, receivedMessageCollector.getReceivedMessageList().size()));
		errorCollector.clearCollectedErrors();
		final JsonMessage<? extends JsonMessageContent> response =
				receivedMessageCollector.getMessageByType(PromocodeMessageTypes.PROMOCODE_USE_RESPONSE);
		assertNotNull(response);
		RetriedAssert.assertWithWait(() -> assertEquals(3, transferRequests));
	}
	
	private void createProfile(String userId) {
		Profile profile = new Profile();
		profile.setUserId(userId);

		aerospikeDao.createJson(profileSet, profilePrimaryKeyUtil.createPrimaryKey(userId),
				CommonProfileAerospikeDao.BLOB_BIN_NAME, profile.getAsString());
	}

	@Override
	protected TestClientInjectionClasses getTestClientInjectionClasses() {
		return new TestClientInjectionClasses(PromocodeDefaultMessageMapper.class, PromocodeMessageSchemaMapper.class);
	}

	@Override
	protected ServiceStartup getServiceStartup() {
		return new PromocodeServiceStartupUsingAerospikeMock(DEFAULT_TEST_STAGE);
	}
	
	private void updateTestData(String fileName, String set, String binName) throws IOException {

		String jsonPromocodes = getPlainTextJsonFromResources(fileName);

		JsonParser parser = new JsonParser();
		JsonObject jsonObject = (JsonObject) parser.parse(jsonPromocodes);

		for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
			String key = entry.getKey();
			String binValue = entry.getValue().toString();
			aerospikeDao.createJson(set, key, binName, binValue);
		}

	}

	private void updateCounterTestData() {
		this.writeCounterTestData(0L, 6L);
	}

	private void updateCounterTestDataNoLongerAvailable() {
		this.writeCounterTestData(6L, 6L);
	}
	
	private void writeCounterTestData(Long bin1Value, Long bin2Value) {
		Bin counterBin1 = new Bin(PROMOCODE_CONSUMING_COUNTER_BIN_NAME, bin1Value);
		Bin counterBin2 = new Bin(PROMOCODE_CREATION_COUNTER_BIN_NAME, bin2Value);
		String set = config.getProperty(PromocodeConfig.AEROSPIKE_PROMOCODE_SET);
		String counterKeyString = promocodePrimaryKeyUtil.createSubRecordKeyByPromocodeId(DEFAULT_TEST_PROMOCODE_CODE, PROMOCODE_COUNTER_RECORD_NAME);
		Key counterKey = new Key("test", set, counterKeyString);
		aerospikeClient.put(null, counterKey, counterBin1, counterBin2);
	}
}