package de.ascendro.f4m.service.json.handler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.json.validator.JsonMessageValidator;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypes;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.session.pool.SessionStore;

public class JsonAuthenticationMessageHandlerTest {
	private static final String USER_ID = UUID.randomUUID().toString();
	private static final ClientInfo CLIENT_INFO = new ClientInfo(USER_ID);

	@Mock
	private SessionWrapper sessionWrapper;

	@Mock
	private LoggingUtil loggingUtil;
	
	@Mock
	private SessionStore sessionStore;

	private JsonAuthenticationMessageHandler authenticationMessageHandler;

	@Mock
	private JsonMessageValidator jsonMessageValidator;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		authenticationMessageHandler = spy(createJsonAuthenticationMessageHandler());

		when(sessionWrapper.getSessionStore()).thenReturn(sessionStore);
	}

	@Test
	public void testOnProcessUsingSupportedMessage() throws F4MException {
		final JsonMessage<JsonMessageContent> heartbeatMessage = new JsonMessage<>(
				ServiceRegistryMessageTypes.HEARTBEAT);
		when(authenticationMessageHandler.onUserDefaultMessage(heartbeatMessage)).thenReturn(null);
		authenticationMessageHandler.onProcess(createContextWithJsonMessageName(null));
		verify(authenticationMessageHandler, times(0)).onUserMessage(heartbeatMessage);
	}

	@Test
	public void testOnProcessUsingUnsupportedDefaultMessage() throws F4MException {
		authenticationMessageHandler.onProcess(createContextWithJsonMessageName("Any unsupported message"));
		verify(authenticationMessageHandler, times(1)).onUserMessage(ArgumentMatchers.<JsonMessage<? extends JsonMessageContent>>any());
	}
	
	@Test
	public void testOnAuthentication(){		
		final JsonMessage<EmptyJsonMessageContent> validMesssage = new JsonMessage<EmptyJsonMessageContent>();
		validMesssage.setClientInfo(CLIENT_INFO);
		
		assertEquals(CLIENT_INFO, authenticationMessageHandler.onAuthentication(new RequestContext(validMesssage)));
	}
	
	private RequestContext createContextWithJsonMessageName(String name) {
		return new RequestContext(new JsonMessage<JsonMessageContent>(name));
	}
	
	private JsonAuthenticationMessageHandler createJsonAuthenticationMessageHandler() {
		final DefaultJsonMessageHandler deafultJsonMessageHandler = new DefaultJsonMessageHandler() {
			@Override
			protected JsonMessageContent onHeartbeat(JsonMessage<? extends JsonMessageContent> heartbeatMessage) {
				return null;
			}
			
			@Override
			public SessionWrapper getSessionWrapper() {
				return sessionWrapper;
			}
		};
		deafultJsonMessageHandler.setLoggingUtil(loggingUtil);
		return deafultJsonMessageHandler;
	}

}
