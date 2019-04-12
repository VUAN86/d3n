package de.ascendro.f4m.service.profile.integration;

import static de.ascendro.f4m.service.integration.F4MAssert.assertSize;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ADMIN_CLIENT_INFO;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_USER_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.REGISTERED_CLIENT_INFO;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Injector;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.profile.util.ApplicationConfigrationPrimaryKeyUtil;
import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.annotation.ClientMessageHandler;
import de.ascendro.f4m.service.dao.aerospike.AerospikeClientMock;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.event.model.publish.PublishMessageContent;
import de.ascendro.f4m.service.exception.ExceptionCodes;
import de.ascendro.f4m.service.friend.FriendManagerMessageTypes;
import de.ascendro.f4m.service.friend.model.api.buddy.BuddyAddForUserRequest;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.integration.collector.ReceivedMessageCollector;
import de.ascendro.f4m.service.integration.rule.MockServiceRule.BindableAbstractModule;
import de.ascendro.f4m.service.integration.rule.MockServiceRule.ThrowingFunction;
import de.ascendro.f4m.service.integration.test.F4MServiceWithMockIntegrationTestBase;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.ISOCountry;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.config.InsertOrUpdateUserRequest;
import de.ascendro.f4m.service.profile.ProfileMessageTypes;
import de.ascendro.f4m.service.profile.config.ProfileConfig;
import de.ascendro.f4m.service.profile.dao.EndConsumerInvoiceAerospikeDao;
import de.ascendro.f4m.service.profile.dao.ProfileAerospikeDao;
import de.ascendro.f4m.service.profile.di.ProfileDefaultMessageMapper;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileFacebookId;
import de.ascendro.f4m.service.profile.model.ProfileIdentifierType;
import de.ascendro.f4m.service.profile.model.api.ApiProfile;
import de.ascendro.f4m.service.profile.model.api.get.ProfileGetResponse;
import de.ascendro.f4m.service.profile.model.api.update.ProfileUpdateResponse;
import de.ascendro.f4m.service.profile.model.create.CreateProfileResponse;
import de.ascendro.f4m.service.profile.model.delete.DeleteProfileResponse;
import de.ascendro.f4m.service.profile.model.find.FindListByIdentifiersResponse;
import de.ascendro.f4m.service.profile.model.find.FindProfileResponse;
import de.ascendro.f4m.service.profile.model.get.app.GetAppConfigurationResponse;
import de.ascendro.f4m.service.profile.model.get.profile.GetProfileResponse;
import de.ascendro.f4m.service.profile.model.invoice.EndConsumerInvoiceListResponse;
import de.ascendro.f4m.service.profile.model.list.ProfileListByIdsResponse;
import de.ascendro.f4m.service.profile.model.merge.MergeProfileResponse;
import de.ascendro.f4m.service.profile.model.schema.ProfileMessageSchemaMapper;
import de.ascendro.f4m.service.profile.model.sub.get.GetProfileBlobResponse;
import de.ascendro.f4m.service.profile.model.sub.update.UpdateProfileBlobResponse;
import de.ascendro.f4m.service.profile.model.update.UpdateProfileRequest;
import de.ascendro.f4m.service.profile.model.update.UpdateProfileResponse;
import de.ascendro.f4m.service.profile.util.ProfileUtil;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.util.JsonTestUtil;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;

public class ProfileServiceStartupTest extends F4MServiceWithMockIntegrationTestBase {
	private static final String APP_ID = "GAME-APP-8268573934";
	private static final String APP_JSON = "{\"test\":\"testvalue\"}";
	private static final String NAMESPACE = "test";
	private static final String SET = "appConfig";

	private ReceivedMessageCollector receivedMessageCollector;

	@Override
	protected void configureInjectionModuleMock(BindableAbstractModule module) {
		module.bindToModule(JsonMessageTypeMap.class).to(ProfileDefaultMessageMapper.class);
		module.bindToModule(JsonMessageSchemaMap.class).to(ProfileMessageSchemaMapper.class);
	};

	@Override
	protected ThrowingFunction<RequestContext, JsonMessageContent> getMessageHandlerMock() {
		return ctx -> onReceivedMessage(ctx);
	}

	private ReceivedMessageCollector dummyServiceMessageCollector = new ReceivedMessageCollector() {
		@Override
		public SessionWrapper getSessionWrapper() {
			return Mockito.mock(SessionWrapper.class);
		};
	};
	private Gson gson;
	private JsonMessageUtil jsonMessageUtil;

	private IAerospikeClient aerospikeClient;
	private ApplicationConfigrationPrimaryKeyUtil primaryKeyUtil;
	private EndConsumerInvoiceAerospikeDao aerospikeDao;
	private ProfileUtil profileUtil;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();

		MockitoAnnotations.initMocks(this);

		gson = JsonTestUtil.getGson();
		Injector injector = jettyServerRule.getServerStartup().getInjector();
		jsonMessageUtil = injector.getInstance(JsonMessageUtil.class);
		aerospikeDao = injector.getInstance(EndConsumerInvoiceAerospikeDao.class);
		profileUtil = injector.getInstance(ProfileUtil.class);
		receivedMessageCollector = (ReceivedMessageCollector) clientInjector.getInstance(
				com.google.inject.Key.get(JsonMessageHandlerProvider.class, ClientMessageHandler.class)).get();

		assertServiceStartup(FriendManagerMessageTypes.SERVICE_NAME, PaymentMessageTypes.SERVICE_NAME,
				EventMessageTypes.SERVICE_NAME);
		
		final AerospikeClientProvider aerospikeClientProvider = injector.getInstance(AerospikeClientProvider.class);

		aerospikeClient = aerospikeClientProvider.get();
		primaryKeyUtil = new ApplicationConfigrationPrimaryKeyUtil(config);
	}
	
	@Test
	public void testIndexCreationCall() throws Exception {
		assertThat(aerospikeClient, instanceOf(AerospikeClientMock.class));
		List<String> createdIndexes = ((AerospikeClientMock)aerospikeClient).getCreatedIndexes();
		assertEquals(1, createdIndexes.size());
	}

	@Test
	public void testCreateProfileByEmailCycle() throws Exception {
		final JsonObject profileObject = testCreateProfileBy(ProfileIdentifierType.EMAIL);

		assertEquals(1, profileObject.get("emails").getAsJsonArray().size());
		assertEquals("test@mail.com", profileObject.get("emails").getAsJsonArray().get(0).getAsJsonObject()
				.get("email").getAsString());
	}

	@Test
	public void testCreateProfileByPhoneCycle() throws Exception {
		final JsonObject profileObject = testCreateProfileBy(ProfileIdentifierType.PHONE);

		assertEquals(1, profileObject.get("phones").getAsJsonArray().size());
		assertEquals("25678956", profileObject.get("phones").getAsJsonArray().get(0).getAsJsonObject().get("phone")
				.getAsString());
	}

	@Test
	public void testCreateProfileByFacebookCycle() throws Exception {
		final JsonObject profileObject = testCreateProfileBy(ProfileIdentifierType.FACEBOOK);
		assertEquals("FB_TEST_TOKEN", profileObject.get("facebook").getAsString());
	}

	@Test
	public void testCreateProfileByGoogleCycle() throws Exception {
		final JsonObject profileObject = testCreateProfileBy(ProfileIdentifierType.GOOGLE);

		assertEquals("G_TEST_TOKEN", profileObject.get("google").getAsString());
	}

	private JsonObject testCreateProfileBy(ProfileIdentifierType identifierType) throws Exception {
		final String typeName = getNameInCameCase(identifierType);

		final CreateProfileResponse createProfileResponse = createProfile("createProfileBy" + typeName + ".json");

		receivedMessageCollector.getReceivedMessageList().clear();

		final GetProfileResponse getProfileResponse = getProfile(createProfileResponse.getUserId());
		assertNotNull(getProfileResponse.getProfile());

		final JsonObject profileObject = getProfileResponse.getProfile().getAsJsonObject();
		assertEquals(createProfileResponse.getUserId(), profileObject.get(Profile.USER_ID_PROPERTY).getAsString());

		return profileObject;
	}

	@Test
	public void testCreateProfileByAllCycle() throws Exception {
		final CreateProfileResponse createProfileResponse = createProfile("createProfileByAll.json");

		receivedMessageCollector.getReceivedMessageList().clear();

		final GetProfileResponse getProfileResponse = getProfile(createProfileResponse.getUserId());
		assertNotNull(getProfileResponse.getProfile());

		final JsonObject profileObject = getProfileResponse.getProfile().getAsJsonObject();
		assertEquals(createProfileResponse.getUserId(), profileObject.get("userId").getAsString());

		assertEquals("G_TEST_TOKEN", profileObject.get("google").getAsString());

		assertEquals("FB_TEST_TOKEN", profileObject.get("facebook").getAsString());

		assertEquals(1, profileObject.get("phones").getAsJsonArray().size());
		assertEquals("25678956", profileObject.get("phones").getAsJsonArray().get(0).getAsJsonObject().get("phone")
				.getAsString());

		assertEquals(1, profileObject.get("emails").getAsJsonArray().size());
		assertEquals("test@mail.com", profileObject.get("emails").getAsJsonArray().get(0).getAsJsonObject()
				.get("email").getAsString());
	}

	@Test
	public void testCreateProfileByNothingCycle() throws Exception {
		final String createProfileByNothingJson = getPlainTextJsonFromResources("createProfileByNothing.json");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), createProfileByNothingJson);

		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> createProfileResponseMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(createProfileResponseMessage, CreateProfileResponse.class);
		final CreateProfileResponse createProfileResponse = (CreateProfileResponse) createProfileResponseMessage
				.getContent();
		assertNotNull(createProfileResponse.getUserId());
	}

	@Test
	public void testDeleteProfileWithoutEventServiceCycle() throws Exception {
		createAnonymousTestProfile();
		receivedMessageCollector.getReceivedMessageList().clear();

		final String deleteProfileJson = getDeleteProfileByCorrectUserId();
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), deleteProfileJson);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));

		final JsonMessage<? extends JsonMessageContent> deleteProfileResponseMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(deleteProfileResponseMessage, DeleteProfileResponse.class);
		final DeleteProfileResponse deleteProfileResponse = (DeleteProfileResponse) deleteProfileResponseMessage
				.getContent();
		assertTrue(deleteProfileResponse.getProfile().isJsonNull());

		//No facebook key should be present

		receivedMessageCollector.clearReceivedMessageList();

		final String findProfileJson = getPlainTextJsonFromResources("findProfileByFacebook.json");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), findProfileJson);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));

		final JsonMessage<? extends JsonMessageContent> findProfileResponseMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(findProfileResponseMessage, FindProfileResponse.class);
		final FindProfileResponse findProfileResponse = (FindProfileResponse) findProfileResponseMessage.getContent();
		assertNull(findProfileResponse.getUserId());
	}

	@Test
	public void testDeleteProfileByUserIdWrongUserId() throws Exception {
		createAnonymousTestProfile();
		receivedMessageCollector.getReceivedMessageList().clear();

		final String deleteProfileJson = getPlainTextJsonFromResources("deleteProfileByUserId.json").replaceAll("<<userId>>", "xxx");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), deleteProfileJson);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));

		final JsonMessage<? extends JsonMessageContent> deleteProfileResponseMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertNotNull(deleteProfileResponseMessage.getError());

		//No user should be deleted

		receivedMessageCollector.clearReceivedMessageList();

		final String findProfileJson = getPlainTextJsonFromResources("findProfileByFacebook.json");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), findProfileJson);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));

		final JsonMessage<? extends JsonMessageContent> findProfileResponseMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(findProfileResponseMessage, FindProfileResponse.class);
		final FindProfileResponse findProfileResponse = (FindProfileResponse) findProfileResponseMessage.getContent();
		assertEquals(ANONYMOUS_USER_ID, findProfileResponse.getUserId());
	}

	@Test
	public void testDeleteProfileByUserId() throws Exception {
		createAnonymousTestProfile();
		receivedMessageCollector.getReceivedMessageList().clear();

		final String deleteProfileJson = getDeleteProfileByCorrectUserId();
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), deleteProfileJson);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));

		final JsonMessage<? extends JsonMessageContent> deleteProfileResponseMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(deleteProfileResponseMessage, DeleteProfileResponse.class);
		final DeleteProfileResponse deleteProfileResponse = (DeleteProfileResponse) deleteProfileResponseMessage
				.getContent();
		assertTrue(deleteProfileResponse.getProfile().isJsonNull());

		//No facebook key should be present

		receivedMessageCollector.clearReceivedMessageList();

		final String findProfileJson = getPlainTextJsonFromResources("findProfileByFacebook.json");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), findProfileJson);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));

		final JsonMessage<? extends JsonMessageContent> findProfileResponseMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(findProfileResponseMessage, FindProfileResponse.class);
		final FindProfileResponse findProfileResponse = (FindProfileResponse) findProfileResponseMessage.getContent();
		assertNull(findProfileResponse.getUserId());
	}

	@Test
	public void testDeleteProfileEventNotificationCycle() throws Exception {
		createAnonymousTestProfile();
		final String deleteProfileJson = getDeleteProfileByCorrectUserId();
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), deleteProfileJson);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));

		final JsonMessage<? extends JsonMessageContent> deleteProfileResponseMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(deleteProfileResponseMessage, DeleteProfileResponse.class);
		final DeleteProfileResponse deleteProfileResponse = (DeleteProfileResponse) deleteProfileResponseMessage
				.getContent();
		assertTrue(deleteProfileResponse.getProfile().isJsonNull());

		RetriedAssert.assertWithWait(() -> assertEquals(1, dummyServiceMessageCollector.getMessagesByType(EventMessageTypes.PUBLISH).size()));
		final JsonMessage<PublishMessageContent> deleteNotificationMessage = dummyServiceMessageCollector.getMessageByType(EventMessageTypes.PUBLISH);
		assertMessageContentType(deleteNotificationMessage, PublishMessageContent.class);
		final PublishMessageContent publishContent = (PublishMessageContent) deleteNotificationMessage.getContent();
		assertEquals("profile/delete-user", publishContent.getTopic());
		assertNotNull(publishContent.getNotificationContent());
		assertNotNull(publishContent.getNotificationContent().getAsJsonObject().get("userId"));
		assertNotNull(ANONYMOUS_USER_ID, publishContent.getNotificationContent().getAsJsonObject().get("userId").getAsString());
	}

	@Test
	public void testUpdateProfileWithDeltas() throws Exception {
		createAnonymousTestProfile();
		final ClientInfo clientInfo = new ClientInfo(ANONYMOUS_USER_ID, new String[]{"any_role"});
		final String updateProfileJson = getPlainTextJsonFromResources("updateProfileWithDeltas.json", clientInfo)
				.replace("<<userId>>", ANONYMOUS_USER_ID);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), updateProfileJson);
		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));

		final JsonMessage<? extends JsonMessageContent> updateProfileResponseMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(updateProfileResponseMessage, UpdateProfileResponse.class);
		final UpdateProfileResponse updateProfileResponse = (UpdateProfileResponse) updateProfileResponseMessage
				.getContent();
		assertNotNull(updateProfileJson, updateProfileResponse.getProfile().toString());

		receivedMessageCollector.getReceivedMessageList().clear();

		final GetProfileResponse getProfileResponse = getProfile(ANONYMOUS_USER_ID);

		JsonObject profile = getProfileResponse.getProfile().getAsJsonObject();
		assertEquals("Another street", profile.get("address").getAsJsonObject().get("street").getAsString());
		JsonArray phonesJson = profile.get(Profile.PHONES_PROPERTY).getAsJsonArray();
		assertEquals(1, phonesJson.size());
		assertEquals("notVerified", phonesJson.get(0).getAsJsonObject().get("verificationStatus").getAsString());
		assertEquals("FB-TEST_TOKEN", profile.get(Profile.FACEBOOK_PROPERTY).getAsString());
		assertEquals("G_TEST_TOKEN", profile.get(Profile.GOOGLE_PROPERTY).getAsString());
		assertEquals("photoS3Id.jpg", profile.get(Profile.PHOTO_ID_PROPERTY).getAsString());

		JsonArray facebookIdsJson = profile.get(Profile.FACEBOOK_IDS_PROPERTY).getAsJsonArray();
		assertFacebookIdsExpected(facebookIdsJson);
	}

	private void assertFacebookIdsExpected(JsonArray facebookIdsJson) {
		assertEquals(2, facebookIdsJson.size());
		assertEquals("userId123", facebookIdsJson.get(0).getAsJsonObject().get(ProfileFacebookId.FACEBOOK_USER_ID_PROPERTY).getAsString());
		assertEquals("userId321", facebookIdsJson.get(1).getAsJsonObject().get(ProfileFacebookId.FACEBOOK_USER_ID_PROPERTY).getAsString());
		assertEquals("appId123", facebookIdsJson.get(0).getAsJsonObject().get(ProfileFacebookId.FACEBOOK_APPLICATION_ID_PROPERTY).getAsString());
		assertEquals("appId321", facebookIdsJson.get(1).getAsJsonObject().get(ProfileFacebookId.FACEBOOK_APPLICATION_ID_PROPERTY).getAsString());
	}

	@Test
	public void testProfileUpdate() throws Exception {
		createAnonymousTestProfile();
		final String updateProfileJson = getPlainTextJsonFromResources("profileUpdate.json", ANONYMOUS_CLIENT_INFO);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), updateProfileJson);
		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));

		JsonMessage<? extends JsonMessageContent> updateProfileResponseMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(updateProfileResponseMessage, ProfileUpdateResponse.class);
		ProfileUpdateResponse updateProfileResponse = (ProfileUpdateResponse) updateProfileResponseMessage.getContent();
		assertNotNull(updateProfileJson, updateProfileResponse.getProfile().toString());

		receivedMessageCollector.getReceivedMessageList().clear();

		GetProfileResponse getProfileResponse = getProfile(ANONYMOUS_USER_ID);

		JsonObject profile = getProfileResponse.getProfile().getAsJsonObject();

		assertEquals("Another postal code", profile.get("address").getAsJsonObject().get("postalCode").getAsString());
		JsonObject gameSettingsJson = profile.get("gameSettings").getAsJsonObject();
		assertEquals("MONEY", gameSettingsJson.get("currency").getAsString());
		assertEquals(1, gameSettingsJson.get("numberOfQuestions").getAsInt());
		assertEquals(true, profile.get("pushNotificationsNews").getAsBoolean());
	}

	@Test
	public void testUpdateProfileByUserIdCycle() throws Exception {
		createAnonymousTestProfile();

		final String updateProfileJson = getUpdateProfileUsingUserId(ANONYMOUS_USER_ID);
		JsonMessage<UpdateProfileRequest> updateProfileMsg = jsonMessageUtil.fromJson(updateProfileJson);
		final JsonElement updateProfile = updateProfileMsg.getContent().getProfile();
		String friendId = "userX";
		updateProfile.getAsJsonObject().addProperty(Profile.RECOMMENDED_BY_PROPERTY, friendId);
		String updated = jsonMessageUtil.toJson(updateProfileMsg);

		final UpdateProfileResponse updateProfileResponse = updateProfileBy(updated);
		JsonTestUtil.assertJsonContentEqual(updateProfile.toString(), updateProfileResponse.getProfile().toString());

		RetriedAssert.assertWithWait(() -> assertTrue(dummyServiceMessageCollector.getMessageByType(FriendManagerMessageTypes.BUDDY_ADD_FOR_USER)
				.getContent() instanceof BuddyAddForUserRequest));
		
		final Set<String> buddyUserIds = dummyServiceMessageCollector.getMessagesByType(FriendManagerMessageTypes.BUDDY_ADD_FOR_USER).stream()
			.map(m -> ((BuddyAddForUserRequest)m.getContent()).getUserIds()[0])
			.collect(Collectors.toSet());
		
		assertThat(buddyUserIds, hasItem(friendId));

		receivedMessageCollector.getReceivedMessageList().clear();
		final GetProfileResponse getProfileResponse = getProfile(ANONYMOUS_USER_ID);
		JsonTestUtil.assertJsonContentEqualIgnoringSeq(updateProfile.toString(), getProfileResponse.getProfile()
				.toString());
	}

	@Test
	public void testFindProfileByEmail() throws Exception {
		testFindProfileBy(ProfileIdentifierType.EMAIL);
	}

	@Test
	public void testFindProfileByPhone() throws Exception {
		testFindProfileBy(ProfileIdentifierType.PHONE);
	}

	@Test
	public void testFindProfileByFacebook() throws Exception {
		testFindProfileBy(ProfileIdentifierType.FACEBOOK);
	}

	@Test
	public void testFindProfileByGoogle() throws Exception {
		testFindProfileBy(ProfileIdentifierType.GOOGLE);
	}

	private void testFindProfileBy(ProfileIdentifierType identifierType) throws Exception {
		createAnonymousTestProfile();

		final String typeName = getNameInCameCase(identifierType);

		receivedMessageCollector.clearReceivedMessageList();

		final String updateProfileJson = getPlainTextJsonFromResources("findProfileBy" + typeName + ".json");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), updateProfileJson);
		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));

		final JsonMessage<? extends JsonMessageContent> findProfileResponseMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(findProfileResponseMessage, FindProfileResponse.class);
		final FindProfileResponse findProfileResponse = (FindProfileResponse) findProfileResponseMessage.getContent();
		assertEquals(ANONYMOUS_USER_ID, findProfileResponse.getUserId());
	}

	@Test
	public void testFindListByIdentifiers() throws Exception {
		createAnonymousTestProfile();
        receivedMessageCollector.clearReceivedMessageList();

        JsonObject createdByEmailJson = testCreateProfileBy(ProfileIdentifierType.EMAIL);
        receivedMessageCollector.clearReceivedMessageList();

		JsonObject createdByPhoneJson = testCreateProfileBy(ProfileIdentifierType.PHONE);
		receivedMessageCollector.clearReceivedMessageList();

		final String updateProfileJson = getPlainTextJsonFromResources("findListByIdentifiers.json");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), updateProfileJson);
		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));

		final JsonMessage<? extends JsonMessageContent> findListByIdentifiersResponseMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(findListByIdentifiersResponseMessage, FindListByIdentifiersResponse.class);
		final FindListByIdentifiersResponse findListByIdentifiersResponse =
				(FindListByIdentifiersResponse) findListByIdentifiersResponseMessage.getContent();
		assertEquals(2, findListByIdentifiersResponse.getProfileList().size());
		Profile profile1 = new Profile(findListByIdentifiersResponse.getProfileList().get(0));
		Profile profile2 = new Profile(findListByIdentifiersResponse.getProfileList().get(1));
        assertEquals(createdByPhoneJson.get(Profile.USER_ID_PROPERTY).getAsString(),
				profile1.getUserId());
        assertEquals(createdByEmailJson.get(Profile.USER_ID_PROPERTY).getAsString(),
				profile2.getUserId());
 }

	@Test
	public void testCreateProfileByDublicateEmail() throws Exception {
		testCreateDublicateProfileBy(ProfileIdentifierType.EMAIL);
	}

	@Test
	public void testCreateProfileByDublicatePhone() throws Exception {
		testCreateDublicateProfileBy(ProfileIdentifierType.PHONE);
	}

	@Test
	public void testCreateProfileByDublicateFacebook() throws Exception {
		testCreateDublicateProfileBy(ProfileIdentifierType.FACEBOOK);
	}

	@Test
	public void testCreateProfileByDublicateGoogle() throws Exception {
		testCreateDublicateProfileBy(ProfileIdentifierType.GOOGLE);
	}

	private void testCreateDublicateProfileBy(ProfileIdentifierType identifierType) throws Exception {
		final String typeName = getNameInCameCase(identifierType);

		final String createProfileJson = getPlainTextJsonFromResources("createProfileBy" + typeName + ".json");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), createProfileJson);

		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
		receivedMessageCollector.getReceivedMessageList().clear();

		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), createProfileJson);
		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));

		final JsonMessage<? extends JsonMessageContent> createProfileResponseMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertNotNull(createProfileResponseMessage);
		assertNotNull(createProfileResponseMessage.getError());
		assertEquals(ExceptionCodes.ERR_ENTRY_ALREADY_EXISTS, createProfileResponseMessage.getError().getCode());
	}

	@Test
	public void testGetApplicationConfigurationWithDeviceIdAndNoDevicePorperties() throws Exception {
		final UpdateProfileResponse updateResponse = createAnonymousTestProfile();
		final JsonElement profileJsonElement = gson.fromJson(updateResponse.getProfile(), JsonElement.class);

		final Profile profile = new Profile(profileJsonElement);
		assertNotNull(profile);

		prepareAerospikeAppConfigRecord();

		assertSize(0, receivedMessageCollector.getReceivedMessageList()); //ensure there are no left-overs from setUp or previous tests
		final String getAppConfigJson = getPlainTextJsonFromResources("getAppConfigNoDeviceProperties.json", ANONYMOUS_CLIENT_INFO);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getAppConfigJson);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> getAppConfigMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(getAppConfigMessage, GetAppConfigurationResponse.class);
		final GetAppConfigurationResponse getAppConfigurationResponse = (GetAppConfigurationResponse) getAppConfigMessage
				.getContent();
		assertNotNull(getAppConfigurationResponse.getAppConfig());

		JsonTestUtil.assertJsonContentEqual(APP_JSON, getAppConfigurationResponse.getAppConfig().toString());

		final GetProfileResponse getProfileResponse = getProfile(profile.getUserId());
		final Profile updatedProfile = new Profile(getProfileResponse.getProfile());

		final JsonArray devices = updatedProfile.getDevicesAsJsonArray();
		assertEquals(1, devices.size());
	}

	@Test
	public void testGetApplicationConfigurationWithDeviceIdAndDevicePorperties() throws Exception {
		final UpdateProfileResponse updateResponse = createAnonymousTestProfile();

		final Profile profile = new Profile(updateResponse.getProfile());
		assertNotNull(profile);

		prepareAerospikeAppConfigRecord();

		assertSize(0, receivedMessageCollector.getReceivedMessageList()); //ensure there are no left-overs from setUp or previous tests
		final String getAppConfigJson = getPlainTextJsonFromResources("getAppConfig.json", ANONYMOUS_CLIENT_INFO);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getAppConfigJson);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));

		final JsonMessage<? extends JsonMessageContent> getAppConfigMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(getAppConfigMessage, GetAppConfigurationResponse.class);
		final GetAppConfigurationResponse getAppConfigurationResponse = (GetAppConfigurationResponse) getAppConfigMessage
				.getContent();
		assertNotNull(getAppConfigurationResponse.getAppConfig());

		JsonTestUtil.assertJsonContentEqual(APP_JSON, getAppConfigurationResponse.getAppConfig().toString());

		final GetProfileResponse getProfileResponse = getProfile(profile.getUserId());
		final Profile updatedProfile = new Profile(getProfileResponse.getProfile());

		final JsonArray devices = updatedProfile.getDevicesAsJsonArray();
		assertEquals(2, devices.size());

		assertNull(devices.get(0).getAsJsonObject().get(Profile.DEVICE_UUID_PROPERTY_NAME));
		assertEquals("ID1", devices.get(1).getAsJsonObject().get(Profile.DEVICE_UUID_PROPERTY_NAME).getAsString());
		assertEquals("123123", devices.get(1).getAsJsonObject().get(Profile.DEVICE_IMEI_PROPERTY_NAME).getAsString());
		assertEquals("TYP1", devices.get(1).getAsJsonObject().get("deviceType").getAsString());
		assertEquals(1246, devices.get(1).getAsJsonObject().get("screenHeight").getAsInt());
		assertEquals("ONESIGNALDEVICEID", devices.get(1).getAsJsonObject().get(Profile.DEVICE_ONESIGNAL_PROPERTY_NAME).getAsString());

		// check that identifier record is created :
		String oneSignalProfile = profileUtil.findByIdentifier(ProfileIdentifierType.ONE_SIGNAL_ID, "ONESIGNALDEVICEID");
		assertEquals(ANONYMOUS_USER_ID, oneSignalProfile);

		assertNotNull(updatedProfile.getApplications());
		assertEquals(1, updatedProfile.getApplications().size());
		assertEquals(APP_ID, updatedProfile.getApplications().iterator().next());

		assertNotNull(updatedProfile.getTenants());
		assertEquals(1, updatedProfile.getTenants().size());
		assertEquals(KeyStoreTestUtil.TENANT_ID, updatedProfile.getTenants().iterator().next());
	}

	@Test
	public void testGetApplicationConfigurationDublicateTenant() throws Exception {
		final UpdateProfileResponse updateResponse = createAnonymousTestProfile();

		final Profile profile = new Profile(updateResponse.getProfile());
		assertNotNull(profile);

		prepareAerospikeAppConfigRecord();

		final String getAppConfigJson = getPlainTextJsonFromResources("getAppConfig.json", ANONYMOUS_CLIENT_INFO);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getAppConfigJson);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));

		receivedMessageCollector.clearReceivedMessageList();

		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getAppConfigJson);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));

		final JsonMessage<? extends JsonMessageContent> getAppConfigMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(getAppConfigMessage, GetAppConfigurationResponse.class);
		final GetAppConfigurationResponse getAppConfigurationResponse = (GetAppConfigurationResponse) getAppConfigMessage
				.getContent();
		assertNotNull(getAppConfigurationResponse.getAppConfig());

		JsonTestUtil.assertJsonContentEqual(APP_JSON, getAppConfigurationResponse.getAppConfig().toString());

		final GetProfileResponse getProfileResponse = getProfile(profile.getUserId());
		final Profile updatedProfile = new Profile(getProfileResponse.getProfile());

		assertNotNull(updatedProfile.getApplications());
		assertEquals(1, updatedProfile.getApplications().size());
		assertEquals(APP_ID, updatedProfile.getApplications().iterator().next());
	}

	@Test
	public void testGetApplicationConfigurationDublicateApplication() throws Exception {
		final UpdateProfileResponse updateResponse = createAnonymousTestProfile();
		final JsonElement profileJsonElement = gson.fromJson(updateResponse.getProfile(), JsonElement.class);

		final Profile profile = new Profile(profileJsonElement);
		assertNotNull(profile);

		prepareAerospikeAppConfigRecord();

		assertSize(0, receivedMessageCollector.getReceivedMessageList()); //ensure there are no left-overs from setUp or previous tests
		final String getAppConfigJson = getPlainTextJsonFromResources("getAppConfig.json", ANONYMOUS_CLIENT_INFO);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getAppConfigJson);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));

		receivedMessageCollector.clearReceivedMessageList();

		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getAppConfigJson);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));

		final JsonMessage<? extends JsonMessageContent> getAppConfigMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(getAppConfigMessage, GetAppConfigurationResponse.class);
		final GetAppConfigurationResponse getAppConfigurationResponse = (GetAppConfigurationResponse) getAppConfigMessage
				.getContent();
		assertNotNull(getAppConfigurationResponse.getAppConfig());

		JsonTestUtil.assertJsonContentEqual(APP_JSON, getAppConfigurationResponse.getAppConfig().toString());

		final GetProfileResponse getProfileResponse = getProfile(profile.getUserId());
		final Profile updatedProfile = new Profile(getProfileResponse.getProfile());

		assertNotNull(updatedProfile.getTenants());
		assertEquals(1, updatedProfile.getTenants().size());
		assertEquals(KeyStoreTestUtil.TENANT_ID, updatedProfile.getTenants().iterator().next());
	}

	@Test
	public void testGetApplicationConfigurationWithoutDeviceId() throws Exception {
		final UpdateProfileResponse updateResponse = createAnonymousTestProfile();
		final JsonElement profileJsonElement = gson.fromJson(updateResponse.getProfile(), JsonElement.class);

		final Profile profile = new Profile(profileJsonElement);
		assertNotNull(profile);

		prepareAerospikeAppConfigRecord();

		assertSize(0, receivedMessageCollector.getReceivedMessageList()); //ensure there are no left-overs from setUp or previous tests
		final String getAppConfigJson = getPlainTextJsonFromResources("getAppConfigNoDeviceId.json", ANONYMOUS_CLIENT_INFO);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getAppConfigJson);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> getAppConfigMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(getAppConfigMessage, GetAppConfigurationResponse.class);
		final GetAppConfigurationResponse getAppConfigurationResponse = (GetAppConfigurationResponse) getAppConfigMessage
				.getContent();
		assertNotNull(getAppConfigurationResponse.getAppConfig());

		JsonTestUtil.assertJsonContentEqual(APP_JSON, getAppConfigurationResponse.getAppConfig().toString());

		final GetProfileResponse getProfileResponse = getProfile(profile.getUserId());
		final Profile updatedProfile = new Profile(getProfileResponse.getProfile());

		final JsonArray devices = updatedProfile.getDevicesAsJsonArray();
		assertEquals(1, devices.size());
	}

	@Test
	public void testGetApplicationConfigurationWithoutAppId() throws Exception {
		final UpdateProfileResponse updateResponse = createAnonymousTestProfile();
		final JsonElement profileJsonElement = gson.fromJson(updateResponse.getProfile(), JsonElement.class);

		final Profile profile = new Profile(profileJsonElement);
		assertNotNull(profile);

		prepareAerospikeAppConfigRecord();

		assertSize(0, receivedMessageCollector.getReceivedMessageList()); //ensure there are no left-overs from setUp or previous tests
		final String getAppConfigJson = getPlainTextJsonFromResources("getAppConfigNoAppId.json", ANONYMOUS_CLIENT_INFO);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getAppConfigJson);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> getAppConfigMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(getAppConfigMessage, GetAppConfigurationResponse.class);
		final GetAppConfigurationResponse getAppConfigurationResponse = (GetAppConfigurationResponse) getAppConfigMessage
				.getContent();
		assertNotNull(getAppConfigurationResponse.getAppConfig());

		String expectedResponse = getPlainTextJsonFromResources("getAppConfigNoAppIdResponse.json");
		JsonTestUtil.assertJsonContentEqual(expectedResponse, getAppConfigurationResponse.getAppConfig().toString());

		final GetProfileResponse getProfileResponse = getProfile(profile.getUserId());
		final Profile updatedProfile = new Profile(getProfileResponse.getProfile());

		final JsonArray devices = updatedProfile.getDevicesAsJsonArray();
		assertEquals(1, devices.size());
	}

	@Test
	public void testGetAppConfigurationAdvertisementSettings() throws Exception {
		final UpdateProfileResponse updateResponse = createAnonymousTestProfile();
		final JsonElement profileJsonElement = gson.fromJson(updateResponse.getProfile(), JsonElement.class);

		final Profile profile = new Profile(profileJsonElement);
		assertNotNull(profile);

		prepareAerospikeAppConfigRecordForAdvertisements();

		final String getAppConfigJson = getPlainTextJsonFromResources("getAppConfig.json", ANONYMOUS_CLIENT_INFO);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getAppConfigJson);

		assertReceivedMessagesWithWait(ProfileMessageTypes.GET_APP_CONFIGURATION_RESPONSE);

		final JsonMessage<GetAppConfigurationResponse> getAppConfigMessage = receivedMessageCollector
				.getMessageByType(ProfileMessageTypes.GET_APP_CONFIGURATION_RESPONSE);
		final GetAppConfigurationResponse getAppConfigurationResponse = getAppConfigMessage
				.getContent();
		assertNotNull(getAppConfigurationResponse.getAppConfig());

		String expectedResponse = getPlainTextJsonFromResources("getAppConfigWithAdvertisement.json");
		JsonTestUtil.assertJsonContentEqual(expectedResponse, getAppConfigurationResponse.getAppConfig().toString());

		final GetProfileResponse getProfileResponse = getProfile(profile.getUserId());
		final Profile updatedProfile = new Profile(getProfileResponse.getProfile());

		final JsonArray devices = updatedProfile.getDevicesAsJsonArray();
		assertEquals(2, devices.size());
	}

	@Test
	public void testMergeProfile() throws Exception {
		final CreateProfileResponse createProfile1Response = createProfile("createProfileByEmail.json");
		final CreateProfileResponse createProfile2Response = createProfile("createProfileByPhone.json");
		final CreateProfileResponse createProfile3Response = createProfile("createProfileByFacebook.json");

		final String mergeProfile1IntoProfil2Json = getMergeProfileJson(createProfile1Response.getUserId(),
				createProfile2Response.getUserId());
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), mergeProfile1IntoProfil2Json);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> mergeProfile1IntoProfil2Message = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(mergeProfile1IntoProfil2Message, MergeProfileResponse.class);

		receivedMessageCollector.clearReceivedMessageList();

		final String mergeProfile2IntoProfil3Json = getMergeProfileJson(createProfile2Response.getUserId(),
				createProfile3Response.getUserId());
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), mergeProfile2IntoProfil3Json);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> mergeProfile2IntoProfil3Message = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(mergeProfile2IntoProfil3Message, MergeProfileResponse.class);

		final MergeProfileResponse mergeProfileResponse = (MergeProfileResponse) mergeProfile2IntoProfil3Message
				.getContent();
		assertNotNull(mergeProfileResponse.getProfile());
		final Profile profile = new Profile(mergeProfileResponse.getProfile());

		assertEquals("FB_TEST_TOKEN", profile.getProfileFacebook());

		assertNotNull(profile.getProfileEmails());
		assertEquals(1, profile.getProfileEmails().size());
		assertEquals("test@mail.com", profile.getProfileEmails().get(0));

		assertNotNull(profile.getProfilePhones());
		assertEquals(1, profile.getProfilePhones().size());
		assertEquals("25678956", profile.getProfilePhones().get(0));
	}

	@Test
	public void testSynchronizeProfile() throws Exception {
		dummyServiceMessageCollector.clearReceivedMessageList();
		final String userId = UUID.randomUUID().toString();
		final String tenantId = UUID.randomUUID().toString();
		createTestProfile(userId, getUpdateProfileUsingUserId(userId), tenantId);
		RetriedAssert.assertWithWait(() -> assertSize(1, dummyServiceMessageCollector.getMessagesByType(PaymentMessageTypes.INSERT_OR_UPDATE_USER)));
		final JsonMessage<InsertOrUpdateUserRequest> message = dummyServiceMessageCollector.getMessageByType(PaymentMessageTypes.INSERT_OR_UPDATE_USER);
		assertEquals("payment/insertOrUpdateUser", message.getName());
	}
	
	@Test
	public void testGetUpdateProfileSubBlobExisting() throws Exception {
		final UpdateProfileResponse updateProfileResponse = createAnonymousTestProfile();

		final Profile updateProfile = new Profile(updateProfileResponse.getProfile());

		final String updateProfileBlobJson = getUpdateProfileBlobRequestJson(updateProfile.getUserId());
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), updateProfileBlobJson);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> updateProfileBlobMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(updateProfileBlobMessage, UpdateProfileBlobResponse.class);
		final UpdateProfileBlobResponse updateProfileBlobResponse = (UpdateProfileBlobResponse) updateProfileBlobMessage
				.getContent();
		assertEquals("handicap", updateProfileBlobResponse.getName());
		assertNotNull(updateProfileBlobResponse.getValue());
		assertEquals("content", updateProfileBlobResponse.getValue().getAsJsonObject().get("any").getAsString());

		receivedMessageCollector.clearReceivedMessageList();

		final String getProfileBlobJson = getProfileBlobRequestJson(updateProfile.getUserId());
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getProfileBlobJson);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> getProfileBlobMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(getProfileBlobMessage, GetProfileBlobResponse.class);
		final GetProfileBlobResponse getProfileBlobResponse = (GetProfileBlobResponse) getProfileBlobMessage
				.getContent();
		assertEquals("handicap", getProfileBlobResponse.getName());
		assertNotNull(getProfileBlobResponse.getValue());
		assertEquals("content", getProfileBlobResponse.getValue().getAsJsonObject().get("any").getAsString());

		final GetProfileResponse getProfileResponse = getProfile(updateProfile.getUserId());
		final Profile finalProfileState = new Profile(getProfileResponse.getProfile());

		assertNotNull(finalProfileState.getSubBlobNames());
		assertEquals(1, finalProfileState.getSubBlobNames().size());
		assertEquals("handicap", finalProfileState.getSubBlobNames().iterator().next());
	}

	@Test
	public void testGetUpdateProfileSubBlobNotExisting() throws Exception {
		final UpdateProfileResponse updateProfileResponse = createAnonymousTestProfile();

		final Profile updateProfile = new Profile(updateProfileResponse.getProfile());

		final String getProfileBlobJson = getProfileBlobRequestJson(updateProfile.getUserId());
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getProfileBlobJson);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> getProfileBlobMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(getProfileBlobMessage, GetProfileBlobResponse.class);
		final GetProfileBlobResponse getProfileBlobResponse = (GetProfileBlobResponse) getProfileBlobMessage
				.getContent();
		assertEquals("handicap", getProfileBlobResponse.getName());
		assertNotNull(getProfileBlobResponse.getValue());
		assertFalse(getProfileBlobResponse.getValue().isJsonNull());
	}

	@Test
	public void testUpdateProfileSubBlobForMissingProfile() throws Exception {
		final String updateProfileBlobJson = getUpdateProfileBlobRequestJson(ANONYMOUS_USER_ID);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), updateProfileBlobJson);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> updateProfileBlobMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertNotNull(updateProfileBlobMessage.getError());

		assertEquals(ExceptionCodes.ERR_ENTRY_NOT_FOUND, updateProfileBlobMessage.getError().getCode());
	}

	@Test
	public void testGetProfileSubBlobForMissingProfile() throws Exception {
		final String getProfileBlobJson = getProfileBlobRequestJson(ANONYMOUS_USER_ID);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getProfileBlobJson);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> getProfileBlobMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(getProfileBlobMessage, GetProfileBlobResponse.class);
		final GetProfileBlobResponse getProfileBlobResponse = (GetProfileBlobResponse) getProfileBlobMessage
				.getContent();
		assertNotNull(getProfileBlobResponse.getValue());
		assertFalse(getProfileBlobResponse.getValue().isJsonNull());
	}

	@Test
	public void testGetProfileSubBlobNoUser() throws Exception {
		final String getProfileBlobJson = getProfileBlobRequestJson("");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getProfileBlobJson);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> getProfileBlobMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertNotNull(getProfileBlobMessage.getError());
		assertEquals(ExceptionCodes.ERR_INSUFFICIENT_RIGHTS, getProfileBlobMessage.getError().getCode());
	}

	@Test
	public void testUpdateProfileSubBlobNoUser() throws Exception {
		final String getProfileBlobJson = getProfileBlobRequestJson("");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getProfileBlobJson);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> getProfileBlobMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertNotNull(getProfileBlobMessage.getError());
		assertEquals(ExceptionCodes.ERR_INSUFFICIENT_RIGHTS, getProfileBlobMessage.getError().getCode());
	}

	@Test
	public void testDeleteProfileSubBlobs() throws Exception {
		createAnonymousTestProfile();

		final String updateProfileBlobJson = getUpdateProfileBlobRequestJson(ANONYMOUS_USER_ID);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), updateProfileBlobJson);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> updateProfileBlobMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(updateProfileBlobMessage, UpdateProfileBlobResponse.class);

		receivedMessageCollector.clearReceivedMessageList();

		final String deleteProfileJson = getDeleteProfileByCorrectUserId();
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), deleteProfileJson);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> deleteProfileBlobMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(deleteProfileBlobMessage, DeleteProfileResponse.class);

		receivedMessageCollector.clearReceivedMessageList();

		final String getProfileBlobJson = getProfileBlobRequestJson(ANONYMOUS_USER_ID);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getProfileBlobJson);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> getProfileBlobMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(getProfileBlobMessage, GetProfileBlobResponse.class);
		final GetProfileBlobResponse getProfileBlobResponse = (GetProfileBlobResponse) getProfileBlobMessage
				.getContent();
		assertEquals("handicap", getProfileBlobResponse.getName());
		assertNotNull(getProfileBlobResponse.getValue());
		assertFalse(getProfileBlobResponse.getValue().isJsonNull());
		assertTrue(getProfileBlobResponse.getValue().getAsJsonObject().entrySet().isEmpty());
	}

	@Test
	public void testUpdateMissingProfile() throws Exception {
		final String updateProfileJson = getUpdateProfileUsingUserId(ANONYMOUS_USER_ID);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), updateProfileJson);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));

		final JsonMessage<? extends JsonMessageContent> updateProfileResponseMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertNotNull(updateProfileResponseMessage.getError());

		assertEquals(ExceptionCodes.ERR_ENTRY_NOT_FOUND, updateProfileResponseMessage.getError().getCode());
	}

	@Test
	public void testSubBlobMerge() throws Exception {
		final String userId1 = UUID.randomUUID().toString();
		createTestProfile(userId1);

		final String userId2 = UUID.randomUUID().toString();
		createTestProfile(userId2, null, null);

		final String handicap1Blob = "{\"any\":\"value1\"}";
		final String handicap2Blob = "{\"any\":\"value2\"}";

		final String handicap1UpdateJson = getUpdateProfileBlobRequestJson(userId1, "handicap1", handicap1Blob);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), handicap1UpdateJson);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));

		receivedMessageCollector.clearReceivedMessageList();

		final String handicap2UpdateJson = getUpdateProfileBlobRequestJson(userId2, "handicap2", handicap2Blob);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), handicap2UpdateJson);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));

		receivedMessageCollector.clearReceivedMessageList();

		final String mergeUser1ToUser2 = getMergeProfileJson(userId1, userId2);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), mergeUser1ToUser2);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> mergeResponseMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(mergeResponseMessage, MergeProfileResponse.class);
		final MergeProfileResponse mergeResponse = (MergeProfileResponse) mergeResponseMessage.getContent();
		final Profile mergeProfile = new Profile(mergeResponse.getProfile());
		assertNotNull(mergeProfile.getSubBlobNames());
		assertEquals(2, mergeProfile.getSubBlobNames().size());

		receivedMessageCollector.clearReceivedMessageList();

		final String getHandicap1Record = getProfileBlobRequestJson(userId2, "handicap1");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getHandicap1Record);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> handicap1Message = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(handicap1Message, GetProfileBlobResponse.class);
		final GetProfileBlobResponse handicap1 = (GetProfileBlobResponse) handicap1Message.getContent();
		assertNotNull(handicap1.getValue());
		assertFalse(handicap1.getValue().getAsJsonObject().entrySet().isEmpty());

		receivedMessageCollector.clearReceivedMessageList();

		final String getHandicap2Record = getProfileBlobRequestJson(userId2, "handicap2");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getHandicap2Record);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> handicap2Message = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(handicap2Message, GetProfileBlobResponse.class);
		final GetProfileBlobResponse handicap2 = (GetProfileBlobResponse) handicap2Message.getContent();
		assertFalse(handicap2.getValue().getAsJsonObject().entrySet().isEmpty());

		receivedMessageCollector.clearReceivedMessageList();

		final String getHandicapRecord = getProfileBlobRequestJson(userId2, "handicap");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getHandicapRecord);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> handicapMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(handicapMessage, GetProfileBlobResponse.class);
		final GetProfileBlobResponse handicap = (GetProfileBlobResponse) handicapMessage.getContent();
		assertTrue(handicap.getValue().getAsJsonObject().entrySet().isEmpty());
	}

	@Test
	public void testMergeProfileSubArrays() throws Exception {
		final String userId1 = UUID.randomUUID().toString();
		createTestProfile(userId1);

		final String userId2 = UUID.randomUUID().toString();
		createTestProfile(userId2, null, null);

		mergeProfileSubBlobs("handicap", userId1, "[1, 3, 7]", userId2, "[3, 56, 1]");

		final String getHandicap1Record = getProfileBlobRequestJson(userId2, "handicap");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getHandicap1Record);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> handicapMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(handicapMessage, GetProfileBlobResponse.class);
		final GetProfileBlobResponse handicap = (GetProfileBlobResponse) handicapMessage.getContent();
		assertNotNull(handicap.getValue());
		assertTrue(handicap.getValue().isJsonArray());
		assertEquals(4, handicap.getValue().getAsJsonArray().size());
	}

	@Test
	public void testMergeProfileSubObject() throws Exception {
		final String userId1 = UUID.randomUUID().toString();
		createTestProfile(userId1);

		final String userId2 = UUID.randomUUID().toString();
		createTestProfile(userId2, null, null);

		mergeProfileSubBlobs("handicap", userId1, "{\"any\":\"value1\"}", userId2, "{\"any\":\"value2\"}");

		final String getHandicap1Record = getProfileBlobRequestJson(userId2, "handicap");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getHandicap1Record);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> handicapMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(handicapMessage, GetProfileBlobResponse.class);
		final GetProfileBlobResponse handicap = (GetProfileBlobResponse) handicapMessage.getContent();
		assertNotNull(handicap.getValue());
		assertTrue(handicap.getValue().isJsonObject());
		assertEquals("value1", handicap.getValue().getAsJsonObject().get("any").getAsString());
	}

	@Test
	public void testUpdateHighlights() throws Exception {
		final UpdateProfileResponse updateProfileResponse = createTestProfile(ANONYMOUS_USER_ID,
				getPlainTextJsonFromResources("updateProfile.json"), null);

		final Profile updateProfile = new Profile(updateProfileResponse.getProfile());

		final String updateProfileBlobJson = getPlainTextJsonFromResources("updateHighlights.json");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), updateProfileBlobJson);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> updateProfileBlobMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(updateProfileBlobMessage, UpdateProfileBlobResponse.class);
		final UpdateProfileBlobResponse updateProfileBlobResponse = (UpdateProfileBlobResponse) updateProfileBlobMessage
				.getContent();
		assertEquals("highlights", updateProfileBlobResponse.getName());
		assertNotNull(updateProfileBlobResponse.getValue());
		assertTrue(updateProfileBlobResponse.getValue().isJsonArray());
		assertEquals(3, updateProfileBlobResponse.getValue().getAsJsonArray().size());

		receivedMessageCollector.clearReceivedMessageList();

		final String getProfileBlobJson = getProfileBlobRequestJson(updateProfile.getUserId(), "highlights");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getProfileBlobJson);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> getProfileBlobMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(getProfileBlobMessage, GetProfileBlobResponse.class);
		final GetProfileBlobResponse getProfileBlobResponse = (GetProfileBlobResponse) getProfileBlobMessage
				.getContent();
		assertEquals("highlights", getProfileBlobResponse.getName());
		assertNotNull(getProfileBlobResponse.getValue());
		assertTrue(getProfileBlobResponse.getValue().isJsonArray());
		assertEquals(3, getProfileBlobResponse.getValue().getAsJsonArray().size());
		JsonTestUtil.assertJsonContentEqual("[\"H1\",\"H2\",\"H7\"]", getProfileBlobResponse.getValue()
				.getAsJsonArray().toString());

		final GetProfileResponse getProfileResponse = getProfile(updateProfile.getUserId());
		final Profile finalProfileState = new Profile(getProfileResponse.getProfile());

		assertNotNull(finalProfileState.getSubBlobNames());
		assertEquals(1, finalProfileState.getSubBlobNames().size());
		assertEquals("highlights", finalProfileState.getSubBlobNames().iterator().next());
	}

	@Test
	public void testMergeBadges() throws Exception {
		final String userId1 = UUID.randomUUID().toString();
		createTestProfile(userId1);
		final String badges1 = "[{\"id\": 1,\"code\":\"bestPlayer\", \"title\":\"Best player badge\"}, "
				+ "{\"id\": 2,\"code\":\"firstPlace\", \"title\":\"First place badge\"}]	";

		final String userId2 = UUID.randomUUID().toString();
		createTestProfile(userId2, null, null);
		final String badges2 = "[{\"id\": 17,\"code\":\"top5Games\", \"title\":\"Played all top 5 games\"}, "
				+ "{\"id\": 1, \"title\":\"Best player badge\", \"code\":\"bestPlayer\"}]";

		mergeProfileSubBlobs("badges", userId1, badges1, userId2, badges2);

		final String getBadgesRecord = getProfileBlobRequestJson(userId2, "badges");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getBadgesRecord);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> badgesResponseMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(badgesResponseMessage, GetProfileBlobResponse.class);
		final GetProfileBlobResponse badgesResponse = (GetProfileBlobResponse) badgesResponseMessage.getContent();
		assertNotNull(badgesResponse.getValue());
		assertTrue(badgesResponse.getValue().isJsonArray());
		assertEquals(3, badgesResponse.getValue().getAsJsonArray().size());
	}

	@Test
	public void testProfileListByProfilesIds() throws Exception {

		final CreateProfileResponse createProfileResponse1 = createProfile("createProfileByNothing.json");

		final CreateProfileResponse createProfileResponse2 = createProfile("createProfileByNothing.json");

		final String profileListJson = getPlainTextJsonFromResources("profileListByIds.json")
                .replaceAll("<<userId1>>", createProfileResponse1.getUserId()).replaceAll("<<userId2>>", createProfileResponse2.getUserId());

		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), profileListJson);

		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> profileListByIdsResponseMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);

		assertMessageContentType(profileListByIdsResponseMessage, ProfileListByIdsResponse.class);
		ProfileListByIdsResponse response = (ProfileListByIdsResponse) profileListByIdsResponseMessage.getContent();

		assertEquals(2, response.getProfileList().size());
		Profile profile1 = new Profile(response.getProfileList().get(0));
		Profile profile2 = new Profile(response.getProfileList().get(1));
		assertEquals(createProfileResponse1.getUserId(), profile1.getUserId());
		assertEquals(createProfileResponse2.getUserId(), profile2.getUserId());
	}

	@Test
	public void testProfileGet() throws Exception {
		createAnonymousTestProfile();
		
		String profileGetJson = getPlainTextJsonFromResources("profileGet.json", ANONYMOUS_CLIENT_INFO);
		
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), profileGetJson);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> profileGetResponseMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		
		receivedMessageCollector.clearReceivedMessageList();
		
		assertMessageContentType(profileGetResponseMessage, ProfileGetResponse.class);
		ProfileGetResponse response = (ProfileGetResponse) profileGetResponseMessage.getContent();
		ApiProfile profile = response.getProfile();
		
		assertEquals(ANONYMOUS_USER_ID, profile.getUserId());
		
		//checking if default data has been added
		assertEquals(true, profile.getSettings().getPush().isPushNotificationsTournament());
		assertEquals(Currency.BONUS, profile.getSettings().getGame().getCurrency());
	}
	
	@Test
	public void testEndConsumerInvoiceList() throws Exception {
		createEndConsumerInvoiceListTestData("EndConsumerInvoiceListTestData.json");
		testEndConsumerInvoiceList(REGISTERED_CLIENT_INFO, "ecInvoice23", "ecInvoice21", "ecInvoice19");
		receivedMessageCollector.clearReceivedMessageList();
		testEndConsumerInvoiceList(ANONYMOUS_CLIENT_INFO, "ecInvoice3", "ecInvoice2", "ecInvoice1");
		receivedMessageCollector.clearReceivedMessageList();
		testEndConsumerInvoiceList(ADMIN_CLIENT_INFO);
	}

	private void testEndConsumerInvoiceList(ClientInfo clientInfo, String...invoices) throws IOException, URISyntaxException {
		String requestJson = getPlainTextJsonFromResources("endConsumerInvoiceListRequest.json", clientInfo)
				.replaceFirst("<<limit>>", "10").replaceFirst("<<offset>>", "0");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(ProfileMessageTypes.END_CONSUMER_INVOICE_LIST_RESPONSE);

		final JsonMessage<EndConsumerInvoiceListResponse> response = receivedMessageCollector
				.getMessageByType(ProfileMessageTypes.END_CONSUMER_INVOICE_LIST_RESPONSE);

		assertNull(response.getError());
		assertEndConsumerInvoiceListResponse(response.getContent(), invoices);
	}
	
	private void assertEndConsumerInvoiceListResponse(EndConsumerInvoiceListResponse content, String...invoiceIds) {
		List<JsonObject> items = content.getItems();
		
		assertEquals(invoiceIds.length, items.size());
		List<String> ids = items.stream().map(item -> item.get("id").getAsString()).collect(Collectors.toList());
		if (invoiceIds.length > 0) {
			assertThat(ids, contains(invoiceIds));
		}
	}

	private void createEndConsumerInvoiceListTestData(String filename) throws IOException {
		String set = config.getProperty(ProfileConfig.AEROSPIKE_END_CONSUMER_INVOICE_SET);
		updateMapTestData(filename, set, EndConsumerInvoiceAerospikeDao.BLOB_BIN_NAME);
	}

	private void updateMapTestData(String fileName, String set, String binName) throws IOException {

		String itemList = getPlainTextJsonFromResources(fileName);

		JsonParser parser = new JsonParser();
		JsonObject jsonObject = (JsonObject) parser.parse(itemList);
		for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
			String key = entry.getKey();
			String binValue = entry.getValue().toString();
			aerospikeDao.createOrUpdateList(set, key, binName,
					(readResult, writePolicy) -> getListObject(binValue));
		}
	}

	private List<Object> getListObject(String data) {
		List<Object> listObject = new ArrayList<>();
		JsonArray jsonArray = jsonMessageUtil.fromJson(data, JsonArray.class);
		jsonArray.forEach( item -> listObject.add(item.toString()));
		return listObject;
	}

	private void mergeProfileSubBlobs(String blobName, String userId1, String sourceBlobValue, String userId2,
			String targetBlobValue) throws Exception {
		final String updateSourceProfileBlobJson = getUpdateProfileBlobRequestJson(userId1, blobName, sourceBlobValue);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), updateSourceProfileBlobJson);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));

		receivedMessageCollector.clearReceivedMessageList();

		final String updateTargetProfileBlobJson = getUpdateProfileBlobRequestJson(userId2, blobName, targetBlobValue);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), updateTargetProfileBlobJson);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));

		receivedMessageCollector.clearReceivedMessageList();

		final String mergeUser1ToUser2 = getMergeProfileJson(userId1, userId2);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), mergeUser1ToUser2);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> mergeResponseMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(mergeResponseMessage, MergeProfileResponse.class);
		final MergeProfileResponse mergeResponse = (MergeProfileResponse) mergeResponseMessage.getContent();
		final Profile mergeProfile = new Profile(mergeResponse.getProfile());
		assertNotNull(mergeProfile.getSubBlobNames());
		assertEquals(1, mergeProfile.getSubBlobNames().size());
		assertEquals(blobName, mergeProfile.getSubBlobNames().iterator().next());

		receivedMessageCollector.clearReceivedMessageList();
	}

	private String getUpdateProfileUsingUserId(String userId) throws IOException {
		Validate.notNull(userId);
		final String updateProfileJson = getPlainTextJsonFromResources("updateProfileUsingUserId.json");
		String subBlobNames = "\"handicap\"";
		return updateProfileJson.replace("<<userId>>", userId).replace("\"<<subBlobNames>>\"", subBlobNames);
	}

	private String getProfileBlobRequestJson(String userId, String name) throws IOException {
		Validate.notNull(userId);
		return getPlainTextJsonFromResources("getPofileBlob.json")
			.replace("<<name>>", name)
			.replaceAll("<<userId>>", userId);
	}

	private String getProfileBlobRequestJson(String userId) throws IOException {
		Validate.notNull(userId);
		return getProfileBlobRequestJson(userId, "handicap");
	}

	private String getUpdateProfileBlobRequestJson(String userId, String name, String value) throws IOException {
		Validate.notNull(userId);
		return getPlainTextJsonFromResources("updatePofileBlob.json")
			.replace("<<name>>", name)
			.replace("\"<<value>>\"", value)
			.replace("<<userId>>", userId);
	}
	
	private String getUpdateProfileBlobRequestJson(String userId, String name) throws IOException {
		return getUpdateProfileBlobRequestJson(userId, name, "{\"any\": \"content\"}");
	}

	private String getUpdateProfileBlobRequestJson(String userId) throws IOException {
		return getUpdateProfileBlobRequestJson(userId, "handicap");
	}

	private void prepareAerospikeAppConfigRecord() throws Exception {
		aerospikeClient.put(null, new Key(NAMESPACE, SET, primaryKeyUtil.createPrimaryKey(KeyStoreTestUtil.TENANT_ID, APP_ID)), new Bin("value", APP_JSON.getBytes()));
		// put mock data for default tenant config :
		String tenantConfig = getPlainTextJsonFromResources("getAppConfigNoAppIdResponse.json");
		aerospikeClient.put(null, new Key(NAMESPACE, SET, primaryKeyUtil.createDefaultTenantConfigPrimaryKey(KeyStoreTestUtil.TENANT_ID)), new Bin("value", tenantConfig.getBytes()));
	}

	private void prepareAerospikeAppConfigRecordForAdvertisements() throws Exception {
		String appConfig = getPlainTextJsonFromResources("getAppConfigWithAdvertisement.json");
		aerospikeClient.put(null, new Key(NAMESPACE, SET, primaryKeyUtil.createPrimaryKey(KeyStoreTestUtil.TENANT_ID, APP_ID)), new Bin("value", appConfig.getBytes()));
	}

	private String getProfileJson(String userId) throws IOException {
		final String getProfileJson = getPlainTextJsonFromResources("getProfile.json");
		return getProfileJson.replaceFirst("<<userId>>", userId);
	}
	
	private GetProfileResponse getProfile(String userId) throws Exception {
		receivedMessageCollector.clearReceivedMessageList();

		final String getProfileJson = getProfileJson(userId);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getProfileJson);

		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> getProfileResponseMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);

		receivedMessageCollector.clearReceivedMessageList();

		assertMessageContentType(getProfileResponseMessage, GetProfileResponse.class);
		return (GetProfileResponse) getProfileResponseMessage.getContent();
	}
	
	/**
	 * Send profile update message and test it
	 * 
	 * @return Profile JSON - profile as result of update
	 * @throws Exception
	 */
	private UpdateProfileResponse updateProfileBy(String updateProfileJson) throws Exception {
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), updateProfileJson);
		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));

		final JsonMessage<? extends JsonMessageContent> updateProfileResponseMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(updateProfileResponseMessage, UpdateProfileResponse.class);

		receivedMessageCollector.clearReceivedMessageList();

		return (UpdateProfileResponse) updateProfileResponseMessage.getContent();
	}

	private UpdateProfileResponse createTestProfile(String userId, String updateProfileJson, String tenantId) throws Exception {
		final ProfileAerospikeDao profileAerospikeDao = jettyServerRule.getServerStartup().getInjector()
				.getInstance(ProfileAerospikeDao.class);

		final UpdateProfileResponse updateResponse;
		final Profile profile;
		if (updateProfileJson != null) {
			final JsonElement updateMessageElement = gson.fromJson(updateProfileJson, JsonElement.class);
			final JsonElement profileElement = updateMessageElement.getAsJsonObject().get("content").getAsJsonObject()
					.get("profile");
			profile = new Profile(new JsonObject());
			for (String property : Profile.NEVER_UPDATABLE_PROPERTIES) {
				JsonElement jsonElement = profileElement.getAsJsonObject().get(property);
				if (jsonElement != null) {
					profile.getJsonObject().add(property, jsonElement);
				}
			}
		} else {
			profile = new Profile(new JsonObject());
		}
		profile.setUserId(userId);
		if (tenantId != null) {
			assertTrue(profile.addTenant(tenantId));
		}

		profileAerospikeDao.createProfile(userId, profile);

		if (updateProfileJson != null) {
			final JsonElement updateMessageElement = gson.fromJson(updateProfileJson, JsonElement.class);
			updateResponse = updateProfileBy(updateMessageElement.toString());
		} else {
			updateResponse = new UpdateProfileResponse(profile.getJsonObject());
		}

		return updateResponse;

	}

	private UpdateProfileResponse createTestProfile(String userId) throws Exception {
		return createTestProfile(userId, getUpdateProfileUsingUserId(userId), null);
	}

	private UpdateProfileResponse createAnonymousTestProfile() throws Exception {
		return createTestProfile(ANONYMOUS_USER_ID);
	}

	private String getNameInCameCase(ProfileIdentifierType identifierType) {
		String typeName = identifierType.name().toLowerCase();
		typeName = Character.toUpperCase(typeName.charAt(0)) + typeName.substring(1);

		return typeName;
	}

	private String getMergeProfileJson(String sourceUserId, String targetUserId) throws IOException {
		String mergeJson = getPlainTextJsonFromResources("mergeProfile.json");

		mergeJson = mergeJson.replace("<<sourceUserId>>", sourceUserId);
		mergeJson = mergeJson.replace("<<targetUserId>>", targetUserId);

		return mergeJson;
	}

	private CreateProfileResponse createProfile(String createJsonPath) throws Exception {
		ClientInfo clientInfo = new ClientInfo();
		clientInfo.setCountryCode(ISOCountry.DE);
		final String createProfileJson = getPlainTextJsonFromResources(createJsonPath, clientInfo);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), createProfileJson);

		RetriedAssert.assertWithWait(() -> assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> createProfileResponseMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(createProfileResponseMessage, CreateProfileResponse.class);
		final CreateProfileResponse createProfileResponse = (CreateProfileResponse) createProfileResponseMessage
				.getContent();
		assertNotNull(createProfileResponse.getUserId());

		receivedMessageCollector.clearReceivedMessageList();

		return createProfileResponse;
	}

	private String getDeleteProfileByCorrectUserId() throws IOException {
		return getPlainTextJsonFromResources("deleteProfileByUserId.json").replaceAll("<<userId>>", ANONYMOUS_USER_ID);
	}

	@Override
	protected ServiceStartup getServiceStartup() {
		return new ProfileServiceStartupUsingAerospikeMock(DEFAULT_TEST_STAGE);
	}

	@Override
	protected TestClientInjectionClasses getTestClientInjectionClasses() {
		return new TestClientInjectionClasses(ProfileDefaultMessageMapper.class, ProfileMessageSchemaMapper.class);
	}
	
	private JsonMessageContent onReceivedMessage(RequestContext ctx) {
		dummyServiceMessageCollector.onProcess(ctx);
		if (PaymentMessageTypes.INSERT_OR_UPDATE_USER == ctx.getMessage().getType(PaymentMessageTypes.class)) {
			return new EmptyJsonMessageContent();
		} else {
			return null;
		}
	}

}