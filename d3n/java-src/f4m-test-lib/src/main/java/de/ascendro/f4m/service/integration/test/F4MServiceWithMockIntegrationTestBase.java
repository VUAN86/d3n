package de.ascendro.f4m.service.integration.test;

import java.net.URI;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.RuleChain;

import com.google.inject.Injector;

import de.ascendro.f4m.client.WebSocketClientSessionPool;
import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.integration.F4MAssert;
import de.ascendro.f4m.service.integration.rule.MockServiceRule;
import de.ascendro.f4m.service.integration.rule.MockServiceRule.BindableAbstractModule;
import de.ascendro.f4m.service.integration.rule.MockServiceRule.ThrowingFunction;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.session.pool.SessionPool;

/**
 * Base test to be extended by all integration tests, which start a real web-server and tries to communicate with
 * service to be tested.
 */
public abstract class F4MServiceWithMockIntegrationTestBase extends F4MIntegrationTestBase {
	protected static final int MOCK_JETTY_PORT = 9876;
	protected static final URI DUMMY_SERVICE_URI = URI.create("wss://localhost:" + MOCK_JETTY_PORT);

	private WebSocketClientSessionPool webSocketClientSessionPool;
	private SessionPool mockServiceSessionPool;

	protected MockServiceRule mockService;

	@Rule
	public RuleChain serviceStartupChain = RuleChain.outerRule(getMockService()).around(jettyServerRule);

	@BeforeClass
	public static void setUpClass() {
		System.setProperty(F4MConfigImpl.SERVICE_REGISTRY_URI, DUMMY_SERVICE_URI.toString());
	}

	protected MockServiceRule getMockService() {
		mockService = new MockServiceRule(MOCK_JETTY_PORT, errorCollector, getServiceStartup()) {
			@Override
			protected void configureInjectionModule(BindableAbstractModule module) {
				configureInjectionModuleMock(module);
			}

			@Override
			protected ThrowingFunction<RequestContext, JsonMessageContent> getMessageHandler() {
				return getMessageHandlerMock();
			}
		};
		return mockService;
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		final Injector injector = jettyServerRule.getServerStartup().getInjector();
		webSocketClientSessionPool = injector.getInstance(JsonWebSocketClientSessionPool.class);
		mockServiceSessionPool = mockService.getServerStartup().getInjector().getInstance(SessionPool.class);
	}

	protected abstract ThrowingFunction<RequestContext, JsonMessageContent> getMessageHandlerMock();

	protected abstract void configureInjectionModuleMock(BindableAbstractModule module);

	protected void sendAsynMessageAsMockClient(JsonMessage<? extends JsonMessageContent> message) {
		final List<SessionWrapper> serviceOpenClientSessions = webSocketClientSessionPool.getOpenSessions();
		F4MAssert.assertSize(1, serviceOpenClientSessions);
		final SessionWrapper clientSessionToMockService = serviceOpenClientSessions.get(0);
		final String mockServiceServerSessionId = changeSessionIdDirection(clientSessionToMockService.getSessionId());
		final SessionWrapper mockServiceServerSession = mockServiceSessionPool.getSession(mockServiceServerSessionId)
				.getSessionWrapper();
		mockServiceServerSession.sendAsynMessage(message);
	}
}
