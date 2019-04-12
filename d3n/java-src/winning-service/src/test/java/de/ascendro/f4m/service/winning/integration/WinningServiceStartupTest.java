package de.ascendro.f4m.service.winning.integration;

import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_USER_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.APP_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.TENANT_ID;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.game.instance.CommonGameInstanceAerospikeDao;
import de.ascendro.f4m.server.game.util.GameEnginePrimaryKeyUtil;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.result.AnswerResults;
import de.ascendro.f4m.server.result.ResultItem;
import de.ascendro.f4m.server.result.ResultType;
import de.ascendro.f4m.server.result.Results;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.server.winning.CommonUserWinningAerospikeDao;
import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.dao.aerospike.AerospikeClientMockProvider;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.event.model.subscribe.SubscribeRequest;
import de.ascendro.f4m.service.exception.ExceptionCodes;
import de.ascendro.f4m.service.exception.UnexpectedTestException;
import de.ascendro.f4m.service.game.engine.model.Answer;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.engine.model.Question;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.game.GameWinningComponentListItem;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.integration.rule.MockServiceRule.BindableAbstractModule;
import de.ascendro.f4m.service.integration.rule.MockServiceRule.ThrowingFunction;
import de.ascendro.f4m.service.integration.test.F4MServiceWithMockIntegrationTestBase;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.exception.F4MPaymentClientException;
import de.ascendro.f4m.service.payment.exception.PaymentServiceExceptionCodes;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.TransactionId;
import de.ascendro.f4m.service.payment.model.internal.GetAccountBalanceResponse;
import de.ascendro.f4m.service.payment.model.internal.LoadOrWithdrawWithoutCoverageRequest;
import de.ascendro.f4m.service.payment.model.internal.TransferBetweenAccountsRequest;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.result.engine.ResultEngineMessageTypes;
import de.ascendro.f4m.service.result.engine.model.get.GetResultsRequest;
import de.ascendro.f4m.service.result.engine.model.get.GetResultsResponse;
import de.ascendro.f4m.service.result.engine.model.respond.RespondToUserInteractionRequest;
import de.ascendro.f4m.service.result.engine.model.store.StoreUserWinningComponentRequest;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.usermessage.model.SendEmailWrapperRequest;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.JsonTestUtil;
import de.ascendro.f4m.service.voucher.VoucherMessageTypes;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherAssignRequest;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherAssignResponse;
import de.ascendro.f4m.service.winning.WinningMessageTypes;
import de.ascendro.f4m.service.winning.config.WinningConfig;
import de.ascendro.f4m.service.winning.dao.SuperPrizeAerospikeDao;
import de.ascendro.f4m.service.winning.dao.UserWinningComponentAerospikeDao;
import de.ascendro.f4m.service.winning.dao.WinningComponentAerospikeDao;
import de.ascendro.f4m.service.winning.dao.WinningComponentPrimaryKeyUtil;
import de.ascendro.f4m.service.winning.di.WinningDefaultMessageMapper;
import de.ascendro.f4m.service.winning.exception.WinningServiceExceptionCodes;
import de.ascendro.f4m.service.winning.manager.InsuranceInterfaceWrapper;
import de.ascendro.f4m.service.winning.model.SuperPrize;
import de.ascendro.f4m.service.winning.model.UserWinning;
import de.ascendro.f4m.service.winning.model.UserWinningComponent;
import de.ascendro.f4m.service.winning.model.UserWinningComponentStatus;
import de.ascendro.f4m.service.winning.model.UserWinningType;
import de.ascendro.f4m.service.winning.model.WinningComponent;
import de.ascendro.f4m.service.winning.model.WinningComponentType;
import de.ascendro.f4m.service.winning.model.WinningOption;
import de.ascendro.f4m.service.winning.model.WinningOptionType;
import de.ascendro.f4m.service.winning.model.component.ApiUserWinningComponent;
import de.ascendro.f4m.service.winning.model.component.ApiWinningComponent;
import de.ascendro.f4m.service.winning.model.component.ApiWinningOption;
import de.ascendro.f4m.service.winning.model.component.UserWinningComponentAssignResponse;
import de.ascendro.f4m.service.winning.model.component.UserWinningComponentGetResponse;
import de.ascendro.f4m.service.winning.model.component.UserWinningComponentListResponse;
import de.ascendro.f4m.service.winning.model.component.UserWinningComponentLoadResponse;
import de.ascendro.f4m.service.winning.model.component.UserWinningComponentStartResponse;
import de.ascendro.f4m.service.winning.model.component.WinningComponentGetResponse;
import de.ascendro.f4m.service.winning.model.component.WinningComponentListResponse;
import de.ascendro.f4m.service.winning.model.schema.WinningMessageSchemaMapper;
import de.ascendro.f4m.service.winning.model.winning.UserWinningGetResponse;
import de.ascendro.f4m.service.winning.model.winning.UserWinningListResponse;

public class WinningServiceStartupTest extends F4MServiceWithMockIntegrationTestBase {

	private static final Logger LOGGER = LoggerFactory.getLogger(WinningServiceStartupTest.class);

	private static final String TEST_USER_ID = ANONYMOUS_USER_ID;
	private static final String ANOTHER_USER_ID = "someUserId";
	
	private static final String GAME_INSTANCE_ID = "game_instance_id_1";
	private static final String GAME_ID = "1";
	private static final String GAME_INSTANCE_ID_NOT_ELIGIBLE = "game_instance_id_2";

	private static final String PAID_WIN_COMPONENT_ID = "win_comp_id_1";
	private static final String FREE_WIN_COMPONENT_ID = "win_comp_id_2";
	private static final String USED_WIN_COMPONENT_ID = "win_comp_id_3";
	
	private static final String USER_WIN_COMPONENT_ID = "user_win_comp_id_1";

	private static final String SUPER_PRIZE_ID = "super_id1";
	
	private static final String SUPER_PRIZE_WINNING_OPTION_ID = "winopt1";
	private static final String VOUCHER_WINNING_OPTION_ID = "winopt3";
	
	private static final String PAID_USER_WIN_COMPONENT_ID = "usr_win_comp_id_1";
	private static final String VOUCHER_ID = "voucher_id1";
	private static final String USER_VOUCHER_ID = "1";

	private static final String PAYMENT_TRANSACTION_ID = "payment_transaction_id_1";

	private static boolean simulateInsufficientFunds = false;

	private CommonGameInstanceAerospikeDao commonGameInstanceAerospikeDao;
	private WinningComponentAerospikeDao winningComponentAerospikeDao;
	private CommonUserWinningAerospikeDao commonUserWinningAerospikeDao;
	private UserWinningComponentAerospikeDao userWinningComponentAerospikeDao;
	private CommonProfileAerospikeDao commonProfileAerospikeDao;
	private JsonUtil jsonUtil;
	private WinningComponentPrimaryKeyUtil winningComponentPrimaryKeyUtil;
	private GameEnginePrimaryKeyUtil gameEnginePrimaryKeyUtil;
	private PrimaryKeyUtil<String> primaryKeyUtil;
	private InsuranceInterfaceWrapper insuranceApi;
	private SuperPrizeAerospikeDao superPrizeDao;

	private List<SendEmailWrapperRequest> sendEmailRequestMessages;

	private TestDataLoader testDataLoader;

	private List<JsonMessage<? extends JsonMessageContent>> mockServiceReceivedMessageCollector = new CopyOnWriteArrayList<>();

	@Override
	protected ThrowingFunction<RequestContext, JsonMessageContent> getMessageHandlerMock() {
		return ctx -> onReceivedMessage(ctx.getMessage());
	}

	@Override
	protected void configureInjectionModuleMock(BindableAbstractModule module) {
		module.bindToModule(JsonMessageTypeMap.class).to(WinningDefaultMessageMapper.class);
		module.bindToModule(JsonMessageSchemaMap.class).to(WinningMessageSchemaMapper.class);
	}

	@Override
	protected ServiceStartup getServiceStartup() {
		return new WinningServiceStartupUsingAerospikeMock(DEFAULT_TEST_STAGE) {
			@Override
		    protected AbstractModule getWinningServiceAerospikeOverrideModule() {
		        return new AbstractModule() {
		            @Override
		            protected void configure() {
						bind(AerospikeClientProvider.class).to(AerospikeClientMockProvider.class).in(Singleton.class);
						insuranceApi = Mockito.mock(InsuranceInterfaceWrapper.class);
						when(insuranceApi.callInsuranceApi("http://test.url/" + ANONYMOUS_USER_ID + "/100")).thenReturn(true);
						bind(InsuranceInterfaceWrapper.class).toInstance(insuranceApi);
		            }
		        };
			}
		};
	}

	@Override
	protected TestClientInjectionClasses getTestClientInjectionClasses() {
		return new TestClientInjectionClasses(WinningDefaultMessageMapper.class, WinningMessageSchemaMapper.class);
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();

		Injector injector = jettyServerRule.getServerStartup().getInjector();
		commonGameInstanceAerospikeDao = injector.getInstance(CommonGameInstanceAerospikeDao.class);
		winningComponentAerospikeDao = injector.getInstance(WinningComponentAerospikeDao.class);
		commonUserWinningAerospikeDao = injector.getInstance(CommonUserWinningAerospikeDao.class);
		userWinningComponentAerospikeDao = injector.getInstance(UserWinningComponentAerospikeDao.class);
		commonProfileAerospikeDao = injector.getInstance(CommonProfileAerospikeDao.class);
		jsonUtil = injector.getInstance(JsonUtil.class);
		winningComponentPrimaryKeyUtil = injector.getInstance(WinningComponentPrimaryKeyUtil.class);
		gameEnginePrimaryKeyUtil = injector.getInstance(GameEnginePrimaryKeyUtil.class);
		primaryKeyUtil = injector.getInstance(Key.get(new TypeLiteral<PrimaryKeyUtil<String>>() {}));
		superPrizeDao = injector.getInstance(SuperPrizeAerospikeDao.class);
		sendEmailRequestMessages = new CopyOnWriteArrayList<>();

		testDataLoader = new TestDataLoader(jsonUtil);
		assertServiceStartup(ResultEngineMessageTypes.SERVICE_NAME, PaymentMessageTypes.SERVICE_NAME,
				VoucherMessageTypes.SERVICE_NAME, UserMessageMessageTypes.SERVICE_NAME);
	}

	@Test
	public void testWinningComponentGet() throws Exception {
		Pair<WinningComponent, GameWinningComponentListItem> winningComponent = prepareWinningComponent(FREE_WIN_COMPONENT_ID, WinningComponentType.FREE);
		prepareGameInstace(GAME_INSTANCE_ID, winningComponent.getRight());

		final String requestJson = getPlainTextJsonFromResources("winningComponentGetRequest.json")
				.replace("<<winningComponentId>>", winningComponent.getLeft().getWinningComponentId());
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		RetriedAssert.assertWithWait(
				() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> responseMessage = testClientReceivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(responseMessage, WinningComponentGetResponse.class);

		WinningComponentGetResponse content = (WinningComponentGetResponse) responseMessage.getContent();
		assertNotNull(content);

		ApiWinningComponent winningComponentInResponse = content.getWinningComponent();
		assertNotNull(winningComponentInResponse);
		assertEquals(winningComponent.getLeft().getWinningComponentId(), winningComponentInResponse.getWinningComponentId());
	}

	@Test
	public void testWinningComponentList() throws Exception {
		Pair<WinningComponent, GameWinningComponentListItem> winningComponent = prepareWinningComponent(FREE_WIN_COMPONENT_ID, WinningComponentType.FREE);
		prepareGameInstace(GAME_INSTANCE_ID, winningComponent.getRight());

		final String requestJson = getPlainTextJsonFromResources("winningComponentListRequest.json")
				.replace("<<gameInstanceId>>", GAME_INSTANCE_ID);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(WinningMessageTypes.WINNING_COMPONENT_LIST_RESPONSE);
		JsonMessage<WinningComponentListResponse> responseMessage = 
				testClientReceivedMessageCollector.getMessageByType(WinningMessageTypes.WINNING_COMPONENT_LIST_RESPONSE);
		WinningComponentListResponse response = responseMessage.getContent();

		assertEquals(1, response.getTotal());
		assertEquals(1, response.getItems().size());
		assertEquals(FREE_WIN_COMPONENT_ID, response.getItems().get(0).getWinningComponentId());
	}

	@Test
	public void testWinningComponentGetNotFound() throws Exception {
		final String requestJson = getPlainTextJsonFromResources("winningComponentGetRequest.json")
				.replace("<<winningComponentId>>", "wrong_winning_component_id");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertResponseContainsError(ExceptionCodes.ERR_ENTRY_NOT_FOUND);
	}

	@Test
	public void testUserWinningComponentAssignFreeComponent() throws Exception {
		String usrWinCmpId = testUserWinningComponentAssign(WinningComponentType.FREE);

		// Validate service communication
		RetriedAssert.assertWithWait(() -> assertEquals(3, mockServiceReceivedMessageCollector.size()));
		assertMessageContentType(mockServiceReceivedMessageCollector.get(0), SubscribeRequest.class);
		assertMessageContentType(mockServiceReceivedMessageCollector.get(1), GetResultsRequest.class);
		assertMessageContentType(mockServiceReceivedMessageCollector.get(2), StoreUserWinningComponentRequest.class);
		StoreUserWinningComponentRequest storeRequest = (StoreUserWinningComponentRequest) mockServiceReceivedMessageCollector.get(2).getContent();
		assertEquals(GAME_INSTANCE_ID, storeRequest.getGameInstanceId());
		assertEquals(usrWinCmpId, storeRequest.getUserWinningComponentId());
	}

	@Test
	public void testUserWinningComponentAssignPaidComponent() throws Exception {
		String usrWinCmpId = testUserWinningComponentAssign(WinningComponentType.PAID);

		// Validate service communication
		RetriedAssert.assertWithWait(() -> assertEquals(4, mockServiceReceivedMessageCollector.size()));
		assertMessageContentType(mockServiceReceivedMessageCollector.get(0), SubscribeRequest.class);
		assertMessageContentType(mockServiceReceivedMessageCollector.get(1), GetResultsRequest.class);
		assertMessageContentType(mockServiceReceivedMessageCollector.get(2), LoadOrWithdrawWithoutCoverageRequest.class);
		assertMessageContentType(mockServiceReceivedMessageCollector.get(3), StoreUserWinningComponentRequest.class);
		StoreUserWinningComponentRequest storeRequest = (StoreUserWinningComponentRequest) mockServiceReceivedMessageCollector.get(3).getContent();
		assertEquals(GAME_INSTANCE_ID, storeRequest.getGameInstanceId());
		assertEquals(usrWinCmpId, storeRequest.getUserWinningComponentId());
	}

	private String testUserWinningComponentAssign(WinningComponentType type) throws Exception {
		Pair<WinningComponent, GameWinningComponentListItem> freeWinningComponent = prepareWinningComponent(FREE_WIN_COMPONENT_ID, WinningComponentType.FREE);
		Pair<WinningComponent, GameWinningComponentListItem> paidWinningComponent = prepareWinningComponent(PAID_WIN_COMPONENT_ID, WinningComponentType.PAID);
		GameInstance gameInstace = prepareGameInstace(GAME_INSTANCE_ID, freeWinningComponent.getRight(), paidWinningComponent.getRight());

		final String requestJson = getPlainTextJsonFromResources("userWinningComponentAssignRequest.json", ANONYMOUS_CLIENT_INFO)
				.replace("<<gameInstanceId>>", GAME_INSTANCE_ID).replace("<<type>>", type.toString());
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		RetriedAssert.assertWithWait(
				() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> responseMessage = testClientReceivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(responseMessage, UserWinningComponentAssignResponse.class);
		assertNotNull(responseMessage.getContent());

		// Validate response content
		UserWinningComponentAssignResponse content = (UserWinningComponentAssignResponse) responseMessage.getContent();
		ApiUserWinningComponent userWinCompFromResponse = content.getUserWinningComponent();
		assertEquals(gameInstace.getId(), userWinCompFromResponse.getGameInstanceId());
		assertEquals(gameInstace.getGame().getType(), GameType.valueOf(userWinCompFromResponse.getSourceGameType()));
		assertEquals(UserWinningComponentStatus.NEW,
				UserWinningComponentStatus.valueOf(userWinCompFromResponse.getStatus()));

		// Validate record from Aerospike
		UserWinningComponent userWinCompFromDB = userWinningComponentAerospikeDao
				.getUserWinningComponent(APP_ID, ANONYMOUS_USER_ID, userWinCompFromResponse.getUserWinningComponentId());
		assertEquals(gameInstace.getId(), userWinCompFromDB.getGameInstanceId());
		assertEquals(gameInstace.getGame().getGameId(), userWinCompFromDB.getGameId());
		if (WinningComponentType.FREE == type) {
			assertEquals(freeWinningComponent.getLeft().getWinningComponentId(), userWinCompFromDB.getWinningComponentId());
		} else {
			assertEquals(paidWinningComponent.getLeft().getWinningComponentId(), userWinCompFromDB.getWinningComponentId());
		}
		return userWinCompFromResponse.getUserWinningComponentId();
	}

	@Test
	public void testUserWinningComponentListWithOrder() throws Exception {
		Pair<WinningComponent, GameWinningComponentListItem> winningComponent1 = prepareWinningComponent(PAID_WIN_COMPONENT_ID, WinningComponentType.PAID);
		Pair<WinningComponent, GameWinningComponentListItem> winningComponent2 = prepareWinningComponent(USED_WIN_COMPONENT_ID, WinningComponentType.PAID);
		Pair<WinningComponent, GameWinningComponentListItem> winningComponent3 = prepareWinningComponent(FREE_WIN_COMPONENT_ID, WinningComponentType.FREE);
		
		UserWinningComponent userWinningComponent1 = prepareUserWinningComponent("user_win_comp_1", winningComponent1.getLeft(), 
				winningComponent1.getRight(), UserWinningComponentStatus.NEW);
		prepareUserWinningComponent("user_win_comp_2", winningComponent2.getLeft(), winningComponent2.getRight(), UserWinningComponentStatus.USED);
		UserWinningComponent userWinningComponent3 = prepareUserWinningComponent("user_win_comp_3", winningComponent3.getLeft(), winningComponent3.getRight(), UserWinningComponentStatus.NEW);
		
		final String requestJson = getPlainTextJsonFromResources("userWinningComponentListRequest.json", ANONYMOUS_CLIENT_INFO)
				.replace("\"<<limit>>\"", "100")
				.replace("\"<<offset>>\"", "0");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		RetriedAssert.assertWithWait(
				() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<UserWinningComponentListResponse> responseMessage = testClientReceivedMessageCollector
				.getMessageByType(WinningMessageTypes.USER_WINNING_COMPONENT_LIST_RESPONSE);
		assertMessageContentType(responseMessage, UserWinningComponentListResponse.class);
		UserWinningComponentListResponse content = responseMessage.getContent();
		assertNotNull(content);
		
		final List<JsonObject> items = content.getItems();
		assertEquals(userWinningComponent3.getUserWinningComponentId(), new UserWinningComponent(items.get(0)).getUserWinningComponentId());
		assertEquals(userWinningComponent1.getUserWinningComponentId(), new UserWinningComponent(items.get(1)).getUserWinningComponentId());
	}

	@Test
	public void testUserWinningComponentGet() throws Exception {
		Pair<WinningComponent, GameWinningComponentListItem> winningComponent = prepareWinningComponent(PAID_WIN_COMPONENT_ID, WinningComponentType.PAID);
		UserWinningComponent userWinningComponent = prepareUserWinningComponent(USER_WIN_COMPONENT_ID, winningComponent.getLeft(), winningComponent.getRight(), UserWinningComponentStatus.NEW);
		
		final String requestJson = getPlainTextJsonFromResources("userWinningComponentGetRequest.json", ANONYMOUS_CLIENT_INFO)
				.replace("<<userWinningComponentId>>", userWinningComponent.getUserWinningComponentId());
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		RetriedAssert.assertWithWait(
				() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> responseMessage = testClientReceivedMessageCollector
				.getMessageByType(WinningMessageTypes.USER_WINNING_COMPONENT_GET_RESPONSE);
		assertNotNull(responseMessage);
		assertNotNull(responseMessage.getContent());
		
		UserWinningComponentGetResponse content = (UserWinningComponentGetResponse) responseMessage.getContent();
		ApiUserWinningComponent userWinCompFromResponse = content.getUserWinningComponent();
		assertEquals(userWinningComponent.getUserWinningComponentId(), userWinCompFromResponse.getUserWinningComponentId());
		assertEquals(userWinningComponent.getGameInstanceId(), userWinCompFromResponse.getGameInstanceId());
	}

	@Test
	public void testUserWinningComponentLoad() throws Exception {
		Pair<WinningComponent, GameWinningComponentListItem> winningComponent = prepareWinningComponent(PAID_WIN_COMPONENT_ID, WinningComponentType.PAID);
		UserWinningComponent userWinningComponent = prepareUserWinningComponent(USER_WIN_COMPONENT_ID, winningComponent.getLeft(), winningComponent.getRight(), UserWinningComponentStatus.NEW);
		
		final String requestJson = getPlainTextJsonFromResources("userWinningComponentLoadRequest.json", ANONYMOUS_CLIENT_INFO)
				.replace("<<userWinningComponentId>>", userWinningComponent.getUserWinningComponentId());
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		RetriedAssert.assertWithWait(
				() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> responseMessage = testClientReceivedMessageCollector
				.getMessageByType(WinningMessageTypes.USER_WINNING_COMPONENT_LOAD_RESPONSE);
		assertNotNull(responseMessage);
		assertNotNull(responseMessage.getContent());
		
		UserWinningComponentLoadResponse content = (UserWinningComponentLoadResponse) responseMessage.getContent();
		List<ApiWinningOption> winningOptions = content.getWinningOptions();
		assertEquals(4, winningOptions.size());
		List<String> winningOptionsIds = winningOptions.stream()
				.map(o -> o.getWinningOptionId())
				.collect(Collectors.toList());
		assertThat(winningOptionsIds, containsInAnyOrder(new String[] { "winopt1", "winopt2", "winopt3", "winopt4" }));
	}
	
	@Test
	public void testUserWinningComponentLoadAlreadyUsed() throws Exception {
		Pair<WinningComponent, GameWinningComponentListItem> winningComponent = prepareWinningComponent(PAID_WIN_COMPONENT_ID, WinningComponentType.PAID);
		UserWinningComponent userWinningComponent = prepareUserWinningComponent(USER_WIN_COMPONENT_ID, winningComponent.getLeft(), winningComponent.getRight(), UserWinningComponentStatus.USED);
		
		final String requestJson = getPlainTextJsonFromResources("userWinningComponentLoadRequest.json", ANONYMOUS_CLIENT_INFO)
				.replace("<<userWinningComponentId>>", userWinningComponent.getUserWinningComponentId());
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertResponseContainsError(WinningServiceExceptionCodes.ERR_COMPONENT_ALREADY_USED);
	}

	@Test
	public void testUserWinningComponentStart() throws Exception {
		prepareWinningComponent(PAID_WIN_COMPONENT_ID, WinningComponentType.PAID);
		prepareUserWinningComponent(ANONYMOUS_USER_ID, UserWinningComponentStatus.NEW, null);
		
		final String requestJson = getPlainTextJsonFromResources("userWinningComponentStartRequest.json", ANONYMOUS_CLIENT_INFO)
				.replace("<<userWinningComponentId>>", PAID_USER_WIN_COMPONENT_ID);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		RetriedAssert.assertWithWait(
				() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> responseMessage = testClientReceivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(responseMessage, UserWinningComponentStartResponse.class);
		UserWinningComponentStartResponse response = (UserWinningComponentStartResponse) responseMessage.getContent();
		assertTrue(response.getWinningOption().getWinningOptionId().startsWith("winopt"));
		assertFalse(response.getWinningOption().getWinningOptionId().equals(SUPER_PRIZE_WINNING_OPTION_ID)); // superprize not configured => cannot be won
		
		// Winning set also in user winning component; winning component marked as used
		UserWinningComponent component = userWinningComponentAerospikeDao.getUserWinningComponent(APP_ID, ANONYMOUS_USER_ID, PAID_USER_WIN_COMPONENT_ID);
		assertEquals(UserWinningComponentStatus.USED, component.getStatus());
		assertTrue(component.getWinning().getWinningOptionId().startsWith("winopt"));
	}

	@Test
	public void testUserWinningComponentStart_SuperprizeWon() throws Exception {
		prepareWinningComponent(PAID_WIN_COMPONENT_ID, WinningComponentType.PAID);
		prepareUserWinningComponent(ANONYMOUS_USER_ID, UserWinningComponentStatus.NEW, null);
		prepareSuperPrize(SUPER_PRIZE_ID);
		
		final String requestJson = getPlainTextJsonFromResources("userWinningComponentStartRequest.json", ANONYMOUS_CLIENT_INFO)
				.replace("<<userWinningComponentId>>", PAID_USER_WIN_COMPONENT_ID);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		
		RetriedAssert.assertWithWait(
				() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> responseMessage = testClientReceivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(responseMessage, UserWinningComponentStartResponse.class);
		UserWinningComponentStartResponse response = (UserWinningComponentStartResponse) responseMessage.getContent();
		assertEquals(SUPER_PRIZE_WINNING_OPTION_ID, response.getWinningOption().getWinningOptionId()); // superprize won
	}

	@Test
	public void testUserWinningComponentStart_InsufficientTenantFunds() throws Exception {
		prepareWinningComponent(PAID_WIN_COMPONENT_ID, WinningComponentType.PAID);
		prepareUserWinningComponent(ANONYMOUS_USER_ID, UserWinningComponentStatus.NEW, null);
		prepareSuperPrize(SUPER_PRIZE_ID);
		simulateInsufficientFunds = true;

		final String requestJson = getPlainTextJsonFromResources("userWinningComponentStartRequest.json", ANONYMOUS_CLIENT_INFO)
				.replace("<<userWinningComponentId>>", PAID_USER_WIN_COMPONENT_ID);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);

		RetriedAssert.assertWithWait(
				() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> responseMessage = testClientReceivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(responseMessage, UserWinningComponentStartResponse.class);
		UserWinningComponentStartResponse response = (UserWinningComponentStartResponse) responseMessage.getContent();
		assertNotEquals(SUPER_PRIZE_WINNING_OPTION_ID, response.getWinningOption().getWinningOptionId()); // we expect an option different than the superprize since tenant does not have funds

		// verify that we got the email send request:
		assertEquals(1, sendEmailRequestMessages.size());
		// reset flag
		simulateInsufficientFunds = false;
	}

	@Test
	public void testUserWinningComponentStart_AlreadyUsed() throws Exception {
		prepareWinningComponent(PAID_WIN_COMPONENT_ID, WinningComponentType.PAID);
		prepareUserWinningComponent(ANONYMOUS_USER_ID, UserWinningComponentStatus.USED, null);
		
		final String requestJson = getPlainTextJsonFromResources("userWinningComponentStartRequest.json", ANONYMOUS_CLIENT_INFO)
				.replace("<<userWinningComponentId>>", PAID_USER_WIN_COMPONENT_ID);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		RetriedAssert.assertWithWait(
				() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
		assertEquals(WinningServiceExceptionCodes.ERR_COMPONENT_ALREADY_USED, testClientReceivedMessageCollector.getMessage(0).getError().getCode());
	}

	@Test
	public void testUserWinningComponentStop() throws Exception {
		prepareWinningComponent(PAID_WIN_COMPONENT_ID, WinningComponentType.PAID);
		WinningOption winningOption = new WinningOption();
		winningOption.setProperty(WinningOption.PROPERTY_WINNING_OPTION_ID, SUPER_PRIZE_WINNING_OPTION_ID);
		winningOption.setProperty(WinningOption.PROPERTY_PRIZE_ID, SUPER_PRIZE_ID);
		winningOption.setProperty(WinningOption.PROPERTY_TYPE, WinningOptionType.SUPER.name());
		prepareUserWinningComponent(ANONYMOUS_USER_ID, UserWinningComponentStatus.USED, winningOption);
		prepareSuperPrize(SUPER_PRIZE_ID);
		testClientReceivedMessageCollector.clearReceivedMessageList();

		final String requestJson = getPlainTextJsonFromResources("userWinningComponentStopRequest.json", ANONYMOUS_CLIENT_INFO)
				.replace("<<userWinningComponentId>>", PAID_USER_WIN_COMPONENT_ID);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		RetriedAssert.assertWithWait(() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));

		// Response to stop
		JsonMessage<? extends JsonMessageContent> response = testClientReceivedMessageCollector.getMessage(0);
		assertMessageContentType(response, EmptyJsonMessageContent.class);

		// Request to transfer winning
		assertEquals(2, mockServiceReceivedMessageCollector.size());
		assertMessageContentType(mockServiceReceivedMessageCollector.get(0), SubscribeRequest.class);
		assertMessageContentType(mockServiceReceivedMessageCollector.get(1), TransferBetweenAccountsRequest.class);
		TransferBetweenAccountsRequest transferRequest = (TransferBetweenAccountsRequest) mockServiceReceivedMessageCollector.get(1).getContent();
		assertEquals(new BigDecimal("10000.00"), transferRequest.getAmount());
		assertEquals(Currency.MONEY, transferRequest.getCurrency());
		assertNull(transferRequest.getFromProfileId());
		assertEquals(TENANT_ID, transferRequest.getTenantId());
		assertEquals(ANONYMOUS_USER_ID, transferRequest.getToProfileId());
		assertEquals(APP_ID, transferRequest.getPaymentDetails().getAppId());
		assertEquals(GAME_ID, transferRequest.getPaymentDetails().getGameId());
		assertEquals(GAME_INSTANCE_ID, transferRequest.getPaymentDetails().getGameInstanceId());
		assertEquals(PAID_USER_WIN_COMPONENT_ID, transferRequest.getPaymentDetails().getUserWinningComponentId());
		assertEquals(PAID_WIN_COMPONENT_ID, transferRequest.getPaymentDetails().getWinningComponentId());

		// Winning component marked as filed
		UserWinningComponent component = userWinningComponentAerospikeDao.getUserWinningComponent(APP_ID, ANONYMOUS_USER_ID, PAID_USER_WIN_COMPONENT_ID);
		assertEquals(UserWinningComponentStatus.FILED, component.getStatus());

		// Winning filed
		List<JsonObject> winnings = commonUserWinningAerospikeDao.getUserWinnings(APP_ID, ANONYMOUS_USER_ID, 100, 0, null);
		assertEquals(1, winnings.size());
		UserWinning winning = new UserWinning(winnings.iterator().next());
		assertEquals(UserWinningType.WINNING_COMPONENT, winning.getType());
		assertEquals(new BigDecimal("10000.00"), winning.getAmount());
		assertEquals(Currency.MONEY, winning.getCurrency());
	}

	@Test
	public void testUserWinningComponentStopInsufficientFunds() throws Exception {
		errorCollector.setExpectedErrors(new F4MPaymentClientException(PaymentServiceExceptionCodes.ERR_INSUFFICIENT_FUNDS, "paymentShouldReturnError is set to true"));

		prepareWinningComponent(PAID_WIN_COMPONENT_ID, WinningComponentType.PAID);
		WinningOption winningOption = new WinningOption();
		winningOption.setProperty(WinningOption.PROPERTY_WINNING_OPTION_ID, SUPER_PRIZE_WINNING_OPTION_ID);
		winningOption.setProperty(WinningOption.PROPERTY_PRIZE_ID, SUPER_PRIZE_ID);
		winningOption.setProperty(WinningOption.PROPERTY_TYPE, WinningOptionType.SUPER.name());
		prepareUserWinningComponent(ANONYMOUS_USER_ID, UserWinningComponentStatus.USED, winningOption);
		prepareSuperPrize(SUPER_PRIZE_ID);
		simulateInsufficientFunds = true;
		testClientReceivedMessageCollector.clearReceivedMessageList();

		final String requestJson = getPlainTextJsonFromResources("userWinningComponentStopRequest.json", ANONYMOUS_CLIENT_INFO)
				.replace("<<userWinningComponentId>>", PAID_USER_WIN_COMPONENT_ID);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		RetriedAssert.assertWithWait(() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));

		// Response to stop
		JsonMessage<? extends JsonMessageContent> response = testClientReceivedMessageCollector.getMessage(0);
		assertEquals(ExceptionCodes.ERR_FATAL_ERROR, response.getError().getCode());
		simulateInsufficientFunds = false;

	}

	@Test
	public void testUserWinningComponentStop_voucher() throws Exception {
		prepareWinningComponent(PAID_WIN_COMPONENT_ID, WinningComponentType.PAID);
		WinningOption winningOption = new WinningOption();
		winningOption.setProperty(WinningOption.PROPERTY_WINNING_OPTION_ID, VOUCHER_WINNING_OPTION_ID);
		winningOption.setProperty(WinningOption.PROPERTY_PRIZE_ID, VOUCHER_ID);
		winningOption.setProperty(WinningOption.PROPERTY_TYPE, WinningOptionType.VOUCHER.name());
		prepareUserWinningComponent(ANONYMOUS_USER_ID, UserWinningComponentStatus.USED, winningOption);
		testClientReceivedMessageCollector.clearReceivedMessageList();
		
		final String requestJson = getPlainTextJsonFromResources("userWinningComponentStopRequest.json", ANONYMOUS_CLIENT_INFO)
				.replace("<<userWinningComponentId>>", PAID_USER_WIN_COMPONENT_ID);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		RetriedAssert.assertWithWait(() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
		
		// Response to stop
		JsonMessage<? extends JsonMessageContent> response = testClientReceivedMessageCollector.getMessage(0);
		assertMessageContentType(response, EmptyJsonMessageContent.class);
		
		// Request to transfer winning
		assertEquals(2, mockServiceReceivedMessageCollector.size());
		assertMessageContentType(mockServiceReceivedMessageCollector.get(0), SubscribeRequest.class);
		assertMessageContentType(mockServiceReceivedMessageCollector.get(1), UserVoucherAssignRequest.class);
		UserVoucherAssignRequest transferRequest = (UserVoucherAssignRequest) mockServiceReceivedMessageCollector.get(1).getContent();
		assertEquals(GAME_INSTANCE_ID, transferRequest.getGameInstanceId());
		assertEquals(VOUCHER_ID, transferRequest.getVoucherId());
		assertEquals(ANONYMOUS_USER_ID, transferRequest.getUserId());
		
		// Winning component marked as filed
		UserWinningComponent component = userWinningComponentAerospikeDao.getUserWinningComponent(APP_ID, ANONYMOUS_USER_ID, PAID_USER_WIN_COMPONENT_ID);
		assertEquals(UserWinningComponentStatus.FILED, component.getStatus());
	}
	
	@Test
	public void testUserWinningComponentMove() throws Exception {
		prepareProfile(ANONYMOUS_USER_ID, APP_ID);
		WinningOption winningOption = new WinningOption();
		winningOption.setProperty(WinningOption.PROPERTY_WINNING_OPTION_ID, VOUCHER_WINNING_OPTION_ID);
		winningOption.setProperty(WinningOption.PROPERTY_PRIZE_ID, VOUCHER_ID);
		winningOption.setProperty(WinningOption.PROPERTY_TYPE, WinningOptionType.VOUCHER.name());
		UserWinningComponent userWinningComponent = prepareUserWinningComponent(ANONYMOUS_USER_ID, UserWinningComponentStatus.NEW, winningOption);

		List<JsonObject> winningComponents11 = userWinningComponentAerospikeDao.getUserWinningComponents(APP_ID, ANOTHER_USER_ID, 100, 0, null);
		List<JsonObject> winningComponents21 = userWinningComponentAerospikeDao.getUserWinningComponents(APP_ID, ANONYMOUS_USER_ID, 100, 0, null);
		
		assertEquals(0, winningComponents11.size());
		assertEquals(1, winningComponents21.size());
		assertEquals(userWinningComponent.getUserWinningComponentId(), new UserWinningComponent(winningComponents21.get(0)).getUserWinningComponentId());

		final String requestJson = getPlainTextJsonFromResources("userWinningComponentMoveRequest.json")
				.replace("<<sourceUserId>>", ANONYMOUS_USER_ID)
				.replace("<<targetUserId>>", ANOTHER_USER_ID);
		
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(WinningMessageTypes.USER_WINNING_COMPONENT_MOVE_RESPONSE);

		List<JsonObject> winningComponents12 = userWinningComponentAerospikeDao.getUserWinningComponents(APP_ID, ANOTHER_USER_ID, 100, 0, null);
		List<JsonObject> winningComponents22 = userWinningComponentAerospikeDao.getUserWinningComponents(APP_ID, ANONYMOUS_USER_ID, 100, 0, null);
		
		assertEquals(0, winningComponents22.size());
		assertEquals(1, winningComponents12.size());
		assertEquals(userWinningComponent.getUserWinningComponentId(), new UserWinningComponent(winningComponents12.get(0)).getUserWinningComponentId());
	}
	
	@Test
	public void testUserWinningList() throws Exception {
		final int limit = 3;
		final long offset = 1L;

		final UserWinning userWinning1 = prepareUserWinning(UserWinningType.QUIZ24,
				ZonedDateTime.of(2016, 1, 2, 12, 0, 0, 0, ZoneOffset.UTC));
		prepareUserWinning(UserWinningType.TOURNAMENT, ZonedDateTime.of(2016, 3, 1, 12, 0, 0, 0, ZoneOffset.UTC));
		final UserWinning userWinning3 = prepareUserWinning(UserWinningType.DUEL, ZonedDateTime.of(2016, 1, 3, 12, 0, 0, 0, ZoneOffset.UTC));
		prepareUserWinning(UserWinningType.DUEL, ZonedDateTime.of(2016, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC));
		final UserWinning userWinning5 = prepareUserWinning(UserWinningType.TOURNAMENT,
				ZonedDateTime.of(2016, 1, 2, 12, 0, 0, 0, ZoneOffset.UTC));

		final String requestJson = getPlainTextJsonFromResources("userWinningListRequest.json", ANONYMOUS_CLIENT_INFO)
				.replace("\"<<limit>>\"", Integer.toString(limit))
				.replace("\"<<offset>>\"", Long.toString(offset));
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		RetriedAssert.assertWithWait(
				() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<UserWinningListResponse> responseMessage = testClientReceivedMessageCollector
				.getMessageByType(WinningMessageTypes.USER_WINNING_LIST_RESPONSE);
		assertNotNull(responseMessage);
		assertNotNull(responseMessage.getContent());

		final UserWinningListResponse content = (UserWinningListResponse) responseMessage.getContent();
		assertEquals(limit, content.getLimit());
		assertEquals(offset, content.getOffset());
		assertEquals(limit, content.getTotal());
		assertEquals(limit, content.getItems().size());

		final List<JsonObject> items = content.getItems();
		assertEquals(userWinning5.getUserWinningId(), new UserWinning(items.get(0)).getUserWinningId());
		assertEquals(userWinning1.getUserWinningId(), new UserWinning(items.get(1)).getUserWinningId());
		assertEquals(userWinning3.getUserWinningId(), new UserWinning(items.get(2)).getUserWinningId());
	}

	@Test
	public void testUserWinningGet() throws Exception {
		UserWinning userWinning = prepareUserWinning(UserWinningType.TOURNAMENT, DateTimeUtil.getCurrentDateTime());

		final String requestJson = getPlainTextJsonFromResources("userWinningGetRequest.json")
				.replace("<<userWinningId>>", userWinning.getUserWinningId());
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		RetriedAssert.assertWithWait(
				() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> responseMessage = testClientReceivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(responseMessage, UserWinningGetResponse.class);

		UserWinningGetResponse content = (UserWinningGetResponse) responseMessage.getContent();
		assertNotNull(content);

		UserWinning userWinningInResponse = content.getUserWinning();
		assertNotNull(userWinningInResponse);
		assertEquals(userWinning.getAsString(), userWinningInResponse.getAsString());
	}

	@Test
	public void testUserWinningGetNotFound() throws Exception {
		final String requestJson = getPlainTextJsonFromResources("userWinningGetRequest.json")
				.replace("<<userWinningId>>", "wrong_user_winning_id");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertResponseContainsError(ExceptionCodes.ERR_ENTRY_NOT_FOUND);
	}

	@Test
	public void testUserWinningMove() throws Exception {
		UserWinning userWinning = prepareUserWinning(UserWinningType.TOURNAMENT, DateTimeUtil.getCurrentDateTime());

		final String requestJson = getPlainTextJsonFromResources("userWinningMoveRequest.json")
				.replace("<<sourceUserId>>", ANONYMOUS_USER_ID)
				.replace("<<targetUserId>>", TEST_USER_ID);
		
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(WinningMessageTypes.USER_WINNING_MOVE_RESPONSE);

		List<JsonObject> winnings = commonUserWinningAerospikeDao.getUserWinnings(APP_ID, TEST_USER_ID, 100, 0, null);
		assertEquals(1, winnings.size());
		assertEquals(userWinning.getUserWinningId(), new UserWinning(winnings.get(0)).getUserWinningId());
	}
	
	private void assertResponseContainsError(String exceptionCode) {
		RetriedAssert.assertWithWait(
				() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> responseMessage = testClientReceivedMessageCollector
				.getReceivedMessageList().get(0);
		assertNull(responseMessage.getContent());
		assertNotNull(responseMessage.getError());
		assertEquals(exceptionCode, responseMessage.getError().getCode());
	}

	private JsonMessageContent onReceivedMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded) {
		mockServiceReceivedMessageCollector.add(originalMessageDecoded);
		if (ResultEngineMessageTypes.GET_RESULTS_INTERNAL == originalMessageDecoded.getType(ResultEngineMessageTypes.class)) {
			GetResultsRequest request = (GetResultsRequest) originalMessageDecoded.getContent();
			Results results = getResults(request.getGameInstanceId());
			GetResultsResponse response = new GetResultsResponse();
			response.setResults(results.getJsonObject());
			return response;
		} else if (ResultEngineMessageTypes.STORE_USER_WINNING_COMPONENT == originalMessageDecoded
				.getType(ResultEngineMessageTypes.class)) {
			return new EmptyJsonMessageContent();
		} else if (VoucherMessageTypes.USER_VOUCHER_ASSIGN == originalMessageDecoded.getType(VoucherMessageTypes.class)) {
			return new UserVoucherAssignResponse(USER_VOUCHER_ID);
		} else if (ResultEngineMessageTypes.RESPOND_TO_USER_INTERACTION == originalMessageDecoded
				.getType(ResultEngineMessageTypes.class)) {
			RespondToUserInteractionRequest request = (RespondToUserInteractionRequest) originalMessageDecoded
					.getContent();
			LOGGER.info(
					"Received respondToUserInteractionRequest (gameInstanceId=[{}], userInteractionType=[{}], response=[{}])",
					request.getGameInstanceId(), request.getUserInteractionType(), request.getResponse());
			return new EmptyJsonMessageContent();
		} else if (PaymentMessageTypes.TRANSFER_BETWEEN_ACCOUNTS == originalMessageDecoded.getType(PaymentMessageTypes.class)
				|| PaymentMessageTypes.LOAD_OR_WITHDRAW_WITHOUT_COVERAGE == originalMessageDecoded.getType(PaymentMessageTypes.class)) {
			if(simulateInsufficientFunds) {
				throw new F4MPaymentClientException(PaymentServiceExceptionCodes.ERR_INSUFFICIENT_FUNDS,
						"paymentShouldReturnError is set to true");
			}
			return new TransactionId(PAYMENT_TRANSACTION_ID);
		} else if (EventMessageTypes.RESUBSCRIBE == originalMessageDecoded.getType(EventMessageTypes.class) ||
        		EventMessageTypes.SUBSCRIBE == originalMessageDecoded.getType(EventMessageTypes.class)) {
			return null;
		} else if(PaymentMessageTypes.GET_ACCOUNT_BALANCE == originalMessageDecoded.getType(PaymentMessageTypes.class)){
			GetAccountBalanceResponse response = new GetAccountBalanceResponse();
			response.setCurrency(Currency.MONEY);
			response.setAmount(BigDecimal.valueOf(10000));

			if(simulateInsufficientFunds) {
				response.setAmount(BigDecimal.ONE);
			}
			return response;
		} else if(UserMessageMessageTypes.SEND_EMAIL == originalMessageDecoded.getType(UserMessageMessageTypes.class)) {
			sendEmailRequestMessages.add((SendEmailWrapperRequest) originalMessageDecoded.getContent());
			return new EmptyJsonMessageContent();
		}else {
			throw new UnexpectedTestException("Unexpected message: " + originalMessageDecoded.getName());
		}
	}

	private Results getResults(String gameInstanceId) {
		Results results = new Results();

		results.setEligibleToWinnings(!GAME_INSTANCE_ID_NOT_ELIGIBLE.equals(gameInstanceId));
		results.setUserId(ANONYMOUS_USER_ID);
		results.setGameInstanceId(gameInstanceId);
		results.setGameFinished(true);
		results.setPaidWinningComponentId(PAID_WIN_COMPONENT_ID);
		results.setFreeWinningComponentId(FREE_WIN_COMPONENT_ID);

		ResultItem resultItem = new ResultItem();
		resultItem.setResultType(ResultType.BONUS_POINTS);
		resultItem.setAmount(13.3);

		AnswerResults answerResults = results.addIncorrectAnswer(0, new Question(), new Answer(new JsonObject()));
		answerResults.addResultItem(new ResultItem(ResultType.GAME_POINTS_FOR_CORRECT_ANSWER, 3));
		results.addResultItem(resultItem);
		return results;
	}

	private Pair<WinningComponent, GameWinningComponentListItem> prepareWinningComponent(String id, WinningComponentType type) throws Exception {
		final WinningComponent winningComponent = testDataLoader.getWinningComponent(id);
		final GameWinningComponentListItem winningComponentConfig = new GameWinningComponentListItem(id, type == WinningComponentType.PAID, 
				type == WinningComponentType.PAID ? new BigDecimal("20") : BigDecimal.ZERO, 
				type == WinningComponentType.PAID ? Currency.CREDIT : null, 
				50);

		String gameSet = config.getProperty(WinningConfig.AEROSPIKE_WINNING_COMPONENT_SET);
		String componentConfigKey = winningComponentPrimaryKeyUtil.createWinningComponentKey(winningComponent.getWinningComponentId());
		winningComponentAerospikeDao.createJson(gameSet, componentConfigKey, WinningComponentAerospikeDao.BLOB_BIN_NAME,
				winningComponent.getAsString());

		return Pair.of(winningComponent, winningComponentConfig);
	}

	private UserWinningComponent prepareUserWinningComponent(String userId, UserWinningComponentStatus status, WinningOption winningOption) throws Exception {
		UserWinningComponent userWinningComponent = testDataLoader.getUserWinningComponent(PAID_USER_WIN_COMPONENT_ID, 
				GAME_INSTANCE_ID, GAME_ID, status, GameType.USER_TOURNAMENT, PAID_WIN_COMPONENT_ID, 
				WinningComponentType.PAID, new BigDecimal("20"), Currency.CREDIT);
		if (winningOption != null) {
			userWinningComponent.setWinning(winningOption);
		}
		userWinningComponentAerospikeDao.saveUserWinningComponent(APP_ID, userId, userWinningComponent);
		return userWinningComponent;
	}

	private SuperPrize prepareSuperPrize(String id) throws Exception {
		SuperPrize superPrize = testDataLoader.getSuperPrize(SUPER_PRIZE_ID, 10000, "http://test.url/{userId}/{random}", 100, 100);
		superPrizeDao.createJson(config.getProperty(WinningConfig.AEROSPIKE_SUPER_PRIZE_SET), 
				primaryKeyUtil.createPrimaryKey(id), SuperPrizeAerospikeDao.BLOB_BIN_NAME, superPrize.getAsString());
		return superPrize;
	}
	
	private GameInstance prepareGameInstace(String gameInstanceId, GameWinningComponentListItem... winningComponentConfigs)
			throws Exception {
		Game game = testDataLoader.getGame(GAME_ID, GameType.QUIZ24, winningComponentConfigs);
		game.setTenantId(TENANT_ID);
		GameInstance gameInstance = testDataLoader.getGameInstace(gameInstanceId, game);

		String gameInstanceSet = config.getProperty(AerospikeConfigImpl.AEROSPIKE_GAME_INSTANCE_SET);
		String gameInstanceKey = gameEnginePrimaryKeyUtil.createPrimaryKey(gameInstance.getId());
		commonGameInstanceAerospikeDao.createJson(gameInstanceSet, gameInstanceKey,
				CommonGameInstanceAerospikeDao.BLOB_BIN_NAME, gameInstance.getAsString());

		return gameInstance;
	}

	private UserWinning prepareUserWinning(UserWinningType type, ZonedDateTime dateTime) throws Exception {
		UserWinning userWinning = new UserWinning("Winning!", type, new BigDecimal("5.00"), Currency.MONEY,
				"You have won some prize", "image_id_1", "gameInstance1");
		userWinning.setObtainDate(dateTime);

		commonUserWinningAerospikeDao.saveUserWinning(APP_ID, TEST_USER_ID, userWinning);

		return userWinning;
	}
	
	private UserWinningComponent prepareUserWinningComponent(String id, WinningComponent winningComponent, 
			GameWinningComponentListItem winningComponentConfiguration, UserWinningComponentStatus status) {
		UserWinningComponent userWinningComponent = new UserWinningComponent(id, winningComponent, 
				winningComponentConfiguration, GAME_INSTANCE_ID, "game_id_1", GameType.DUEL);
		userWinningComponent.setStatus(status);
		userWinningComponentAerospikeDao.saveUserWinningComponent(APP_ID, TEST_USER_ID, userWinningComponent);
		
		return userWinningComponent;
	}
	
	private void prepareProfile(String testUserId, String appId) throws IOException {
		final String sourceJsonString = getProfileJson(testUserId, appId, "anyName", "anySurname");
		final JsonElement jsonElement = JsonTestUtil.getGson().fromJson(sourceJsonString, JsonElement.class);

		final Profile profile = new Profile(jsonElement);

		commonProfileAerospikeDao.createJson("profile", "profile:" + testUserId, "value", profile.getAsString());
	}

	private String getProfileJson(String userId, String appId, String name, String surname) throws IOException {
		final String profileJson = jsonLoader.getPlainTextJsonFromResources("profile.json")
				.replaceFirst("<<userId>>", userId).replaceFirst("<<firstName>>", name)
				.replaceFirst("<<lastName>>", surname)
				.replaceFirst("<<appId>>", appId);
		return profileJson;
	} 

}
