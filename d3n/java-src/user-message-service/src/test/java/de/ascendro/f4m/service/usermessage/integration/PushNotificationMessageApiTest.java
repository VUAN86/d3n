package de.ascendro.f4m.service.usermessage.integration;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import de.ascendro.f4m.server.onesignal.OneSignalWrapper;
import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.json.model.ISOLanguage;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;

public class PushNotificationMessageApiTest extends UserMessageServiceApiTestBase {
	static final String TEST_JSON_LOCATION = "amazon/";

	private OneSignalWrapper oneSignalWrapper;

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		oneSignalWrapper = jettyServerRule.getServerStartup().getInjector().getInstance(OneSignalWrapper.class);
	}

	@Override
	protected ServiceStartup getServiceStartup() {
		return AmazonMessageApiTest.createTestUserMessageServiceStartup();
	}

	@Override
	public void verifyResponseOnRequest(String requestPath, String expectedResponsePath) throws Exception {
		super.verifyResponseOnRequest(TEST_JSON_LOCATION + requestPath, TEST_JSON_LOCATION + expectedResponsePath);
	}

	@Test
	public void testMobilePushToUser() throws Exception {
		when(oneSignalWrapper.sendMobilePushWithScheduledTime(anyString(), any(), any(), any(), any(), any()))
				.thenReturn("mockedPushMessageId1");
		verifyResponseOnRequest("sendUserPush.json", "sendUserPushResponseExpected.json");
		ArgumentCaptor<Map<ISOLanguage, String>> arg = messageMapCaptor();
		verify(oneSignalWrapper).sendMobilePushWithScheduledTime(eq("17"), any(), arg.capture(), eq(null), any(), any());
		assertThat(arg.getValue().get(ISOLanguage.EN), equalTo("mobile push to all user's devices test"));
		assertThat(arg.getValue().values(), hasSize(2));
	}

	@Test
	public void testMobilePushToTopic() throws Exception {
		when(oneSignalWrapper.sendMobilePushToTag(anyString(), any(), any()))
			.thenReturn("mockedPushMessageId1").thenReturn("mockedPushMessageId2");
		verifyResponseOnRequest("sendTopicPushRequest.json", "sendTopicPushResponseExpected.json");
		ArgumentCaptor<Map<ISOLanguage, String>> arg = messageMapCaptor();
		verify(oneSignalWrapper).sendMobilePushToTag(eq("topic_value"), eq("appIdValue1"), arg.capture());
		verify(oneSignalWrapper).sendMobilePushToTag(eq("topic_value"), eq("appIdValue2"), arg.capture());
		assertThat(arg.getValue().get(ISOLanguage.EN), equalTo("mobile push to all user's devices test"));
		assertThat(arg.getValue().values(), hasSize(2));
	}

	@Test
	public void testSendMobilePushWithTranslation() throws Exception {
		when(oneSignalWrapper.sendMobilePushWithScheduledTime(any(), any(), any(), any(), any(), any()))
				.thenReturn("mockedPushMessageId1").thenReturn("mockedPushMessageId2");
		String request = jsonLoader.getPlainTextJsonFromResources("sendUserPushWithTranslation.json");
		JsonMessage<? extends JsonMessageContent> jsonContent = jsonUtil.fromJson(request);
		jsonWebSocketClientSessionPool.sendAsyncMessage(getServiceConnectionInformation(), jsonContent);
		assertReceivedMessagesWithWait(UserMessageMessageTypes.SEND_USER_PUSH_RESPONSE);
		ArgumentCaptor<Map<ISOLanguage, String>> arg = messageMapCaptor();
		verify(oneSignalWrapper).sendMobilePushWithScheduledTime(any(), any(), arg.capture(), any(), any(), any());

		assertEquals(arg.getValue().get(ISOLanguage.EN), "Hello John");

	}

	@SuppressWarnings("unchecked")
	public static ArgumentCaptor<Map<ISOLanguage, String>> messageMapCaptor() {
		ArgumentCaptor<Map<ISOLanguage, String>> arg = ArgumentCaptor.forClass(Map.class);
		return arg;
	}
}
