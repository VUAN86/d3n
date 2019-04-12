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

public class MessageSendHandlerTest {

	@Mock
	private JsonMessageUtil jsonMessageUtil;
	@Mock
	private SessionWrapper sessionWrapper;
	private JsonMessage<JsonMessageContent> message;
	private JsonMessage<JsonMessageContent> errorMessage;

	private MessageSendHandler messageSendHandler;

	private SendResult okSendResult;
	private SendResult nokSendResult;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		message = new JsonMessage<>();
		message.setSeq(System.currentTimeMillis());
		message.setName("testMessage");

		errorMessage = new JsonMessage<>();
		errorMessage.setAck(message.getSeq());
		when(jsonMessageUtil.createResponseErrorMessage(any(F4MException.class), any()))
				.thenReturn(errorMessage);

		messageSendHandler = new MessageSendHandler(jsonMessageUtil, sessionWrapper, message);
		okSendResult = new SendResult();
		nokSendResult = new SendResult(new RuntimeException("Connection problem"));
	}

	@Test
	public void testOnResultOk() {
		messageSendHandler.onResult(okSendResult);

		verify(sessionWrapper, never()).sendAsynMessage(any());
	}

	@Test
	public void testOnResultNok() {
		messageSendHandler.onResult(nokSendResult);

		verify(sessionWrapper, times(1)).sendErrorMessage(eq(message), any(F4MConnectionErrorException.class));
	}

}
