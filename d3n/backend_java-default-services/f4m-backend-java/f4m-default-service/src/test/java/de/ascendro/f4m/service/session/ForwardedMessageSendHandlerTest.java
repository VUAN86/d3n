package de.ascendro.f4m.service.session;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.websocket.SendResult;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.exception.server.F4MConnectionErrorException;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.request.RequestInfoImpl;

public class ForwardedMessageSendHandlerTest {

	@Mock
	private JsonMessageUtil jsonMessageUtil;
	@Mock
	private SessionWrapper sessionWrapper;
	@Mock
	private SessionWrapper requestSessionWrapper;

	private JsonMessage<JsonMessageContent> requestMessage;
	private JsonMessage<JsonMessageContent> errorMessage;
	private RequestInfo requestInfo;

	private ForwardedMessageSendHandler forwardedMessageSendHandler;

	private SendResult okSendResult;
	private SendResult nokSendResult;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		requestMessage = new JsonMessage<>();
		requestMessage.setSeq(100L);
		requestMessage.setName("testRequestMessage");

		requestInfo = new RequestInfoImpl();
		requestInfo.setSourceMessage(requestMessage);
		requestInfo.setSourceSession(requestSessionWrapper);

		errorMessage = new JsonMessage<>();
		errorMessage.setAck(requestMessage.getSeq());
		when(jsonMessageUtil.createResponseErrorMessage(any(F4MException.class), any()))
				.thenReturn(errorMessage);

		forwardedMessageSendHandler = new ForwardedMessageSendHandler(jsonMessageUtil, sessionWrapper, errorMessage,
				requestInfo);
		okSendResult = new SendResult();
		nokSendResult = new SendResult(new RuntimeException("Connection problem"));
	}

	@Test
	public void testOnResultOk() {
		forwardedMessageSendHandler.onResult(okSendResult);

		verify(sessionWrapper, never()).sendAsynMessage(any());
		verify(requestSessionWrapper, never()).sendAsynMessage(any());
	}

	@Test
	public void testOnResultNok() {
		forwardedMessageSendHandler.onResult(nokSendResult);

		verify(sessionWrapper, never()).sendAsynMessage(any());
		verify(requestSessionWrapper, times(1)).sendErrorMessage(eq(requestMessage),
				any(F4MConnectionErrorException.class));
	}

}
