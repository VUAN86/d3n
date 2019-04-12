package de.ascendro.f4m.service.analytics.client;

import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.service.analytics.module.achievement.processor.AchievementsLoader;
import de.ascendro.f4m.service.analytics.notification.NotificationCommon;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.event.model.NotifySubscriberMessageContent;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.util.EventServiceClient;

@RunWith(MockitoJUnitRunner.class)
public class AnalyticsServiceClientMessageHandlerTest {
	private static final String ACHIEVEMENTS_REFRESH_EVENT_TOPIC = "analytics.achievements.refresh";
	
	@Mock
	private NotificationCommon notificationCommon;
	
	@Mock
    private Tracker tracker;
	
	@Mock
	private EventServiceClient eventServiceClient;
	
	@Mock
    private AchievementsLoader achievementsLoader;
	
	private AnalyticsServiceClientMessageHandler service; 

	@Before
	public void setUp() throws Exception {
		service = new AnalyticsServiceClientMessageHandler(notificationCommon, tracker, eventServiceClient, achievementsLoader);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testAchievementRefreshEvent() {
		RequestContext context = mock(RequestContext.class);
		
		JsonMessage<JsonMessageContent> message = mock(JsonMessage.class);
		when(message.getType(eq(EventMessageTypes.class))).thenReturn(EventMessageTypes.NOTIFY_SUBSCRIBER);
		
		when(context.getMessage()).thenReturn(message);
		
		NotifySubscriberMessageContent content = mock(NotifySubscriberMessageContent.class);
		when(content.getTopic()).thenReturn(ACHIEVEMENTS_REFRESH_EVENT_TOPIC);
		when(message.getContent()).thenReturn(content);
		
		JsonMessageContent result = service.onUserMessage(context);
		
		assertNull(result);
		verify(achievementsLoader).loadTenants();
	}

}
