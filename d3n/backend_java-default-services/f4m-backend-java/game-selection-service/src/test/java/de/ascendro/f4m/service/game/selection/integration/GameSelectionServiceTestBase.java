package de.ascendro.f4m.service.game.selection.integration;

import java.math.BigDecimal;

import org.junit.Before;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.inject.Injector;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.auth.AuthMessageTypes;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.event.model.subscribe.SubscribeRequest;
import de.ascendro.f4m.service.event.model.subscribe.SubscribeResponse;
import de.ascendro.f4m.service.exception.UnexpectedTestException;
import de.ascendro.f4m.service.friend.FriendManagerMessageTypes;
import de.ascendro.f4m.service.friend.model.api.buddy.BuddyListAllIdsResponse;
import de.ascendro.f4m.service.game.engine.GameEngineMessageTypes;
import de.ascendro.f4m.service.game.selection.GameSelectionMessageTypes;
import de.ascendro.f4m.service.game.selection.di.GameSelectionDefaultMessageTypeMapper;
import de.ascendro.f4m.service.game.selection.model.schema.GameSelectionMessageSchemaMapper;
import de.ascendro.f4m.service.game.selection.model.subscription.GameStartCountDownNotification;
import de.ascendro.f4m.service.game.selection.model.subscription.GameStartNotification;
import de.ascendro.f4m.service.gateway.GatewayMessageTypes;
import de.ascendro.f4m.service.integration.collector.ReceivedMessageCollector;
import de.ascendro.f4m.service.integration.rule.MockServiceRule.BindableAbstractModule;
import de.ascendro.f4m.service.integration.rule.MockServiceRule.ThrowingFunction;
import de.ascendro.f4m.service.integration.test.F4MServiceWithMockIntegrationTestBase;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.internal.GetJackpotResponse;
import de.ascendro.f4m.service.profile.ProfileMessageTypes;
import de.ascendro.f4m.service.profile.model.sub.get.GetProfileBlobRequest;
import de.ascendro.f4m.service.profile.model.sub.get.GetProfileBlobResponse;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;

public abstract class GameSelectionServiceTestBase extends F4MServiceWithMockIntegrationTestBase {

	private static final Logger LOGGER = LoggerFactory.getLogger(GameSelectionServiceTestBase.class);

	protected final ReceivedMessageCollector mockServiceReceivedMessageServerCollector = new ReceivedMessageCollector(){
		@Override
		public SessionWrapper getSessionWrapper() {
			return Mockito.mock(SessionWrapper.class);
		};
	};

	protected JsonWebSocketClientSessionPool mockServiceJsonWebSocketClientSessionPool;
	protected TestDataLoader testDataLoader;

	@Override
	protected void configureInjectionModuleMock(BindableAbstractModule module) {
		module.bindToModule(JsonMessageTypeMap.class).to(GameSelectionDefaultMessageTypeMapper.class);
		module.bindToModule(JsonMessageSchemaMap.class).to(GameSelectionMessageSchemaMapper.class);
	};

	@Override
	protected ThrowingFunction<RequestContext, JsonMessageContent> getMessageHandlerMock() {
		return ctx -> onReceivedMessage(ctx);
	}

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		mockServiceJsonWebSocketClientSessionPool = mockService.getServerStartup().getInjector()
				.getInstance(JsonWebSocketClientSessionPool.class);
		
		final Injector injector = jettyServerRule.getServerStartup().getInjector();
		final AerospikeClientProvider aerospikeClientProvider = injector.getInstance(AerospikeClientProvider.class);
		final JsonUtil jsonUtil = injector.getInstance(JsonUtil.class);
		final JsonMessageUtil jsonMessageUtil = injector.getInstance(JsonMessageUtil.class);
		testDataLoader = new TestDataLoader(config, aerospikeClientProvider, jsonUtil, jsonMessageUtil);

		assertServiceStartup(FriendManagerMessageTypes.SERVICE_NAME, UserMessageMessageTypes.SERVICE_NAME,
				ProfileMessageTypes.SERVICE_NAME, EventMessageTypes.SERVICE_NAME, GameEngineMessageTypes.SERVICE_NAME,
				PaymentMessageTypes.SERVICE_NAME);
	}

	@Override
	protected TestClientInjectionClasses getTestClientInjectionClasses() {
		return new TestClientInjectionClasses(GameSelectionDefaultMessageTypeMapper.class, GameSelectionMessageSchemaMapper.class);
	}
	
	@Override
	protected void startTestClient() {
		super.startTestClient();
		Config clientConfig = clientInjector.getInstance(Config.class);
		clientConfig.setProperty(F4MConfigImpl.SERVICE_NAME, GatewayMessageTypes.SERVICE_NAME);
	}
	
	protected JsonMessageContent onReceivedMessage(RequestContext requestContext)
			throws Exception {
		JsonMessage<? extends JsonMessageContent> originalMessageDecoded = requestContext.getMessage();
		JsonMessageContent response = null;
		LOGGER.debug("Mocked Service received request {}", originalMessageDecoded.getContent());
		if (FriendManagerMessageTypes.BUDDY_LIST_ALL_IDS == originalMessageDecoded.getType(FriendManagerMessageTypes.class)) {
			response = getFriendsResponse();
		} else if (ProfileMessageTypes.GET_PROFILE_BLOB == originalMessageDecoded.getType(ProfileMessageTypes.class)) {
			response = getProfileBlobResponse((GetProfileBlobRequest) originalMessageDecoded.getContent());
		} else if (GameSelectionMessageTypes.GAME_START_NOTIFICATION == originalMessageDecoded
				.getType(GameSelectionMessageTypes.class)) {
			final GameStartNotification gameStartNotification = (GameStartNotification) originalMessageDecoded.getContent();
			LOGGER.debug("Mocked Service received game start notification of MGI [{}]",
					gameStartNotification.getMultiplayerGameInstanceId());
		} else if (GameSelectionMessageTypes.GAME_START_COUNT_DOWN_NOTIFICATION == originalMessageDecoded
				.getType(GameSelectionMessageTypes.class)) {
			final GameStartCountDownNotification gameStartCountDownNotification = (GameStartCountDownNotification) originalMessageDecoded.getContent();
			LOGGER.debug("Mocked Service received game start count-down of MGI [{}], Time [{}]",
					gameStartCountDownNotification.getMultiplayerGameInstanceId(), gameStartCountDownNotification.getTime());
		} else if (EventMessageTypes.SUBSCRIBE == originalMessageDecoded.getType(EventMessageTypes.class)) {
			SubscribeRequest content = (SubscribeRequest) originalMessageDecoded.getContent();
			response = new SubscribeResponse(100L, false, null, content.getTopic());
		} else if (PaymentMessageTypes.GET_JACKPOT == originalMessageDecoded.getType(PaymentMessageTypes.class)) {
			GetJackpotResponse jack = new GetJackpotResponse();
			jack.setBalance(new BigDecimal("10"));
			jack.setCurrency(Currency.CREDIT);
			response = jack;
		} else if (PaymentMessageTypes.CREATE_JACKPOT == originalMessageDecoded.getType(PaymentMessageTypes.class)) {
		} else if (EventMessageTypes.SUBSCRIBE_RESPONSE == originalMessageDecoded.getType(EventMessageTypes.class) 
				|| EventMessageTypes.RESUBSCRIBE == originalMessageDecoded.getType(EventMessageTypes.class)
				|| EventMessageTypes.UNSUBSCRIBE == originalMessageDecoded.getType(EventMessageTypes.class) 
				|| originalMessageDecoded.getType(AuthMessageTypes.class) != null) {
			//ignore particular messages
		} else {
			LOGGER.error("Mocked Service received unexpected message [{}]", originalMessageDecoded);
			throw new UnexpectedTestException("Unexpected message");
		}

		return response;
	}

	private BuddyListAllIdsResponse getFriendsResponse() {
		return new BuddyListAllIdsResponse("friend_id_1", "friend_id_2");
	}

	private GetProfileBlobResponse getProfileBlobResponse(GetProfileBlobRequest requestContent) {
		GetProfileBlobResponse response = new GetProfileBlobResponse(requestContent.getName());

		JsonArray value = new JsonArray();
		if ("friend_id_1".equals(requestContent.getUserId())) {
			value.add("game_id_1");
			value.add("game_id_2");
		} else if ("friend_id_2".equals(requestContent.getUserId())) {
			value.add("game_id_2");
			value.add("game_id_3");
		}
		response.setValue(value);

		return response;
	}

}
