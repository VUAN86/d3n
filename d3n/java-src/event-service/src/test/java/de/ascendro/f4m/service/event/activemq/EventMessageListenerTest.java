package de.ascendro.f4m.service.event.activemq;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.event.model.publish.PublishMessageContent;
import de.ascendro.f4m.service.exception.validation.F4MValidationException;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.logging.LoggingUtil;

public class EventMessageListenerTest {

	private List<PublishMessageContent> publishMessageContents;
	private EventMessageListener eventMessgeListener;
	@Mock
	private TextMessage textMessage;
	@Mock
	private JsonMessageUtil jsonUtil;
	@Mock
	private Config config;
	@Mock
	private LoggingUtil loggingUtil;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		publishMessageContents = new ArrayList<>();

		eventMessgeListener = new EventMessageListener(jsonUtil, config, loggingUtil) {

			@Override
			protected void onMessage(PublishMessageContent publishMessageContent) {
				publishMessageContents.add(publishMessageContent);
			}
		};
	}

	@Test
	public void testOnMessage() throws IOException, JMSException, F4MValidationException {
		final String publishJson = IOUtils.toString(this.getClass().getClassLoader()
				.getResourceAsStream("de/ascendro/f4m/service/event/integration/publish.json"), "UTF-8");
		when(textMessage.getText()).thenReturn(publishJson);

		doReturn(new JsonMessage<PublishMessageContent>(EventMessageTypes.PUBLISH, new PublishMessageContent())).when(jsonUtil)
				.fromJson(publishJson);

		eventMessgeListener.onMessage(textMessage);
		assertFalse(publishMessageContents.isEmpty());
		assertNotNull(publishMessageContents.get(0));
	}

}
