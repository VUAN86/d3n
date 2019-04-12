package de.ascendro.f4m.service.profile.util;

import static de.ascendro.f4m.service.profile.util.ProfileMerger.ArrayMergePolicy.DONT_UPDATE;
import static de.ascendro.f4m.service.profile.util.ProfileMerger.ArrayMergePolicy.MERGE_BY_ID;
import static de.ascendro.f4m.service.profile.util.ProfileMerger.ArrayMergePolicy.OVERWRITE;
import static de.ascendro.f4m.service.profile.util.ProfileMerger.ArrayMergePolicy.OVERWRITE_IF_NULL;
import static de.ascendro.f4m.service.profile.util.ProfileMerger.ArrayMergePolicy.UNION;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileAddress;
import de.ascendro.f4m.service.profile.model.ProfileFacebookId;
import de.ascendro.f4m.service.profile.model.ProfileSettingsGame;
import de.ascendro.f4m.service.profile.model.ProfileUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements profile merging functionality on JSON level. 
 */
public class ProfileMerger {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProfileMerger.class);


	public enum MergeType {
		MERGE, UPDATE
	}
	
	protected enum ArrayMergePolicy {
		OVERWRITE, OVERWRITE_IF_NULL, UNION, MERGE_BY_ID, DONT_UPDATE
	}
	
	private final JsonMessageUtil jsonUtil;
	
	private Map<String, ArrayMergePolicy> arrayMergePolicies;
	private Map<String, String> propertyIdentifiers;
	private final MergeType mergeType;
	
	public ProfileMerger(JsonMessageUtil jsonUtil, MergeType mergeType) {
		this.jsonUtil = jsonUtil;
		this.mergeType = mergeType;
		initMergePolicies();
	}
	
	private void initMergePolicies() {
		arrayMergePolicies = new HashMap<>();
		propertyIdentifiers = new HashMap<>();
		if (MergeType.UPDATE.equals(mergeType)) {
			arrayMergePolicies.put(Profile.ROLES_PROPERTY, OVERWRITE);
			arrayMergePolicies.put(Profile.APPLICATIONS_PROPERTY_NAME, OVERWRITE);
			arrayMergePolicies.put(Profile.TENANTS_PROPERTY_NAME, OVERWRITE);
			arrayMergePolicies.put(Profile.EMAILS_PROPERTY, MERGE_BY_ID);
			propertyIdentifiers.put(Profile.EMAILS_PROPERTY, Profile.EMAIL_PROPERTY);
			arrayMergePolicies.put(Profile.PHONES_PROPERTY, MERGE_BY_ID);
			propertyIdentifiers.put(Profile.PHONES_PROPERTY, Profile.PHONE_PROPERTY);
			arrayMergePolicies.put(Profile.DEVICES_PROPERTY_NAME, DONT_UPDATE);
			arrayMergePolicies.put(ProfileSettingsGame.CATEGORIES_PROPERTY, OVERWRITE);
		} else {
			arrayMergePolicies.put(Profile.ROLES_PROPERTY, UNION);
			arrayMergePolicies.put(Profile.APPLICATIONS_PROPERTY_NAME, UNION);
			arrayMergePolicies.put(Profile.TENANTS_PROPERTY_NAME, UNION);
			arrayMergePolicies.put(Profile.EMAILS_PROPERTY, UNION); 
			arrayMergePolicies.put(Profile.PHONES_PROPERTY, UNION);
			arrayMergePolicies.put(ProfileSettingsGame.CATEGORIES_PROPERTY, UNION);

			for (int i = 0; i < Profile.NEVER_UPDATABLE_PROPERTIES.length; i++) {
				arrayMergePolicies.put(Profile.NEVER_UPDATABLE_PROPERTIES[i], DONT_UPDATE);
			}
			
			arrayMergePolicies.put(Profile.DEVICES_PROPERTY_NAME, DONT_UPDATE);
			
			arrayMergePolicies.put(Profile.PUSH_NOTIFICATIONS_NEWS_PROPERTY, DONT_UPDATE);
			arrayMergePolicies.put(Profile.PUSH_NOTIFICATIONS_TOMBOLA_PROPERTY, DONT_UPDATE);
			arrayMergePolicies.put(Profile.PUSH_NOTIFICATIONS_TOURNAMENT_PROPERTY, DONT_UPDATE);
			arrayMergePolicies.put(Profile.PUSH_NOTIFICATIONS_DUEL_PROPERTY, DONT_UPDATE);
			arrayMergePolicies.put(Profile.PUSH_NOTIFICATIONS_FRIEND_ACTIVITY_PROPERTY, DONT_UPDATE);
			arrayMergePolicies.put(Profile.SHOW_FULL_NAME_PROPERTY, DONT_UPDATE);
			arrayMergePolicies.put(Profile.AUTO_SHARE_PROPERTY, DONT_UPDATE);
			arrayMergePolicies.put(Profile.MONEY_GAMES_PROPERTY, DONT_UPDATE);

			arrayMergePolicies.put(Profile.LANGUAGE_PROPERTY, OVERWRITE_IF_NULL);
			arrayMergePolicies.put(Profile.PHOTO_ID_PROPERTY, OVERWRITE_IF_NULL);
			arrayMergePolicies.put(Profile.REGION_PROPERTY, OVERWRITE_IF_NULL);
			arrayMergePolicies.put(Profile.GOOGLE_PROPERTY, OVERWRITE_IF_NULL);
			arrayMergePolicies.put(Profile.NICKNAME_PROPERTY, OVERWRITE_IF_NULL);
			arrayMergePolicies.put(Profile.FACEBOOK_PROPERTY, OVERWRITE_IF_NULL);
			arrayMergePolicies.put(Profile.INVITATION_AUTO_ACCEPT_PROPERTY, OVERWRITE_IF_NULL);
			arrayMergePolicies.put(Profile.RECOMMENDED_BY_PROPERTY, OVERWRITE_IF_NULL);
			arrayMergePolicies.put(Profile.RECOMMENDED_BY_PROPERTY, OVERWRITE_IF_NULL);

			arrayMergePolicies.put(ProfileSettingsGame.CATEGORIES_PROPERTY, UNION);
			arrayMergePolicies.put(ProfileSettingsGame.CURRENCY_PROPERTY, DONT_UPDATE);
			arrayMergePolicies.put(ProfileSettingsGame.ENTRY_FEE_PROPERTY, DONT_UPDATE);
			arrayMergePolicies.put(ProfileSettingsGame.INTERNATIONAL_ONLY_PROPERTY, DONT_UPDATE);
			arrayMergePolicies.put(ProfileSettingsGame.MINIMUM_GAME_LEVEL_PROPERTY, DONT_UPDATE);
			arrayMergePolicies.put(ProfileSettingsGame.NUMBER_OF_QUESTIONS_PROPERTY, DONT_UPDATE);
			
			arrayMergePolicies.put(ProfileUser.BIRTH_DATE_PROPERTY, ArrayMergePolicy.OVERWRITE_IF_NULL);
			arrayMergePolicies.put(ProfileUser.NICKNAME_PROPERTY, ArrayMergePolicy.OVERWRITE_IF_NULL);
			arrayMergePolicies.put(ProfileUser.PERSON_FIRST_NAME_PROPERTY, ArrayMergePolicy.OVERWRITE_IF_NULL);
			arrayMergePolicies.put(ProfileUser.PERSON_LAST_NAME_PROPERTY, ArrayMergePolicy.OVERWRITE_IF_NULL);
			arrayMergePolicies.put(ProfileUser.SEX_PROPERTY, ArrayMergePolicy.OVERWRITE_IF_NULL);

			arrayMergePolicies.put(ProfileAddress.ADDRESS_CITY_PROPERTY, ArrayMergePolicy.OVERWRITE_IF_NULL);
			arrayMergePolicies.put(ProfileAddress.ADDRESS_COUNTRY_PROPERTY, ArrayMergePolicy.OVERWRITE_IF_NULL);
			arrayMergePolicies.put(ProfileAddress.ADDRESS_POSTAL_CODE_PROPERTY, ArrayMergePolicy.OVERWRITE_IF_NULL);
			arrayMergePolicies.put(ProfileAddress.ADDRESS_STREET_NUMBER_PROPERTY, ArrayMergePolicy.OVERWRITE_IF_NULL);
			arrayMergePolicies.put(ProfileAddress.ADDRESS_STREET_PROPERTY, ArrayMergePolicy.OVERWRITE_IF_NULL);
		}
		arrayMergePolicies.put(Profile.SUB_BLOB_NAMES_PROPERTY_NAME, DONT_UPDATE);
		arrayMergePolicies.put(Profile.FACEBOOK_IDS_PROPERTY, ArrayMergePolicy.MERGE_BY_ID);
		propertyIdentifiers.put(Profile.FACEBOOK_IDS_PROPERTY, ProfileFacebookId.FACEBOOK_USER_ID_PROPERTY);
	}

	public Profile mergeProfileObjects(Profile sourceProfile, Profile targetProfile, boolean isServiceResultEngineOrGamEngine) {
		// Special handling of handicap
		double sourceHandicap = sourceProfile.getHandicap() == null ? 0.0 : sourceProfile.getHandicap();
		double targetHandicap = targetProfile.getHandicap() == null ? 0.0 : targetProfile.getHandicap();
		double newHandicap = 0.0;
		if (isServiceResultEngineOrGamEngine) {
			newHandicap = sourceHandicap;
		} else {
			newHandicap = Math.max(sourceHandicap, targetHandicap);
		}


		// Special handling of devices
		JsonArray sourceDevices = sourceProfile.getDevicesAsJsonArray();
		JsonArray targetDevices = targetProfile.getDevicesAsJsonArray();

		// Merge profile objects
		mergeProfileObjects(null, null, sourceProfile.getJsonObject(), targetProfile.getJsonObject());

		// Set handicap to highest
		targetProfile.setHandicap(newHandicap);
		// Merge devices
		if (mergeType == MergeType.MERGE) {
			mergeDevices(sourceDevices, targetDevices);
		}

		return new Profile(targetProfile.getJsonObject());
	}
	
	private void mergeDevices(JsonArray sourceDevices, JsonArray targetDevices) {
		List<JsonElement> devicesToAdd = new ArrayList<>(sourceDevices.size());
		sourceDevices.forEach(sourceDevice -> {
			boolean exists = false;
			for (JsonElement targetDevice : targetDevices) {
				String sourceUUID = sourceDevice.isJsonObject() && sourceDevice.getAsJsonObject().get(Profile.DEVICE_UUID_PROPERTY_NAME) != null
						? sourceDevice.getAsJsonObject().get(Profile.DEVICE_UUID_PROPERTY_NAME).getAsString() : null;
				String targetUUID = targetDevice.isJsonObject() && targetDevice.getAsJsonObject().get(Profile.DEVICE_UUID_PROPERTY_NAME) != null
						? targetDevice.getAsJsonObject().get(Profile.DEVICE_UUID_PROPERTY_NAME).getAsString() : null;
				if (StringUtils.equals(sourceUUID, targetUUID)) {
					exists = true;
					break;
				}
			}
			if (! exists) {
				devicesToAdd.add(sourceDevice);
			}
		});
		devicesToAdd.forEach(targetDevices::add);
	}

	protected void mergeProfileObjects(JsonElement parent, String propertyName, JsonObject sourcePropertyObject,
			JsonObject targetPropertyObject) {
		LOGGER.debug("mergeProfileObjects source {} ", sourcePropertyObject);
		if (targetPropertyObject != null) {
			for (Map.Entry<String, JsonElement> sourceProperty : sourcePropertyObject.entrySet()) {
				mergeObjectProperties(sourceProperty.getKey(), sourceProperty.getValue(), targetPropertyObject);
			}
		} else if (parent != null) {
			ArrayMergePolicy policy = getArrayMergePolicy(propertyName);
			if (policy != DONT_UPDATE) {
				jsonUtil.addToParent(parent, propertyName, sourcePropertyObject, policy != OVERWRITE_IF_NULL);
			}
		}
	}

	/**
	 *
	 * Merge source property into target property object
	 *
	 * @param propertyName             - property name
	 * @param sourcePropertyValue      - source property value
	 * @param targetPropertyObject     - target property object
	 *
	 */
	private void mergeObjectProperties(final String propertyName, JsonElement sourcePropertyValue,
			JsonObject targetPropertyObject) {
		final JsonElement targetPropertyValue = targetPropertyObject.get(propertyName);

		if (sourcePropertyValue != null && !sourcePropertyValue.isJsonNull()) {
			if (sourcePropertyValue.isJsonObject()) {
				mergeProfileObjects(targetPropertyObject, propertyName, sourcePropertyValue.getAsJsonObject(),
						targetPropertyValue != null ? targetPropertyValue.getAsJsonObject() : null);
			} else if (sourcePropertyValue.isJsonArray()) {
				mergeProfileArrays(targetPropertyObject, propertyName, sourcePropertyValue.getAsJsonArray(),
						targetPropertyValue != null ? targetPropertyValue.getAsJsonArray() : null);
			} else {
				mergeProfilePrimitives(targetPropertyObject, propertyName,
						sourcePropertyValue.getAsJsonPrimitive());
			}
		}
	}

	protected void mergeProfileArrays(JsonElement parent, String propertyName, JsonArray sourcePropertyArray,
			JsonArray targetPropertyArray) {
		ArrayMergePolicy policy = getArrayMergePolicy(propertyName);

		if (targetPropertyArray != null) {
			if (policy == null) {
				throw new F4MFatalErrorException("No arrayMergePolicy for property " + propertyName);
			}

			switch (policy) {
			case OVERWRITE:
				jsonUtil.addToParent(parent, propertyName, sourcePropertyArray, true);
				break;
			case OVERWRITE_IF_NULL:
				jsonUtil.addToParent(parent, propertyName, sourcePropertyArray, false);
				break;
			case UNION:
				sourcePropertyArray.forEach(e -> mergeArrayPropertiesUnion(e, targetPropertyArray));
				break;
			case MERGE_BY_ID:
				sourcePropertyArray.forEach(e -> mergeArrayPropertiesMergeById(propertyName, e, targetPropertyArray));
				break;
			case DONT_UPDATE:
				//allow insert, don't allow update
				break;
			default:
				assert false : "No arrayMergePolicy for property " + propertyName;
			}
		} else if (propertyName != null) {
			jsonUtil.addToParent(parent, propertyName, sourcePropertyArray, policy != OVERWRITE_IF_NULL);
		}
	}

	private void mergeArrayPropertiesMergeById(String propertyName, JsonElement sourceArrayElement, JsonArray targetPropertyArray) {
		String identifierProperty = getArrayMergeIdentifier(propertyName);
		Integer targetArrayIndex = indexOfJsonArrayElement(targetPropertyArray, sourceArrayElement.getAsJsonObject(), identifierProperty);
		if (targetArrayIndex == null) {
			addToJsonArray(sourceArrayElement, targetPropertyArray);
		} else if (sourceArrayElement.isJsonObject()) {
			mergeProfileObjects(null, null, sourceArrayElement.getAsJsonObject(),
					targetPropertyArray.get(targetArrayIndex).getAsJsonObject());
		} else {
			assert false : "Incorrect arrayMergePolicies - MERGE_BY_ID is supported only for JSON objects";
		}
	}

	private void mergeArrayPropertiesUnion(JsonElement sourceArrayElement, JsonArray targetPropertyArray) {
		Integer targetArrayIndex = jsonUtil.indexOfJsonArrayElement(targetPropertyArray, sourceArrayElement);
		if (targetArrayIndex == null) {
			addToJsonArray(sourceArrayElement, targetPropertyArray);
		}
	}

	private void addToJsonArray(JsonElement sourceArrayElement, JsonArray targetPropertyArray) {
		if (sourceArrayElement.isJsonObject()) {
			mergeProfileObjects(targetPropertyArray, null, sourceArrayElement.getAsJsonObject(), null);
		} else if (sourceArrayElement.isJsonPrimitive()) {
			mergeProfilePrimitives(targetPropertyArray, null, sourceArrayElement.getAsJsonPrimitive());
		}
	}

	private Integer indexOfJsonArrayElement(JsonArray searchArray, JsonObject value, String identifierProperty) {
		Integer index = null;
		if (searchArray != null) {
			for (int i = 0; i < searchArray.size(); i++) {
				//move method to JsonMessageUtil next to indexOfJsonArrayElement? No - too closely bound with assumptions about Profile structure.
				JsonPrimitive primitiveValue = getPrimitiveValue(value, identifierProperty);
				if (primitiveValue != null
						&& primitiveValue.equals(getPrimitiveValue(searchArray.get(i), identifierProperty))) {
					index = i;
					break;
				}
			}
		}
		return index;
	}

	private JsonPrimitive getPrimitiveValue(JsonElement value, String identifierProperty) {
		JsonElement element = value.getAsJsonObject().get(identifierProperty);
		if (element != null) {
			return element.getAsJsonPrimitive();
		} else {
			return null;
		}
	}

	protected void mergeProfilePrimitives(JsonElement parent, String propertyName, JsonPrimitive sourcePropertyPrimitive) {
		ArrayMergePolicy policy = getArrayMergePolicy(propertyName);
		if (policy != DONT_UPDATE) {
			if (propertyName != null) {
				jsonUtil.addToParent(parent, propertyName, sourcePropertyPrimitive, policy != OVERWRITE_IF_NULL);
			} else {
				jsonUtil.addToParent(parent, null, sourcePropertyPrimitive, policy != OVERWRITE_IF_NULL);
			}
		}
	}

	protected ArrayMergePolicy getArrayMergePolicy(String propertyName) {
		return arrayMergePolicies.get(propertyName);
	}
	
	protected String getArrayMergeIdentifier(String propertyName) {
		return propertyIdentifiers.get(propertyName);
	}
}
