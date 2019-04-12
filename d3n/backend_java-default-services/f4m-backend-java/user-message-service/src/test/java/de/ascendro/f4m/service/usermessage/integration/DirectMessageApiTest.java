package de.ascendro.f4m.service.usermessage.integration;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.google.gson.JsonObject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Modules;

import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.server.session.GlobalClientSessionDao;
import de.ascendro.f4m.server.session.GlobalClientSessionInfo;
import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.json.model.ISOLanguage;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.usermessage.UserMessageServiceStartup;
import de.ascendro.f4m.service.usermessage.model.NewWebsocketMessageRequest;
import de.ascendro.f4m.service.usermessage.model.NewWebsocketMessageResponse;
import de.ascendro.f4m.service.usermessage.model.WebsocketMessageType;
import de.ascendro.f4m.server.onesignal.OneSignalWrapper;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;

public class DirectMessageApiTest extends UserMessageServiceApiTestBase {
	
	private static final String PAYMENT_SUCCESS_MESSAGE = "{\"type\":\"PAYMENT_SUCCESS\",\"transactionId\":\"123456\",\"amount\":17,\"currency\":\"EUR\""
					+ ",\"text\":\"We are happy to inform you that your payment transaction of 17 EUR has been successfully completed."
					+ " Now you should see the changed values in your player account overview. Happy playing.\"}";
	private PrimaryKeyUtil<String> primaryKeyUtil;
	private List<NewWebsocketMessageRequest> receivedGatewayCalls = Collections.synchronizedList(new ArrayList<>());
	private NewWebsocketMessageResponse.Status gatewayMessageDeliveryStatus = NewWebsocketMessageResponse.Status.FAIL;
	private GlobalClientSessionDao globalClientSessionDao;
	
	private UserMessageServiceStartup userMessageServiceStartup;
	private OneSignalWrapper oneSignalWrapper;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		final Injector injector = jettyServerRule.getServerStartup().getInjector();
		primaryKeyUtil = injector.getInstance(Key.get(new TypeLiteral<PrimaryKeyUtil<String>>() {}));
		GlobalClientSessionInfo clientSession = new GlobalClientSessionInfo();
		clientSession.setGatewayURL(DUMMY_SERVICE_URI.toString());
		when(globalClientSessionDao.getGlobalClientSessionInfoByUserId(anyString())).thenReturn(clientSession);
		oneSignalWrapper = jettyServerRule.getServerStartup().getInjector().getInstance(OneSignalWrapper.class);
	}
	
	@After
	public void tearDown(){
		userMessageServiceStartup = null;
	}

	@Override
	protected ServiceStartup getServiceStartup() {
		if(userMessageServiceStartup == null){
			userMessageServiceStartup =  new UserMessageServiceStartup(DEFAULT_TEST_STAGE) {
				@Override
				protected Module getModule() {
					return Modules.override(super.getModule()).with(new UserMessageMockModule() {
						@SuppressWarnings("unchecked")
						@Override
						protected void configure() {
							super.configure();
							bind(Key.get(new TypeLiteral<PrimaryKeyUtil<String>>() {})).toInstance(mock(PrimaryKeyUtil.class));
							globalClientSessionDao = mock(GlobalClientSessionDao.class);
							bind(GlobalClientSessionDao.class).toInstance(globalClientSessionDao);
						}
					});
				}
			};
		}
		return userMessageServiceStartup;
	}

	@Test
	public void testSendWebsocketMessage() throws Exception {
		when(primaryKeyUtil.generateId()).thenReturn("mockedDirectMessageId");
		when(oneSignalWrapper.sendMobilePushToUser(anyString(), anyString(), mockedLanguageMap(), any(), any())).thenReturn("mockedNotificationId");
		verifyResponseOnRequest("sendWebsocketMessage.json", "sendWebsocketMessageResponseExpected.json", KeyStoreTestUtil.REGISTERED_CLIENT_INFO);
		ArgumentCaptor<Map<ISOLanguage, String>> arg = PushNotificationMessageApiTest.messageMapCaptor();
		verify(oneSignalWrapper).sendMobilePushToUser(eq("17"), eq(KeyStoreTestUtil.APP_ID), arg.capture(), eq(WebsocketMessageType.PAYMENT_SUCCESS.toString()), any());
		assertThat(arg.getValue().get(ISOLanguage.EN), equalTo(PAYMENT_SUCCESS_MESSAGE));
		assertThat(arg.getValue().values(), hasSize(2));

		verifyResponseOnRequest("listUnreadWebsocketMessagesDevice1.json", "listUnreadWebsocketMessagesTranslatedResponseExpected.json");
		verifyResponseOnRequest("listUnreadWebsocketMessagesDevice1.json", "listUnreadWebsocketMessagesResponseEmptyExpected.json");
		verifyResponseOnRequest("listUnreadWebsocketMessagesDevice2.json", "listUnreadWebsocketMessagesResponseEmptyExpected.json");
	}

	@Test
	public void testSendWebsocketMessageNoClientInfo() throws Exception {
		when(primaryKeyUtil.generateId()).thenReturn("mockedDirectMessageId");
		when(oneSignalWrapper.sendMobilePushToUser(anyString(), anyString(), mockedLanguageMap(), any(), any())).thenReturn("mockedNotificationId");
		verifyResponseOnRequest("sendWebsocketMessage.json", "sendWebsocketMessageResponseExpectedNoClientInfo.json");
		verifyZeroInteractions(oneSignalWrapper);

		verifyResponseOnRequest("listUnreadWebsocketMessagesDevice1.json", "listUnreadWebsocketMessagesTranslatedResponseExpected.json");
		verifyResponseOnRequest("listUnreadWebsocketMessagesDevice1.json", "listUnreadWebsocketMessagesResponseEmptyExpected.json");
		verifyResponseOnRequest("listUnreadWebsocketMessagesDevice2.json", "listUnreadWebsocketMessagesResponseEmptyExpected.json");
	}
	
	@Test
	public void testSendWebsocketMessageWithForwardToUserViaGateway() throws Exception {
		when(primaryKeyUtil.generateId()).thenReturn("mockedDirectMessageId");
		when(oneSignalWrapper.sendMobilePushToUser(anyString(), anyString(), mockedLanguageMap(), any(), any())).thenReturn("mockedNotificationId");
		gatewayMessageDeliveryStatus = NewWebsocketMessageResponse.Status.SUCCESS;
		verifyResponseOnRequest("sendWebsocketMessage.json", "sendWebsocketMessageResponseExpected.json", KeyStoreTestUtil.REGISTERED_CLIENT_INFO);
		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedGatewayCalls.size()));
		NewWebsocketMessageRequest messageToGateway = receivedGatewayCalls.get(0);
		assertEquals(PAYMENT_SUCCESS_MESSAGE, messageToGateway.getMessage());
		assertEquals(WebsocketMessageType.PAYMENT_SUCCESS, messageToGateway.getType());
		assertEquals("123456", getPayloadAttribute(messageToGateway, "transactionId"));
		assertEquals("17", getPayloadAttribute(messageToGateway, "amount"));
		assertEquals("EUR", getPayloadAttribute(messageToGateway, "currency"));
		
		verifyResponseOnRequest("listUnreadWebsocketMessagesDevice1.json", "listUnreadWebsocketMessagesResponseEmptyExpected.json");
		verifyResponseOnRequest("listUnreadWebsocketMessagesDevice2.json", "listUnreadWebsocketMessagesResponseEmptyExpected.json");
		verify(globalClientSessionDao).getGlobalClientSessionInfoByUserId("17");
	}

	private String getPayloadAttribute(NewWebsocketMessageRequest messageToGateway, String memberName) {
		return ((JsonObject) messageToGateway.getPayload()).get(memberName).getAsString();
	}
	
	@Test
	public void testSendWebsocketMessageToAllUserDevices() throws Exception {
		when(primaryKeyUtil.generateId()).thenReturn("mockedDirectMessageId");
		when(primaryKeyUtil.createPrimaryKey(any())).thenReturn("userMessage:ID"); 
		verifyResponseOnRequest("sendWebsocketMessageToAllUserDevicesMessage.json", "sendWebsocketMessageToAllUserDevicesResponseExpected.json");
		verifyResponseOnRequest("listUnreadWebsocketMessagesDevice1.json", "listUnreadWebsocketMessagesResponseExpected.json");
		verifyResponseOnRequest("listUnreadWebsocketMessagesDevice1.json", "listUnreadWebsocketMessagesResponseEmptyExpected.json");
		verifyResponseOnRequest("listUnreadWebsocketMessagesDevice2.json", "listUnreadWebsocketMessagesResponseExpected.json");
		verifyResponseOnRequest("listUnreadWebsocketMessagesDevice2.json", "listUnreadWebsocketMessagesResponseEmptyExpected.json");
		verify(primaryKeyUtil).generateId();
	}
	
	@Test
	public void testSendWebsocketMessageToAllUserDevicesWithForwardToUserViaGateway() throws Exception {
		when(primaryKeyUtil.generateId()).thenReturn("mockedDirectMessageId");
		when(primaryKeyUtil.createPrimaryKey(any())).thenReturn("userMessage:ID");
		gatewayMessageDeliveryStatus = NewWebsocketMessageResponse.Status.SUCCESS;
		verifyResponseOnRequest("sendWebsocketMessageToAllUserDevicesMessage.json", "sendWebsocketMessageToAllUserDevicesResponseExpected.json");
		//should we wait here for response from gateway to be processed?
		verifyResponseOnRequest("listUnreadWebsocketMessagesDevice1.json", "listUnreadWebsocketMessagesResponseEmptyExpected.json");
		verifyResponseOnRequest("listUnreadWebsocketMessagesDevice2.json", "listUnreadWebsocketMessagesResponseExpected.json");
		verifyResponseOnRequest("listUnreadWebsocketMessagesDevice2.json", "listUnreadWebsocketMessagesResponseEmptyExpected.json");
		verify(primaryKeyUtil).generateId();
		verify(globalClientSessionDao).getGlobalClientSessionInfoByUserId("17");
	}

	@Test
	public void testCancelUserPush() throws Exception {
		verifyResponseOnRequest("cancelUserPush.json", "cancelUserPushResponseExpected.json");
		verify(oneSignalWrapper).cancelNotification(eq("notification-abc"));
		verify(oneSignalWrapper).cancelNotification(eq("notification-123"));
	}

	@Test
	public void testSendUserPush() throws Exception {
		when(oneSignalWrapper.sendMobilePushWithScheduledTime(anyString(), anyString(), mockedLanguageMap(), any(), any(), any(ZonedDateTime.class))).thenReturn("mockedNotificationId");
		verifyResponseOnRequest("sendUserPushMessage.json", "sendUserPushResponseExpected.json");
		ArgumentCaptor<Map<ISOLanguage, String>> arg = PushNotificationMessageApiTest.messageMapCaptor();
		verify(oneSignalWrapper).sendMobilePushWithScheduledTime(eq("17"), eq(KeyStoreTestUtil.APP_ID), arg.capture(), eq(WebsocketMessageType.PAYMENT_SUCCESS.toString()), any(), any(ZonedDateTime.class));
		assertThat(arg.getValue().get(ISOLanguage.EN), equalTo(PAYMENT_SUCCESS_MESSAGE));
		assertThat(arg.getValue().values(), hasSize(2));
	}

	@SuppressWarnings("unchecked")
	private Map<ISOLanguage, String> mockedLanguageMap() {
		return (Map<ISOLanguage, String>) any(Map.class);
	}
	
	@Override
	public void verifyResponseOnRequest(String requestPath, String expectedResponsePath) throws Exception {
		super.verifyResponseOnRequest("direct/" + requestPath, "direct/" + expectedResponsePath);
	}
	
	public void verifyResponseOnRequest(String requestPath, String expectedResponsePath, ClientInfo clientInfo) throws Exception {
		super.verifyResponseOnRequest("direct/" + requestPath, clientInfo, "direct/" + expectedResponsePath, true);
	}

	@Override
	protected JsonMessageContent onReceivedMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded)
			throws Exception {
		if (UserMessageMessageTypes.NEW_WEBSOCKET_MESSAGE == originalMessageDecoded.getType(UserMessageMessageTypes.class)) {
			NewWebsocketMessageResponse response = new NewWebsocketMessageResponse();
			response.setDeliveryStatus(gatewayMessageDeliveryStatus);
			response.setDeviceUUID("device_id_1");
			receivedGatewayCalls.add((NewWebsocketMessageRequest)originalMessageDecoded.getContent());
			return response;
		} else {
			return super.onReceivedMessage(originalMessageDecoded);
		}
	}
}
