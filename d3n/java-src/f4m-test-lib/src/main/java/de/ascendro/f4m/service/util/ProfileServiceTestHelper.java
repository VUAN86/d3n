package de.ascendro.f4m.service.util;

import java.io.IOException;

import org.apache.commons.io.IOUtils;

import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.di.GsonProvider;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.event.model.NotifySubscriberMessageContent;
import de.ascendro.f4m.service.exception.UnexpectedTestException;
import de.ascendro.f4m.service.json.JsonMessageDeserializer;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.validator.JsonMessageValidator;
import de.ascendro.f4m.service.profile.ProfileMessageTypeMapper;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.get.profile.GetProfileResponse;
import de.ascendro.f4m.service.profile.model.merge.ProfileMergeEvent;
import de.ascendro.f4m.service.profile.model.schema.ProfileMessageSchemaMapper;

/**
 * Class intended to be used in testing all services that are using profile service.
 */
public class ProfileServiceTestHelper {

	private static final long SUBSCRIPTION_ID = 7464L;
	
	/**
	 * Returns GetProfileResponse containing full set of profile attributes which should be used for all service testing.
	 * 
	 * @param jsonUtil
	 * @return
	 */
	public static GetProfileResponse getProfileServiceGetProfileResponse(JsonMessageUtil jsonUtil) {
		String profileServiceGetProfileResponseString = getProfileServiceGetProfileResponseAsString();
		JsonMessage<? extends JsonMessageContent> json = jsonUtil.fromJson(profileServiceGetProfileResponseString);
		return (GetProfileResponse) json.getContent();
	}
	
	public static Profile getTestProfile() {
		JsonMessageSchemaMap jsonMessageSchemaMap = new ProfileMessageSchemaMapper();
		JsonMessageValidator jsonMessageValidator = new JsonMessageValidator(jsonMessageSchemaMap, new F4MConfigImpl());
		JsonMessageUtil jsonUtil = new JsonMessageUtil(new GsonProvider(new JsonMessageDeserializer(new ProfileMessageTypeMapper())),
				null, jsonMessageValidator);
		return getTestProfile(jsonUtil);
	}

	public static Profile getTestProfile(JsonMessageUtil jsonUtil) {
		GetProfileResponse getProfileResponse = getProfileServiceGetProfileResponse(jsonUtil);
		return new Profile(getProfileResponse.getProfile());
	}

	public static String getProfileServiceGetProfileResponseAsString() {
		String profileServiceGetProfileResponseString;
		try {
			profileServiceGetProfileResponseString = IOUtils.toString(ProfileServiceTestHelper.class
					.getResourceAsStream("/integration/profile/profileServiceGetProfileResponse.json"), "UTF-8");
			return profileServiceGetProfileResponseString;
		} catch (IOException e) {
			throw new UnexpectedTestException("Error reading test file", e);
		}
	}

	public static JsonMessage<NotifySubscriberMessageContent> createMergeProfileMessage(JsonMessageUtil jsonMessageUtil,
			String sourceUserId, String targetUserId) {
		final NotifySubscriberMessageContent notifySubscriberMessageContent = new NotifySubscriberMessageContent(
				SUBSCRIPTION_ID, ProfileMergeEvent.PROFILE_MERGE_EVENT_TOPIC);
		notifySubscriberMessageContent
				.setNotificationContent((new ProfileMergeEvent(sourceUserId, targetUserId)).getJsonObject());
		return jsonMessageUtil.createNewMessage(EventMessageTypes.NOTIFY_SUBSCRIBER, notifySubscriberMessageContent);
	}

}
