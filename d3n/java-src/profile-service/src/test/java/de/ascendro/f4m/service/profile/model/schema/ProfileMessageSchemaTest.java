package de.ascendro.f4m.service.profile.model.schema;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.validator.JsonMessageValidator;
import de.ascendro.f4m.service.profile.ProfileMessageTypes;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.get.app.GetAppConfigurationRequest;
import de.ascendro.f4m.service.profile.model.get.profile.GetProfileResponse;
import de.ascendro.f4m.service.profile.model.schema.ProfileMessageSchemaMapper;
import de.ascendro.f4m.service.util.JsonTestUtil;
import de.ascendro.f4m.service.util.ProfileServiceTestHelper;

public class ProfileMessageSchemaTest {
	private JsonMessageValidator jsonMessageValidator;
	private Gson gson;

	@Before
	public void setUp() {
		ProfileMessageSchemaMapper schemaMap = new ProfileMessageSchemaMapper();
		F4MConfigImpl config = new F4MConfigImpl();
		jsonMessageValidator = new JsonMessageValidator(schemaMap, config);
		gson = JsonTestUtil.getGson();
	}

	@Test
	public void testProfileDevices() throws Exception {
		Profile profile = ProfileServiceTestHelper.getTestProfile();
		JsonMessage<GetProfileResponse> message = new JsonMessage<GetProfileResponse>(
				ProfileMessageTypes.GET_PROFILE_RESPONSE, new GetProfileResponse(profile.getJsonObject()));
		message.setSeq(2L);
		message.setAck(1L);
		validate(message);

		JsonObject device = (JsonObject) profile.getDevicesAsJsonArray().get(0);
		device.addProperty(Profile.DEVICE_IMEI_PROPERTY_NAME, "imei");
		validate(message);

		device.addProperty("lolol", "lol");
		validateWithFailure(message, "lolol should not be permitted");
	}

	@Test
	public void testGetAppConfigurationDevices() throws Exception {
		GetAppConfigurationRequest content = new GetAppConfigurationRequest();
		JsonMessage<GetAppConfigurationRequest> message = new JsonMessage<GetAppConfigurationRequest>(
				ProfileMessageTypes.GET_APP_CONFIGURATION, content);
		message.setSeq(2L);
		message.setAck(1L);
		content.setTenantId("tenant");
		content.setIMEI("imei");
		JsonObject device = new JsonObject();
		content.setDevice(device);
		device.addProperty("screenHeight", 255);
		validate(message);
		
		device.addProperty("lolol", "lol");
		validateWithFailure(message, "lolol should not be permitted");
	}

	private void validate(JsonMessage<?> message) {
		jsonMessageValidator.validate(gson.toJson(message));
	}

	private void validateWithFailure(JsonMessage<?> message, String failureMessage) {
		try {
			validate(message);
			fail(failureMessage);
		} catch (F4MValidationFailedException e) {
		}
	}
}
