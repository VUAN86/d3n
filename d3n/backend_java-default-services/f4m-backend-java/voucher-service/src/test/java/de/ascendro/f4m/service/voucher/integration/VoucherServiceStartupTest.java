package de.ascendro.f4m.service.voucher.integration;

import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_USER_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.TENANT_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;

import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Injector;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.analytics.model.VoucherUsedEvent;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.game.instance.CommonGameInstanceAerospikeDao;
import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.model.AppConfigBuilder;
import de.ascendro.f4m.server.profile.util.ProfilePrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.server.voucher.util.VoucherPrimaryKeyUtil;
import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.annotation.ClientMessageHandler;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.exception.ExceptionCodes;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.exception.UnexpectedTestException;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.integration.collector.ReceivedMessageCollector;
import de.ascendro.f4m.service.integration.rule.MockServiceRule.BindableAbstractModule;
import de.ascendro.f4m.service.integration.rule.MockServiceRule.ThrowingFunction;
import de.ascendro.f4m.service.integration.test.F4MServiceWithMockIntegrationTestBase;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.exception.F4MPaymentClientException;
import de.ascendro.f4m.service.payment.exception.PaymentServiceExceptionCodes;
import de.ascendro.f4m.service.payment.model.TransactionId;
import de.ascendro.f4m.service.profile.ProfileMessageTypes;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.usermessage.model.SendEmailWrapperRequest;
import de.ascendro.f4m.service.usermessage.model.SendEmailWrapperResponse;
import de.ascendro.f4m.service.usermessage.translation.Messages;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;
import de.ascendro.f4m.service.voucher.VoucherMessageTypes;
import de.ascendro.f4m.service.voucher.config.VoucherConfig;
import de.ascendro.f4m.service.voucher.dao.VoucherAerospikeDao;
import de.ascendro.f4m.service.voucher.dao.VoucherAerospikeDaoImpl;
import de.ascendro.f4m.service.voucher.di.VoucherDefaultMessageMapper;
import de.ascendro.f4m.service.voucher.exception.VoucherServiceExceptionCodes;
import de.ascendro.f4m.service.voucher.model.UserVoucher;
import de.ascendro.f4m.service.voucher.model.UserVoucherReferenceEntry;
import de.ascendro.f4m.service.voucher.model.UserVoucherResponseModel;
import de.ascendro.f4m.service.voucher.model.Voucher;
import de.ascendro.f4m.service.voucher.model.schema.VoucherMessageSchemaMapper;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherAssignForTombolaResponse;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherAssignResponse;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherGetResponse;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherListByUserResponse;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherListByVoucherResponse;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherListResponse;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherUseResponse;
import de.ascendro.f4m.service.voucher.model.voucher.VoucherGetResponse;
import de.ascendro.f4m.service.voucher.model.voucher.VoucherListResponse;
import de.ascendro.f4m.service.voucher.util.VoucherUtil;

public class VoucherServiceStartupTest extends F4MServiceWithMockIntegrationTestBase {

	private static final String TEST_USER_ID = "test_user";

	private VoucherAerospikeDao aerospikeDao;
    private IAerospikeClient aerospikeClient;
    private String profileSet;
    private VoucherPrimaryKeyUtil voucherPrimaryKeyUtil;
    private ProfilePrimaryKeyUtil profilePrimaryKeyUtil;
    private JsonUtil jsonUtil;
	private ReceivedMessageCollector receivedMessageCollector;

	private static final String PROFILE_USER_VOUCHER_BLOB_NAME = "voucher";
	private static final String VOUCHER_BIN_NAME = "voucher";
	private static final String VOUCHER_LIST_BIN_NAME = "vouchers";
	private static final String USER_VOUCHER_BIN_NAME = "usrVoucher";
    private static final String VOUCHER_COUNTER_RECORD_NAME = "counter";
    private static final String VOUCHER_CREATION_COUNTER_BIN_NAME = "creationCnt";
    private static final String VOUCHER_LOCK_COUNTER_BIN_NAME = "lockCnt";
    private static final String VOUCHER_CONSUMING_COUNTER_BIN_NAME = "consumingCnt";

    private static final String DEFAULT_TEST_VOUCHER_ID = "11";

	/** Used to save email content to check when {@link SendEmailWrapperRequest} received */
	private List<SendEmailWrapperRequest> emailsContentToCheck;
    private int emailCount;

    private int purchaseCounter = 0;

	private Tracker tracker;

	private ArgumentCaptor<ClientInfo> clientInfoArgumentCaptor;
	private ArgumentCaptor<VoucherUsedEvent> voucherUsedEventArgumentCaptor;

	@Override
	protected void configureInjectionModuleMock(BindableAbstractModule module) {
		module.bindToModule(JsonMessageTypeMap.class).to(VoucherDefaultMessageMapper.class);
		module.bindToModule(JsonMessageSchemaMap.class).to(VoucherMessageSchemaMapper.class);
	}

	@Override
	protected ThrowingFunction<RequestContext, JsonMessageContent> getMessageHandlerMock() {
		return ctx -> onReceivedMessage(ctx.getMessage());
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		MockitoAnnotations.initMocks(this);
		clientInfoArgumentCaptor = ArgumentCaptor.forClass(ClientInfo.class);
		voucherUsedEventArgumentCaptor = ArgumentCaptor.forClass(VoucherUsedEvent.class);
		receivedMessageCollector = (ReceivedMessageCollector) clientInjector
				.getInstance(com.google.inject.Key.get(JsonMessageHandlerProvider.class, ClientMessageHandler.class))
				.get();

		assertServiceStartup(ProfileMessageTypes.SERVICE_NAME, UserMessageMessageTypes.SERVICE_NAME,
				PaymentMessageTypes.SERVICE_NAME);

		Injector injector = jettyServerRule.getServerStartup().getInjector();
		aerospikeDao = injector.getInstance(VoucherAerospikeDaoImpl.class);

        aerospikeClient = jettyServerRule.getServerStartup().getInjector()
                .getInstance(AerospikeClientProvider.class).get();
        this.profileSet = config.getProperty(AerospikeConfigImpl.AEROSPIKE_PROFILE_SET);
        this.profilePrimaryKeyUtil = new ProfilePrimaryKeyUtil(config);
        this.voucherPrimaryKeyUtil = new VoucherPrimaryKeyUtil(config);
        this.jsonUtil = new JsonUtil();
        emailsContentToCheck = new ArrayList<>();
        emailCount = 0;

        CommonGameInstanceAerospikeDao gameInstanceAerospikeDao = injector.getInstance(
        		CommonGameInstanceAerospikeDao.class);
        Game testGame = new Game();
        testGame.setTitle("GameTitle");
		when(gameInstanceAerospikeDao.getGameByInstanceId(anyString())).thenReturn(testGame);
		ApplicationConfigurationAerospikeDao appConfigDao = injector.getInstance(ApplicationConfigurationAerospikeDao.class);
		when(appConfigDao.getAppConfiguration(anyString(), anyString())).thenReturn(AppConfigBuilder.buildDefaultAppConfig());
		tracker = injector.getInstance(Tracker.class);
	}

	private JsonMessageContent onReceivedMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded)
			throws Exception {
		if (UserMessageMessageTypes.SEND_EMAIL == originalMessageDecoded.getType(UserMessageMessageTypes.class)){
			return onSendEmailReceived(originalMessageDecoded);
		} else if (EventMessageTypes.RESUBSCRIBE == originalMessageDecoded.getType(EventMessageTypes.class) ||
        		EventMessageTypes.SUBSCRIBE == originalMessageDecoded.getType(EventMessageTypes.class)) {
			return null;
		} else if (PaymentMessageTypes.LOAD_OR_WITHDRAW_WITHOUT_COVERAGE == originalMessageDecoded
						.getType(PaymentMessageTypes.class)) {
			purchaseCounter++;
			if(purchaseCounter != 2) {
				return onTransferRequestReceived(originalMessageDecoded);
			} else {
				throw new F4MPaymentClientException(PaymentServiceExceptionCodes.ERR_INSUFFICIENT_FUNDS,
						"Insufficient funds");
			}
		} else {
			throw new UnexpectedTestException("Unexpected message " + originalMessageDecoded);
		}
	}

	private JsonMessageContent onSendEmailReceived(JsonMessage<? extends JsonMessageContent> originalMessageDecoded) {
		verifyEmail(emailCount, (SendEmailWrapperRequest) originalMessageDecoded.getContent());
		emailCount++;
		return new SendEmailWrapperResponse();
	}

	private void verifyEmail(int emailIndex, SendEmailWrapperRequest request) {
		assertEquals(emailsContentToCheck.get(emailIndex).getUserId(), request.getUserId());
		assertEquals(emailsContentToCheck.get(emailIndex).getMessage(), request.getMessage());
		//voucher code is not the only parameter
		assertEquals(emailsContentToCheck.get(emailIndex).getParameters()[0], request.getParameters()[2]);
	}

	private TransactionId onTransferRequestReceived(JsonMessage<? extends JsonMessageContent> originalMessageDecoded)
			throws F4MException {
		verifyReceivedTransferRequest();
		return new TransactionId("123");
	}

	private void verifyReceivedTransferRequest() {
		// @todo check message
	}

	private void createProfile(String userId) {
		Profile profile = new Profile();
		profile.setUserId(userId);

		aerospikeDao.createJson(profileSet, profilePrimaryKeyUtil.createPrimaryKey(userId),
				CommonProfileAerospikeDao.BLOB_BIN_NAME, profile.getAsString());
	}

	private void createEmptyJsonObjectInVoucherProfileBlob(String userId) {
		final String subRecordKey = profilePrimaryKeyUtil.createSubRecordKeyByUserId(userId,
				CommonProfileAerospikeDao.USER_VOUCHER_BLOB_NAME);
		aerospikeDao.createJson(profileSet, subRecordKey, CommonProfileAerospikeDao.BLOB_BIN_NAME, "{}");
		assertEquals("{}", aerospikeDao.readJson(profileSet, subRecordKey, CommonProfileAerospikeDao.BLOB_BIN_NAME));
	}

	private void updateVoucherClassTestData() throws IOException {
		updateVoucherClassTestData1("voucherData.json");
	}

	private void updateVoucherClassTestDataIsNotExchange() throws IOException {
		updateVoucherClassTestData1("voucherDataNotExchange.json");
	}

	private void updateVoucherClassTestData1(String voucherDataFileName) throws IOException {
		String set = config.getProperty(AerospikeConfigImpl.AEROSPIKE_VOUCHER_SET);
		updateTestData(voucherDataFileName, set, VOUCHER_BIN_NAME);
		set = config.getProperty(VoucherConfig.AEROSPIKE_USER_VOUCHER_SET);
		updateTestData("userVoucherData.json", set, USER_VOUCHER_BIN_NAME);
		updateCounterTestData();
	}

	private void updateVoucherListTestData() throws IOException {
        String set = config.getProperty(VoucherConfig.AEROSPIKE_VOUCHER_LIST_SET);
        updateVoucherMapTestData("voucherListData.json", set, VOUCHER_LIST_BIN_NAME);
    }

	private void updateVoucherListExpiredAndInvalidTestData() throws IOException {
		String set = config.getProperty(VoucherConfig.AEROSPIKE_VOUCHER_LIST_SET);
		updateVoucherMapTestData("voucherListDataWithExpiredAndInvalid.json", set, VOUCHER_LIST_BIN_NAME);
	}

	private void updateTestData(String fileName, String set, String binName) throws IOException {
		String jsonVouchers = getPlainTextJsonFromResources(fileName);

		JsonParser parser = new JsonParser();
		JsonObject jsonObject = (JsonObject) parser.parse(jsonVouchers);

		for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
			String key = entry.getKey();
			String binValue = entry.getValue().toString();
			aerospikeDao.createJson(set, key, binName, binValue);
		}
	}

	private void updateCounterTestData() {
        String counterKeyString = voucherPrimaryKeyUtil.createSubRecordKeyByVoucherId(DEFAULT_TEST_VOUCHER_ID,
				VOUCHER_COUNTER_RECORD_NAME);
        Key counterKey = new Key("test", config.getProperty(VoucherConfig.AEROSPIKE_USER_VOUCHER_SET),
				counterKeyString);
        Bin counterBin1 = new Bin(VOUCHER_CONSUMING_COUNTER_BIN_NAME, 1L);
        Bin counterBin2 = new Bin(VOUCHER_LOCK_COUNTER_BIN_NAME, 2L);
        Bin counterBin3 = new Bin(VOUCHER_CREATION_COUNTER_BIN_NAME, 6L);
		aerospikeClient.put(null, counterKey, counterBin1, counterBin2, counterBin3);
	}

	private void updateCounterTestDataAllUserVouchersConsumed() {
		String set = config.getProperty(VoucherConfig.AEROSPIKE_USER_VOUCHER_SET);
		String counterKeyString = voucherPrimaryKeyUtil.createSubRecordKeyByVoucherId(DEFAULT_TEST_VOUCHER_ID,
				VOUCHER_COUNTER_RECORD_NAME);
		Key counterKey = new Key("test", set, counterKeyString);
		Bin counterBin1 = new Bin(VOUCHER_CONSUMING_COUNTER_BIN_NAME, 6L);
		Bin counterBin2 = new Bin(VOUCHER_LOCK_COUNTER_BIN_NAME, 0L);
        Bin counterBin3 = new Bin(VOUCHER_CREATION_COUNTER_BIN_NAME, 6L);
		aerospikeClient.put(null, counterKey, counterBin1, counterBin2, counterBin3);
	}

	private void updateTestDataForUserVoucherList() throws IOException {
		String voucherId = "11";
		String counterKeyString = voucherPrimaryKeyUtil.createSubRecordKeyByVoucherId(voucherId, VOUCHER_COUNTER_RECORD_NAME);
		Key counterKey = new Key("test", config.getProperty(VoucherConfig.AEROSPIKE_USER_VOUCHER_SET), counterKeyString);
		Bin counterBin1 = new Bin(VOUCHER_CONSUMING_COUNTER_BIN_NAME, 1L);
		Bin counterBin2 = new Bin(VOUCHER_LOCK_COUNTER_BIN_NAME, 3L);
		Bin counterBin3 = new Bin(VOUCHER_CREATION_COUNTER_BIN_NAME, 6L);
		aerospikeClient.put(null, counterKey, counterBin1, counterBin2, counterBin3);
		aerospikeDao.assignVoucherToUser(voucherId, ANONYMOUS_USER_ID, "notImportant", "notImportant");
		aerospikeDao.assignVoucherToUser(voucherId, ANONYMOUS_USER_ID, "notImportant", "notImportant");
		aerospikeDao.assignVoucherToUser(voucherId, ANONYMOUS_USER_ID, "notImportant", "notImportant");

		String vouchersBlobKey = profilePrimaryKeyUtil.createSubRecordKeyByUserId(ANONYMOUS_USER_ID,
				PROFILE_USER_VOUCHER_BLOB_NAME);
		final String vouchersBlob = getPlainTextJsonFromResources("VouchersBlob.json");
		aerospikeDao.createJson(profileSet, vouchersBlobKey, CommonProfileAerospikeDao.BLOB_BIN_NAME, vouchersBlob);

	}

	private void updateTestDataForUserVoucherListByUser() throws IOException {
		String voucherId = "11";
		String counterKeyString = voucherPrimaryKeyUtil.createSubRecordKeyByVoucherId(voucherId, VOUCHER_COUNTER_RECORD_NAME);
		Key counterKey = new Key("test", config.getProperty(VoucherConfig.AEROSPIKE_USER_VOUCHER_SET), counterKeyString);
		Bin counterBin1 = new Bin(VOUCHER_CONSUMING_COUNTER_BIN_NAME, 0L);
		Bin counterBin2 = new Bin(VOUCHER_LOCK_COUNTER_BIN_NAME, 3L);
		Bin counterBin3 = new Bin(VOUCHER_CREATION_COUNTER_BIN_NAME, 6L);
		aerospikeClient.put(null, counterKey, counterBin1, counterBin2, counterBin3);

		aerospikeDao.assignVoucherToUser(voucherId, ANONYMOUS_USER_ID, "notImportant", "notImportant");
		aerospikeDao.assignVoucherToUser(voucherId, ANONYMOUS_USER_ID, "notImportant", "notImportant");
		aerospikeDao.assignVoucherToUser(voucherId, ANONYMOUS_USER_ID, "notImportant", "notImportant");

		String vouchersBlobKey = profilePrimaryKeyUtil.createSubRecordKeyByUserId(ANONYMOUS_USER_ID,
				PROFILE_USER_VOUCHER_BLOB_NAME);
		final String vouchersBlob = getPlainTextJsonFromResources("VouchersBlob.json");
		aerospikeDao.createJson(profileSet, vouchersBlobKey, CommonProfileAerospikeDao.BLOB_BIN_NAME, vouchersBlob);

	}

	@Override
	protected TestClientInjectionClasses getTestClientInjectionClasses() {
		return new TestClientInjectionClasses(VoucherDefaultMessageMapper.class, VoucherMessageSchemaMapper.class);
	}

	@Override
	protected ServiceStartup getServiceStartup() {
		return new VoucherServiceStartupUsingAerospikeMock(DEFAULT_TEST_STAGE);
	}

	@Test
	public void testVoucherGet() throws Exception {
		updateVoucherClassTestData();
		String voucherId = "11";
		final String voucherGetRequest = getPlainTextJsonFromResources("VoucherGetRequest.json");
		String requestJson = voucherGetRequest.replace("<<voucherId>>", voucherId);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> response = receivedMessageCollector.getReceivedMessageList()
				.get(0);
		receivedMessageCollector.clearReceivedMessageList();
		assertMessageContentType(response, (Class<? extends JsonMessageContent>) VoucherGetResponse.class);

		VoucherGetResponse content=(VoucherGetResponse) response.getContent();
		assertEquals("11", content.getVoucher().getVoucherId());
		assertEquals("50% off voucher", content.getVoucher().getTitle());
		assertEquals("short Title lol", content.getVoucher().getShortTitle());
		assertEquals("company lol", content.getVoucher().getCompany());
		assertEquals("www.voucher.me", content.getVoucher().getRedemptionURL());
		assertTrue(content.getVoucher().isExchange());
	}

    private SendEmailWrapperRequest getSendEmailRequestWrapper(String messageContent, String messageSubject,
                                                               String toUserID, String voucherCode) {
        SendEmailWrapperRequest sendEmailWrapperRequest = new SendEmailWrapperRequest();
        sendEmailWrapperRequest.setUserId(toUserID);
        sendEmailWrapperRequest.setMessage(messageContent);
        sendEmailWrapperRequest.setSubject(messageSubject);
        String[] parameters = new String[1];
        parameters[0] = voucherCode;
		sendEmailWrapperRequest.setParameters(parameters);
        sendEmailWrapperRequest.setLanguageAuto();

        return sendEmailWrapperRequest;
    }

	@Test
    public void testUserVoucherAssign() throws Exception {
		String userId="1";
		String gameInstanceId="111";
		String expectedUserVoucherId = "11:1";
		String expectedVoucherCode = "ABC124";
		int expectedEmailCount = 1;

        emailsContentToCheck.add(getSendEmailRequestWrapper(Messages.VOUCHER_WON_ASSIGNED_TO_USER_CONTENT_WITH_REDEMPTION_URL,
				Messages.VOUCHER_WON_ASSIGNED_TO_USER_SUBJECT, userId, expectedVoucherCode));

		String userVoucherAssignJsonMessage = getUserVoucherAssignJson(userId, gameInstanceId);

        // create user profile
        createProfile(userId);
        // create voucher test data
        updateVoucherClassTestData();

        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), userVoucherAssignJsonMessage);

        RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
        RetriedAssert.assertWithWait(() -> assertEquals(expectedEmailCount, emailCount));
        final JsonMessage<? extends JsonMessageContent> userVoucherAssignResponseMessage =
				receivedMessageCollector.getReceivedMessageList().get(0);
        assertMessageContentType(userVoucherAssignResponseMessage, UserVoucherAssignResponse.class);
        UserVoucherAssignResponse userVoucherAssignResponse =
				(UserVoucherAssignResponse) userVoucherAssignResponseMessage.getContent();
        assertEquals(expectedUserVoucherId, userVoucherAssignResponse.getUserVoucherId());

		checkCounterValues(DEFAULT_TEST_VOUCHER_ID, 6 /* expectedCreationCounterValue */,
				2 /* expectedConsumingCounterValue */, 1 /* expectedLockCounterValue */);

		checkUserVoucherContents(userVoucherAssignResponse.getUserVoucherId(), userId, gameInstanceId, expectedVoucherCode,
				null, false);
    }

	@Test
	public void testUserVoucherAssignJsonObjectInsteadOfArrayInBlob() throws Exception {
		String userId="1";
		String gameInstanceId="111";
		String expectedUserVoucherId = "11:1";
		String expectedVoucherCode = "ABC124";
		int expectedEmailCount = 1;

		emailsContentToCheck.add(getSendEmailRequestWrapper(Messages.VOUCHER_WON_ASSIGNED_TO_USER_CONTENT_WITH_REDEMPTION_URL,
				Messages.VOUCHER_WON_ASSIGNED_TO_USER_SUBJECT, userId, expectedVoucherCode));

		String userVoucherAssignJsonMessage = getUserVoucherAssignJson(userId, gameInstanceId);

		// create user profile
		createProfile(userId);
		createEmptyJsonObjectInVoucherProfileBlob(userId);
		// create voucher test data
		updateVoucherClassTestData();

		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), userVoucherAssignJsonMessage);

		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
		RetriedAssert.assertWithWait(() -> assertEquals(expectedEmailCount, emailCount));
		final JsonMessage<? extends JsonMessageContent> userVoucherAssignResponseMessage =
				receivedMessageCollector.getReceivedMessageList().get(0);
		assertMessageContentType(userVoucherAssignResponseMessage, UserVoucherAssignResponse.class);
		UserVoucherAssignResponse userVoucherAssignResponse =
				(UserVoucherAssignResponse) userVoucherAssignResponseMessage.getContent();
		assertEquals(expectedUserVoucherId, userVoucherAssignResponse.getUserVoucherId());

		checkCounterValues(DEFAULT_TEST_VOUCHER_ID, 6 /* expectedCreationCounterValue */,
				2 /* expectedConsumingCounterValue */, 1 /* expectedLockCounterValue */);

		checkUserVoucherContents(userVoucherAssignResponse.getUserVoucherId(), userId, gameInstanceId, expectedVoucherCode,
				null, false);
	}

	@Test
	public void testUserVoucherAssignForTombola() throws Exception {
		String userId="1";
		String tombolaId="111";
		String tombolaName="Tombola Name";
		String expectedUserVoucherId = "11:1";
		String expectedVoucherCode = "ABC124";
		int expectedEmailCount = 1;

		emailsContentToCheck.add(getSendEmailRequestWrapper(
				Messages.VOUCHER_WON_IN_TOMBOLA_ASSIGNED_TO_USER_CONTENT_WITH_REDEMPTION_URL,
				Messages.VOUCHER_WON_ASSIGNED_TO_USER_SUBJECT, userId, expectedVoucherCode));

		String userVoucherAssignForTombolaJson = getUserVoucherAssignForTombolaJson(userId, tombolaId, tombolaName);

		// create user profile
		createProfile(userId);
		// create voucher test data
		updateVoucherClassTestData();

		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), userVoucherAssignForTombolaJson);

		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
		RetriedAssert.assertWithWait(() -> assertEquals(expectedEmailCount, emailCount));
		final JsonMessage<? extends JsonMessageContent> message =
				receivedMessageCollector.getReceivedMessageList().get(0);
		assertMessageContentType(message, UserVoucherAssignForTombolaResponse.class);
		UserVoucherAssignForTombolaResponse messageContent =
				(UserVoucherAssignForTombolaResponse) message.getContent();
		assertEquals(expectedUserVoucherId, messageContent.getUserVoucherId());

		checkCounterValues(DEFAULT_TEST_VOUCHER_ID, 6 /* expectedCreationCounterValue */,
				2 /* expectedConsumingCounterValue */,
				1 /* expectedLockCounterValue */);

		checkUserVoucherContents(messageContent.getUserVoucherId(), userId, null, expectedVoucherCode, tombolaId, false);
	}

	@Test
	public void testUserVoucherAssignEmptyAppConfiguration() throws Exception {
		Injector injector = jettyServerRule.getServerStartup().getInjector();
		ApplicationConfigurationAerospikeDao appConfigDao = injector.getInstance(ApplicationConfigurationAerospikeDao.class);
		when(appConfigDao.getAppConfiguration(anyString(), anyString())).thenReturn(null);
		String userId="1";
		String gameInstanceId="111";
		String expectedUserVoucherId = "11:1";
		String expectedVoucherCode = "ABC124";
		int expectedEmailCount = 1;

		emailsContentToCheck.add(getSendEmailRequestWrapper(Messages.VOUCHER_WON_ASSIGNED_TO_USER_CONTENT_WITH_REDEMPTION_URL,
				Messages.VOUCHER_WON_ASSIGNED_TO_USER_SUBJECT, userId, expectedVoucherCode));

		String userVoucherAssignJsonMessage = getUserVoucherAssignJson(userId, gameInstanceId);

		createProfile(userId);
		updateVoucherClassTestData();

		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), userVoucherAssignJsonMessage);

		assertReceivedMessagesWithWait(1, VoucherMessageTypes.USER_VOUCHER_ASSIGN_RESPONSE);
		RetriedAssert.assertWithWait(() -> assertEquals(expectedEmailCount, emailCount));
		final JsonMessage<UserVoucherAssignResponse> userVoucherAssignResponseMessage =
				receivedMessageCollector.getMessageByType(VoucherMessageTypes.USER_VOUCHER_ASSIGN_RESPONSE);
		UserVoucherAssignResponse userVoucherAssignResponse = userVoucherAssignResponseMessage.getContent();
		assertEquals(expectedUserVoucherId, userVoucherAssignResponse.getUserVoucherId());

		checkCounterValues(DEFAULT_TEST_VOUCHER_ID, 6 /* expectedCreationCounterValue */,
				2 /* expectedConsumingCounterValue */, 1 /* expectedLockCounterValue */);

		checkUserVoucherContents(userVoucherAssignResponse.getUserVoucherId(), userId, gameInstanceId, expectedVoucherCode,
				null, false);
	}

	private String getUserVoucherAssignJson(String userId, String gameInstanceId) throws IOException {
		final String userVoucherAssignJson = getPlainTextJsonFromResources("UserVoucherAssignRequest.json");
		return userVoucherAssignJson.replace("<<userId>>", userId)
				.replace("<<voucherId>>", DEFAULT_TEST_VOUCHER_ID)
				.replace("<<gameInstanceId>>", gameInstanceId);
	}

	private String getUserVoucherAssignForTombolaJson(String userId, String tombolaId, String tombolaName)
			throws IOException {
		final String userVoucherAssignJson = getPlainTextJsonFromResources("UserVoucherAssignForTombolaRequest.json");
		return userVoucherAssignJson.replace("<<userId>>", userId)
				.replace("<<voucherId>>", DEFAULT_TEST_VOUCHER_ID)
				.replace("<<tombolaId>>", tombolaId)
				.replace("<<tombolaName>>", tombolaName);
	}

	@Test
    public void testUserVoucherAssignWhenNoVoucherAvailable() throws Exception {
        final String userVoucherAssignJson = getPlainTextJsonFromResources("UserVoucherAssignRequest.json");
        String userVoucherAssignJsonMessage = userVoucherAssignJson.replace("<<userId>>", "user_1")
                .replace("<<voucherId>>", "voucher_1")
                .replace("<<gameInstanceId>>", "game_1");

        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), userVoucherAssignJsonMessage);

        RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
        final JsonMessage<? extends JsonMessageContent> userVoucherAssignResponseMessage =
				receivedMessageCollector.getReceivedMessageList().get(0);
        assertEquals(VoucherServiceExceptionCodes.ERR_ENTRY_NOT_FOUND,
				userVoucherAssignResponseMessage.getError().getCode());
    }

	@Test
	public void testUserVoucherAssignWhenAllUserVouchersAlreadyAssigned() throws Exception {
		String userId="1";
		String gameInstanceId="111";

		String userVoucherAssignJsonMessage = getUserVoucherAssignJson(userId, gameInstanceId);

		// create user profile
		createProfile(userId);
		// create voucher test data
		updateVoucherClassTestData();
		updateCounterTestDataAllUserVouchersConsumed();

		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), userVoucherAssignJsonMessage);

		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> userVoucherAssignResponseMessage =
				receivedMessageCollector.getReceivedMessageList().get(0);
		assertEquals(VoucherServiceExceptionCodes.ERR_NO_VOUCHER_AVAILABLE,
				userVoucherAssignResponseMessage.getError().getCode());
	}

    @Test
    public void testUserVoucherAssignRepeat() throws Exception {
        final String userId = "1";
		final String firstExpectedUserVoucherCode = "ABC124";
		final String secondExpectedUserVoucherCode = "ABC125";
        final String firstGameInstanceId = "111";
		final String secondGameInstanceId = "222";
		int expectedEmailCount = 2;

        emailsContentToCheck.add(getSendEmailRequestWrapper(Messages.VOUCHER_WON_ASSIGNED_TO_USER_CONTENT_WITH_REDEMPTION_URL,
				Messages.VOUCHER_WON_ASSIGNED_TO_USER_SUBJECT, userId, firstExpectedUserVoucherCode));
        emailsContentToCheck.add(getSendEmailRequestWrapper(Messages.VOUCHER_WON_ASSIGNED_TO_USER_CONTENT_WITH_REDEMPTION_URL,
				Messages.VOUCHER_WON_ASSIGNED_TO_USER_SUBJECT, userId, secondExpectedUserVoucherCode));

		String firstUserVoucherAssignJsonMessage = getUserVoucherAssignJson(userId, firstGameInstanceId);

        String secondUserVoucherAssignJsonMessage = getUserVoucherAssignJson(userId, secondGameInstanceId);

        // create user profile
        createProfile(userId);
        // create vouchers test data
        updateVoucherClassTestData();


        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(),
				firstUserVoucherAssignJsonMessage);
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(),
				secondUserVoucherAssignJsonMessage);

        RetriedAssert.assertWithWait(() -> assertEquals(2, receivedMessageCollector.getReceivedMessageList().size()));
        RetriedAssert.assertWithWait(() -> assertEquals(expectedEmailCount, emailCount));

        final JsonMessage<? extends JsonMessageContent> userVoucherAssignFirstResponseMessage =
				receivedMessageCollector.getReceivedMessageList().get(0);

        assertMessageContentType(userVoucherAssignFirstResponseMessage, UserVoucherAssignResponse.class);

        UserVoucherAssignResponse userVoucherFirstAssignResponse =
				(UserVoucherAssignResponse) userVoucherAssignFirstResponseMessage.getContent();

        final JsonMessage<? extends JsonMessageContent> userVoucherAssignSecondResponseMessage =
				receivedMessageCollector.getReceivedMessageList().get(1);

        assertMessageContentType(userVoucherAssignSecondResponseMessage, UserVoucherAssignResponse.class);

        UserVoucherAssignResponse userVoucherSecondAssignResponse =
				(UserVoucherAssignResponse) userVoucherAssignSecondResponseMessage.getContent();

        final String firstExpectedUserVoucherId = "11:1";
		final String secondExpectedUserVoucherId = "11:2";

        List<String> expectedUserVoucherIds = Arrays.asList(firstExpectedUserVoucherId, secondExpectedUserVoucherId);
        assertTrue(expectedUserVoucherIds.contains(userVoucherFirstAssignResponse.getUserVoucherId()));
        assertTrue(expectedUserVoucherIds.contains(userVoucherSecondAssignResponse.getUserVoucherId()));

        final long expectedCreationCounterValue = 6;
		final long expectedConsumingCounterValue = 3;
		final long expectedLockCounterValue = 0;

		checkCounterValues(DEFAULT_TEST_VOUCHER_ID, expectedCreationCounterValue, expectedConsumingCounterValue,
				expectedLockCounterValue);

		String expectedCode;
		if (firstExpectedUserVoucherId.equals(userVoucherFirstAssignResponse.getUserVoucherId())) {
			expectedCode = firstExpectedUserVoucherCode;
		} else {
			expectedCode = secondExpectedUserVoucherCode;
		}

		checkUserVoucherContents(userVoucherFirstAssignResponse.getUserVoucherId(), userId, firstGameInstanceId, expectedCode,
				null, false);

		if (firstExpectedUserVoucherId.equals(userVoucherSecondAssignResponse.getUserVoucherId())) {
			expectedCode = firstExpectedUserVoucherCode;
		} else {
			expectedCode = secondExpectedUserVoucherCode;
		}

		checkUserVoucherContents(userVoucherSecondAssignResponse.getUserVoucherId(), userId, secondGameInstanceId,
				expectedCode, null, false);
    }

	@Test
	public void testUserVoucherAssignForTombolaRepeat() throws Exception {
		final String userId = "1";
		final String tombolaId="111";
		String tombolaName="Tombola Name";
		final String firstExpectedUserVoucherCode = "ABC124";
		final String secondExpectedUserVoucherCode = "ABC125";
		int expectedEmailCount = 2;

		emailsContentToCheck.add(getSendEmailRequestWrapper(
				Messages.VOUCHER_WON_IN_TOMBOLA_ASSIGNED_TO_USER_CONTENT_WITH_REDEMPTION_URL,
				Messages.VOUCHER_WON_ASSIGNED_TO_USER_SUBJECT, userId, firstExpectedUserVoucherCode));
		emailsContentToCheck.add(getSendEmailRequestWrapper(
				Messages.VOUCHER_WON_IN_TOMBOLA_ASSIGNED_TO_USER_CONTENT_WITH_REDEMPTION_URL,
				Messages.VOUCHER_WON_ASSIGNED_TO_USER_SUBJECT, userId, secondExpectedUserVoucherCode));

		String firstUserVoucherAssignForTombolaJson = getUserVoucherAssignForTombolaJson(userId, tombolaId, tombolaName);

		String secondUserVoucherAssignForTombolaJson = getUserVoucherAssignForTombolaJson(userId, tombolaId, tombolaName);

		// create user profile
		createProfile(userId);
		// create vouchers test data
		updateVoucherClassTestData();


		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), firstUserVoucherAssignForTombolaJson);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(),
				secondUserVoucherAssignForTombolaJson);

		RetriedAssert.assertWithWait(() -> assertEquals(2, receivedMessageCollector.getReceivedMessageList().size()));
		RetriedAssert.assertWithWait(() -> assertEquals(expectedEmailCount, emailCount));

		final JsonMessage<? extends JsonMessageContent> userVoucherAssignForTombolaFirstResponseMessage =
				receivedMessageCollector.getReceivedMessageList().get(0);

		assertMessageContentType(userVoucherAssignForTombolaFirstResponseMessage, UserVoucherAssignForTombolaResponse.class);

		UserVoucherAssignForTombolaResponse userVoucherFirstAssignResponse =
				(UserVoucherAssignForTombolaResponse) userVoucherAssignForTombolaFirstResponseMessage.getContent();

		final JsonMessage<? extends JsonMessageContent> userVoucherAssignForTombolaSecondResponseMessage =
				receivedMessageCollector.getReceivedMessageList().get(1);

		assertMessageContentType(userVoucherAssignForTombolaSecondResponseMessage, UserVoucherAssignForTombolaResponse.class);

		UserVoucherAssignForTombolaResponse userVoucherSecondAssignResponse =
				(UserVoucherAssignForTombolaResponse) userVoucherAssignForTombolaSecondResponseMessage.getContent();

		final String firstExpectedUserVoucherId = "11:1";
		final String secondExpectedUserVoucherId = "11:2";

		List<String> expectedUserVoucherIds = Arrays.asList(firstExpectedUserVoucherId, secondExpectedUserVoucherId);
		assertTrue(expectedUserVoucherIds.contains(userVoucherFirstAssignResponse.getUserVoucherId()));
		assertTrue(expectedUserVoucherIds.contains(userVoucherSecondAssignResponse.getUserVoucherId()));

		final long expectedCreationCounterValue = 6;
		final long expectedConsumingCounterValue = 3;
		final long expectedLockCounterValue = 0;

		checkCounterValues(DEFAULT_TEST_VOUCHER_ID, expectedCreationCounterValue, expectedConsumingCounterValue,
				expectedLockCounterValue);

		String expectedCode;
		if (firstExpectedUserVoucherId.equals(userVoucherFirstAssignResponse.getUserVoucherId())) {
			expectedCode = firstExpectedUserVoucherCode;
		} else {
			expectedCode = secondExpectedUserVoucherCode;
		}

		checkUserVoucherContents(userVoucherFirstAssignResponse.getUserVoucherId(), userId, null,
				expectedCode, tombolaId, false);

		if (firstExpectedUserVoucherId.equals(userVoucherSecondAssignResponse.getUserVoucherId())) {
			expectedCode = firstExpectedUserVoucherCode;
		} else {
			expectedCode = secondExpectedUserVoucherCode;
		}

		checkUserVoucherContents(userVoucherSecondAssignResponse.getUserVoucherId(), userId, null,
				expectedCode, tombolaId, false);
	}

	private void checkCounterValues(String voucherId, long expectedCreationCounterValue,
			long expectedConsumingCounterValue, long expectedLockCounterValue) {
        String counterKey = voucherPrimaryKeyUtil.createSubRecordKeyByVoucherId(voucherId, VOUCHER_COUNTER_RECORD_NAME);
        String set = config.getProperty(VoucherConfig.AEROSPIKE_USER_VOUCHER_SET);
        long consumingCounter = aerospikeDao.readLong(set, counterKey, VOUCHER_CONSUMING_COUNTER_BIN_NAME);
        long lockedCounter = aerospikeDao.readLong(set, counterKey, VOUCHER_LOCK_COUNTER_BIN_NAME);
        long creationCounter = aerospikeDao.readLong(set, counterKey, VOUCHER_CREATION_COUNTER_BIN_NAME);

	    assertEquals(expectedCreationCounterValue, creationCounter);
		assertEquals(expectedConsumingCounterValue, consumingCounter);
		assertEquals(expectedLockCounterValue, lockedCounter);
	}

	private void checkUserVoucherContents(String userVoucherId, String userId, String gameInstanceId, String expectedCode,
			String tombolaId, boolean isPurchased) {
		String voucherSet = config.getProperty(VoucherConfig.AEROSPIKE_USER_VOUCHER_SET);
		String voucherKey = voucherPrimaryKeyUtil.createUserVoucherKey(userVoucherId);
        String userVoucherJson = aerospikeDao.readJson(voucherSet,voucherKey, USER_VOUCHER_BIN_NAME);
        UserVoucher userVoucher = jsonUtil.fromJson(userVoucherJson, UserVoucher.class);

		assertEquals(expectedCode, userVoucher.getCode());
		assertNotNull(userVoucher.getObtainDate());
		assertEquals(gameInstanceId, userVoucher.getGameInstanceId());
		assertEquals(tombolaId, userVoucher.getTombolaId());
		assertEquals(isPurchased, userVoucher.isPurchased());
		assertEquals(userId, userVoucher.getUserId());
		
		JsonArray userVoucherArray = getProfileVoucherSubBlob(userId);
		List<String> userVoucherIds = new ArrayList<>(); 
		userVoucherArray.forEach(v -> {
			UserVoucherReferenceEntry entry = jsonUtil.fromJson(v, UserVoucherReferenceEntry.class);
			userVoucherIds.add(VoucherUtil.createUserVoucherIdForResponse(entry.getVoucherId(), entry.getUserVoucherId()));
		});
		
		assertTrue(userVoucherIds.contains(userVoucherId));
	}

	private JsonArray getProfileVoucherSubBlob(String userId) {
		return jsonUtil.fromJson(aerospikeDao.readJson(profileSet, 
				profilePrimaryKeyUtil.createSubRecordKeyByUserId(userId, CommonProfileAerospikeDao.USER_VOUCHER_BLOB_NAME), 
				CommonProfileAerospikeDao.BLOB_BIN_NAME), JsonArray.class);
	}
	
	@Test
	public void testVoucherPurchase() throws Exception {
		String userId=ANONYMOUS_USER_ID;
		String expectedUserVoucherId = "11:1";
		String expectedVoucherCode = "ABC124";
		int expectedEmailCount = 1;

        emailsContentToCheck.add(getSendEmailRequestWrapper(Messages.VOUCHER_BOUGHT_ASSIGNED_TO_USER_CONTENT_WITH_REDEMPTION_URL,
				Messages.VOUCHER_BOUGHT_ASSIGNED_TO_USER_SUBJECT, userId, expectedVoucherCode));

        final String voucherPurchaseRequest = getPlainTextJsonFromResources("VoucherPurchaseRequest.json",
				ANONYMOUS_CLIENT_INFO);
		String userVoucherPurchaseRequestJsonMessage = voucherPurchaseRequest.replace("<<voucherId>>",
				DEFAULT_TEST_VOUCHER_ID);


		// create user profile
		createProfile(userId);
		// create voucher test data
		updateVoucherClassTestData();

		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(),
				userVoucherPurchaseRequestJsonMessage);

		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList()
				.size()));
        RetriedAssert.assertWithWait(
                () -> assertEquals(expectedEmailCount, emailCount));
		final JsonMessage<? extends JsonMessageContent> userVoucherPurchaseResponse =
				receivedMessageCollector.getReceivedMessageList().get(0);
		assertMessageContentType(userVoucherPurchaseResponse, EmptyJsonMessageContent.class);
		receivedMessageCollector.clearReceivedMessageList();

		final long expectedCreationCounterValue = 6;
		final long expectedConsumingCounterValue = 2;
		final long expectedLockCounterValue = 2;

		checkCounterValues(DEFAULT_TEST_VOUCHER_ID, expectedCreationCounterValue, expectedConsumingCounterValue,
				expectedLockCounterValue);

		checkUserVoucherContents(expectedUserVoucherId, userId, null, expectedVoucherCode, null, true);

		// test for failed purchase, 2nd call is mocked to fail :
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(),
				userVoucherPurchaseRequestJsonMessage);
		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
		errorCollector.clearCollectedErrors();
		checkCounterValues(DEFAULT_TEST_VOUCHER_ID, expectedCreationCounterValue, expectedConsumingCounterValue,
				expectedLockCounterValue);
		checkUserVoucherContents(expectedUserVoucherId, userId, null, expectedVoucherCode, null, true);
	}

	@Test
	public void testVoucherPurchaseNotExchange() throws Exception {
		final String voucherPurchaseRequest = getPlainTextJsonFromResources("VoucherPurchaseRequest.json",
				ANONYMOUS_CLIENT_INFO);
		String userVoucherPurchaseRequestJsonMessage = voucherPurchaseRequest.replace("<<voucherId>>",
				DEFAULT_TEST_VOUCHER_ID);

		// create user profile
		createProfile(ANONYMOUS_USER_ID);
		// create voucher test data
		updateVoucherClassTestDataIsNotExchange();

		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(),
				userVoucherPurchaseRequestJsonMessage);

		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> response = receivedMessageCollector.getReceivedMessageList().get(0);
		assertEquals(VoucherServiceExceptionCodes.ERR_VOUCHER_IS_NOT_EXCHANGE, response.getError().getCode());
	}

	@Test
	public void testUserVoucherList() throws Exception {
		updateVoucherClassTestData();
		updateTestDataForUserVoucherList();

		final String userVoucherListRequest = getPlainTextJsonFromResources("UserVoucherListRequest.json",
				ANONYMOUS_CLIENT_INFO);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), userVoucherListRequest);
		RetriedAssert.assertWithWait(() ->
				assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> response =
				receivedMessageCollector.getMessageByType(VoucherMessageTypes.USER_VOUCHER_LIST_RESPONSE);
		assertMessageContentType(response, UserVoucherListResponse.class);

		UserVoucherListResponse content = (UserVoucherListResponse) response.getContent();
		assertEquals(3, content.getItems().size());

		UserVoucherResponseModel responseItem = jsonUtil.fromJson(content.getItems().get(0), UserVoucherResponseModel.class);
		assertEquals("11:1", responseItem.getUserVoucherId());
		assertEquals("50% off voucher", responseItem.getTitle());
		assertEquals("short Title lol", responseItem.getShortTitle());
		assertEquals("company lol", responseItem.getCompany());
		assertEquals("ABC124", responseItem.getCode());

		responseItem = jsonUtil.fromJson(content.getItems().get(1), UserVoucherResponseModel.class);
		assertEquals("11:2",responseItem.getUserVoucherId());
		assertEquals("50% off voucher", responseItem.getTitle());
		assertEquals("short Title lol", responseItem.getShortTitle());
		assertEquals("company lol", responseItem.getCompany());
		assertEquals("ABC125", responseItem.getCode());

		responseItem = jsonUtil.fromJson(content.getItems().get(2), UserVoucherResponseModel.class);
		assertEquals("11:3",responseItem.getUserVoucherId());
		assertEquals("50% off voucher", responseItem.getTitle());
		assertEquals("short Title lol", responseItem.getShortTitle());
		assertEquals("company lol", responseItem.getCompany());
		assertEquals("ABC126", responseItem.getCode());

	}

	@Test
	public void testUserVoucherGet() throws Exception {
		updateVoucherClassTestData();
		String userVoucherId="11:2";
		final String voucherPurchaseRequest = getPlainTextJsonFromResources("UserVoucherGetRequest.json");
		String requestJson = voucherPurchaseRequest.replace("<<userVoucherId>>", userVoucherId);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> response = receivedMessageCollector.getReceivedMessageList().get(0);
		receivedMessageCollector.clearReceivedMessageList();
		assertMessageContentType(response, (Class<? extends JsonMessageContent>) UserVoucherGetResponse.class);
		UserVoucherGetResponse content=(UserVoucherGetResponse) response.getContent();
		assertEquals("11:2", content.getVoucher().getUserVoucherId());
		assertEquals("ABC125", content.getVoucher().getCode());
		assertEquals("short Title lol", content.getVoucher().getShortTitle());
		assertEquals("company lol", content.getVoucher().getCompany());
		assertEquals("www.voucher.me", content.getVoucher().getRedemptionURL());
	}

	@Test
	public void testUserVoucherUse() throws Exception {
        // create voucher test data
        updateVoucherClassTestData();

		String expectedUserVoucherId = "11:1";
		Long expectedVoucherId = 11L;

		aerospikeDao.assignVoucherToUser(DEFAULT_TEST_VOUCHER_ID, ANONYMOUS_USER_ID, "notImportant", "notImportant");

		final String voucherPurchaseRequest = getPlainTextJsonFromResources("UserVoucherUseRequest.json",
				ANONYMOUS_CLIENT_INFO);
		String requestJson = voucherPurchaseRequest.replace("<<userVoucherId>>", expectedUserVoucherId);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> response = receivedMessageCollector.getReceivedMessageList().get(0);
		receivedMessageCollector.clearReceivedMessageList();
		assertMessageContentType(response, (Class<? extends JsonMessageContent>) UserVoucherUseResponse.class);
		UserVoucherUseResponse content=(UserVoucherUseResponse) response.getContent();
		assertEquals(expectedUserVoucherId, content.getUserVoucherId());

		String voucherSet = config.getProperty(VoucherConfig.AEROSPIKE_USER_VOUCHER_SET);
		String voucherKey = voucherPrimaryKeyUtil.createUserVoucherKey(content.getUserVoucherId());
		String userVoucherJson = aerospikeDao.readJson(voucherSet,voucherKey, USER_VOUCHER_BIN_NAME);
		UserVoucher userVoucher = jsonUtil.fromJson(userVoucherJson, UserVoucher.class);
		assertTrue(userVoucher.isUsed());
		assertNotNull(userVoucher.getUsedDate());
		verify(tracker, times(1)).addEvent(clientInfoArgumentCaptor.capture(), voucherUsedEventArgumentCaptor.capture());
		assertEquals(ANONYMOUS_USER_ID,clientInfoArgumentCaptor.getValue().getUserId());
		assertEquals(expectedUserVoucherId, voucherUsedEventArgumentCaptor.getValue().getVoucherInstanceId());
		assertEquals(expectedVoucherId, voucherUsedEventArgumentCaptor.getValue().getVoucherId()); 
	}

	@Test
	public void testUserVoucherUseInexistentVoucher() throws Exception {
		String userVoucherId = "11:1";
		final String voucherPurchaseRequest = getPlainTextJsonFromResources("UserVoucherUseRequest.json",
				ANONYMOUS_CLIENT_INFO);
		String requestJson = voucherPurchaseRequest.replace("<<userVoucherId>>", userVoucherId);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> response = receivedMessageCollector.getReceivedMessageList().get(0);
		assertEquals(VoucherServiceExceptionCodes.ERR_ENTRY_NOT_FOUND, response.getError().getCode());
	}

	@Test
	public void testUserVoucherUseAlreadyUsed() throws Exception {
		// create voucher test data
		updateVoucherClassTestData();

		String expectedUserVoucherId = "11:1";

		aerospikeDao.assignVoucherToUser(DEFAULT_TEST_VOUCHER_ID, ANONYMOUS_USER_ID, "notImportant", "notImportant");
		aerospikeDao.useVoucherForUser(expectedUserVoucherId, ANONYMOUS_USER_ID);

		final String voucherPurchaseRequest = getPlainTextJsonFromResources("UserVoucherUseRequest.json",
				ANONYMOUS_CLIENT_INFO);
		String requestJson = voucherPurchaseRequest.replace("<<userVoucherId>>", expectedUserVoucherId);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> response = receivedMessageCollector.getReceivedMessageList().get(0);
		assertEquals(VoucherServiceExceptionCodes.ERR_VOUCHER_ALREADY_USED, response.getError().getCode());
	}

	@Test
	public void testUserVoucherUseNotYourVoucher() throws Exception {
		// create voucher test data
		updateVoucherClassTestData();

		String expectedUserVoucherId = "11:1";

		final String voucherPurchaseRequest = getPlainTextJsonFromResources("UserVoucherUseRequest.json");
		String requestJson = voucherPurchaseRequest.replace("<<userVoucherId>>", expectedUserVoucherId);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		
		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> response = receivedMessageCollector.getReceivedMessageList().get(0);
		assertEquals(ExceptionCodes.ERR_INSUFFICIENT_RIGHTS, response.getError().getCode());
	}

	@Test
	public void testUserVoucherReserve() throws Exception {
        // create voucher test data
        updateVoucherClassTestData();

		final String userVoucherReserveRequest = getPlainTextJsonFromResources("UserVoucherReserveRequest.json");
		String requestJson = userVoucherReserveRequest.replace("<<voucherId>>", DEFAULT_TEST_VOUCHER_ID);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);

		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
        final JsonMessage<? extends JsonMessageContent> userVoucherReserveResponseMessage =
				receivedMessageCollector.getReceivedMessageList().get(0);
        assertMessageContentType(userVoucherReserveResponseMessage,
				(Class<? extends JsonMessageContent>) EmptyJsonMessageContent.class);
        // check if the lock counter was incremented
        checkCounterValues(DEFAULT_TEST_VOUCHER_ID, 6, 1, 3);
	}

    @Test
    public void testUserVoucherReserveWhenAllConsumed() throws Exception {
        updateVoucherClassTestData();
        updateCounterTestDataAllUserVouchersConsumed();

        final String userVoucherReserveRequest = getPlainTextJsonFromResources("UserVoucherReserveRequest.json");
        String requestJson = userVoucherReserveRequest.replace("<<voucherId>>", DEFAULT_TEST_VOUCHER_ID);
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);

        RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));

        final JsonMessage<? extends JsonMessageContent> userVoucherReserveResponseMessage =
				receivedMessageCollector.getReceivedMessageList().get(0);
        assertEquals(VoucherServiceExceptionCodes.ERR_NO_VOUCHER_AVAILABLE,
				userVoucherReserveResponseMessage.getError().getCode());
    }

    @Test
    public void testUserVoucherReserveWhenVoucherDoesNotExist() throws Exception {
        final String userVoucherReserveRequest = getPlainTextJsonFromResources("UserVoucherReserveRequest.json");
        String requestJson = userVoucherReserveRequest.replace("<<voucherId>>", DEFAULT_TEST_VOUCHER_ID);
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);

        RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList()
                .size()));

        final JsonMessage<? extends JsonMessageContent> userVoucherReserveResponseMessage =
				receivedMessageCollector.getReceivedMessageList()
                .get(0);
        assertEquals(VoucherServiceExceptionCodes.ERR_NO_VOUCHER_AVAILABLE,
				userVoucherReserveResponseMessage.getError().getCode());
    }

    @Test
    public void testUserVoucherRelease() throws Exception {
        // create voucher test data
        updateVoucherClassTestData();

        final String userVoucherReleaseRequest = getPlainTextJsonFromResources("UserVoucherReleaseRequest.json");
        String requestJson = userVoucherReleaseRequest.replace("<<voucherId>>", DEFAULT_TEST_VOUCHER_ID);
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);

        RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList()
                .size()));
        final JsonMessage<? extends JsonMessageContent> userVoucherReleaseResponseMessage =
				receivedMessageCollector.getReceivedMessageList()
                .get(0);
        assertMessageContentType(userVoucherReleaseResponseMessage,
				(Class<? extends JsonMessageContent>) EmptyJsonMessageContent.class);
        // check if the lock counter was incremented
        checkCounterValues(DEFAULT_TEST_VOUCHER_ID, 6, 1, 1);
    }

    @Test
    public void testUserVoucherReleaseWhenNoLockAvailable() throws Exception {
        updateVoucherClassTestData();
        updateCounterTestDataAllUserVouchersConsumed();

        final String userVoucherReleaseRequest = getPlainTextJsonFromResources("UserVoucherReleaseRequest.json");
        String requestJson = userVoucherReleaseRequest.replace("<<voucherId>>", DEFAULT_TEST_VOUCHER_ID);
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);

        RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList()
                .size()));

        final JsonMessage<? extends JsonMessageContent> userVoucherReserveResponseMessage =
				receivedMessageCollector.getReceivedMessageList()
                .get(0);
        assertEquals(VoucherServiceExceptionCodes.ERR_NO_VOUCHER_AVAILABLE,
				userVoucherReserveResponseMessage.getError().getCode());
    }

    @Test
    public void testUserVoucherReleaseWhenVoucherDoesNotExist() throws Exception {
        final String userVoucherReleaseRequest = getPlainTextJsonFromResources("UserVoucherReleaseRequest.json");
        String requestJson = userVoucherReleaseRequest.replace("<<voucherId>>", DEFAULT_TEST_VOUCHER_ID);
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);

        RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList()
                .size()));

        final JsonMessage<? extends JsonMessageContent> userVoucherReserveResponseMessage =
				receivedMessageCollector.getReceivedMessageList()
                .get(0);
        assertEquals(VoucherServiceExceptionCodes.ERR_ENTRY_NOT_FOUND,
				userVoucherReserveResponseMessage.getError().getCode());
    }

	@Test
	public void testGetVoucherListByVoucherIdAndType() throws F4MException, IOException, URISyntaxException {

		updateVoucherClassTestData();
		String requestJson = getPlainTextJsonFromResources("getVoucherListByVoucherIdAndType.json",
				KeyStoreTestUtil.ADMIN_CLIENT_INFO);

		receivedMessageCollector.getReceivedMessageList()
				.clear();

		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson.replaceFirst(
				"<<voucherId>>", DEFAULT_TEST_VOUCHER_ID).replaceFirst("<<type>>", "available"));

		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList()
				.size()));

		JsonMessage<? extends JsonMessageContent> response = receivedMessageCollector.getReceivedMessageList()
				.get(0);
		receivedMessageCollector.clearReceivedMessageList();
		assertMessageContentType(response, (Class<? extends JsonMessageContent>) UserVoucherListByVoucherResponse.class);

		assertEquals(3, ((UserVoucherListByVoucherResponse)response.getContent()).getItems().size());
		// ensure 1st one is the 4th position :
		assertEquals("11:3", ((UserVoucherListByVoucherResponse)response.getContent()).getItems().get(0).get("userVoucherId")
				.getAsString());
		assertEquals("ABC126", ((UserVoucherListByVoucherResponse)response.getContent()).getItems().get(0).get("code")
				.getAsString());

        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson.replaceFirst(
        		"<<voucherId>>", DEFAULT_TEST_VOUCHER_ID).replaceFirst("<<type>>", "locked"));

        RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList()
                .size()));

        response = receivedMessageCollector.getReceivedMessageList()
                .get(0);

        assertMessageContentType(response,
				(Class<? extends JsonMessageContent>) UserVoucherListByVoucherResponse.class);

        assertEquals(2, ((UserVoucherListByVoucherResponse)response.getContent()).getItems().size());
        // ensure 1st one is the 4th position :
		assertEquals("11:1", ((UserVoucherListByVoucherResponse)response.getContent()).getItems().get(0).get("userVoucherId")
				.getAsString());
		assertEquals("ABC124", ((UserVoucherListByVoucherResponse)response.getContent()).getItems().get(0).get("code")
				.getAsString());

	}

	@Test
	public void testGetVoucherListByVoucherId() throws F4MException, IOException, URISyntaxException {

		updateVoucherClassTestData();
		String requestJson = getPlainTextJsonFromResources("getVoucherListByVoucherId.json",
				KeyStoreTestUtil.ADMIN_CLIENT_INFO);

		receivedMessageCollector.getReceivedMessageList()
				.clear();

		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson.replaceFirst(
				"<<voucherId>>", DEFAULT_TEST_VOUCHER_ID));

		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList()
				.size()));

		JsonMessage<? extends JsonMessageContent> response = receivedMessageCollector.getReceivedMessageList()
				.get(0);
		receivedMessageCollector.clearReceivedMessageList();
		assertMessageContentType(response, (Class<? extends JsonMessageContent>) UserVoucherListByVoucherResponse.class);

		assertEquals(6, ((UserVoucherListByVoucherResponse)response.getContent()).getItems().size());
		
		// this is one is used
		assertEquals("11:0", ((UserVoucherListByVoucherResponse)response.getContent()).getItems().get(0).get("userVoucherId")
				.getAsString());
		assertEquals("ABC123", ((UserVoucherListByVoucherResponse)response.getContent()).getItems().get(0).get("code")
				.getAsString());
		assertEquals(true, ((UserVoucherListByVoucherResponse)response.getContent()).getItems().get(0).get("isUsed")
				.getAsBoolean());
		assertEquals("2001-01-01T10:00:00Z", ((UserVoucherListByVoucherResponse)response.getContent()).getItems().get(0)
				.get("usedDate").getAsString());
		assertEquals(true, ((UserVoucherListByVoucherResponse)response.getContent()).getItems().get(0).get("isWon")
				.getAsBoolean());
		
		// this one is not assigned
		assertEquals(false, ((UserVoucherListByVoucherResponse)response.getContent()).getItems().get(1).get("isUsed")
				.getAsBoolean());
		assertEquals(null, ((UserVoucherListByVoucherResponse)response.getContent()).getItems().get(1).get("usedDate"));
	}

	@Test
	public void testGetVoucherListByUserId() throws F4MException, IOException, URISyntaxException {

		updateVoucherClassTestData();
		updateTestDataForUserVoucherListByUser();

		String requestJson = getPlainTextJsonFromResources("getVoucherListByUserId.json",
				KeyStoreTestUtil.ADMIN_CLIENT_INFO);

		receivedMessageCollector.getReceivedMessageList()
				.clear();

		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson.replaceFirst("<<userId>>",
				ANONYMOUS_USER_ID));

		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList()
				.size()));

		JsonMessage<? extends JsonMessageContent> response = receivedMessageCollector.getReceivedMessageList()
				.get(0);
		receivedMessageCollector.clearReceivedMessageList();
		assertMessageContentType(response, (Class<? extends JsonMessageContent>) UserVoucherListByUserResponse.class);

		assertEquals(4, ((UserVoucherListByUserResponse)response.getContent()).getItems().size());
		
		assertEquals("11:0", ((UserVoucherListByUserResponse)response.getContent()).getItems().get(0).get("userVoucherId")
				.getAsString());
		assertEquals("ABC123", ((UserVoucherListByUserResponse)response.getContent()).getItems().get(0).get("code")
				.getAsString());
		assertEquals(true, ((UserVoucherListByUserResponse)response.getContent()).getItems().get(0).get("isUsed")
				.getAsBoolean());
		assertEquals("2001-01-01T10:00:00Z", ((UserVoucherListByUserResponse)response.getContent()).getItems().get(0)
				.get("usedDate").getAsString());
		assertEquals(true, ((UserVoucherListByUserResponse)response.getContent()).getItems().get(0).get("isWon")
				.getAsBoolean());
	}

	@Test
	public void testVoucherList() throws F4MException, IOException, URISyntaxException {
		updateVoucherListTestData();

		String requestJson = getPlainTextJsonFromResources("getVoucherListByTenantId.json", ANONYMOUS_CLIENT_INFO);
		receivedMessageCollector.getReceivedMessageList().clear();

		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(),
				requestJson);

		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));

		JsonMessage<? extends JsonMessageContent> response = receivedMessageCollector.getReceivedMessageList().get(0);

		assertMessageContentType(response, (Class<? extends JsonMessageContent>) VoucherListResponse.class);

		List<JsonObject> responseItems = ((VoucherListResponse) response.getContent()).getItems();
		assertEquals(2, responseItems.size());

		assertEquals("2", responseItems.get(0).get("voucherId").getAsString());
		assertEquals("mega short Title lol", responseItems.get(0).get("shortTitle").getAsString());
		assertEquals("company lol", responseItems.get(0).get("company").getAsString());
		assertEquals(50, responseItems.get(0).get("bonuspointsCosts").getAsInt());
		assertEquals("3", responseItems.get(1).get("voucherId").getAsString());
		assertEquals(60, responseItems.get(1).get("bonuspointsCosts").getAsInt());
		assertFalse(responseItems.get(0).get("isExchange").getAsBoolean());
		assertTrue(responseItems.get(1).get("isExchange").getAsBoolean());

	}

	@Test
	public void testVoucherListWithExpiredAndInvalid() throws F4MException, IOException, URISyntaxException {
		updateVoucherListExpiredAndInvalidTestData();

		String requestJson = getPlainTextJsonFromResources("getVoucherListByTenantId.json", ANONYMOUS_CLIENT_INFO);
		receivedMessageCollector.getReceivedMessageList().clear();

		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(),
				requestJson);

		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));

		JsonMessage<? extends JsonMessageContent> response = receivedMessageCollector.getReceivedMessageList().get(0);

		assertMessageContentType(response, (Class<? extends JsonMessageContent>) VoucherListResponse.class);

		assertEquals(2, ((VoucherListResponse) response.getContent()).getItems().size());

		assertEquals("2", ((VoucherListResponse) response.getContent()).getItems().get(0).get("voucherId").getAsString());
		assertEquals(50, ((VoucherListResponse) response.getContent()).getItems().get(0).get("bonuspointsCosts").getAsInt());
		assertEquals("4", ((VoucherListResponse) response.getContent()).getItems().get(1).get("voucherId").getAsString());
		assertEquals(60, ((VoucherListResponse) response.getContent()).getItems().get(1).get("bonuspointsCosts").getAsInt());
	}

	@Test
	public void testMoveVouchers() throws Exception {
		String expectedVoucherCode = "ABC124";
		String gameInstanceId="111";

		// Create profile and voucher
		createProfile(TEST_USER_ID);
		createProfile(ANONYMOUS_USER_ID);
		updateVoucherClassTestData();
		
		// Assign voucher to user
        emailsContentToCheck.add(getSendEmailRequestWrapper(Messages.VOUCHER_WON_ASSIGNED_TO_USER_CONTENT_WITH_REDEMPTION_URL,
				Messages.VOUCHER_WON_ASSIGNED_TO_USER_SUBJECT, ANONYMOUS_USER_ID, expectedVoucherCode));
        final String userVoucherAssignJson = getPlainTextJsonFromResources("UserVoucherAssignRequest.json")
        		.replace("<<userId>>", ANONYMOUS_USER_ID)
                .replace("<<voucherId>>", DEFAULT_TEST_VOUCHER_ID)
                .replace("<<gameInstanceId>>", "111");
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), userVoucherAssignJson);
        RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
        String userVoucherId = receivedMessageCollector.<UserVoucherAssignResponse> getMessageByType(
        		VoucherMessageTypes.USER_VOUCHER_ASSIGN_RESPONSE).getContent().getUserVoucherId();
        receivedMessageCollector.clearReceivedMessageList();
		assertTrue(getProfileVoucherSubBlob(ANONYMOUS_USER_ID).size() == 1);
		assertNull(getProfileVoucherSubBlob(TEST_USER_ID));
        
		// Perform move
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getPlainTextJsonFromResources(
				"moveVouchersRequest.json")
				.replaceAll("<<sourceUserId>>", ANONYMOUS_USER_ID)
				.replaceAll("<<targetUserId>>", TEST_USER_ID));
		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));

		// Verify that vouchers are now assigned to target user and target the target user
		checkUserVoucherContents(userVoucherId, TEST_USER_ID, gameInstanceId, expectedVoucherCode, null, false);
		
		// Verify that old user does not have the vouchers any more
		assertNull(getProfileVoucherSubBlob(ANONYMOUS_USER_ID));
	}

	@Test
	public void testDeleteExpiredVouchersFromList() throws Exception {
		updateVoucherListExpiredAndInvalidTestData();

		String requestJson = getPlainTextJsonFromResources("voucherListDeleteExpiredEntriesRequest.json",
				ANONYMOUS_CLIENT_INFO);
		receivedMessageCollector.getReceivedMessageList().clear();

		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(),
				requestJson);

		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));

		checkVoucherListContentsAfterDeleteCall();
	}

	
	private void updateVoucherMapTestData(String fileName, String set, String binName) throws IOException {

		String jsonVouchers = getPlainTextJsonFromResources(fileName)
				.replaceAll("<<expirationDate>>",
						DateTimeUtil.formatISODateTime(DateTimeUtil.getCurrentDateTime().plusDays(1)))
				.replaceAll("<<expirationDateExpired>>",
						DateTimeUtil.formatISODateTime(DateTimeUtil.getCurrentDateTime().minusDays(1)));

		JsonParser parser = new JsonParser();
		JsonObject jsonObject = (JsonObject) parser.parse(jsonVouchers);
			for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
				String key = entry.getKey();
				String binValue = entry.getValue().toString();
				aerospikeDao.createOrUpdateMap(set, key, binName,
						(readResult, writePolicy) -> getMapObject(binValue));
			}

	}

	private Map<Object, Object> getMapObject(String data) {
		Map<Object, Object> mapObject = new HashMap<>();
		JsonObject jsonArray = jsonUtil.fromJson(data, JsonObject.class);
		for (Map.Entry<String, JsonElement> item : jsonArray.entrySet()) {
			mapObject.put(item.getKey(), item.getValue().toString());
		}
		return mapObject;
	}

	private void checkVoucherListContentsAfterDeleteCall() {
		List<Voucher> vouchers = aerospikeDao.getVouchersByTenantId(0, 0, TENANT_ID);
		assertEquals(3, vouchers.size());
		for(Voucher item : vouchers) {
			assertTrue(DateTimeUtil.parseISODateTimeString(item.getExpirationDate())
					.isAfter(DateTimeUtil.getCurrentDateTime()));
		}
	}
}