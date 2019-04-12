package de.ascendro.f4m.client.json;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.UUID;

import javax.websocket.RemoteEndpoint.Async;
import javax.websocket.Session;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.di.client.JsonServiceClientEndpointProvider;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.request.RequestInfoImpl;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.session.SessionWrapperFactory;
import de.ascendro.f4m.service.session.pool.SessionPool;
import de.ascendro.f4m.service.session.pool.SessionStore;

public class JsonWebSocketClientSessionPoolImplTest {
	private static final URI TEST_URI = URI.create("wss://localhost:8988");
	private static final String SESSION_ID = UUID.randomUUID().toString();
	private static final ServiceConnectionInformation TEST_SERVICE_INFO = new ServiceConnectionInformation("testService", TEST_URI, "testNamespace");

	@Mock
	private SessionPool sessionPool;
	@Mock
	private JsonServiceClientEndpointProvider clientServiceEndpointProvider;

	@Mock
	private SessionWrapperFactory sessionWrapperFactory;
	@Mock
	private SessionWrapper sessionWrapper;
	@Mock
	private Session session;

	@Mock
	private SessionStore sessionStore;

	@Mock
	private Async async;

	private JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;
	private JsonMessage<JsonMessageContent> message;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		when(sessionWrapper.getSessionId()).thenReturn(SESSION_ID);
		when(sessionPool.getSession(SESSION_ID)).thenReturn(sessionStore);
		when(session.getAsyncRemote()).thenReturn(async);

		message = new JsonMessage<>();
		message.setSeq(System.currentTimeMillis());

		when(sessionWrapperFactory.create(eq(session))).thenReturn(sessionWrapper);

		jsonWebSocketClientSessionPool = spy(new JsonWebSocketClientSessionPoolImpl(clientServiceEndpointProvider,
				sessionPool, sessionWrapperFactory, new F4MConfigImpl()));

		doReturn(sessionWrapper).when(jsonWebSocketClientSessionPool).getSession(TEST_SERVICE_INFO);
	}

	@Test
		public void testSendAsyncMessageWithClientInfoWithRequestInfo() throws Exception {
			final RequestInfo requestInfo = new RequestInfoImpl(0);
	
			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(TEST_SERVICE_INFO, message, requestInfo);
	
			verify(sessionWrapper, times(1)).sendAsynMessage(message, requestInfo, true);
			verify(sessionStore, times(1)).registerRequest(message.getSeq(), requestInfo);
		}
}
