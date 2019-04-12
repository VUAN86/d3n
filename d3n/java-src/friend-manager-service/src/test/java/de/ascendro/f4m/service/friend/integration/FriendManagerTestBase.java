package de.ascendro.f4m.service.friend.integration;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.auth.AuthMessageTypes;
import de.ascendro.f4m.service.friend.di.FriendManagerDefaultMessageTypeMapper;
import de.ascendro.f4m.service.friend.model.schema.FriendManagerMessageSchemaMapper;
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
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.usermessage.model.SendWebsocketMessageRequest;
import de.ascendro.f4m.service.usermessage.model.WebsocketMessageType;

public class FriendManagerTestBase extends F4MServiceWithMockIntegrationTestBase {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(F4MServiceWithMockIntegrationTestBase.class);

	protected AerospikeDao aerospikeDao;
	protected JsonUtil jsonUtil;

	protected Injector injector = jettyServerRule.getServerStartup().getInjector();

	protected final ReceivedMessageCollector mockServiceReceivedMessageServerCollector = new ReceivedMessageCollector(){
		@Override
		public SessionWrapper getSessionWrapper() {
			return Mockito.mock(SessionWrapper.class);
		};
	};

	@Override
	protected TestClientInjectionClasses getTestClientInjectionClasses() {
		return new TestClientInjectionClasses(FriendManagerDefaultMessageTypeMapper.class,
				FriendManagerMessageSchemaMapper.class);
	}

	@Override
	protected ServiceStartup getServiceStartup() {
		return new FriendManagerServiceStartupUsingAerospikeMock(DEFAULT_TEST_STAGE);
	}

	@Override
	protected ThrowingFunction<RequestContext, JsonMessageContent> getMessageHandlerMock() {
		return ctx -> onReceivedMessage(ctx);
	}

	@Override
	protected void configureInjectionModuleMock(BindableAbstractModule module) {
		module.bindToModule(JsonMessageTypeMap.class).to(FriendManagerDefaultMessageTypeMapper.class);
		module.bindToModule(JsonMessageSchemaMap.class).to(FriendManagerMessageSchemaMapper.class);
	}

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();

		assertServiceStartup(AuthMessageTypes.SERVICE_NAME);

		aerospikeDao = injector.getInstance(AerospikeDao.class);
		jsonUtil = clientInjector.getInstance(JsonUtil.class);
	}

	protected JsonMessageContent onReceivedMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded)
			throws Exception {
		throw new RuntimeException("Not implemented");
	}

	protected JsonMessageContent onReceivedMessage(RequestContext context) throws Exception {
		mockServiceReceivedMessageServerCollector.onProcess(context);
		final JsonMessage<? extends JsonMessageContent> message = context.getMessage();
		LOGGER.debug("Mocked Service received request {}", message.getContent());
		return onReceivedMessage(message);
	}

	protected void assertPlayerAddedNotifications(String...players) {
		RetriedAssert.assertWithWait(() -> assertThat(mockServiceReceivedMessageServerCollector
				.getMessagesByType(UserMessageMessageTypes.SEND_WEBSOCKET_MESSAGE), hasSize(players.length)));
        
		if (players.length > 0) {
	        final List<SendWebsocketMessageRequest> notificationRequests = mockServiceReceivedMessageServerCollector
					.<SendWebsocketMessageRequest>getMessagesByType(UserMessageMessageTypes.SEND_WEBSOCKET_MESSAGE).stream()
					.map(m -> m.getContent())
					.filter(content -> content.getType() == WebsocketMessageType.PLAYER_ADDED_TO_GROUP)
					.collect(Collectors.toList());
			assertThat(notificationRequests.stream().map(r -> r.getUserId()).collect(Collectors.toSet()), containsInAnyOrder(players));
		}
		mockServiceReceivedMessageServerCollector.clearReceivedMessageList();
	}

}
