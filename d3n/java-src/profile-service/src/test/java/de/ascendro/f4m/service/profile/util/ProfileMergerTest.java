package de.ascendro.f4m.service.profile.util;

import static de.ascendro.f4m.service.profile.model.Profile.APPLICATIONS_PROPERTY_NAME;
import static de.ascendro.f4m.service.profile.model.Profile.DEVICES_PROPERTY_NAME;
import static de.ascendro.f4m.service.profile.model.Profile.DEVICE_IMEI_PROPERTY_NAME;
import static de.ascendro.f4m.service.profile.model.Profile.DEVICE_UUID_PROPERTY_NAME;
import static de.ascendro.f4m.service.profile.model.Profile.EMAILS_PROPERTY;
import static de.ascendro.f4m.service.profile.model.Profile.EMAIL_PROPERTY;
import static de.ascendro.f4m.service.profile.model.Profile.PERSON_PROPERTY;
import static de.ascendro.f4m.service.profile.model.Profile.PHONES_PROPERTY;
import static de.ascendro.f4m.service.profile.model.Profile.PHONE_PROPERTY;
import static de.ascendro.f4m.service.profile.model.Profile.ROLES_PROPERTY;
import static de.ascendro.f4m.service.profile.model.Profile.SUB_BLOB_NAMES_PROPERTY_NAME;
import static de.ascendro.f4m.service.profile.model.Profile.TENANTS_PROPERTY_NAME;
import static de.ascendro.f4m.service.profile.model.Profile.USER_ID_PROPERTY;
import static de.ascendro.f4m.service.profile.model.ProfileUser.PERSON_FIRST_NAME_PROPERTY;
import static de.ascendro.f4m.service.profile.model.ProfileUser.PERSON_LAST_NAME_PROPERTY;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.lang.reflect.Type;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import de.ascendro.f4m.service.di.GsonProvider;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.validator.JsonMessageValidator;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileFacebookId;
import de.ascendro.f4m.service.util.JsonTestUtil;
import de.ascendro.f4m.service.util.ServiceUtil;

public class ProfileMergerTest {
	private JsonMessageUtil jsonUtil;
	private Gson gson;
	private ProfileMerger merger;
	private ProfileMerger updater;

	@Mock
	private ServiceUtil serviceUtil;
	@Mock
	private JsonMessageValidator jsonMessageValidator;
	@Mock
	private GsonProvider gsonProvider;
	private Profile existingProfile;
	private Profile deltaProfile;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		gson = JsonTestUtil.getGson();
		when(gsonProvider.get()).thenReturn(gson);

		jsonUtil = new JsonMessageUtil(gsonProvider, serviceUtil, jsonMessageValidator);
		merger = new ProfileMerger(jsonUtil, ProfileMerger.MergeType.MERGE);
		updater = new ProfileMerger(jsonUtil, ProfileMerger.MergeType.UPDATE);

		existingProfile = new Profile();
		deltaProfile = new Profile();
	}

	@Test
	public void testUpdate() throws Exception {
		//try out JUnit Theories instead of copying methods?
		testSimpleValues(updater, true);
		testPersonValues(updater, true);
		testPersonValuesInsert(updater, true);
	}
	
	@Test
	public void testMerge() throws Exception {
		testSimpleValues(merger, false);
		testPersonValues(merger, false);
		testPersonValuesInsert(merger, false);
	}
	
	private void testSimpleValues(ProfileMerger profileMerger, boolean update) throws Exception {
		existingProfile.getJsonObject().addProperty(USER_ID_PROPERTY, "userX");
		existingProfile.getJsonObject().addProperty("autoShare", false);

		deltaProfile.getJsonObject().addProperty("region", "Germany");
		deltaProfile.getJsonObject().addProperty("autoShare", true);

		JsonObject result = profileMerger.mergeProfileObjects(deltaProfile, existingProfile).getJsonObject();
		assertEquals("userX", result.get(USER_ID_PROPERTY).getAsString()); // from db
		assertEquals("Germany", result.get("region").getAsString()); // new from delta
		assertEquals(update ? true : false, result.get("autoShare").getAsBoolean());
	}

	private void testPersonValues(ProfileMerger profileMerger, boolean update) throws Exception {
		JsonObject existingPerson = new JsonObject();
		existingPerson.addProperty(PERSON_FIRST_NAME_PROPERTY, "Firstname");
		existingPerson.addProperty("sex", "M");
		existingProfile.getJsonObject().add(PERSON_PROPERTY, existingPerson);

		JsonObject deltaPerson = new JsonObject();
		deltaPerson.addProperty(PERSON_LAST_NAME_PROPERTY, "Lastname");
		deltaPerson.addProperty("sex", "F");
		deltaProfile.getJsonObject().add(PERSON_PROPERTY, deltaPerson);

		JsonObject result = profileMerger.mergeProfileObjects(deltaProfile, existingProfile).getJsonObject()
				.get(PERSON_PROPERTY).getAsJsonObject();
		assertEquals("Firstname", result.get(PERSON_FIRST_NAME_PROPERTY).getAsString());
		assertEquals("Lastname", result.get(PERSON_LAST_NAME_PROPERTY).getAsString());
		assertEquals(update ? "F" : "M", result.get("sex").getAsString()); // old kept
	}

	private void testPersonValuesInsert(ProfileMerger profileMerger, boolean update) throws Exception {
		JsonObject deltaPerson = new JsonObject();
		deltaPerson.addProperty(PERSON_LAST_NAME_PROPERTY, "Lastname");
		deltaPerson.addProperty("sex", "F");
		deltaProfile.getJsonObject().add(PERSON_PROPERTY, deltaPerson);

		JsonObject result = profileMerger.mergeProfileObjects(deltaProfile, existingProfile).getJsonObject()
				.get(PERSON_PROPERTY).getAsJsonObject();
		assertEquals("Lastname", result.get(PERSON_LAST_NAME_PROPERTY).getAsString());
		assertEquals(update ? "F" : "M", result.get("sex").getAsString()); // old kept
	}

	@Test
	public void testSimpleArraysUpdate() throws Exception {
		JsonArray result = testSimpleArrays(updater, ROLES_PROPERTY);
		String[] expected = new String[] {"3", "2", "4"};
		assertArrayEquals(expected, toStringArray(result));
		
		result = testSimpleArrays(updater, APPLICATIONS_PROPERTY_NAME);
		assertArrayEquals(expected, toStringArray(result));

		result = testSimpleArrays(updater, TENANTS_PROPERTY_NAME);
		assertArrayEquals(expected, toStringArray(result));
	}
	
	@Test
	public void testSimpleArraysMerge() throws Exception {
		JsonArray result = testSimpleArrays(merger, ROLES_PROPERTY);
		String[] expected = new String[] {"1", "2", "3", "4"};
		assertArrayEquals(expected, toStringArray(result));
		
		result = testSimpleArrays(merger, APPLICATIONS_PROPERTY_NAME);
		assertArrayEquals(expected, toStringArray(result));

		result = testSimpleArrays(merger, TENANTS_PROPERTY_NAME);
		assertArrayEquals(expected, toStringArray(result));
	}
	
	
	@Test
	public void testSubBlobNames() throws Exception {
		String[] expected = new String[] {"1", "2"};
		JsonArray result = testSimpleArrays(merger, SUB_BLOB_NAMES_PROPERTY_NAME);
		assertArrayEquals(expected, toStringArray(result));
		
		result = testSimpleArrays(updater, SUB_BLOB_NAMES_PROPERTY_NAME);
		assertArrayEquals(expected, toStringArray(result));
	}
	
	private JsonArray testSimpleArrays(ProfileMerger profileMerger, String property) throws Exception {
		JsonArray existingRoles = new JsonArray();
		existingRoles.add("1");
		existingRoles.add("2");
		existingProfile.getJsonObject().add(property, existingRoles);

		String[] expectedRoles = {"3", "2", "4"};
		JsonArray deltaRoles = new JsonArray();
		for (int i = 0; i < expectedRoles.length; i++) {
			deltaRoles.add(expectedRoles[i]);
		}
		deltaProfile.getJsonObject().add(property, deltaRoles);

		return profileMerger.mergeProfileObjects(deltaProfile, existingProfile).getJsonObject()
				.get(property).getAsJsonArray();
	}
	
	@Test
	public void testEmailMerge() throws Exception {
		testEmailAndPhoneMerge(EMAILS_PROPERTY, EMAIL_PROPERTY);
	}
	
	@Test
	public void testPhoneMerge() throws Exception {
		testEmailAndPhoneMerge(PHONES_PROPERTY, PHONE_PROPERTY);
	}
	
	public void testEmailAndPhoneMerge(String arrayProperty, String identifierProperty) throws Exception {
		JsonArray result = testEmail(arrayProperty, identifierProperty, merger);
		assertEquals(4, result.size());
		assertEquals("1@mail.com", getArrayObjectProperty(result, 0, identifierProperty));
		assertEquals("2@mail.com", getArrayObjectProperty(result, 1, identifierProperty));
		assertEquals("verified", getArrayObjectProperty(result, 1, "verificationStatus"));
		assertEquals("3@mail.com", getArrayObjectProperty(result, 2, identifierProperty));
		assertEquals("2@mail.com", getArrayObjectProperty(result, 3, identifierProperty));
		assertEquals("notVerified", getArrayObjectProperty(result, 3, "verificationStatus"));
	}
	
	@Test
	public void testEmailUpdate() throws Exception {
		testEmailAndPhoneUpdate(EMAILS_PROPERTY, EMAIL_PROPERTY);
	}
	
	@Test
	public void testPhoneUpdate() throws Exception {
		testEmailAndPhoneUpdate(PHONES_PROPERTY, PHONE_PROPERTY);
	}
	
	public void testEmailAndPhoneUpdate(String arrayProperty, String identifierProperty) throws Exception {
		JsonArray result = testEmail(arrayProperty, identifierProperty, updater);
		assertEquals(3, result.size());
		assertEquals("1@mail.com", getArrayObjectProperty(result, 0, identifierProperty));
		assertEquals("2@mail.com", getArrayObjectProperty(result, 1, identifierProperty));
		assertEquals("notVerified", getArrayObjectProperty(result, 1, "verificationStatus"));
		assertEquals("3@mail.com", getArrayObjectProperty(result, 2, identifierProperty));
	}
	
	private JsonArray testEmail(String arrayProperty, String identifierProperty, ProfileMerger profileMerger) throws Exception {
		JsonArray existingEmails = new JsonArray();
		existingEmails.add(createEmailJson("1@mail.com", "verified", identifierProperty));
		existingEmails.add(createEmailJson("2@mail.com", "verified", identifierProperty));
		existingProfile.getJsonObject().add(arrayProperty, existingEmails);

		JsonArray deltaRoles = new JsonArray();
		deltaRoles.add(createEmailJson("3@mail.com", "notVerified", identifierProperty));
		deltaRoles.add(createEmailJson("2@mail.com", "notVerified", identifierProperty));
		deltaProfile.getJsonObject().add(arrayProperty, deltaRoles);
		return profileMerger.mergeProfileObjects(deltaProfile, existingProfile).getJsonObject()
				.get(arrayProperty).getAsJsonArray();
	}
	
	private JsonObject createEmailJson(String email, String status, String identifierProperty) {
		JsonObject mail = new JsonObject();
		mail.addProperty(identifierProperty, email);
		mail.addProperty("verificationStatus", status);
		return mail;
	}
	
	@Test
	public void testDeviceMerge() throws Exception {
		JsonArray result = prepareDevices(merger);
		assertEquals(3, result.size());
		assertEquals("uuid1", getArrayObjectProperty(result, 0, DEVICE_UUID_PROPERTY_NAME));
		assertEquals("uuid2", getArrayObjectProperty(result, 1, DEVICE_UUID_PROPERTY_NAME));
		assertEquals("uuid3", getArrayObjectProperty(result, 2, DEVICE_UUID_PROPERTY_NAME));
	}

	@Test
	public void testDeviceUpdate() throws Exception {
		JsonArray result = prepareDevices(updater);
		assertEquals(2, result.size());
		assertEquals("uuid1", getArrayObjectProperty(result, 0, DEVICE_UUID_PROPERTY_NAME));
		assertEquals("uuid2", getArrayObjectProperty(result, 1, DEVICE_UUID_PROPERTY_NAME));
		//deltaDevices should be ignored
	}

	private JsonArray prepareDevices(ProfileMerger profileMerger) {
		JsonArray existingDevices = new JsonArray();
		existingDevices.add(createDeviceJson("uuid1", "imei1", "apnsToken1"));
		existingDevices.add(createDeviceJson("uuid2", "imei2", "apnsToken2"));
		existingProfile.getJsonObject().add(DEVICES_PROPERTY_NAME, existingDevices);

		JsonArray deltaDevices = new JsonArray();
		deltaDevices.add(createDeviceJson("uuid2", "imei2-updated", "apnsToken2-updated"));
		deltaDevices.add(createDeviceJson("uuid3", "imei3", "apnsToken3"));
		deltaProfile.getJsonObject().add(DEVICES_PROPERTY_NAME, deltaDevices);
		return profileMerger.mergeProfileObjects(deltaProfile, existingProfile).getJsonObject()
				.get(DEVICES_PROPERTY_NAME).getAsJsonArray();
	}

	@Test
	public void testDeviceInsert() throws Exception {
		//JsonArray existingDevices = new JsonArray();
		//existingProfile.getJsonObject().add(DEVICES_PROPERTY_NAME, existingDevices);
		JsonArray deltaDevices = new JsonArray();
		deltaDevices.add(createDeviceJson("uuid1", "imei1", "apnsToken"));
		deltaProfile.getJsonObject().add(DEVICES_PROPERTY_NAME, deltaDevices);
		
		JsonArray result = updater.mergeProfileObjects(deltaProfile, existingProfile).getJsonObject()
				.get(DEVICES_PROPERTY_NAME).getAsJsonArray();

		assertEquals(1, result.size());
		assertEquals("imei1", getArrayObjectProperty(result, 0, DEVICE_IMEI_PROPERTY_NAME));
	}

	@Test
	public void testFacebookUpdate() throws Exception {
		testFbPropertyUpdateOrMerge(updater);
	}
	
	@Test
	public void testFacebookMerge() throws Exception {
		testFbPropertyUpdateOrMerge(merger);
	}
	
	private void testFbPropertyUpdateOrMerge(ProfileMerger profileMerger) throws Exception {
		JsonArray existingFbIds = new JsonArray();
		existingFbIds.add(createFacebookIdJson("123456", "existing"));
		existingProfile.getJsonObject().add(Profile.FACEBOOK_IDS_PROPERTY, existingFbIds);

		JsonArray deltaFacebookIds = new JsonArray();
		deltaFacebookIds.add(createFacebookIdJson("88564", "new"));
		deltaFacebookIds.add(createFacebookIdJson("123456", "new"));
		deltaProfile.getJsonObject().add(Profile.FACEBOOK_IDS_PROPERTY, deltaFacebookIds);
		JsonArray result = profileMerger.mergeProfileObjects(deltaProfile, existingProfile).getJsonObject()
				.get(Profile.FACEBOOK_IDS_PROPERTY).getAsJsonArray();

		assertEquals(2, result.size());
		assertEquals("123456", getArrayObjectProperty(result, 0, ProfileFacebookId.FACEBOOK_USER_ID_PROPERTY));
		assertEquals("new", getArrayObjectProperty(result, 0, ProfileFacebookId.FACEBOOK_APPLICATION_ID_PROPERTY));
		assertEquals("88564", getArrayObjectProperty(result, 1, ProfileFacebookId.FACEBOOK_USER_ID_PROPERTY));
	}
	
	private JsonObject createFacebookIdJson(String facebookUserId, String appId) {
		ProfileFacebookId id = new ProfileFacebookId();
		id.setFacebookUserId(facebookUserId);
		id.setFacebookAppId(appId);
		return id.getJsonObject();
	}
	
	private JsonObject createDeviceJson(String deviceUUID, String imei, String apnsDeviceToken) {
		JsonObject mail = new JsonObject();
		if (deviceUUID != null) {
			mail.addProperty(DEVICE_UUID_PROPERTY_NAME, deviceUUID);
		}
		mail.addProperty(DEVICE_IMEI_PROPERTY_NAME, imei);
		return mail;
	}
	
	private String getArrayObjectProperty(JsonArray result, int index, String attributeName) {
		return result.get(index).getAsJsonObject().get(attributeName).getAsString();
	}
	
	private String[] toStringArray(JsonArray jsonArray) {
		Type listType = new TypeToken<List<String>>(){}.getType();
		List<String> json = gson.fromJson(jsonArray.toString(), listType);
		return json.toArray(new String[json.size()]);
	}
}
