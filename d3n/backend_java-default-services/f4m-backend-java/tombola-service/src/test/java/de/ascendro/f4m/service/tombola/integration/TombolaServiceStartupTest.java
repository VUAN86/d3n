package de.ascendro.f4m.service.tombola.integration;

import static de.ascendro.f4m.service.usermessage.translation.Messages.TOMBOLA_OPEN_ANNOUNCEMENT_PUSH;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_USER_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.APP_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.REGISTERED_CLIENT_INFO;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.REGISTERED_USER_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.TENANT_ID;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Injector;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.analytics.model.RewardEvent;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.util.ProfilePrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.annotation.ClientMessageHandler;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.event.model.publish.PublishMessageContent;
import de.ascendro.f4m.service.exception.ExceptionCodes;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.exception.UnexpectedTestException;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.integration.collector.ReceivedMessageCollector;
import de.ascendro.f4m.service.integration.rule.MockServiceRule.BindableAbstractModule;
import de.ascendro.f4m.service.integration.rule.MockServiceRule.ThrowingFunction;
import de.ascendro.f4m.service.integration.test.F4MServiceWithMockIntegrationTestBase;
import de.ascendro.f4m.service.json.JsonMessageUtil;
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
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.TransactionId;
import de.ascendro.f4m.service.payment.model.internal.LoadOrWithdrawWithoutCoverageRequest;
import de.ascendro.f4m.service.payment.model.internal.TransferBetweenAccountsRequest;
import de.ascendro.f4m.service.profile.ProfileMessageTypes;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.tombola.TombolaMessageTypes;
import de.ascendro.f4m.service.tombola.config.TombolaConfig;
import de.ascendro.f4m.service.tombola.dao.TombolaAerospikeDaoImpl;
import de.ascendro.f4m.service.tombola.di.TombolaDefaultMessageMapper;
import de.ascendro.f4m.service.tombola.exception.TombolaServiceExceptionCodes;
import de.ascendro.f4m.service.tombola.model.Bundle;
import de.ascendro.f4m.service.tombola.model.Prize;
import de.ascendro.f4m.service.tombola.model.PrizeType;
import de.ascendro.f4m.service.tombola.model.PurchasedBundle;
import de.ascendro.f4m.service.tombola.model.Tombola;
import de.ascendro.f4m.service.tombola.model.TombolaBuyer;
import de.ascendro.f4m.service.tombola.model.TombolaDrawing;
import de.ascendro.f4m.service.tombola.model.TombolaStatus;
import de.ascendro.f4m.service.tombola.model.TombolaWinner;
import de.ascendro.f4m.service.tombola.model.UserStatistics;
import de.ascendro.f4m.service.tombola.model.UserTombolaInfo;
import de.ascendro.f4m.service.tombola.model.drawing.TombolaDrawingGetResponse;
import de.ascendro.f4m.service.tombola.model.drawing.TombolaDrawingListRequest;
import de.ascendro.f4m.service.tombola.model.drawing.TombolaDrawingListResponse;
import de.ascendro.f4m.service.tombola.model.events.TombolaEvents;
import de.ascendro.f4m.service.tombola.model.get.PurchasedBundleResponseModel;
import de.ascendro.f4m.service.tombola.model.get.TombolaBuyerListResponse;
import de.ascendro.f4m.service.tombola.model.get.TombolaGetResponse;
import de.ascendro.f4m.service.tombola.model.get.TombolaListResponse;
import de.ascendro.f4m.service.tombola.model.get.TombolaWinnerListResponse;
import de.ascendro.f4m.service.tombola.model.get.UserTombolaGetResponse;
import de.ascendro.f4m.service.tombola.model.get.UserTombolaInfoResponseModel;
import de.ascendro.f4m.service.tombola.model.get.UserTombolaListResponse;
import de.ascendro.f4m.service.tombola.model.schema.TombolaMessageSchemaMapper;
import de.ascendro.f4m.service.tombola.util.TombolaPrimaryKeyUtil;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.usermessage.model.SendEmailWrapperRequest;
import de.ascendro.f4m.service.usermessage.model.SendEmailWrapperResponse;
import de.ascendro.f4m.service.usermessage.model.SendTopicPushRequest;
import de.ascendro.f4m.service.usermessage.model.SendUserPushResponse;
import de.ascendro.f4m.service.usermessage.translation.Messages;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.voucher.VoucherMessageTypes;
import de.ascendro.f4m.service.voucher.model.Voucher;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherAssignForTombolaRequest;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherAssignForTombolaResponse;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherReleaseRequest;

public class TombolaServiceStartupTest extends F4MServiceWithMockIntegrationTestBase {

    private static final String AVAILABLE_TOMBOLA_LIST_BIN_NAME = "tombolas";
    private static final String TOMBOLA_BIN_NAME = "tombola";
    private static final String USER_TOMBOLA_BIN_NAME = "usrTombola";
    private static final String DRAWING_BIN_NAME = "drawing";
    private static final String AVAILABLE_TOMBOLA_DRAWING_LIST_BIN_NAME = "drawings";
    private static final String HISTORY_BIN_NAME = "history";
    private static final String STATISTICS_BIN_NAME = "statistics";

    private static final String TOMBOLA_OPEN_NOTIFICATION_TOPIC = "tombola.open.announcement.push";

    private static final String TOMBOLA_COUNTER_RECORD_NAME = "counters";
    private static final String TOMBOLA_LOCKED_COUNTER_BIN_NAME = "locked";
    private static final String TOMBOLA_PURCHASED_COUNTER_BIN_NAME = "purchased";
    private static final String DEFAULT_TEST_TOMBOLA_ID = "1";
    private static final int DRAW_WAIT_TIME_OUT_MS = 4000;
    private static final int EXPECTED_TOTAL_TICKETS_AFTER_DRAWING = 100;
    private static final int EXPECTED_NUMBER_OF_WINNERS_AFTER_DRAWING = 7;
    private static final String VOUCHER_ID = "22";
    private static final String VOUCHER_ID_2 = "33";
    public static final String VOUCHER_BIN_NAME = "voucher";
    public static final int VOUCHER_WINNER_POSITION = 2;


    private TombolaAerospikeDaoImpl aerospikeDao;
    private IAerospikeClient aerospikeClient;

    private ReceivedMessageCollector receivedMessageCollector;
    private JsonUtil jsonUtil;
    private JsonMessageUtil jsonMessageUtil;
    private TombolaPrimaryKeyUtil tombolaPrimaryKeyUtil;
    private ProfilePrimaryKeyUtil profilePrimaryKeyUtil;
    private boolean paymentsShouldFail = false;
    private TestDataLoader testDataLoader;
    private List<String> expectedTombolaUsers;
    private List<PayoutPrize> payoutPrizes;
    private int payoutPrizesMoneyCount;
    private int payoutPrizesBonusCount;
    private int payoutPrizesCreditCount;
    private int payoutPrizesVoucherCount;
    private Map<String, Integer> releaseCountPerVoucherId;
    private int tombolaDrawEventReceivedCount;

    private Tracker tracker;

    private ArgumentCaptor<ClientInfo> clientInfoArgumentCaptor;
    private ArgumentCaptor<RewardEvent> rewardEventArgumentCaptor;

    /** Used to save email content to check when {@link SendEmailWrapperRequest} received */
    private List<SendEmailWrapperRequest> receivedEmailsContent;
    private int emailCount;
    private boolean creditPayoutShouldFail;
    private boolean voucherPayoutShouldFail;

    private class PayoutPrize {
        private PrizeType prizeType;
        private String voucherId;
        private BigDecimal amount;
        private String userId;

        private PayoutPrize(PrizeType prizeType, String voucherId, BigDecimal amount, String userId) {
            this.prizeType = prizeType;
            this.voucherId = voucherId;
            this.amount = amount;
            this.userId = userId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PayoutPrize that = (PayoutPrize) o;
            return prizeType == that.prizeType &&
                    Objects.equals(voucherId, that.voucherId) &&
                    Objects.equals(amount, that.amount) &&
                    Objects.equals(userId, that.userId);
        }
    }

    @Override
	protected void configureInjectionModuleMock(BindableAbstractModule module) {
		module.bindToModule(JsonMessageTypeMap.class).to(TombolaDefaultMessageMapper.class);
		module.bindToModule(JsonMessageSchemaMap.class).to(TombolaMessageSchemaMapper.class);
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
        rewardEventArgumentCaptor = ArgumentCaptor.forClass(RewardEvent.class);

        receivedMessageCollector = (ReceivedMessageCollector) clientInjector.getInstance(
                com.google.inject.Key.get(JsonMessageHandlerProvider.class, ClientMessageHandler.class)).get();

		assertServiceStartup(ProfileMessageTypes.SERVICE_NAME, PaymentMessageTypes.SERVICE_NAME,
                VoucherMessageTypes.SERVICE_NAME, EventMessageTypes.SERVICE_NAME, UserMessageMessageTypes.SERVICE_NAME);
        Injector injector = jettyServerRule.getServerStartup().getInjector();
        aerospikeDao = injector.getInstance(TombolaAerospikeDaoImpl.class);
        aerospikeClient = jettyServerRule.getServerStartup().getInjector().getInstance(AerospikeClientProvider.class)
                .get();
        tombolaPrimaryKeyUtil = injector.getInstance(TombolaPrimaryKeyUtil.class);
        profilePrimaryKeyUtil = injector.getInstance(ProfilePrimaryKeyUtil.class);
        jsonMessageUtil = injector.getInstance(JsonMessageUtil.class);
        jsonUtil = new JsonUtil();
        testDataLoader = new TestDataLoader(this);
        payoutPrizes = new ArrayList<>();
        payoutPrizesMoneyCount = 0;
        payoutPrizesBonusCount = 0;
        payoutPrizesCreditCount = 0;
        payoutPrizesVoucherCount = 0;
        releaseCountPerVoucherId = new HashMap<>();
        tracker = injector.getInstance(Tracker.class);
        Mockito.reset(tracker); //reset tracker if SimpleRepeatRule is used for debugging
        receivedEmailsContent = new ArrayList<>();
        creditPayoutShouldFail = false;
        voucherPayoutShouldFail = false;
        tombolaDrawEventReceivedCount = 0;
    }
    
    
	@After
	public void clearAerospikeData() {
		this.aerospikeClient.close();
	}

    private JsonMessageContent onReceivedMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded) {
        final EventMessageTypes eventMessageType;
        PaymentMessageTypes paymentType = originalMessageDecoded.getType(PaymentMessageTypes.class);
        if (PaymentMessageTypes.TRANSFER_BETWEEN_ACCOUNTS == paymentType
                || PaymentMessageTypes.LOAD_OR_WITHDRAW_WITHOUT_COVERAGE == paymentType) {
            if(paymentsShouldFail) {
                return onFailingTransferBetweenAccountsReceived();
            } else {
                return onTransferBetweenAccountsReceived(paymentType, originalMessageDecoded);
            }
        } else if (EventMessageTypes.RESUBSCRIBE == originalMessageDecoded.getType(EventMessageTypes.class) ||
        		EventMessageTypes.SUBSCRIBE == originalMessageDecoded.getType(EventMessageTypes.class)) {
            return null;// ignore event/resubscribe
        } else if (VoucherMessageTypes.USER_VOUCHER_ASSIGN_FOR_TOMBOLA ==
                originalMessageDecoded.getType(VoucherMessageTypes.class)) {
            if (voucherPayoutShouldFail) {
                throw new F4MEntryNotFoundException("User Voucher not found");
            }
            UserVoucherAssignForTombolaRequest request = (UserVoucherAssignForTombolaRequest)
                    originalMessageDecoded.getContent();
            addPrizeToExpected(PrizeType.VOUCHER, null, request.getVoucherId(), request.getUserId());
            payoutPrizesVoucherCount++;
            return new UserVoucherAssignForTombolaResponse("userVoucherId");
        } else if (VoucherMessageTypes.USER_VOUCHER_RELEASE ==
                originalMessageDecoded.getType(VoucherMessageTypes.class)) {
        	synchronized (releaseCountPerVoucherId) {
	            UserVoucherReleaseRequest request = (UserVoucherReleaseRequest) originalMessageDecoded.getContent();
	            int count = Optional.ofNullable(releaseCountPerVoucherId.get(request.getVoucherId())).orElse(0) + 1;
	            releaseCountPerVoucherId.put(request.getVoucherId(), count);
        	}
            return new EmptyJsonMessageContent();
		} else if (UserMessageMessageTypes.SEND_TOPIC_PUSH == originalMessageDecoded.getType(UserMessageMessageTypes.class)) {
            return onTombolaOpenNotificationRequest((SendTopicPushRequest) originalMessageDecoded.getContent());
        } else if (UserMessageMessageTypes.SEND_EMAIL == originalMessageDecoded.getType(
                UserMessageMessageTypes.class)) {
            return onSendEmailReceived(originalMessageDecoded);
        } else if ((eventMessageType = originalMessageDecoded.getType(EventMessageTypes.class)) != null) {
            if (eventMessageType == EventMessageTypes.PUBLISH) {
                PublishMessageContent content =
                        (PublishMessageContent) originalMessageDecoded.getContent();
                if (TombolaEvents.isDrawEventTopic(content.getTopic())) {
                    TombolaEvents drawEvent = new TombolaEvents(content.getNotificationContent().getAsJsonObject());
                    tombolaDrawEventReceivedCount ++;
                    assertEquals(DEFAULT_TEST_TOMBOLA_ID, drawEvent.getTombolaId());
                    assertEquals(TENANT_ID, drawEvent.getTenantId());
                    return null;
                } else {
                    throw new UnexpectedTestException("Unexpected message " + originalMessageDecoded);
                }
            } else {
                throw new UnexpectedTestException("Unexpected message " + originalMessageDecoded);
            }
        } else {
            throw new UnexpectedTestException("Unexpected message " + originalMessageDecoded);
        }
    }

    private JsonMessageContent onSendEmailReceived(JsonMessage<? extends JsonMessageContent> originalMessageDecoded) {
        receivedEmailsContent.add((SendEmailWrapperRequest) originalMessageDecoded.getContent());
        emailCount++;
        return new SendEmailWrapperResponse();
    }

    private SendUserPushResponse onTombolaOpenNotificationRequest(SendTopicPushRequest message) {
        assertEquals(TOMBOLA_OPEN_NOTIFICATION_TOPIC, message.getTopic());
        assertEquals(TOMBOLA_OPEN_ANNOUNCEMENT_PUSH, message.getMessage());
		assertArrayEquals(new String[] { "60551c1e-a718-333-80f5-76304dec7eb7", "asd1233", "dsf23" },
				message.getAppIds());
        SendUserPushResponse response = new SendUserPushResponse();
        response.setNotificationIds(new String[]{"123"});
        return response;
    }

    private TransactionId onFailingTransferBetweenAccountsReceived() {
        throw new F4MPaymentClientException(PaymentServiceExceptionCodes.ERR_ACCOUNT_STATE_NOT_CHANGEABLE,
                "Error occurred");
    }

    private TransactionId onTransferBetweenAccountsReceived(PaymentMessageTypes paymentType,
            JsonMessage<? extends JsonMessageContent> originalMessageDecoded) throws F4MException {
        if (paymentType == PaymentMessageTypes.TRANSFER_BETWEEN_ACCOUNTS) {
            TransferBetweenAccountsRequest request = (TransferBetweenAccountsRequest)
                    originalMessageDecoded.getContent();
            if (StringUtils.isNotBlank(request.getToProfileId())) {
                addPrizeToExpected(PrizeType.MONEY, request.getAmount(), null, request.getToProfileId());
                payoutPrizesMoneyCount++;
            }
        } else if (paymentType == PaymentMessageTypes.LOAD_OR_WITHDRAW_WITHOUT_COVERAGE) {
            LoadOrWithdrawWithoutCoverageRequest request = (LoadOrWithdrawWithoutCoverageRequest)
                    originalMessageDecoded.getContent();
            if (StringUtils.isNotBlank(request.getToProfileId())) {
                if (Currency.BONUS == request.getCurrency()) {
                    addPrizeToExpected(PrizeType.BONUS, request.getAmount(), null, request.getToProfileId());
                    payoutPrizesBonusCount++;
                } else {
                    if (creditPayoutShouldFail){
                        throw new F4MPaymentClientException(PaymentServiceExceptionCodes.ERR_INSUFFICIENT_FUNDS,
                                "Insufficient funds");
                    }
                    addPrizeToExpected(PrizeType.CREDIT, request.getAmount(), null, request.getToProfileId());
                    payoutPrizesCreditCount++;
                }
            }
        }
        return new TransactionId("123");
    }

    private void addPrizeToExpected(PrizeType prizeType, BigDecimal amount, String voucherId, String userId) {
        payoutPrizes.add(new PayoutPrize(prizeType, voucherId, amount, userId));
    }

    private void updateTombolaListTestData(String fileName) throws IOException {
        String set = config.getProperty(TombolaConfig.AEROSPIKE_AVAILABLE_TOMBOLA_LIST_SET);
        updateMapTestData(fileName, set, AVAILABLE_TOMBOLA_LIST_BIN_NAME);
    }

    private List<Tombola> updateTombolaTestData(String fileName) throws IOException {
        String set = config.getProperty(TombolaConfig.AEROSPIKE_TOMBOLA_SET);
        return insertTombolaTestData(fileName, set, TOMBOLA_BIN_NAME);
    }

    private List<UserTombolaInfo> updateUserTombolaTestData(String fileName) throws IOException {
        String set = config.getProperty(TombolaConfig.AEROSPIKE_USER_TOMBOLA_SET);
        return insertUserTombolaTestData(fileName, set, USER_TOMBOLA_BIN_NAME);
    }

    private void updateUserTombolaDrawnMonthsTestData() throws IOException {
        String set = config.getProperty(TombolaConfig.AEROSPIKE_USER_TOMBOLA_SET);
        insertUserTombolaTestDataForBin("userTombolaHistoryDrawnList.json", set, HISTORY_BIN_NAME);
    }

    private List<Tombola> insertTombolaTestData(String fileName, String set, String binName) throws IOException {
        List<Tombola> expectedTombolas = new ArrayList<>();
        String jsonVouchers = getPlainTextJsonFromResources(fileName);

        JsonParser parser = new JsonParser();
        JsonObject jsonObject = (JsonObject) parser.parse(jsonVouchers);

        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String key = entry.getKey();
            String binValue = entry.getValue().toString();
            aerospikeDao.createJson(set, key, binName, binValue);
            expectedTombolas.add(jsonUtil.fromJson(binValue, Tombola.class));
        }
        return expectedTombolas;
    }

    private List<UserTombolaInfo> insertUserTombolaTestData(String fileName, String set, String binName)
            throws IOException {
        List<UserTombolaInfo> expectedTombolas = new ArrayList<>();
        String userTombolaInfos = getPlainTextJsonFromResources(fileName);

        JsonParser parser = new JsonParser();
        JsonObject jsonObject = (JsonObject) parser.parse(userTombolaInfos);

        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String key = entry.getKey();
            String binValue = entry.getValue().toString();
            aerospikeDao.createOrUpdateJson(set, key, binName, (readResult, writePolicy) -> binValue);
            expectedTombolas.add(jsonUtil.fromJson(binValue, UserTombolaInfo.class));
        }
        return expectedTombolas;
    }

    private void updateTombolaDrawingListTestData(String fileName) throws IOException {
        String set = config.getProperty(TombolaConfig.AEROSPIKE_TOMBOLA_DRAWING_LIST_SET);
        updateMapTestData(fileName, set, AVAILABLE_TOMBOLA_DRAWING_LIST_BIN_NAME);
    }

    private void updateUserTombolaListTestData(String fileName) throws IOException {
        String set = config.getProperty(TombolaConfig.AEROSPIKE_USER_TOMBOLA_SET);
        updateMapTestData(fileName, set, HISTORY_BIN_NAME);
    }

    private void updateMapTestData(String fileName, String set, String binName) throws IOException {

        String itemList = getPlainTextJsonFromResources(fileName);

        JsonParser parser = new JsonParser();
        JsonObject jsonObject = (JsonObject) parser.parse(itemList);
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

	@Override
	protected TestClientInjectionClasses getTestClientInjectionClasses() {
		return new TestClientInjectionClasses(TombolaDefaultMessageMapper.class, TombolaMessageSchemaMapper.class);
	}

    @Test
    public void testAvailableTombolaList() throws Exception {
        updateTombolaListTestData("tombolaListData.json");
        updateCounterTestData();

        String requestJson = getPlainTextJsonFromResources("TombolaListRequest.json", ANONYMOUS_CLIENT_INFO)
                .replaceFirst("<<limit>>", "10").replaceFirst("<<offset>>", "0");
        receivedMessageCollector.getReceivedMessageList().clear();

        final TombolaListResponse response = getTombolaListResponse(requestJson);
        assertEquals(2, response.getItems().size());

        List<Tombola> items = response.getItems();
        // check first tombola item
        assertEquals("1", items.get(0).getId());
        assertEquals("Mega Tombola", items.get(0).getName());
        assertEquals(2, items.get(0).getBundles().size());
        assertEquals(100, items.get(0).getBundles().get(1).getAmount());
        assertEquals(1, items.get(0).getPrizes().size());
        assertEquals("consolation", items.get(0).getConsolationPrize().getId());
        assertEquals(0, items.get(0).getPurchasedTicketsAmount());

        // check second tombola item
        assertEquals("2", items.get(1).getId());
        assertEquals("Second Mega Tombola", items.get(1).getName());
        assertEquals(3, items.get(1).getBundles().size());
        assertEquals(100, items.get(1).getBundles().get(1).getAmount());
        assertEquals(2, items.get(1).getPrizes().size());
        assertEquals("2", items.get(1).getPrizes().get(1).getId());
        assertEquals("consolation", items.get(1).getConsolationPrize().getId());
        assertEquals(5, items.get(1).getPurchasedTicketsAmount());

    }

    @Test
    public void testAvailableTombolaListCountersNotInitialized() throws Exception {
        updateTombolaListTestData("tombolaListData.json");

        String requestJson = getPlainTextJsonFromResources("TombolaListRequest.json", ANONYMOUS_CLIENT_INFO)
                .replaceFirst("<<limit>>", "10").replaceFirst("<<offset>>", "0");
        receivedMessageCollector.getReceivedMessageList().clear();

        final TombolaListResponse response = getTombolaListResponse(requestJson);
        assertEquals(2, response.getItems().size());

        List<Tombola> items = response.getItems();
        // check first tombola item
        assertEquals("1", items.get(0).getId());
        assertEquals(0, items.get(0).getPurchasedTicketsAmount());

        // check second tombola item
        assertEquals("2", items.get(1).getId());
        assertEquals(0, items.get(1).getPurchasedTicketsAmount());

    }

    @Test
    public void testAvailableTombolaListPagination() throws Exception {
        updateCounterTestData();
        updateTombolaListTestData("tombolaListDataForPaginationTest.json");

        String requestJson = getPlainTextJsonFromResources("TombolaListRequest.json", ANONYMOUS_CLIENT_INFO)
                .replaceFirst("<<limit>>", "3").replaceFirst("<<offset>>", "1");
        receivedMessageCollector.getReceivedMessageList().clear();

        final TombolaListResponse response = getTombolaListResponse(requestJson);
        assertEquals(3, response.getItems().size());

        List<Tombola> items = response.getItems();
        // check first tombola item
        assertEquals("2", items.get(0).getId());

        // check second tombola item
        assertEquals("3", items.get(1).getId());

        // check second tombola item
        assertEquals("4", items.get(2).getId());

        //check pagination variables
        assertEquals(5, response.getTotal());
        assertEquals(3, response.getLimit());
        assertEquals(1, response.getOffset());

    }

    @Test
    public void testAvailableTombolaListEmpty() throws Exception {
        String requestJson = getPlainTextJsonFromResources("TombolaListRequest.json", ANONYMOUS_CLIENT_INFO)
                .replaceFirst("<<limit>>", "10").replaceFirst("<<offset>>", "0");
        receivedMessageCollector.getReceivedMessageList().clear();

        final TombolaListResponse response = getTombolaListResponse(requestJson);
        assertEquals(0, response.getItems().size());
    }

    @Test
    public void testBuyTombola() throws Exception {
        updateTombolaTestData("tombolaBuyData.json");
        updateCounterTestData();
        paymentsShouldFail = false;


        String requestJson = getPlainTextJsonFromResources("TombolaBuyRequest.json", ANONYMOUS_CLIENT_INFO);
        requestJson = requestJson.replaceFirst("<<TOMBOLAID>>", DEFAULT_TEST_TOMBOLA_ID);

        receivedMessageCollector.getReceivedMessageList().clear();

        final EmptyJsonMessageContent response = getTombolaBuyResponse(requestJson);
        assertNotNull(response);

        checkCounterTestData(10);
        checkWrittenHistoricalData(Currency.MONEY);
    }

    @Test
    public void testBuyTombolaNonMoneyCurrency() throws Exception {
        updateTombolaTestData("tombolaBuyDataNonMoney.json");
        updateCounterTestData();
        paymentsShouldFail = false;


        String requestJson = getPlainTextJsonFromResources("TombolaBuyRequest.json", ANONYMOUS_CLIENT_INFO);
        requestJson = requestJson.replaceFirst("<<TOMBOLAID>>", DEFAULT_TEST_TOMBOLA_ID);

        receivedMessageCollector.getReceivedMessageList().clear();

        final EmptyJsonMessageContent response = getTombolaBuyResponse(requestJson);
        assertNotNull(response);

        checkCounterTestData(10);
        checkWrittenHistoricalData(Currency.BONUS);
    }

    @Test
    public void testBuyTombolaCountersNotInitialized() throws Exception {
        updateTombolaTestData("tombolaBuyData.json");
        paymentsShouldFail = false;


        String requestJson = getPlainTextJsonFromResources("TombolaBuyRequest.json", ANONYMOUS_CLIENT_INFO);
        requestJson = requestJson.replaceFirst("<<TOMBOLAID>>", DEFAULT_TEST_TOMBOLA_ID);

        receivedMessageCollector.getReceivedMessageList().clear();

        final EmptyJsonMessageContent response = getTombolaBuyResponse(requestJson);
        assertNotNull(response);

        checkCounterTestData(10);
        checkWrittenHistoricalData(Currency.MONEY);
    }

    @Test
    public void testBuyExpiredTombola() throws Exception {
        updateTombolaTestData("tombolaExpiredBuyData.json");
        updateCounterTestData();
        paymentsShouldFail = false;


        String requestJson = getPlainTextJsonFromResources("TombolaBuyRequest.json", ANONYMOUS_CLIENT_INFO);
        requestJson = requestJson.replaceFirst("<<TOMBOLAID>>", "1");

		receivedMessageCollector.getReceivedMessageList().clear();

		final JsonMessage<? extends JsonMessageContent> response = getErrorResponse(requestJson);
		assertEquals(TombolaServiceExceptionCodes.ERR_NO_LONGER_AVAILABLE, response.getError().getCode());
        assertNotNull(response);

        checkCounterTestData(0);
    }

    @Test
    public void testBuyTombolaMissingStatus() throws Exception {
        updateTombolaTestData("tombolaMissingStatusBuyData.json");
        updateCounterTestData();
        paymentsShouldFail = false;


        String requestJson = getPlainTextJsonFromResources("TombolaBuyRequest.json", ANONYMOUS_CLIENT_INFO);
        requestJson = requestJson.replaceFirst("<<TOMBOLAID>>", "1");

        receivedMessageCollector.getReceivedMessageList().clear();

        final JsonMessage<? extends JsonMessageContent> response = getErrorResponse(requestJson);
        assertEquals(TombolaServiceExceptionCodes.ERR_NO_LONGER_AVAILABLE, response.getError().getCode());
        assertNotNull(response);

        checkCounterTestData(0);
    }

    @Test
    public void testBuyTombolaMissingAppIdList() throws Exception {
        updateTombolaTestData("tombolaMissingAppIdListBuyData.json");
        updateCounterTestData();
        paymentsShouldFail = false;


        String requestJson = getPlainTextJsonFromResources("TombolaBuyRequest.json", ANONYMOUS_CLIENT_INFO);
        requestJson = requestJson.replaceFirst("<<TOMBOLAID>>", "1");

        receivedMessageCollector.getReceivedMessageList().clear();

        final JsonMessage<? extends JsonMessageContent> response = getErrorResponse(requestJson);
        assertEquals(TombolaServiceExceptionCodes.ERR_ENTRY_NOT_FOUND, response.getError().getCode());
        assertNotNull(response);

        checkCounterTestData(0);
    }

    @Test
    public void testBuyTombolaMissingAppIdFromList() throws Exception {
        updateTombolaTestData("tombolaMissingAppIdFromListBuyData.json");
        updateCounterTestData();
        paymentsShouldFail = false;


        String requestJson = getPlainTextJsonFromResources("TombolaBuyRequest.json", ANONYMOUS_CLIENT_INFO);
        requestJson = requestJson.replaceFirst("<<TOMBOLAID>>", "1");

        receivedMessageCollector.getReceivedMessageList().clear();

        final JsonMessage<? extends JsonMessageContent> response = getErrorResponse(requestJson);
        assertEquals(TombolaServiceExceptionCodes.ERR_ENTRY_NOT_FOUND, response.getError().getCode());
        assertNotNull(response);

        checkCounterTestData(0);
    }

    @Test
    public void testBuyWrongAppTombola() throws Exception {
        updateTombolaTestData("tombolaWrongAppBuyData.json");
        updateCounterTestData();
        paymentsShouldFail = false;


        String requestJson = getPlainTextJsonFromResources("TombolaBuyRequest.json", ANONYMOUS_CLIENT_INFO);
        requestJson = requestJson.replaceFirst("<<TOMBOLAID>>", "1");

        receivedMessageCollector.getReceivedMessageList().clear();

        final JsonMessage<? extends JsonMessageContent> response = getErrorResponse(requestJson);
        assertNotNull(response);
        assertEquals(TombolaServiceExceptionCodes.ERR_ENTRY_NOT_FOUND, response.getError().getCode());

        checkCounterTestData(0);
    }

    @Test
    public void testBuyTombolaFailingTransaction() throws Exception {
        updateTombolaTestData("tombolaBuyData.json");
        updateCounterTestData();

        paymentsShouldFail = true;

        String requestJson = getPlainTextJsonFromResources("TombolaBuyRequest.json");
        requestJson = requestJson.replaceFirst("<<TOMBOLAID>>", "1");

        receivedMessageCollector.getReceivedMessageList().clear();

        final JsonMessage<? extends JsonMessageContent> response = getErrorResponse(requestJson);
        assertNotNull(response);

        checkCounterTestData(0);
    }


    @Test
    public void testGetTombolaDetails() throws Exception {
        // add test data
        List<Tombola> expectedTombolas = updateTombolaTestData("tombolaData.json");
        this.writeCounterTestData(5, 2, "1");
        expectedTombolas.get(0).setPurchasedTicketsAmount(5);
        assertEquals(1, expectedTombolas.size());

        String requestJson = getPlainTextJsonFromResources("TombolaGetRequest.json");
        receivedMessageCollector.getReceivedMessageList().clear();

        final TombolaGetResponse response = getTombolaGetResponse(requestJson);
        assertNotNull(response.getTombola());
        assertTombolaEquals(expectedTombolas.get(0), response.getTombola());
    }

    @Test
    public void testGetTombolaDetailsCountersNotInitialized() throws Exception {
        // add test data
        List<Tombola> expectedTombolas = updateTombolaTestData("tombolaData.json");
        expectedTombolas.get(0).setPurchasedTicketsAmount(0);
        assertEquals(1, expectedTombolas.size());

        String requestJson = getPlainTextJsonFromResources("TombolaGetRequest.json");
        receivedMessageCollector.getReceivedMessageList().clear();

        final TombolaGetResponse response = getTombolaGetResponse(requestJson);
        assertNotNull(response.getTombola());
        assertTombolaEquals(expectedTombolas.get(0), response.getTombola());
    }

    private void assertTombolaEquals(Tombola expectedTombola, Tombola actualTombola) {
        assertEquals(expectedTombola.getId(), actualTombola.getId());
        assertEquals(expectedTombola.getName(), actualTombola.getName());
        assertEquals(expectedTombola.getDescription(), actualTombola.getDescription());
        assertEquals(expectedTombola.getImageId(), actualTombola.getImageId());
        assertEquals(expectedTombola.getEndDate(), actualTombola.getEndDate());
        assertEquals(expectedTombola.getStartDate(), actualTombola.getStartDate());
        assertEquals(expectedTombola.getTargetDate(), actualTombola.getTargetDate());
        assertEquals(expectedTombola.getWinningRules(), actualTombola.getWinningRules());
        assertEquals(expectedTombola.getApplicationsIds(), actualTombola.getApplicationsIds());
        assertEquals(expectedTombola.getRegionalSettingsIds(), actualTombola.getRegionalSettingsIds());
        assertEquals(expectedTombola.getPercentOfTicketsAmount(), actualTombola.getPercentOfTicketsAmount());
        assertEquals(expectedTombola.getPurchasedTicketsAmount(), actualTombola.getPurchasedTicketsAmount());
        assertEquals(expectedTombola.getTotalTicketsAmount(), actualTombola.getTotalTicketsAmount());
        assertEquals(expectedTombola.getPlayoutTarget(), actualTombola.getPlayoutTarget());
        assertEquals(expectedTombola.getStatus(), actualTombola.getStatus());

        List<Prize> expectedPrizes = expectedTombola.getPrizes();
        List<Prize> actualPrizes = actualTombola.getPrizes();
        assertEquals(expectedPrizes.size(), actualPrizes.size());
        for (int i = 0; i < expectedPrizes.size(); i++) {
            Prize expectedPrize = expectedPrizes.get(i);
            Prize actualPrize = actualPrizes.get(i);
            assertPrizeEquals(expectedPrize, actualPrize);
        }

        List<Bundle> expectedBundles = expectedTombola.getBundles();
        List<Bundle> actualBundles = actualTombola.getBundles();
        assertEquals(expectedBundles.size(), actualBundles.size());
        assertBundles(expectedBundles, actualBundles);
        assertPrizeEquals(expectedTombola.getConsolationPrize(), actualTombola.getConsolationPrize());
    }

    private void assertPrizeEquals(Prize expectedPrize, Prize actualPrize) {
        assertEquals(expectedPrize.getId(), actualPrize.getId());
        assertEquals(expectedPrize.getImageId(), actualPrize.getImageId());
        assertEquals(expectedPrize.getName(), actualPrize.getName());
        assertEquals(expectedPrize.getVoucherId(), actualPrize.getVoucherId());
        assertEquals(expectedPrize.getAmount(), actualPrize.getAmount());
        assertEquals(expectedPrize.getDraws(), actualPrize.getDraws());
        assertEquals(expectedPrize.getType(), actualPrize.getType());
    }

    private void assertUserTombolaEquals(UserTombolaInfo expectedUserTombola, UserTombolaInfo actualUserTombola) {
        assertEquals(expectedUserTombola.getTombolaId(), actualUserTombola.getTombolaId());
        assertEquals(expectedUserTombola.getDrawDate(), actualUserTombola.getDrawDate());
        assertEquals(expectedUserTombola.getTotalTicketsBought(), actualUserTombola.getTotalTicketsBought());
        assertEquals(expectedUserTombola.getTotalPrizesWon(), actualUserTombola.getTotalPrizesWon());
        assertEquals(expectedUserTombola.getBundles().size(), actualUserTombola.getBundles().size());
        assertEquals(expectedUserTombola.isPending(), actualUserTombola.isPending());

        List<PurchasedBundle> expectedBundles = expectedUserTombola.getBundles();
        List<PurchasedBundle> actualBundles = actualUserTombola.getBundles();
        assertEquals(expectedBundles.size(), actualBundles.size());
        assertPurchasedBundles(expectedBundles, actualBundles);

        List<TombolaWinner> expectedPrizes = expectedUserTombola.getPrizes();
        List<TombolaWinner> actualPrizes = actualUserTombola.getPrizes();
        assertUserTombolaPrizes(expectedPrizes, actualPrizes);
    }

    private void assertUserTombolaResponseModelEquals(UserTombolaInfoResponseModel expectedUserTombola,
            UserTombolaInfoResponseModel actualUserTombola) {
        assertEquals(expectedUserTombola.getTombolaId(), actualUserTombola.getTombolaId());
        assertEquals(expectedUserTombola.getDrawDate(), actualUserTombola.getDrawDate());
        assertEquals(expectedUserTombola.getTotalTicketsBought(), actualUserTombola.getTotalTicketsBought());
        assertEquals(expectedUserTombola.getTotalPrizesWon(), actualUserTombola.getTotalPrizesWon());
        assertEquals(expectedUserTombola.getBundles().size(), actualUserTombola.getBundles().size());
        assertEquals(expectedUserTombola.isPending(), actualUserTombola.isPending());

        List<PurchasedBundleResponseModel> expectedBundles = expectedUserTombola.getBundles();
        List<PurchasedBundleResponseModel> actualBundles = actualUserTombola.getBundles();
        assertEquals(expectedBundles.size(), actualBundles.size());
        assertPurchasedBundlesResponseModel(expectedBundles, actualBundles);

        List<TombolaWinner> expectedPrizes = expectedUserTombola.getPrizes();
        List<TombolaWinner> actualPrizes = actualUserTombola.getPrizes();
        assertUserTombolaPrizes(expectedPrizes, actualPrizes);
    }

    private void assertUserTombolaPrizes(List<TombolaWinner> expectedPrizes, List<TombolaWinner> actualPrizes) {
        assertEquals(expectedPrizes.size(), actualPrizes.size());
        for (int i = 0; i < expectedPrizes.size(); i++) {
            TombolaWinner expectedPrize = expectedPrizes.get(i);
            TombolaWinner actualPrize = actualPrizes.get(i);
            assertEquals(expectedPrize.getPrizeId(), actualPrize.getPrizeId());
            assertEquals(expectedPrize.getImageId(), actualPrize.getImageId());
            assertEquals(expectedPrize.getName(), actualPrize.getName());
            assertEquals(expectedPrize.getVoucherId(), actualPrize.getVoucherId());
            assertEquals(expectedPrize.getAmount(), actualPrize.getAmount());
            assertEquals(expectedPrize.getUser().getUserId(), actualPrize.getUser().getUserId());
            assertEquals(expectedPrize.getAppId(), actualPrize.getAppId());
            assertEquals(expectedPrize.getType(), actualPrize.getType());
        }
    }

    private void assertBundles(List<Bundle> expectedBundles, List<Bundle> actualBundles) {
        for (int i = 0; i < expectedBundles.size(); i++) {
            Bundle expectedBundle = expectedBundles.get(i);
            Bundle actualBundle = actualBundles.get(i);
            assertEquals(expectedBundle.getAmount(), actualBundle.getAmount());
            assertEquals(expectedBundle.getCurrency(), actualBundle.getCurrency());
            assertEquals(expectedBundle.getImageId(), actualBundle.getImageId());
            assertEquals(expectedBundle.getPrice(), actualBundle.getPrice());
        }
    }

    private void assertPurchasedBundles(List<PurchasedBundle> expectedBundles, List<PurchasedBundle> actualBundles) {
        for (int i = 0; i < expectedBundles.size(); i++) {
            PurchasedBundle expectedBundle = expectedBundles.get(i);
            PurchasedBundle actualBundle = actualBundles.get(i);
            assertEquals(expectedBundle.getAmount(), actualBundle.getAmount());
            assertEquals(expectedBundle.getCurrency(), actualBundle.getCurrency());
            assertEquals(expectedBundle.getImageId(), actualBundle.getImageId());
            assertEquals(expectedBundle.getPrice(), actualBundle.getPrice());
            assertNotNull(actualBundle.getPurchaseDate());
            assertEquals(expectedBundle.getFirstTicketId(), actualBundle.getFirstTicketId());
        }
    }

    private void assertPurchasedBundlesResponseModel(List<PurchasedBundleResponseModel> expectedBundles,
            List<PurchasedBundleResponseModel> actualBundles) {
        for (int i = 0; i < expectedBundles.size(); i++) {
            PurchasedBundleResponseModel expectedBundle = expectedBundles.get(i);
            PurchasedBundleResponseModel actualBundle = actualBundles.get(i);
            assertEquals(expectedBundle.getAmount(), actualBundle.getAmount());
            assertEquals(expectedBundle.getCurrency(), actualBundle.getCurrency());
            assertEquals(expectedBundle.getImageId(), actualBundle.getImageId());
            assertEquals(expectedBundle.getPrice(), actualBundle.getPrice());
            assertNotNull(actualBundle.getPurchaseDate());
        }
    }

    private List<Voucher> updateVoucherTestData(String fileName) throws IOException {
        String set = config.getProperty(AerospikeConfigImpl.AEROSPIKE_VOUCHER_SET);
        List<Voucher> expectedVouchers = new ArrayList<>();
        String jsonVouchers = getPlainTextJsonFromResources(fileName);

        JsonParser parser = new JsonParser();
        JsonObject jsonObject = (JsonObject) parser.parse(jsonVouchers);

        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String key = entry.getKey();
            String binValue = entry.getValue().toString();
            aerospikeDao.createJson(set, key, VOUCHER_BIN_NAME, binValue);
            expectedVouchers.add(jsonUtil.fromJson(binValue, Voucher.class));
        }

        return expectedVouchers;
    }

    private List<TombolaDrawing> updateTombolaDrawingTestData(String fileName) throws IOException {
        String set = config.getProperty(TombolaConfig.AEROSPIKE_TOMBOLA_SET);
        return insertTombolaDrawingTestData(fileName, set, DRAWING_BIN_NAME);
    }

    private List<TombolaDrawing> insertTombolaDrawingTestData(String fileName, String set, String binName)
            throws IOException {
        List<TombolaDrawing> expectedTombolas = new ArrayList<>();
        String jsonDrawings = getPlainTextJsonFromResources(fileName);

        JsonParser parser = new JsonParser();
        JsonObject jsonObject = (JsonObject) parser.parse(jsonDrawings);

        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String key = entry.getKey();
            String binValue = entry.getValue().toString();
            aerospikeDao.createOrUpdateJson(set, key, binName, (readResult, writePolicy) -> binValue);
            expectedTombolas.add(jsonUtil.fromJson(binValue, TombolaDrawing.class));
        }
        return expectedTombolas;
    }

    @Test
    public void testTombolaDrawEngine() throws Exception {
        int numberOfUsersBuyingTickets = 10;
        int numberOfTicketsPerUser = 10;
        int initialLockCounterValue = 100;
        Tombola expectedTombola = createTombolaDrawingEngineTestData(numberOfUsersBuyingTickets,
                numberOfTicketsPerUser, "tombolaData.json", initialLockCounterValue);

        // call tombola draw
        String eventJson = testDataLoader.getTombolaDrawNotificationJson(expectedTombola.getId(), TENANT_ID);
        sendAsynMessageAsMockClient(jsonMessageUtil.fromJson(eventJson));

        waitForTombolaStatusChange(expectedTombola, TombolaStatus.ARCHIVED, DRAW_WAIT_TIME_OUT_MS);

        assertTombolaNotInAvailableTombolaLists(expectedTombola);

        // verify that drawn months list was updated for each participant
        String currMonthStr = DateTimeUtil.getCurrentDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        expectedTombolaUsers.forEach(userId -> {
            assertEquals(1, aerospikeDao.getDrawnMonthsForUser(userId).size());
            assertEquals(currMonthStr, aerospikeDao.getDrawnMonthsForUser(userId).get(0));
        });

        TombolaDrawing drawing = aerospikeDao.getTombolaDrawing(expectedTombola.getId());

        assertTombolaDrawing(expectedTombola, EXPECTED_TOTAL_TICKETS_AFTER_DRAWING,
                EXPECTED_NUMBER_OF_WINNERS_AFTER_DRAWING, drawing);

        ZonedDateTime drawDateTime = DateTimeUtil.parseISODateTimeString(drawing.getDrawDate());

        assertTombolaDrawingFromListRecord(expectedTombola);

        assertTombolaDrawingWinnersUserData(drawing, drawDateTime);

        assertTombolaDrawingLosersUserData(drawing, drawDateTime);

        int expectedPrizesMoneyCount = 1;
        int expectedPrizesBonusCount = 2;
        int expectedPrizesCreditCount = 3;
        int expectedPrizesVoucherCount = 1;
        assertTombolaDrawingWinnersPayout(drawing, expectedPrizesMoneyCount, expectedPrizesBonusCount,
                expectedPrizesCreditCount, expectedPrizesVoucherCount);

        assertEquals(0, releaseCountPerVoucherId.size());

        int expectedTotalPrizesCount = 7;
        assertAnalyticsTrackerCalls(expectedTotalPrizesCount);
    }

    @Test
    public void testTombolaDrawCall() throws Exception {
        int numberOfUsersBuyingTickets = 10;
        int numberOfTicketsPerUser = 10;
        int initialLockCounterValue = 100;
        Tombola expectedTombola = createTombolaDrawingEngineTestData(numberOfUsersBuyingTickets,
                numberOfTicketsPerUser, "tombolaData.json", initialLockCounterValue);

        verifyResponseOnRequest("TombolaDrawRequest.json", "TombolaDrawResponse.json");

        waitForTombolaStatusChange(expectedTombola, TombolaStatus.ARCHIVED, DRAW_WAIT_TIME_OUT_MS);

        assertTombolaNotInAvailableTombolaLists(expectedTombola);

        // verify that drawn months list was updated for each participant
        String currMonthStr = DateTimeUtil.getCurrentDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        expectedTombolaUsers.forEach(userId -> {
            assertEquals(1, aerospikeDao.getDrawnMonthsForUser(userId).size());
            assertEquals(currMonthStr, aerospikeDao.getDrawnMonthsForUser(userId).get(0));
        });

        TombolaDrawing drawing = aerospikeDao.getTombolaDrawing(expectedTombola.getId());

        assertTombolaDrawing(expectedTombola, EXPECTED_TOTAL_TICKETS_AFTER_DRAWING,
                EXPECTED_NUMBER_OF_WINNERS_AFTER_DRAWING, drawing);

        ZonedDateTime drawDateTime = DateTimeUtil.parseISODateTimeString(drawing.getDrawDate());

        assertTombolaDrawingFromListRecord(expectedTombola);

        assertTombolaDrawingWinnersUserData(drawing, drawDateTime);

        assertTombolaDrawingLosersUserData(drawing, drawDateTime);

        int expectedPrizesMoneyCount = 1;
        int expectedPrizesBonusCount = 2;
        int expectedPrizesCreditCount = 3;
        int expectedPrizesVoucherCount = 1;
        assertTombolaDrawingWinnersPayout(drawing, expectedPrizesMoneyCount, expectedPrizesBonusCount,
                expectedPrizesCreditCount, expectedPrizesVoucherCount);

        assertEquals(0, releaseCountPerVoucherId.size());

        int expectedTotalPrizesCount = 7;
        assertAnalyticsTrackerCalls(expectedTotalPrizesCount);
    }

    private void assertAnalyticsTrackerCalls(int expectedTotalPrizesCount) {
        verify(tracker, timeout(RetriedAssert.DEFAULT_TIMEOUT_MS).times(expectedTotalPrizesCount))
                        .addEvent(clientInfoArgumentCaptor.capture(), rewardEventArgumentCaptor.capture());
        assertEquals(expectedTotalPrizesCount, clientInfoArgumentCaptor.getAllValues().size());
        clientInfoArgumentCaptor.getAllValues().forEach(clientInfo ->
                assertEquals(TENANT_ID,clientInfo.getTenantId()));
    }

    @Test
    public void testTombolaDrawEngineNoTicketsBought() throws Exception {
        int numberOfUsersBuyingTickets = 0;
        int numberOfTicketsPerUser = 0;
        int initialLockCounterValue = 0;
        Tombola expectedTombola = createTombolaDrawingEngineTestData(numberOfUsersBuyingTickets,
                numberOfTicketsPerUser, "tombolaData.json", initialLockCounterValue);

        // call tombola draw
        String eventJson = testDataLoader.getTombolaDrawNotificationJson(expectedTombola.getId(), TENANT_ID);
        sendAsynMessageAsMockClient(jsonMessageUtil.fromJson(eventJson));

        waitForTombolaStatusChange(expectedTombola, TombolaStatus.ARCHIVED, DRAW_WAIT_TIME_OUT_MS);

        assertTombolaNotInAvailableTombolaLists(expectedTombola);

        TombolaDrawing drawing = aerospikeDao.getTombolaDrawing(expectedTombola.getId());

        int expectedTotalTicketsAfterDrawing = 0;
        int expectedNumberOfWinnersAfterDrawing = 0;
        assertTombolaDrawing(expectedTombola, expectedTotalTicketsAfterDrawing, expectedNumberOfWinnersAfterDrawing,
                drawing);

        assertTombolaDrawingFromListRecord(expectedTombola);

        int expectedPrizesMoneyCount = 0;
        int expectedPrizesBonusCount = 0;
        int expectedPrizesCreditCount = 0;
        int expectedPrizesVoucherCount = 0;
        assertTombolaDrawingWinnersPayout(drawing, expectedPrizesMoneyCount, expectedPrizesBonusCount,
                expectedPrizesCreditCount, expectedPrizesVoucherCount);

        assertEquals(Integer.valueOf(1), releaseCountPerVoucherId.get(VOUCHER_ID));
    }

    @Test
    public void testTombolaDrawEngineMorePrizesThanTickets() throws Exception {
        int numberOfUsersBuyingTickets = 10;
        int numberOfTicketsPerUser = 1;
        int initialLockCounterValue = 10;
        Tombola expectedTombola = createTombolaDrawingEngineTestData(numberOfUsersBuyingTickets,
                numberOfTicketsPerUser, "tombolaDataMorePrizesThanTickets.json", initialLockCounterValue);

        // call tombola draw
        String eventJson = testDataLoader.getTombolaDrawNotificationJson(expectedTombola.getId(), TENANT_ID);
        sendAsynMessageAsMockClient(jsonMessageUtil.fromJson(eventJson));

        waitForTombolaStatusChange(expectedTombola, TombolaStatus.ARCHIVED, DRAW_WAIT_TIME_OUT_MS);

        assertTombolaNotInAvailableTombolaLists(expectedTombola);

        // verify that drawn months list was updated for each participant
        String currMonthStr = DateTimeUtil.getCurrentDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        expectedTombolaUsers.forEach(userId -> {
            assertEquals(1, aerospikeDao.getDrawnMonthsForUser(userId).size());
            assertEquals(currMonthStr, aerospikeDao.getDrawnMonthsForUser(userId).get(0));
        });

        TombolaDrawing drawing = aerospikeDao.getTombolaDrawing(expectedTombola.getId());

        int expectedTotalTicketsAfterDrawing = 10;
        int expectedNumberOfWinnersAfterDrawing = 10;
        assertTombolaDrawing(expectedTombola, expectedTotalTicketsAfterDrawing,
                expectedNumberOfWinnersAfterDrawing, drawing);

        ZonedDateTime drawDateTime = DateTimeUtil.parseISODateTimeString(drawing.getDrawDate());

        assertTombolaDrawingFromListRecord(expectedTombola);

        assertTombolaDrawingWinnersUserData(drawing, drawDateTime);

        int expectedTotalPrizesCount = 10;
        assertTombolaDrawingWinnersPayout(drawing, expectedTotalPrizesCount);

        assertEquals(Integer.valueOf(1), releaseCountPerVoucherId.get(VOUCHER_ID));

        assertAnalyticsTrackerCalls(expectedTotalPrizesCount);
    }

    @Test
    public void testTombolaDrawEngineMoreConsolationPrizesThanTickets() throws Exception {
        int numberOfUsersBuyingTickets = 10;
        int numberOfTicketsPerUser = 1;
        int initialLockCounterValue = 10;
        Tombola expectedTombola = createTombolaDrawingEngineTestData(numberOfUsersBuyingTickets,
                numberOfTicketsPerUser, "tombolaDataMoreConsolationPrizesThanTickets.json", initialLockCounterValue);

        // call tombola draw
        String eventJson = testDataLoader.getTombolaDrawNotificationJson(expectedTombola.getId(), TENANT_ID);
        sendAsynMessageAsMockClient(jsonMessageUtil.fromJson(eventJson));

        waitForTombolaStatusChange(expectedTombola, TombolaStatus.ARCHIVED, DRAW_WAIT_TIME_OUT_MS);

        assertTombolaNotInAvailableTombolaLists(expectedTombola);

        // verify that drawn months list was updated for each participant
        String currMonthStr = DateTimeUtil.getCurrentDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        expectedTombolaUsers.forEach(userId -> {
            assertEquals(1, aerospikeDao.getDrawnMonthsForUser(userId).size());
            assertEquals(currMonthStr, aerospikeDao.getDrawnMonthsForUser(userId).get(0));
        });

        TombolaDrawing drawing = aerospikeDao.getTombolaDrawing(expectedTombola.getId());

        int expectedTotalTicketsAfterDrawing = 10;
        int expectedNumberOfWinnersAfterDrawing = 10;
        assertTombolaDrawing(expectedTombola, expectedTotalTicketsAfterDrawing,
                expectedNumberOfWinnersAfterDrawing, drawing);

        ZonedDateTime drawDateTime = DateTimeUtil.parseISODateTimeString(drawing.getDrawDate());

        assertTombolaDrawingFromListRecord(expectedTombola);

        assertTombolaDrawingWinnersUserData(drawing, drawDateTime);

        int expectedTotalPrizesCount = 10;
        assertTombolaDrawingWinnersPayout(drawing, expectedTotalPrizesCount);

        RetriedAssert.assertWithWait(() -> assertEquals(Integer.valueOf(6), releaseCountPerVoucherId.get(VOUCHER_ID)));

        assertAnalyticsTrackerCalls(expectedTotalPrizesCount);
    }

    @Test
    public void testTombolaDrawEngineWithFailedPayoutTransactions() throws Exception {
        creditPayoutShouldFail = true;
        voucherPayoutShouldFail = true;
        int expectedEmailCount = 5;

        int numberOfUsersBuyingTickets = 10;
        int numberOfTicketsPerUser = 10;
        int initialLockCounterValue = 100;
        Tombola expectedTombola = createTombolaDrawingEngineTestData(numberOfUsersBuyingTickets,
                numberOfTicketsPerUser, "tombolaDataFailedPayoutTransactions.json", initialLockCounterValue);

        // call tombola draw
        String eventJson = testDataLoader.getTombolaDrawNotificationJson(expectedTombola.getId(), TENANT_ID);
        sendAsynMessageAsMockClient(jsonMessageUtil.fromJson(eventJson));

        waitForTombolaStatusChange(expectedTombola, TombolaStatus.ARCHIVED, DRAW_WAIT_TIME_OUT_MS);

        assertTombolaNotInAvailableTombolaLists(expectedTombola);

        // verify that drawn months list was updated for each participant
        String currMonthStr = DateTimeUtil.getCurrentDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        expectedTombolaUsers.forEach(userId -> {
            assertEquals(1, aerospikeDao.getDrawnMonthsForUser(userId).size());
            assertEquals(currMonthStr, aerospikeDao.getDrawnMonthsForUser(userId).get(0));
        });

        TombolaDrawing drawing = aerospikeDao.getTombolaDrawing(expectedTombola.getId());

        int expectedNumberOfWinnersAfterDrawing = 8;
        assertTombolaDrawing(expectedTombola, EXPECTED_TOTAL_TICKETS_AFTER_DRAWING,
                expectedNumberOfWinnersAfterDrawing, drawing);

        ZonedDateTime drawDateTime = DateTimeUtil.parseISODateTimeString(drawing.getDrawDate());

        assertTombolaDrawingFromListRecord(expectedTombola);

        assertTombolaDrawingWinnersUserData(drawing, drawDateTime);

        assertTombolaDrawingLosersUserData(drawing, drawDateTime);

        int expectedPrizesMoneyCount = 1;
        int expectedPrizesBonusCount = 2;
        int expectedPrizesCreditCount = 0;
        int expectedPrizesVoucherCount = 0;
        assertTombolaDrawingWinnersPayout(drawing, expectedPrizesMoneyCount, expectedPrizesBonusCount,
                expectedPrizesCreditCount, expectedPrizesVoucherCount);

        // verify that exactly one error mail was sent to the admin and clear the collected error
        RetriedAssert.assertWithWait(() -> assertEquals(expectedEmailCount, emailCount));
        assertAdminEmails();
        errorCollector.clearCollectedErrors();

        assertEquals(Integer.valueOf(1), releaseCountPerVoucherId.get(VOUCHER_ID));
        assertEquals(Integer.valueOf(3), releaseCountPerVoucherId.get(VOUCHER_ID_2));

        int expectedTotalPrizesCount = 3;
        assertAnalyticsTrackerCalls(expectedTotalPrizesCount);
    }

    @Test
    public void testTombolaDrawEngineDrawStartedByBuyingAllTickets() throws Exception {
        int numberOfUsersBuyingTickets = 10;
        int numberOfTicketsPerUser = 10;
        int initialLockCounterValue = 100;
        createTombolaDrawingEngineTestData(numberOfUsersBuyingTickets,
                numberOfTicketsPerUser, "tombolaDataDrawByBuyingAllTickets.json", initialLockCounterValue);

        String requestJson = getPlainTextJsonFromResources("TombolaBuyRequest.json", ANONYMOUS_CLIENT_INFO);
        requestJson = requestJson.replaceFirst("<<TOMBOLAID>>", DEFAULT_TEST_TOMBOLA_ID);

        receivedMessageCollector.getReceivedMessageList().clear();

        final EmptyJsonMessageContent response = getTombolaBuyResponse(requestJson);
        assertNotNull(response);

        checkCounterTestData(110);

        RetriedAssert.assertWithWait(() -> assertEquals(1, tombolaDrawEventReceivedCount));
    }

    private void assertAdminEmails() {
        final int[] voucherId1count = {0};
        final int[] voucherId2count = {0};
        final int[] creditCount = {0};

        receivedEmailsContent.forEach(sendEmailWrapperRequest -> {
            assertEquals(config.getProperty(TombolaConfig.ADMIN_EMAIL), sendEmailWrapperRequest.getAddress());
            assertEquals(Messages.TOMBOLA_PRIZE_PAYOUT_ERROR_TO_ADMIN_SUBJECT, sendEmailWrapperRequest.getSubject());
            assertEquals(Messages.TOMBOLA_PRIZE_PAYOUT_ERROR_TO_ADMIN_CONTENT, sendEmailWrapperRequest.getMessage());
            if (Currency.CREDIT.toString().equals(sendEmailWrapperRequest.getParameters()[4])) {
                assertEquals(PaymentServiceExceptionCodes.ERR_INSUFFICIENT_FUNDS,
                        sendEmailWrapperRequest.getParameters()[0]);
                creditCount[0]++;
            } else if (PrizeType.VOUCHER.toString().equals(sendEmailWrapperRequest.getParameters()[4])) {
                assertEquals(ExceptionCodes.ERR_ENTRY_NOT_FOUND, sendEmailWrapperRequest.getParameters()[0]);
                if (VOUCHER_ID.equals(sendEmailWrapperRequest.getParameters()[7])) {
                    voucherId1count[0]++;
                }
                if (VOUCHER_ID_2.equals(sendEmailWrapperRequest.getParameters()[7])) {
                    voucherId2count[0]++;
                }
            }
        });
        assertEquals(voucherId1count[0], 1);
        assertEquals(voucherId2count[0], 3);
        assertEquals(creditCount[0], 1);
    }

    private void assertTombolaDrawingWinnersPayout(TombolaDrawing drawing, int expectedPrizesMoneyCount,
            int expectedPrizesBonusCount, int expectedPrizesCreditCount, int expectedPrizesVoucherCount) {
        // wait for all calls to finish
        RetriedAssert.assertWithWait(() -> assertEquals(expectedPrizesMoneyCount, payoutPrizesMoneyCount));
        RetriedAssert.assertWithWait(() -> assertEquals(expectedPrizesBonusCount, payoutPrizesBonusCount));
        RetriedAssert.assertWithWait(() -> assertEquals(expectedPrizesCreditCount, payoutPrizesCreditCount));
        RetriedAssert.assertWithWait(() -> assertEquals(expectedPrizesVoucherCount, payoutPrizesVoucherCount));

        assertPayoutCalloutsContents(drawing);
    }

    private void assertPayoutCalloutsContents(TombolaDrawing drawing) {
        drawing.getWinners().forEach(tombolaWinner -> {
            if (tombolaWinner.getType() == PrizeType.VOUCHER && voucherPayoutShouldFail) {
                assertFalse(payoutPrizes.contains(new PayoutPrize(tombolaWinner.getType(), tombolaWinner.getVoucherId(),
                        tombolaWinner.getAmount(), tombolaWinner.getUser().getUserId())));
            } else if (tombolaWinner.getType() == PrizeType.CREDIT && creditPayoutShouldFail) {
                assertFalse(payoutPrizes.contains(new PayoutPrize(tombolaWinner.getType(), tombolaWinner.getVoucherId(),
                        tombolaWinner.getAmount(), tombolaWinner.getUser().getUserId())));
            } else {
                assertTrue(payoutPrizes.contains(new PayoutPrize(tombolaWinner.getType(), tombolaWinner.getVoucherId(),
                        tombolaWinner.getAmount(), tombolaWinner.getUser().getUserId())));
            }
        });
    }

    private void assertTombolaDrawingWinnersPayout(TombolaDrawing drawing, int expectedTotalPrizesCount) {
        // wait for all calls to finish
        RetriedAssert.assertWithWait(() -> assertEquals(expectedTotalPrizesCount,
                payoutPrizesMoneyCount + payoutPrizesBonusCount + payoutPrizesCreditCount + payoutPrizesVoucherCount));

        assertPayoutCalloutsContents(drawing);
    }

    @Test
    public void testMoveTombolas() throws Exception {
        String sourceUserId = "sourceUserId";
        String targetUserId = "targetUserId";
        Tombola expectedTombola = createTombolaMoveTestData(sourceUserId, targetUserId);

        // Perform move
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(),
                getPlainTextJsonFromResources("moveTombolasRequest.json")
                .replaceAll("<<sourceUserId>>", sourceUserId)
                .replaceAll("<<targetUserId>>", targetUserId));
        RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));

        // check that there is no information for source user available
        assertSourceUserInfo(sourceUserId, expectedTombola.getId());

        // check that target user has merged info available
        assertTargetUserInfo(targetUserId, expectedTombola.getId());
    }

    private void assertTargetUserInfo(String targetUserId, String tombolaId) throws IOException {
	    UserTombolaInfo expectedUserTombolaInfo = jsonUtil.fromJson(getPlainTextJsonFromResources(
	            "userTombolaDataMerged.json"), UserTombolaInfo.class);
	    assertUserTombolaEquals(expectedUserTombolaInfo, aerospikeDao.getUserTombola(targetUserId, tombolaId));
	    assertUserTombolaEquals(expectedUserTombolaInfo, aerospikeDao.getUserTombolaHistoryForMonth(targetUserId,
                "2016-01").get(0));

	    assertEquals(2, aerospikeDao.getUserTombolaHistoryForPending(targetUserId).size());
        UserTombolaInfo expectedPendingUserTombolaInfo = jsonUtil.fromJson(getPlainTextJsonFromResources(
                "userPendingTombolaDataMerged.json"), UserTombolaInfo.class);
        assertUserTombolaEquals(expectedPendingUserTombolaInfo,
                aerospikeDao.getUserTombolaHistoryForPending(targetUserId).get(1));

        assertEquals(2, aerospikeDao.getDrawnMonthsForUser(targetUserId).size());
	    assertEquals("2016-01", aerospikeDao.getDrawnMonthsForUser(targetUserId).get(0));
	    assertEquals("2017-01", aerospikeDao.getDrawnMonthsForUser(targetUserId).get(1));

	    // check that source user reference was replaced in tombola drawing
        assertEquals(3, aerospikeDao.getTombolaDrawing(tombolaId).getWinners().stream()
                .filter(winner -> winner.getUser().getUserId().equals(targetUserId)).count());

        // check that user statistics were merged
        assertEquals(2, aerospikeDao.getUserStatisticsAppIds(targetUserId).size());
        assertEquals(APP_ID, aerospikeDao.getUserStatisticsAppIds(targetUserId).get(0));
        assertEquals("another one", aerospikeDao.getUserStatisticsAppIds(targetUserId).get(1));
        assertEquals(2, aerospikeDao.getUserStatisticsTenantIds(targetUserId).size());
        assertEquals(TENANT_ID, aerospikeDao.getUserStatisticsTenantIds(targetUserId).get(0));
        assertEquals("another one", aerospikeDao.getUserStatisticsTenantIds(targetUserId).get(1));
        UserStatistics expectedUserStatistics = jsonUtil.fromJson(getPlainTextJsonFromResources(
                "userTombolaStatisticsDataMerged.json"), UserStatistics.class);
        assertUserStatistics(expectedUserStatistics, aerospikeDao.getUserStatisticsByTenantId(targetUserId, TENANT_ID));
        assertUserStatistics(expectedUserStatistics, aerospikeDao.getUserStatisticsByAppId(targetUserId, APP_ID));
    }

    private void assertUserStatistics(UserStatistics expected, UserStatistics actual) {
        assertEquals(expected.getBoughtTicketsNumber(), actual.getBoughtTicketsNumber());
        assertEquals(expected.getTicketsBoughtSinceLastWin(), actual.getTicketsBoughtSinceLastWin());
        assertEquals(expected.getWinningTicketsNumber(), actual.getWinningTicketsNumber());
        assertEquals(expected.getLastWinDate(), actual.getLastWinDate());
    }

    private void assertSourceUserInfo(String sourceUserId, String  tombolaId) {
        assertNull(aerospikeDao.getUserTombola(sourceUserId, tombolaId));
        assertTrue(aerospikeDao.getUserTombolaHistoryForMonth(sourceUserId, "2017-01").isEmpty());
        assertTrue(aerospikeDao.getUserTombolaHistoryForPending(sourceUserId).isEmpty());
        assertNull(aerospikeDao.getDrawnMonthsForUser(sourceUserId));
        assertNull(aerospikeDao.getUserStatisticsAppIds(sourceUserId));
        assertNull(aerospikeDao.getUserStatisticsTenantIds(sourceUserId));
        assertFalse(aerospikeDao.getTombolaDrawing(tombolaId).getWinners().stream()
                .anyMatch(winner -> winner.getUser().getUserId().equals(sourceUserId)));
    }

    @Test
    public void testTombolaOpenCheckoutEvent() throws IOException {
        List<Tombola> expectedTombolas = updateTombolaTestData("tombolaOpenCheckoutData.json");
        Tombola expectedTombola = expectedTombolas.get(0);
        assertEquals(TombolaStatus.INACTIVE, expectedTombola.getStatus());

        // call tombola open checkout event
        String eventJson = testDataLoader.getTombolaOpenCheckoutNotificationJson(expectedTombola.getId());
        sendAsynMessageAsMockClient(jsonMessageUtil.fromJson(eventJson));

        RetriedAssert.assertWithWait(() ->
                assertEquals(1, aerospikeDao.getAvailableTombolaList(APP_ID).size()));
        RetriedAssert.assertWithWait(() ->
                assertEquals(1, aerospikeDao.getAvailableTombolaList("asd1233").size()));
        RetriedAssert.assertWithWait(() ->
                assertEquals(1, aerospikeDao.getAvailableTombolaList("dsf23").size()));

        expectedTombola.getApplicationsIds().forEach(appId -> {
            List<Tombola> availableTombolas = aerospikeDao.getAvailableTombolaList(appId);
            assertEquals(expectedTombola.getId(), availableTombolas.get(0).getId());
            assertEquals(TombolaStatus.ACTIVE, availableTombolas.get(0).getStatus());
        } );

        assertEquals(TombolaStatus.ACTIVE, aerospikeDao.getTombola(expectedTombola.getId()).getStatus());
    }

    @Test
    public void testTombolaOpenCheckoutCall() throws Exception {
        List<Tombola> expectedTombolas = updateTombolaTestData("tombolaOpenCheckoutData.json");
        Tombola expectedTombola = expectedTombolas.get(0);
        assertEquals(TombolaStatus.INACTIVE, expectedTombola.getStatus());

        verifyResponseOnRequest("TombolaOpenCheckoutRequest.json", "TombolaOpenCheckoutResponse.json");

        RetriedAssert.assertWithWait(() ->
                assertEquals(1, aerospikeDao.getAvailableTombolaList(APP_ID).size()));
        RetriedAssert.assertWithWait(() ->
                assertEquals(1, aerospikeDao.getAvailableTombolaList("asd1233").size()));
        RetriedAssert.assertWithWait(() ->
                assertEquals(1, aerospikeDao.getAvailableTombolaList("dsf23").size()));

        expectedTombola.getApplicationsIds().forEach(appId -> {
            List<Tombola> availableTombolas = aerospikeDao.getAvailableTombolaList(appId);
            assertEquals(expectedTombola.getId(), availableTombolas.get(0).getId());
            assertEquals(TombolaStatus.ACTIVE, availableTombolas.get(0).getStatus());
        } );

        assertEquals(TombolaStatus.ACTIVE, aerospikeDao.getTombola(expectedTombola.getId()).getStatus());
    }

    @Test
    public void testTombolaCloseCheckoutEvent() throws IOException {
        List<Tombola> expectedTombolas = updateTombolaTestData("tombolaData.json");
        updateTombolaListTestData("tombolaListData.json");
        Tombola expectedTombola = expectedTombolas.get(0);
        assertEquals(TombolaStatus.ACTIVE, expectedTombola.getStatus());
        assertEquals(TombolaStatus.ACTIVE, aerospikeDao.getAvailableTombolaList(APP_ID).get(0).getStatus());

        // call tombola open checkout event
        String eventJson = testDataLoader.getTombolaCloseCheckoutNotificationJson(expectedTombola.getId());
        sendAsynMessageAsMockClient(jsonMessageUtil.fromJson(eventJson));

        RetriedAssert.assertWithWait(() ->
                assertEquals(TombolaStatus.EXPIRED, aerospikeDao.getAvailableTombolaList(APP_ID).get(0).getStatus()));

        assertEquals(TombolaStatus.EXPIRED, aerospikeDao.getTombola(expectedTombola.getId()).getStatus());
    }

    @Test
    public void testTombolaCloseCheckoutCall() throws Exception {
        List<Tombola> expectedTombolas = updateTombolaTestData("tombolaData.json");
        updateTombolaListTestData("tombolaListData.json");
        Tombola expectedTombola = expectedTombolas.get(0);
        assertEquals(TombolaStatus.ACTIVE, expectedTombola.getStatus());
        assertEquals(TombolaStatus.ACTIVE, aerospikeDao.getAvailableTombolaList(APP_ID).get(0).getStatus());

        verifyResponseOnRequest("TombolaCloseCheckoutRequest.json", "TombolaCloseCheckoutResponse.json");

        RetriedAssert.assertWithWait(() ->
                assertEquals(TombolaStatus.EXPIRED, aerospikeDao.getAvailableTombolaList(APP_ID).get(0).getStatus()));

        assertEquals(TombolaStatus.EXPIRED, aerospikeDao.getTombola(expectedTombola.getId()).getStatus());
    }

    private void assertTombolaDrawingLosersUserData(TombolaDrawing drawing, ZonedDateTime drawDateTime) {
        Set<String> alreadyHandledPlayers = new HashSet<>();
        List<String> drawingWinnerUserIdList = drawing.getWinners().stream().map(w -> w.getUser().getUserId())
                .collect(Collectors.toList());
        IntStream.range(0, aerospikeDao.getPurchasedTicketsNumber(drawing.getTombolaId()))
                .forEach(ticketId -> {
                    String playerId = aerospikeDao.getTombolaTicket(drawing.getTombolaId(),
                            Integer.toString(ticketId)).getUserId();
                    if (!alreadyHandledPlayers.contains(playerId)) {
                        if (!drawingWinnerUserIdList.contains(playerId)) {
                            UserTombolaInfo userTombolaInfo = verifyAndGetUserTombolaInfoForLoser(drawing, playerId);
                            verifyLoserStatistics(drawing, playerId, userTombolaInfo);

                            // - verify that user details for tombola in list are moved from pending
                            // to month based record and contents are ok
                            checkUserHistoryData(playerId, drawing.getTombolaId(), drawDateTime, userTombolaInfo);
                        }
                        alreadyHandledPlayers.add(playerId);
                    }
                });
    }

    private void assertTombolaDrawingWinnersUserData(TombolaDrawing drawing, ZonedDateTime drawDateTime) {
        drawing.getWinners().forEach(winner -> {
            UserTombolaInfo userTombolaInfo = verifyAndGetUserTombolaInfoForWinner(drawing, winner);
            verifyWinnerStatistics(drawing, winner, userTombolaInfo);

            // - verify that user details for tombola in list are moved from pending
            // to month based record and contents are ok
            checkUserHistoryData(winner.getUser().getUserId(), drawing.getTombolaId(), drawDateTime, userTombolaInfo);
        });
    }

    private void assertTombolaDrawingFromListRecord(Tombola expectedTombola) {
        ZonedDateTime dateFrom = DateTimeUtil.getCurrentDateTime().minusDays(1);
        ZonedDateTime dateTo = DateTimeUtil.getCurrentDateTime().plusDays(1);
        List<TombolaDrawing> tombolaDrawings = aerospikeDao.getTombolaDrawings(APP_ID, 0,
                TombolaDrawingListRequest.MAX_LIST_LIMIT, dateFrom, dateTo).getItems();
        assertTrue(tombolaDrawings.stream().anyMatch(t -> t.getTombolaId().equals(expectedTombola.getId())));
    }

    private void assertTombolaDrawing(Tombola expectedTombola, int expectedTotalTicketsAfterDrawing,
            int expectedNumberOfWinnersAfterDrawing, TombolaDrawing drawing) {
        assertNotNull(drawing);
        assertEquals(expectedTombola.getId(), drawing.getTombolaId());
        assertEquals(expectedTombola.getName(), drawing.getName());
        assertEquals(expectedTombola.getImageId(), drawing.getImageId());
        ZonedDateTime currentDateTime = DateTimeUtil.parseISODateTimeString(
                DateTimeUtil.getCurrentTimestampInISOFormat());
        ZonedDateTime drawDateTime = DateTimeUtil.parseISODateTimeString(drawing.getDrawDate());
        assertTrue(drawDateTime.isBefore(currentDateTime.plusHours(1)) &&
                drawDateTime.isAfter(currentDateTime.minusHours(1)));
        assertEquals(expectedTotalTicketsAfterDrawing, drawing.getTotalTicketsNumber());
        assertEquals(expectedNumberOfWinnersAfterDrawing, drawing.getWinners().size());
    }

    private Tombola createTombolaDrawingEngineTestData(int numberOfUsersBuyingTickets, int numberOfTicketsPerUser,
            String tombolaDataFileName, int initialLockCounterValue) throws IOException {
        // we need a tombola
        List<Tombola> expectedTombolas = updateTombolaTestData(tombolaDataFileName);
        updateTombolaListTestData("tombolaListData.json");
        // we need some bought tickets preferably by different users
        writeCounterTestData(0, initialLockCounterValue, "1");
        Tombola expectedTombola = expectedTombolas.get(0);
        buyTickets(expectedTombola, numberOfUsersBuyingTickets, numberOfTicketsPerUser);
        return expectedTombola;
    }

    private Tombola createTombolaMoveTestData(String sourceUserId, String targetUserId) throws IOException {
    	// we need profiles
    	addProfile(sourceUserId);
    	addProfile(targetUserId);
    	
        // we need a tombola
        List<Tombola> expectedTombolas = updateTombolaTestData("tombolaDataToMerge.json");
        assertEquals(4, expectedTombolas.size());
        Tombola expectedTombola = expectedTombolas.get(0);

        // we need some bought tickets for both users
        writeCounterTestData(0, 500, "1");
        writeCounterTestData(0, 500, "2");
        writeCounterTestData(0, 500, "3");
        writeCounterTestData(0, 500, "4");

        // mock user tombola statistics data
        updateUserTombolaStatisticsData();

        // buy tickets to tombolas for both source and target
        buyTicketsForUsers(sourceUserId, targetUserId, expectedTombolas);
        aerospikeDao.deleteUserTombolaHistoryItemFromPending(sourceUserId, "1");
        aerospikeDao.deleteUserTombolaHistoryItemFromPending(sourceUserId, "2");
        aerospikeDao.deleteUserTombolaHistoryItemFromPending(targetUserId, "1");
        aerospikeDao.deleteUserTombolaHistoryItemFromPending(targetUserId, "2");

        // mock a tombola drawing
        updateTombolaDrawingTestData("tombolaDrawingDataToMerge.json");
        updateTombolaDrawingListTestData("tombolaDrawingListDataToMerge.json");

        // mock userTombola data
        updateUserTombolaTestData("userTombolaDataToMerge.json");

        // mock userTombola history data
        updateUserTombolaListTestData("userTombolaListDataToMerge.json");
        updateUserTombolaListTestData("userTombolaPendingListData.json");
        updateUserTombolaDrawnMonthsTestData();

        return expectedTombola;
    }

    private void updateUserTombolaStatisticsData() throws IOException {
        String set = config.getProperty(TombolaConfig.AEROSPIKE_USER_TOMBOLA_SET);
        insertUserTombolaTestDataForBin("userTombolaStatisticsData.json", set, STATISTICS_BIN_NAME);
    }

    private void insertUserTombolaTestDataForBin(String fileName, String set, String binName) throws IOException {
        String userTombolaInfos = getPlainTextJsonFromResources(fileName);

        JsonParser parser = new JsonParser();
        JsonObject jsonObject = (JsonObject) parser.parse(userTombolaInfos);

        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String key = entry.getKey();
            if (entry.getValue().isJsonArray()) {
                List<Object> binValue = jsonUtil.getEntityListFromJsonString(entry.getValue().toString(),
                        new TypeToken<List<Object>>() {}.getType() );
                aerospikeDao.createOrUpdateList(set, key, binName, (readResult, writePolicy) -> binValue);
            } else {
                String binValue = entry.getValue().toString();
                aerospikeDao.createJson(set, key, binName, binValue);
            }
        }
    }

    private void buyTicketsForUsers(String sourceUserId, String targetUserId, List<Tombola> expectedTombolas) {
	    for (Tombola expectedTombola : expectedTombolas) {
            aerospikeDao.buyTickets(sourceUserId, expectedTombola.getName(), expectedTombola.getId(),
                    expectedTombola.getTargetDate(),
                    expectedTombola.getTotalTicketsAmount(), 100, BigDecimal.valueOf(100), Currency.MONEY,
                    "bundleImg", APP_ID, TENANT_ID);
            aerospikeDao.buyTickets(targetUserId, expectedTombola.getName(), expectedTombola.getId(),
                    expectedTombola.getTargetDate(),
                    expectedTombola.getTotalTicketsAmount(), 50, BigDecimal.valueOf(100), Currency.MONEY,
                    "bundleImg", APP_ID, TENANT_ID);
        }
    }

    private void assertTombolaNotInAvailableTombolaLists(Tombola expectedTombola) {
        // verify that tombola is removed from active tombola list
        expectedTombola.getApplicationsIds().forEach(appId -> {
            List<Tombola> availableTombolas = aerospikeDao.getAvailableTombolaList(appId);
            assertFalse(availableTombolas.stream().anyMatch(t -> t.getId().equals(expectedTombola.getId())));
        } );
    }

    private void waitForTombolaStatusChange(Tombola expectedTombola, TombolaStatus status, int waitTimeOutMs) {
		RetriedAssert.assertWithWait(
				() -> assertEquals(status, aerospikeDao.getTombola(expectedTombola.getId()).getStatus()),
				waitTimeOutMs);
    }

    private void verifyLoserStatistics(TombolaDrawing drawing, String userId, UserTombolaInfo userTombolaInfo) {
        UserStatistics userStatistics = aerospikeDao.getUserStatisticsByTenantId(userId, TENANT_ID);
        assertNotEquals(drawing.getDrawDate(), userStatistics.getLastWinDate());
        assertEquals(userTombolaInfo.getPrizes().size(), userStatistics.getWinningTicketsNumber());
        assertTrue(userStatistics.getBoughtTicketsNumber() > 0);
        assertTrue(userStatistics.getTicketsBoughtSinceLastWin() > 0);
    }

    private UserTombolaInfo verifyAndGetUserTombolaInfoForLoser(TombolaDrawing drawing, String userId) {
        UserTombolaInfo userTombolaInfo = aerospikeDao.getUserTombola(userId, drawing.getTombolaId());
        assertEquals(0, userTombolaInfo.getTotalPrizesWon());
        assertTrue(userTombolaInfo.getPrizes().isEmpty());
        return userTombolaInfo;
    }

    private void verifyWinnerStatistics(TombolaDrawing drawing, TombolaWinner winner, UserTombolaInfo userTombolaInfo) {
        UserStatistics userStatistics = aerospikeDao.getUserStatisticsByTenantId(winner.getUser().getUserId(), TENANT_ID);
        assertEquals(drawing.getDrawDate(), userStatistics.getLastWinDate());
        assertEquals(userTombolaInfo.getPrizes().size(), userStatistics.getWinningTicketsNumber());
        assertEquals(0, userStatistics.getTicketsBoughtSinceLastWin());
    }

    private UserTombolaInfo verifyAndGetUserTombolaInfoForWinner(TombolaDrawing drawing, TombolaWinner winner) {
        UserTombolaInfo userTombolaInfo = aerospikeDao.getUserTombola(winner.getUser().getUserId(), drawing.getTombolaId());
        assertTrue(userTombolaInfo.getPrizes().stream().map(TombolaWinner::getPrizeId).collect(Collectors.toList())
                .contains(winner.getPrizeId()));
        assertTrue(userTombolaInfo.getTotalPrizesWon() > 0);
        return userTombolaInfo;
    }

    private void checkUserHistoryData(String userId, String tombolaId, ZonedDateTime drawDate,
            UserTombolaInfo expectedUserTombolaInfo) {
        // verify that user tombola is removed from pending list
        List<UserTombolaInfo> pendingTombolas = aerospikeDao.getUserTombolaHistoryForPending(userId);
        assertFalse(pendingTombolas.stream().anyMatch(t -> t.getTombolaId().equals(tombolaId)));

        // verify that user tombola is available in month based records and that its content is as expected
        List<UserTombolaInfoResponseModel> userTombolaInfos = aerospikeDao.getUserTombolaList(userId, 0, 1,
                drawDate.minusHours(1), drawDate.plusHours(1), false).getItems();
        assertEquals(1, userTombolaInfos.size());
        assertFalse(userTombolaInfos.get(0).isPending());
        assertUserTombolaResponseModelEquals(new UserTombolaInfoResponseModel(expectedUserTombolaInfo),
                userTombolaInfos.get(0));
    }

    private void buyTickets(Tombola tombola, int numberOfUsersBuyingTickets, int numberOfTicketsPerUser) {
	    expectedTombolaUsers = new ArrayList<>();
        for (int i = 0; i < numberOfUsersBuyingTickets; i++) {
            String userId = "user" + i;
            aerospikeDao.buyTickets(userId, tombola.getName(), tombola.getId(), tombola.getTargetDate(),
                    tombola.getTotalTicketsAmount(), numberOfTicketsPerUser, BigDecimal.valueOf(100), Currency.MONEY,
                    "bundleImg", APP_ID, TENANT_ID);
            expectedTombolaUsers.add(userId);
            addProfile(userId);
        }
    }

    private void addProfile(String userId) {
		Profile profile = new Profile();
		profile.setUserId(userId);
		String set = config.getProperty(AerospikeConfigImpl.AEROSPIKE_PROFILE_SET);
		String key = profilePrimaryKeyUtil.createPrimaryKey(userId);
		aerospikeDao.createJson(set, key, CommonProfileAerospikeDao.BLOB_BIN_NAME, profile.getAsString());
	}

	@Test
    public void testGetTombolaDrawDetails() throws Exception {
        // add test data
        List<TombolaDrawing> expectedTombolas = updateTombolaDrawingTestData("tombolaDrawingData.json");
        List<Voucher> expectedVouchers = updateVoucherTestData("voucherTestData.json");
        assertEquals(1, expectedTombolas.size());

        String requestJson = getPlainTextJsonFromResources("TombolaDrawingGetRequest.json");
        receivedMessageCollector.getReceivedMessageList().clear();

        final TombolaDrawingGetResponse response = getTombolaDrawingGetResponse(requestJson);
        assertNotNull(response.getDrawing());
        TombolaDrawing drawing = response.getDrawing();
        assertTombolaDrawingEquals(expectedTombolas.get(0), drawing);
        assertTombolaDrawingVoucherEquals(expectedVouchers.get(0), drawing);
    }

    @Test
    public void testGetTombolaWinnerList() throws Exception {
        // add test data
        List<Tombola> expectedTombolas = updateTombolaTestData("tombolaWinnerListData.json");
        updateTombolaDrawingTestData("tombolaWinnerListDrawingData.json");
        List<TombolaDrawing> expectedTombolaDrawings = getExpectedTombolaDrawingDataFromFile(
        );
        assertEquals(1, expectedTombolas.size());

        String requestJson = getPlainTextJsonFromResources("TombolaWinnerListGetRequest.json");
        receivedMessageCollector.getReceivedMessageList().clear();

        final TombolaWinnerListResponse response = getTombolaWinnerListResponse(requestJson);
        assertNotNull(response.getDrawing());
        TombolaDrawing drawing = response.getDrawing();
        assertTombolaDrawingEquals(expectedTombolaDrawings.get(0), drawing);
    }

    private List<TombolaDrawing> getExpectedTombolaDrawingDataFromFile() throws IOException {
        List<TombolaDrawing> expectedTombolas = new ArrayList<>();
        String jsonDrawings = getPlainTextJsonFromResources("tombolaWinnerListExpectedData.json");

        JsonParser parser = new JsonParser();
        JsonObject jsonObject = (JsonObject) parser.parse(jsonDrawings);
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            expectedTombolas.add(jsonUtil.fromJson(entry.getValue().toString(), TombolaDrawing.class));
        }
        return expectedTombolas;
    }

    private void assertTombolaDrawingEquals(TombolaDrawing expectedDrawing, TombolaDrawing actualDrawing) {
        assertEquals(expectedDrawing.getName(), actualDrawing.getName());
        assertEquals(expectedDrawing.getDrawDate(), actualDrawing.getDrawDate());
        assertEquals(expectedDrawing.getImageId(), actualDrawing.getImageId());
        assertEquals(expectedDrawing.getTotalTicketsNumber(), actualDrawing.getTotalTicketsNumber());
        assertEquals(expectedDrawing.getWinners().size(), actualDrawing.getWinners().size());

        List<TombolaWinner> expectedWinners = expectedDrawing.getWinners();
        List<TombolaWinner> actualWinners = actualDrawing.getWinners();
        assertEquals(expectedWinners.size(), actualWinners.size());
        for (int i = 0; i < expectedWinners.size(); i++) {
            TombolaWinner expectedWinner = expectedWinners.get(i);
            TombolaWinner actualWinner = actualWinners.get(i);
            assertEquals(expectedWinner.getPrizeId(), actualWinner.getPrizeId());
            assertEquals(expectedWinner.getName(), actualWinner.getName());
            assertEquals(expectedWinner.getType(), actualWinner.getType());
            assertEquals(expectedWinner.getImageId(), actualWinner.getImageId());
            assertEquals(expectedWinner.getAmount(), actualWinner.getAmount());
            assertEquals(expectedWinner.getTicketCode(), actualWinner.getTicketCode());
            assertEquals(expectedWinner.getTicketId(), actualWinner.getTicketId());
            assertEquals(expectedWinner.getUser().getUserId(), actualWinner.getUser().getUserId());
            assertEquals(expectedWinner.getVoucherId(), actualWinner.getVoucherId());
        }
    }

    private void assertTombolaDrawingVoucherEquals(Voucher expectedVoucher, TombolaDrawing actualDrawing) {
        TombolaWinner winner = actualDrawing.getWinners().get(VOUCHER_WINNER_POSITION);
        assertEquals(expectedVoucher.getId(), winner.getVoucher().getId());
        assertEquals(expectedVoucher.getTitle(), winner.getVoucher().getTitle());
        assertEquals(expectedVoucher.getShortTitle(), winner.getVoucher().getShortTitle());
        assertEquals(expectedVoucher.getExpirationDate(), winner.getVoucher().getExpirationDate());
        assertEquals(expectedVoucher.isSpecialPrize(), winner.getVoucher().isSpecialPrize());
        assertEquals(expectedVoucher.isQRCode(), winner.getVoucher().isQRCode());
        assertEquals(expectedVoucher.getBonuspointsCosts(), winner.getVoucher().getBonuspointsCosts());
        assertEquals(expectedVoucher.getCompany(), winner.getVoucher().getCompany());
        assertEquals(expectedVoucher.getDescription(), winner.getVoucher().getDescription());
        assertEquals(expectedVoucher.getRedemptionURL(), winner.getVoucher().getRedemptionURL());
    }

    @Test
    public void testTombolaDrawList() throws Exception {
        updateTombolaDrawingListTestData("tombolaDrawingListData.json");
        List<Voucher> expectedVouchers = updateVoucherTestData("voucherTestData.json");

        String requestJson = getPlainTextJsonFromResources("TombolaDrawingListRequest.json", ANONYMOUS_CLIENT_INFO)
                .replaceFirst("<<limit>>", String.valueOf(10)).replaceFirst("<<offset>>", String.valueOf(0));
        receivedMessageCollector.getReceivedMessageList().clear();

        final TombolaDrawingListResponse response = getTombolaDrawingListResponse(requestJson);
        assertEquals(2, response.getItems().size());

        List<TombolaDrawing> items = response.getItems();

        // check first drawing
        assertEquals("best tombola", items.get(0).getName());
        assertEquals("2017-01-11T01:01:01Z", items.get(0).getDrawDate());
        assertEquals("tombola1", items.get(0).getImageId());
        assertEquals(153, items.get(0).getTotalTicketsNumber());
        assertEquals(4, items.get(0).getWinners().size());

        // check second drawing
        assertEquals("worst tombola", items.get(1).getName());
        assertEquals("2017-01-11T01:01:01Z", items.get(1).getDrawDate());
        assertEquals("tombola2", items.get(1).getImageId());
        assertEquals(0, items.get(1).getTotalTicketsNumber());
        assertEquals(3, items.get(1).getWinners().size());

        assertTombolaDrawingVoucherEquals(expectedVouchers.get(0), items.get(0));
    }

    @Test
    public void testTombolaDrawingListPagination() throws Exception {
        updateTombolaDrawingListTestData("tombolaDrawingListPaginationData.json");

        String requestJson = getPlainTextJsonFromResources("TombolaDrawingListRequest.json", ANONYMOUS_CLIENT_INFO)
                .replaceFirst("<<limit>>", String.valueOf(3)).replaceFirst("<<offset>>", String.valueOf(1));
        receivedMessageCollector.getReceivedMessageList().clear();

        final TombolaDrawingListResponse response = getTombolaDrawingListResponse(requestJson);
        assertEquals(3, response.getItems().size());

        List<TombolaDrawing> items = response.getItems();
        // check first tombola item
        assertEquals("4", items.get(0).getTombolaId());

        // check second tombola item
        assertEquals("3", items.get(1).getTombolaId());

        // check second tombola item
        assertEquals("2", items.get(2).getTombolaId());

        //check pagination variables
        assertEquals(5, response.getTotal());
        assertEquals(3, response.getLimit());
        assertEquals(1, response.getOffset());

    }

    @Test
    public void testGetUserTombolaDetails() throws Exception {
        // add test data
        List<UserTombolaInfo> expectedTombolas = updateUserTombolaTestData("userTombolaData.json");
        assertEquals(1, expectedTombolas.size());

        String requestJson = getPlainTextJsonFromResources("UserTombolaGetRequest.json", ANONYMOUS_CLIENT_INFO);
        receivedMessageCollector.getReceivedMessageList().clear();

        final UserTombolaGetResponse response = getUserTombolaGetResponse(requestJson);
        assertNotNull(response.getTombola());
        assertUserTombolaResponseModelEquals(new UserTombolaInfoResponseModel(expectedTombolas.get(0)),
                response.getTombola());
    }

    @Test
    public void testUserTombolaList() throws Exception {
        updateUserTombolaListTestData("userTombolaListData.json");

        String requestJson = getPlainTextJsonFromResources("UserTombolaListRequest.json", ANONYMOUS_CLIENT_INFO)
                .replaceFirst("<<limit>>", String.valueOf(10)).replaceFirst("<<offset>>", String.valueOf(0))
                .replaceFirst("<<dateFrom>>", "2017-01-11T01:01:01Z")
                .replaceFirst("<<dateTo>>", "2017-02-13T01:01:01Z");
        receivedMessageCollector.getReceivedMessageList().clear();

        final UserTombolaListResponse response = getUserTombolaListResponse(requestJson);
        assertTombolaListResponse(response, false);

    }

    @Test
    public void testUserTombolaListWithPending() throws Exception {
        updateUserTombolaListTestData("userTombolaPendingListData.json");

        String requestJson = getPlainTextJsonFromResources("UserTombolaPendingListRequest.json", ANONYMOUS_CLIENT_INFO)
                .replaceFirst("<<limit>>", String.valueOf(10)).replaceFirst("<<offset>>", String.valueOf(0))
                .replaceFirst("<<dateFrom>>", "2017-01-11T01:01:01Z")
                .replaceFirst("<<dateTo>>", "2017-02-13T01:01:01Z");
        receivedMessageCollector.getReceivedMessageList().clear();

        final UserTombolaListResponse response = getUserTombolaListResponse(requestJson);
        assertTombolaListResponse(response, true);

    }

    private void assertTombolaListResponse(UserTombolaListResponse response, boolean isPending) {
        assertEquals(2, response.getItems().size());

        List<UserTombolaInfoResponseModel> items = response.getItems();
        // check first user tombola info
        UserTombolaInfoResponseModel firstItem = items.get(0);
        assertEquals("2", firstItem.getTombolaId());
        assertEquals("worst tombola", firstItem.getName());
        assertEquals("2017-01-15T01:01:01Z", firstItem.getDrawDate());
        assertEquals(isPending, firstItem.isPending());
        assertEquals(15, firstItem.getTotalTicketsBought());
        assertEquals(3, firstItem.getTotalPrizesWon());
        assertEquals(3, firstItem.getBundles().size());
        assertEquals(3, firstItem.getPrizes().size());

        // check second user tombola info
        UserTombolaInfoResponseModel secondItem = items.get(1);
        assertEquals("1", secondItem.getTombolaId());
        assertEquals("best tombola", secondItem.getName());
        assertEquals("2017-01-11T01:01:01Z", secondItem.getDrawDate());
        assertEquals(isPending, secondItem.isPending());
        assertEquals(153, secondItem.getTotalTicketsBought());
        assertEquals(1, secondItem.getTotalPrizesWon());
        assertEquals(2, secondItem.getBundles().size());
        assertEquals(2, secondItem.getPrizes().size());
    }

    @Test
    public void testUserTombolaListPagination() throws Exception {
        updateUserTombolaListTestData("userTombolaListPaginationData.json");

        String requestJson = getPlainTextJsonFromResources("UserTombolaListRequest.json", ANONYMOUS_CLIENT_INFO)
                .replaceFirst("<<limit>>", String.valueOf(4)).replaceFirst("<<offset>>", String.valueOf(2))
                .replaceFirst("<<dateFrom>>", "2017-02-02T00:01:01Z")
                .replaceFirst("<<dateTo>>", "2017-05-03T02:01:01Z");
        receivedMessageCollector.getReceivedMessageList().clear();

        final UserTombolaListResponse response = getUserTombolaListResponse(requestJson);
        assertTombolaListPaginationResponse(response);

    }

    @Test
    public void testUserTombolaPendingListPagination() throws Exception {
        updateUserTombolaListTestData("userTombolaPendingListPaginationData.json");

        String requestJson = getPlainTextJsonFromResources("UserTombolaPendingListRequest.json", ANONYMOUS_CLIENT_INFO)
                .replaceFirst("<<limit>>", String.valueOf(4)).replaceFirst("<<offset>>", String.valueOf(2))
                .replaceFirst("<<dateFrom>>", "2017-02-02T00:01:01Z")
                .replaceFirst("<<dateTo>>", "2017-05-03T02:01:01Z");
        receivedMessageCollector.getReceivedMessageList().clear();

        final UserTombolaListResponse response = getUserTombolaListResponse(requestJson);
        assertTombolaListPaginationResponse(response);

    }

    @Test
    public void testTombolaBuyerList() throws Exception {

        // mock data :
        List<Tombola> expectedTombolas = updateTombolaTestData("tombolaBuyData.json");
        Tombola tombola = expectedTombolas.get(0);
        writeCounterTestData(0, 500, "1");

        aerospikeDao.buyTickets(ANONYMOUS_USER_ID, tombola.getName(), tombola.getId(), tombola.getTargetDate(),
                tombola.getTotalTicketsAmount(), 1, BigDecimal.valueOf(100), Currency.MONEY,
                "bundleImg", APP_ID, TENANT_ID);

        aerospikeDao.buyTickets(REGISTERED_USER_ID, tombola.getName(), tombola.getId(), tombola.getTargetDate(),
                tombola.getTotalTicketsAmount(), 1, BigDecimal.valueOf(100), Currency.MONEY,
                "bundleImg", APP_ID, TENANT_ID);

        updateUserTombolaTestData("userTombolaDataForBuyerCall.json");

        String requestJson = getTombolaBuyerListRequestJson()
                .replaceFirst("<<limit>>", "10").replaceFirst("<<offset>>", "0");
        receivedMessageCollector.getReceivedMessageList().clear();

        final TombolaBuyerListResponse response = getTombolaBuyerListResponse(requestJson);
        assertNotNull(response.getItems());

        verifyTombolaBuyerListResponse(response.getItems());
    }

    private String getTombolaBuyerListRequestJson() throws IOException {
        return getPlainTextJsonFromResources("TombolaBuyerListRequest.json");
    }

    @Test
    public void testTombolaBuyerListPagination() throws Exception {
        List<Tombola> expectedTombolas = updateTombolaTestData("tombolaBuyData.json");
        Tombola tombola = expectedTombolas.get(0);
        writeCounterTestData(0, 500, "1");

        for(int i=0; i<20; i++) {
            // this will create all the data we need to test pagination
            aerospikeDao.buyTickets(Integer.toString(i + 1), tombola.getName(), tombola.getId(),
                    tombola.getTargetDate(), tombola.getTotalTicketsAmount(), 1, BigDecimal.valueOf(100),
                    Currency.MONEY, "bundleImg", APP_ID, TENANT_ID);
        }

        String requestJson = getTombolaBuyerListRequestJson()
                .replaceFirst("<<limit>>", String.valueOf(4)).replaceFirst("<<offset>>", String.valueOf(2));
        receivedMessageCollector.getReceivedMessageList().clear();

        final TombolaBuyerListResponse response = getTombolaBuyerListResponse(requestJson);
        assertTombolaBuyerListPaginationResponse(response);

    }

    private void assertTombolaBuyerListPaginationResponse(TombolaBuyerListResponse response) {
        assertEquals(4, response.getItems().size());

        List<TombolaBuyer> items = response.getItems();
        assertEquals("3", items.get(0).getUserId());
        assertEquals("4", items.get(1).getUserId());
        assertEquals("5", items.get(2).getUserId());
        assertEquals("6", items.get(3).getUserId());

        assertEquals(4, response.getTotal());
        assertEquals(4, response.getLimit());
        assertEquals(2, response.getOffset());
    }

    private void verifyTombolaBuyerListResponse(List<TombolaBuyer> items) {
        final int ticketsBought = 120;
        final BigDecimal pricePerBuy = new BigDecimal(1200);
        assertEquals(ANONYMOUS_CLIENT_INFO.getUserId(), items.get(0).getUserId());
        assertEquals(ticketsBought, items.get(0).getPurchasedTickets());
        assertEquals(pricePerBuy.multiply(new BigDecimal(1)), items.get(0).getPurchasePrice());
        assertEquals(1, items.get(0).getNumberOfPrizesWon());

        // 2nd one has 2 times the tickets, wins and 2 times the price
        assertEquals(REGISTERED_CLIENT_INFO.getUserId(), items.get(1).getUserId());
        assertEquals(2 * ticketsBought, items.get(1).getPurchasedTickets());
        assertEquals(pricePerBuy.multiply(new BigDecimal(2)), items.get(1).getPurchasePrice());
        assertEquals(2, items.get(1).getNumberOfPrizesWon());
    }

    private TombolaBuyerListResponse getTombolaBuyerListResponse(String requestJson) throws Exception {
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);

        assertReceivedMessagesWithWait(TombolaMessageTypes.TOMBOLA_BUYER_LIST_RESPONSE);
        final JsonMessage<TombolaBuyerListResponse> response = receivedMessageCollector.getMessageByType(
                TombolaMessageTypes.TOMBOLA_BUYER_LIST_RESPONSE);

        return response.getContent();
    }

    private void assertTombolaListPaginationResponse(UserTombolaListResponse response) {
        assertEquals(4, response.getItems().size());

        List<UserTombolaInfoResponseModel> items = response.getItems();
        assertEquals("7", items.get(0).getTombolaId());
        assertEquals("6", items.get(1).getTombolaId());
        assertEquals("5", items.get(2).getTombolaId());
        assertEquals("4", items.get(3).getTombolaId());

        assertEquals(7, response.getTotal());
        assertEquals(4, response.getLimit());
        assertEquals(2, response.getOffset());
    }

    private UserTombolaGetResponse getUserTombolaGetResponse(String requestJson) throws Exception {
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);

        RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
        final JsonMessage<? extends JsonMessageContent> response = receivedMessageCollector.getReceivedMessageList()
                .get(0);
        assertMessageContentType(response,
                (Class<? extends JsonMessageContent>) UserTombolaGetResponse.class);
        return (UserTombolaGetResponse) response.getContent();
    }

    private UserTombolaListResponse getUserTombolaListResponse(String requestJson) throws Exception {
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);

        RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
        final JsonMessage<? extends JsonMessageContent> response = receivedMessageCollector.getReceivedMessageList()
                .get(0);
        receivedMessageCollector.clearReceivedMessageList();
        assertMessageContentType(response,
                UserTombolaListResponse.class);
        return (UserTombolaListResponse) response.getContent();
    }

    private TombolaGetResponse getTombolaGetResponse(String requestJson) throws Exception {
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);

        RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
        final JsonMessage<? extends JsonMessageContent> response = receivedMessageCollector.getReceivedMessageList()
                .get(0);
        receivedMessageCollector.clearReceivedMessageList();
        assertMessageContentType(response,
                (Class<? extends JsonMessageContent>) TombolaGetResponse.class);
        return (TombolaGetResponse) response.getContent();
    }

    private TombolaListResponse getTombolaListResponse(String requestJson) throws Exception {
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);

        RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
        final JsonMessage<? extends JsonMessageContent> response = receivedMessageCollector.getReceivedMessageList()
                .get(0);
        receivedMessageCollector.clearReceivedMessageList();
        assertMessageContentType(response,
                (Class<? extends JsonMessageContent>) TombolaListResponse.class);
        return (TombolaListResponse) response.getContent();
    }

    private TombolaDrawingListResponse getTombolaDrawingListResponse(String requestJson) throws Exception {
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);

        RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
        final JsonMessage<? extends JsonMessageContent> response = receivedMessageCollector.getReceivedMessageList()
                .get(0);
        receivedMessageCollector.clearReceivedMessageList();
        assertMessageContentType(response,
                (Class<? extends JsonMessageContent>) TombolaDrawingListResponse.class);
        return (TombolaDrawingListResponse) response.getContent();

    }

    private TombolaDrawingGetResponse getTombolaDrawingGetResponse(String requestJson) throws Exception {
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);

        RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
        final JsonMessage<? extends JsonMessageContent> response = receivedMessageCollector.getReceivedMessageList()
                .get(0);
        receivedMessageCollector.clearReceivedMessageList();
        assertMessageContentType(response,
                (Class<? extends JsonMessageContent>) TombolaDrawingGetResponse.class);
        return (TombolaDrawingGetResponse) response.getContent();
    }

    private TombolaWinnerListResponse getTombolaWinnerListResponse(String requestJson) throws Exception {
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);

        RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
        final JsonMessage<? extends JsonMessageContent> response = receivedMessageCollector.getReceivedMessageList()
                .get(0);
        receivedMessageCollector.clearReceivedMessageList();
        assertMessageContentType(response,
                (Class<? extends JsonMessageContent>) TombolaWinnerListResponse.class);
        return (TombolaWinnerListResponse) response.getContent();
    }

    private EmptyJsonMessageContent getTombolaBuyResponse(String requestJson) throws Exception {
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);

        RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
        final JsonMessage<? extends JsonMessageContent> response = receivedMessageCollector.getReceivedMessageList()
                .get(0);
        receivedMessageCollector.clearReceivedMessageList();
        assertMessageContentType(response,
                (Class<? extends JsonMessageContent>) EmptyJsonMessageContent.class);

        return (EmptyJsonMessageContent) response.getContent();
    }

	private JsonMessage<? extends JsonMessageContent> getErrorResponse(String requestJson)
			throws Exception {
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);

		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
		errorCollector.clearCollectedErrors();
		final JsonMessage<? extends JsonMessageContent> response = receivedMessageCollector.getReceivedMessageList()
				.get(0);

		receivedMessageCollector.clearReceivedMessageList();

		return response;
	}

    @Override
    protected ServiceStartup getServiceStartup() {
        return new TombolaServiceStartupUsingAerospikeMock(DEFAULT_TEST_STAGE);
    }

    private void updateCounterTestData() {
        this.writeDefaultCounterTestData();
        this.writeCounterTestData(5, 2, "2");
        this.writeCounterTestData(6, 0, "3");
        this.writeCounterTestData(3, 1, "4");
        this.writeCounterTestData(1, 2, "5");
    }

    private void writeDefaultCounterTestData() {
        writeCounterTestData(0, 0, DEFAULT_TEST_TOMBOLA_ID);
    }

    private void writeCounterTestData(Integer bin1Value, Integer bin2Value, String tombolaId) {
        String set = config.getProperty(TombolaConfig.AEROSPIKE_TOMBOLA_TICKET_SET);
        String counterKeyString = tombolaPrimaryKeyUtil.createSubRecordKeyByTombolaId(tombolaId,
                TOMBOLA_COUNTER_RECORD_NAME);
        Key counterKey = new Key("test", set, counterKeyString);
        Bin counterBin1 = new Bin(TOMBOLA_PURCHASED_COUNTER_BIN_NAME, bin1Value);
        Bin counterBin2 = new Bin(TOMBOLA_LOCKED_COUNTER_BIN_NAME, bin2Value);

        aerospikeClient.put(null, counterKey, counterBin1, counterBin2);
    }

    private void checkCounterTestData(int expectedConsumingCounter) {
        int currentConsumingCounter = aerospikeDao.readInteger(
                config.getProperty(TombolaConfig.AEROSPIKE_TOMBOLA_TICKET_SET),
                tombolaPrimaryKeyUtil.createSubRecordKeyByTombolaId(DEFAULT_TEST_TOMBOLA_ID,
                        TOMBOLA_COUNTER_RECORD_NAME), TOMBOLA_PURCHASED_COUNTER_BIN_NAME);
        int currentLockCounter = aerospikeDao.readInteger(config.getProperty(
                TombolaConfig.AEROSPIKE_TOMBOLA_TICKET_SET), tombolaPrimaryKeyUtil.createSubRecordKeyByTombolaId(
                        DEFAULT_TEST_TOMBOLA_ID, TOMBOLA_COUNTER_RECORD_NAME), TOMBOLA_LOCKED_COUNTER_BIN_NAME);

        assertEquals(expectedConsumingCounter, currentConsumingCounter);
        assertEquals(0, currentLockCounter);
    }

    private void checkWrittenHistoricalData(Currency currency) {
        UserTombolaInfo userTombolaInfo = aerospikeDao.getUserTombola(ANONYMOUS_USER_ID, DEFAULT_TEST_TOMBOLA_ID);
        assertUserTombolaInfoAfterBuy(userTombolaInfo, currency);

        List<UserTombolaInfo> userTombolaInfos = aerospikeDao.getUserTombolaHistoryForPending(ANONYMOUS_USER_ID);
        assertHistoryData(userTombolaInfos, currency);

        UserStatistics userStatisticsByAppId = aerospikeDao.getUserTombolaStatisticsByAppIdKey(ANONYMOUS_USER_ID,
                APP_ID);
        assertUserStatistics(userStatisticsByAppId);

        List<String> appIds = aerospikeDao.getUserStatisticsAppIds(ANONYMOUS_USER_ID);
        assertEquals(1, appIds.size());
        assertTrue(appIds.contains(APP_ID));

        UserStatistics userStatisticsByTenantId = aerospikeDao.getUserTombolaStatisticsByTenantIdKey(ANONYMOUS_USER_ID,
                TENANT_ID);
        assertUserStatistics(userStatisticsByTenantId);

        List<String> tenantIds = aerospikeDao.getUserStatisticsTenantIds(ANONYMOUS_USER_ID);
        assertEquals(1, appIds.size());
        assertTrue(tenantIds.contains(TENANT_ID));
    }

    private void assertHistoryData(List<UserTombolaInfo> userTombolaInfos, Currency currency) {
        assertEquals(1, userTombolaInfos.size());
        UserTombolaInfo firstItem = userTombolaInfos.get(0);

        assertUserTombolaInfoAfterBuy(firstItem, currency);
    }

    private void assertUserTombolaInfoAfterBuy(UserTombolaInfo actual, Currency currency) {
        assertEquals(DEFAULT_TEST_TOMBOLA_ID, actual.getTombolaId());
        assertEquals("Mega Tombola", actual.getName());
        assertEquals("2099-01-12T01:01:01Z", actual.getDrawDate());
        assertEquals(true, actual.isPending());
        assertEquals(10, actual.getTotalTicketsBought());
        assertEquals(0, actual.getTotalPrizesWon());

        assertEquals(10, actual.getBundles().get(0).getAmount());
        assertEquals(BigDecimal.valueOf(100), actual.getBundles().get(0).getPrice());
        assertEquals(currency, actual.getBundles().get(0).getCurrency());
        assertEquals("abc", actual.getBundles().get(0).getImageId());
        assertNotNull(actual.getBundles().get(0).getPurchaseDate());
        assertEquals(0, actual.getBundles().get(0).getFirstTicketId());
    }

    private void assertUserStatistics(UserStatistics statistics) {
        assertEquals(10, statistics.getBoughtTicketsNumber());
        assertEquals(0, statistics.getWinningTicketsNumber());
        assertEquals(10, statistics.getTicketsBoughtSinceLastWin());
    }
}
