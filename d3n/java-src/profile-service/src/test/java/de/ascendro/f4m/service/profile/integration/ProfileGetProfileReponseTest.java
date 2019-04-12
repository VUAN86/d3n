package de.ascendro.f4m.service.profile.integration;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.hamcrest.core.CombinableMatcher;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.di.GsonProvider;
import de.ascendro.f4m.service.exception.validation.F4MValidationException;
import de.ascendro.f4m.service.json.JsonMessageDeserializer;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.validator.JsonMessageValidator;
import de.ascendro.f4m.service.profile.ProfileMessageTypeMapper;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.schema.ProfileMessageSchemaMapper;
import de.ascendro.f4m.service.util.ProfileServiceTestHelper;

public class ProfileGetProfileReponseTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProfileGetProfileReponseTest.class);
	private JsonMessageUtil jsonUtil;
	
	@Before
	public void setUp() {
		JsonMessageSchemaMap jsonMessageSchemaMap = new ProfileMessageSchemaMapper();
		JsonMessageValidator jsonMessageValidator = new JsonMessageValidator(jsonMessageSchemaMap, new F4MConfigImpl());
		jsonUtil = new JsonMessageUtil(new GsonProvider(new JsonMessageDeserializer(new ProfileMessageTypeMapper())),
				null, jsonMessageValidator);
	}
	
	@Test
	public void testThatCommonGetProfileResponseIfValidAccordingToSchema() throws Exception {
		jsonUtil.validate(ProfileServiceTestHelper.getProfileServiceGetProfileResponseAsString());
	}

	@Test
	public void testProfileUpdateDoesNotAllowIncorrectProfile() throws Exception {
		String updateProfileWithDeltas = IOUtils.toString(this.getClass()
				.getResourceAsStream("updateProfileWithDeltas.json"), "UTF-8");
		jsonUtil.validate(updateProfileWithDeltas);
		String addressPropertyStart = "\"address\": {";
		updateProfileWithDeltas = updateProfileWithDeltas.replace(addressPropertyStart,
				"\"unexpectedProperty\" : 2," + addressPropertyStart);
		try {
			jsonUtil.validate(updateProfileWithDeltas);
			fail("unexpectedProperty should not be allowed");
		} catch (F4MValidationException e) {
			LOGGER.debug("Expected validation error", e);
			//keep in mind that Jenkins uses compiled schemas (locally f4m-backend-api-schemas/development is used)
			// - messages might be different when building with Jenkins, for example "only 1 subschema matches out of 2" might be missing
			assertThat(e.getCause().getMessage(), containsString("[unexpectedProperty] is not permitted\n"));
		}
	}

	@Test
	public void testThatCommonGetProfileResponseIfValid() throws Exception {
		//add here assertions about assumptions used in other service tests
		Profile profile = ProfileServiceTestHelper.getTestProfile(jsonUtil);
		CombinableMatcher<Collection<? extends Object>> exists = both(not(empty())).and(notNullValue());
		assertThat(profile.getApplications(), notNullValue());
		assertNotNull(profile.getJsonObject());
		assertThat(profile.getDevicesAsJsonArray(), not(emptyIterable()));
		List<String> deviceIds = new ArrayList<>();
		for (JsonElement deviceJson : profile.getDevicesAsJsonArray()) {
			assertTrue(deviceJson.isJsonObject());
			JsonObject deviceObject = (JsonObject)deviceJson;
			deviceIds.add(deviceObject.get(Profile.DEVICE_UUID_PROPERTY_NAME).getAsString());
		}
		assertThat(deviceIds, contains("device_id_1", "device_id_2"));
		assertThat(profile.getIdentifiers(), exists);
		assertThat(profile.getProfileEmails(), exists);
		assertThat(profile.getProfileGoogle(), not(isEmptyOrNullString()));
		assertThat(profile.getProfilePhones(), exists);
		assertThat(profile.getSubBlobNames(), exists);
		assertThat(profile.getTenants(), notNullValue());
		assertThat(profile.getUserId(), not(isEmptyOrNullString()));
		assertEquals("NaSu2", profile.getFullNameOrNickname());
		assertEquals("nasu2", profile.getSearchName());
		profile.setShowFullName(true);
		assertEquals("Name Surname", profile.getFullNameOrNickname());
		assertEquals("name surname", profile.getSearchName());
	}
}