package de.ascendro.f4m.service.json;

import static de.ascendro.f4m.service.json.JsonMessageDeserializer.CLIENT_INFO_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.ping.model.JsonPingMessageContent;
import de.ascendro.f4m.service.util.JsonLoader;

public class JsonMessageDeserializerTest {
	private static final String USER_ID = "userId-1";
	private static final String LANGUAGE = "en";
	private static final double HANDICAP = 5.7d;
	private static final String[] ROLES = {"ANONYMOUS", "REGISTERED"};
	private static final String[] EMAILS = {"mail@mail.de", "mail@mail.eu"};
	private static final String[] PHONES = {"+42"};
	
	private static final String APP_ID = "appId-1";
	private static final String TENANT_ID = "tenantId-1";
	private static final String DEVICE_UUID = "deviceUUID-1";
	
	private static final String IP = "127.0.0.1";
	
	private static final Long SEQ = 5L;
	private static final Long TIMESTAMP = 123456789L;
	private static final String CLIENT_ID = "CLIENT-1";
	private static final String PING_TEXT = "PING-124";
	
	private static final Type PING_MESSAGE_TYPE = new TypeToken<JsonPingMessageContent>() {
	}.getType();
	
	private final Gson gson = new Gson();
	private final JsonLoader jsonLoader = new JsonLoader(this);

	@Mock
	private JsonMessageTypeMap jsonMessageTypeMap;
	@Mock
	private JsonDeserializationContext context;
	
	private ClientInfo completeClientInfo;
	private JsonPingMessageContent pingMessageContent;
	private JsonMessage<JsonPingMessageContent> pingMessage;
	private String pingMessageAsJsonString;
	private JsonMessageDeserializer jsonMessageDeserializer;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		//Complete clientInfo
		completeClientInfo = new ClientInfo(USER_ID);
		completeClientInfo.setLanguage(LANGUAGE);
		completeClientInfo.setHandicap(HANDICAP);
		completeClientInfo.setRoles(ROLES);
		completeClientInfo.setEmails(EMAILS);
		completeClientInfo.setPhones(PHONES);
		
		completeClientInfo.setAppId(APP_ID);
		completeClientInfo.setTenantId(TENANT_ID);
		completeClientInfo.setDeviceUUID(DEVICE_UUID);
		
		completeClientInfo.setIp(IP);		
		completeClientInfo.setClientId(CLIENT_ID);		
		
		//content
		pingMessageContent = new JsonPingMessageContent(PING_TEXT);
		
		//message
		pingMessage = new JsonMessage<JsonPingMessageContent>(
				JsonPingMessageContent.MESSAGE_NAME);
		pingMessage.setSeq(SEQ);
		pingMessage.setTimestamp(TIMESTAMP);
		pingMessage.setContent(pingMessageContent);
		pingMessage.setClientInfo(completeClientInfo);
		
		when(jsonMessageTypeMap.get(JsonPingMessageContent.MESSAGE_NAME)).thenReturn(PING_MESSAGE_TYPE);
		when(context.deserialize(eq(gson.toJsonTree(pingMessageContent)), isNotNull()))
			.thenReturn(pingMessageContent);
		
		when(context.deserialize(eq(gson.toJsonTree(SEQ)), any())).thenReturn(SEQ);
		when(context.deserialize(eq(gson.toJsonTree(TIMESTAMP)), any())).thenReturn(TIMESTAMP);
		when(context.deserialize(eq(gson.toJsonTree(CLIENT_ID)), any())).thenReturn(CLIENT_ID);
		
		when(context.deserialize(any(JsonElement.class), eq(CLIENT_INFO_TYPE)))
			.thenAnswer(new Answer<ClientInfo>() {
				@Override
				public ClientInfo answer(InvocationOnMock invocation) throws Throwable {
					return gson.fromJson((JsonElement) invocation.getArgument(0), CLIENT_INFO_TYPE);
				}
			});
		
		
		pingMessageAsJsonString = jsonLoader.getPlainTextJsonFromResources("pingMessage.json");
		jsonMessageDeserializer = new JsonMessageDeserializer(jsonMessageTypeMap);
	}

	@Test
	public void testDeserializePingMessageFromGateway() {
		final JsonElement pingMessageAsJsonElement = gson.fromJson(pingMessageAsJsonString, JsonElement.class);
		final JsonMessage<? extends JsonMessageContent> resultPingMessage = jsonMessageDeserializer
				.deserialize(pingMessageAsJsonElement, PING_MESSAGE_TYPE, context);

		assertNotNull(resultPingMessage);
		assertEquals(SEQ, resultPingMessage.getSeq());
		assertEquals(TIMESTAMP, resultPingMessage.getTimestamp());
		assertEquals(CLIENT_ID, resultPingMessage.getClientInfo().getClientId());
		assertEquals(completeClientInfo, resultPingMessage.getClientInfo());

		assertNotNull(resultPingMessage.getContent());
		assertTrue(resultPingMessage.getContent() instanceof JsonPingMessageContent);

		final JsonPingMessageContent resultPingMessageContent = (JsonPingMessageContent) resultPingMessage.getContent();
		assertEquals(resultPingMessageContent.getClass(), pingMessageContent.getClass());
		assertEquals(resultPingMessageContent.getPing(), PING_TEXT);
	}
	
	@Test
	public void testDeserializeNoClient() throws IOException{
		final String noClientJson = jsonLoader.getPlainTextJsonFromResources("noClientJson.json");
		final JsonElement noClientAsJsonElement = gson.fromJson(noClientJson, JsonElement.class);
		final JsonMessage<? extends JsonMessageContent> resultPingMessage = jsonMessageDeserializer
				.deserialize(noClientAsJsonElement, PING_MESSAGE_TYPE, context);

		assertNotNull(resultPingMessage);
		assertEquals(SEQ, resultPingMessage.getSeq());
		assertEquals(TIMESTAMP, resultPingMessage.getTimestamp());
		assertNull(resultPingMessage.getClientInfo());
	}
	
	@Test
	public void testDeserializeNoClientId() throws IOException{
		final String noClientIdJson = jsonLoader.getPlainTextJsonFromResources("noClientIdJson.json");
		final JsonElement noClientIdAsJsonElement = gson.fromJson(noClientIdJson, JsonElement.class);
		final JsonMessage<? extends JsonMessageContent> resultPingMessage = jsonMessageDeserializer
				.deserialize(noClientIdAsJsonElement, PING_MESSAGE_TYPE, context);

		assertNotNull(resultPingMessage);
		assertEquals(SEQ, resultPingMessage.getSeq());
		assertEquals(TIMESTAMP, resultPingMessage.getTimestamp());
		assertNull(resultPingMessage.getClientInfo().getClientId());

		completeClientInfo.setClientId(null);
		assertEquals(completeClientInfo, resultPingMessage.getClientInfo());
	}
		
	@Test
	public void testDeserializeOnEmptyObject() {
		JsonElement jsonMessageElement = gson.toJsonTree(new Object());
		try {
			jsonMessageDeserializer.deserialize(jsonMessageElement, null, context);
			fail();
		} catch (F4MValidationFailedException e) {
			assertEquals("Mandatory attribute 'message' is missing", e.getMessage());
		}
	}

	@Test
	public void testIsOtherField() {
		assertFalse(jsonMessageDeserializer.isOtherField(JsonMessage.MESSAGE_CONTENT_PROPERTY));
		assertFalse(jsonMessageDeserializer.isOtherField(JsonMessage.MESSAGE_NAME_PROPERTY));
		assertTrue(jsonMessageDeserializer.isOtherField("ABC"));
	}
	
	@Test
	public void testParseClientInfo() throws IOException, IllegalAccessException, InstantiationException,
			InvocationTargetException, NoSuchMethodException {
		// full profile and appConfig
		final ClientInfo parsedCompleteClientInfo = jsonMessageDeserializer.parseClientInfo(context,
				readClientInfo("completeClientInfo.json"));
		completeClientInfo.setClientId(null);
		assertEquals(completeClientInfo, parsedCompleteClientInfo);

		// only mandatory profile and appConfig
		final ClientInfo actualOnlyMandatoryClientInfo = jsonMessageDeserializer.parseClientInfo(context,
				readClientInfo("onlyMandatoryClientInfo.json"));
		final ClientInfo expectedOnlyMandatoryClientInfo = new ClientInfo(USER_ID);
		expectedOnlyMandatoryClientInfo.setRoles(ROLES);
		expectedOnlyMandatoryClientInfo.setTenantId(TENANT_ID);
		expectedOnlyMandatoryClientInfo.setIp(IP);
		assertEquals(expectedOnlyMandatoryClientInfo, actualOnlyMandatoryClientInfo);

		// mandatory profile and missing app config
		final ClientInfo actualOnlyProfileClientInfo = jsonMessageDeserializer.parseClientInfo(context,
				readClientInfo("onlyProfileClientInfo.json"));
		final ClientInfo expectedOnlyProfileClientInfo = new ClientInfo(USER_ID);
		expectedOnlyProfileClientInfo.setRoles(ROLES);
		assertEquals(expectedOnlyProfileClientInfo, actualOnlyProfileClientInfo);

		// empty client Info
		final ClientInfo emptyClientInfo = jsonMessageDeserializer.parseClientInfo(context,
				readClientInfo("emptyClientInfo.json"));
		assertEquals(new ClientInfo(), emptyClientInfo);
	}
	
	@Test
	public void testSerialize(){
		final JsonElement expectedPingMessage = gson.fromJson(pingMessageAsJsonString, JsonElement.class);
		final JsonElement serializedPingMessage = jsonMessageDeserializer.serialize(pingMessage, PING_MESSAGE_TYPE, null);
		assertEquals(expectedPingMessage, serializedPingMessage);
	}
	
	private JsonElement readClientInfo(String localJsonFileName) throws IOException{
		try (final InputStream localJsonFileInputStream = this.getClass().getResourceAsStream(localJsonFileName)) {
			final String clientInfoJson = IOUtils.toString(localJsonFileInputStream);
			return gson.fromJson(clientInfoJson, JsonObject.class).get(JsonMessage.MESSAGE_CLIENT_INFO_PROPERTY);
		}
	}

}
