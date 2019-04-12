package de.ascendro.f4m.service.session;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.cert.Certificate;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.websocket.RemoteEndpoint.Async;
import javax.websocket.SendResult;
import javax.websocket.Session;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.exception.server.F4MConnectionErrorException;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessageError;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.request.RequestInfoImpl;
import de.ascendro.f4m.service.session.pool.SessionPool;
import de.ascendro.f4m.service.session.pool.SessionStore;
import de.ascendro.f4m.service.session.pool.SessionStoreImpl;

public class WebSocketSessionWrapperTest {

	@Mock
	private Session session;
	@Mock
	private SessionPool sessionPool;
	@Mock
	private Config config;
	@Mock
	private JsonMessageUtil jsonMessageUtil;
	@Mock
	private LoggingUtil loggedMessageUtil;

	@Mock
	private Async async;

	private MessageSendHandler messageSendHandler;
	private SessionWrapper sessionWrapper;

	private ForwardedMessageSendHandler forwardedMessageSendHandler;
	@Mock
	private SessionWrapper requestSessionWrapper;

	private JsonMessage<JsonMessageContent> message;
	private JsonMessage<JsonMessageContent> requestMessage;
	private JsonMessage<JsonMessageContent> errorMessage;

	private SendResult nokSendResult;

	private RequestInfo requestInfo;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		sessionWrapper = spy(new WebSocketSessionWrapper(session, sessionPool, config, jsonMessageUtil,
				loggedMessageUtil) {
			final SessionStoreImpl sessionStore = new SessionStoreImpl(config, loggedMessageUtil, this);

			@Override
			public Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException {
				return null;
			}

			@Override
			public Certificate[] getLocalCertificates() {
				return null;
			}

			@Override
			protected MessageSendHandler createMessageSendHandler(JsonMessage<? extends JsonMessageContent> message) {
				messageSendHandler = super.createMessageSendHandler(message);
				return messageSendHandler;
			}

			@Override
			protected ForwardedMessageSendHandler createForwardedMessageSendHandler(
					JsonMessage<? extends JsonMessageContent> message, RequestInfo requestInfo) {
				forwardedMessageSendHandler = super.createForwardedMessageSendHandler(message, requestInfo);
				return forwardedMessageSendHandler;
			}

			@SuppressWarnings("unchecked")
			@Override
			public <S extends SessionStore> S getSessionStore() {
				return (S) sessionStore;
			}
		});

		message = new JsonMessage<>();
		message.setSeq(System.currentTimeMillis());
		message.setName("testMessage");

		requestMessage = new JsonMessage<>();
		requestMessage.setSeq(100L);
		requestMessage.setName("testRequestMessage");

		requestInfo = new RequestInfoImpl();
		requestInfo.setSourceMessage(requestMessage);
		requestInfo.setSourceSession(requestSessionWrapper);

		errorMessage = new JsonMessage<>();
		errorMessage.setError(new JsonMessageError(new F4MConnectionErrorException("Error while sending message")));
		when(jsonMessageUtil.createResponseErrorMessage(any(F4MException.class), any())).thenReturn(errorMessage);

		nokSendResult = new SendResult(new RuntimeException("Connection problem"));

		when(session.getAsyncRemote()).thenReturn(async);
	}

	@Test
	public void testCurrentSessionWrapperSendsErrorMessage() {
		sessionWrapper.sendAsynMessage(message);
		messageSendHandler.onResult(nokSendResult);

		verify(sessionWrapper, times(1)).sendAsynMessage(errorMessage);
		verify(requestSessionWrapper, never()).sendAsynMessage(errorMessage);
	}

	@Test
	public void testRequestSessionWrapperSendsErrorMessage() {
		sessionWrapper.sendAsynMessage(message, requestInfo, true);
		forwardedMessageSendHandler.onResult(nokSendResult);

		verify(requestSessionWrapper, times(1)).sendErrorMessage(eq(requestMessage),
				any(F4MConnectionErrorException.class));
		verify(sessionWrapper, never()).sendErrorMessage(any(), any(F4MConnectionErrorException.class));

		assertNotNull(sessionWrapper.getSessionStore().popRequest(message.getSeq()));
	}

}
