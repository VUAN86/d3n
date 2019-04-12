package de.ascendro.f4m.service.profile.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import javax.inject.Inject;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.ResultCode;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.util.ProfilePrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.exception.auth.F4MInsufficientRightsException;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.profile.config.ProfileConfig;
import de.ascendro.f4m.service.profile.dao.ProfileAerospikeDao;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileIdentifierType;
import de.ascendro.f4m.service.profile.model.ProfileStats;
import de.ascendro.f4m.service.profile.model.ProfileUser;
import de.ascendro.f4m.service.profile.model.delete.DeleteProfileEvent;
import de.ascendro.f4m.service.profile.model.find.FindListParameter;
import de.ascendro.f4m.service.profile.model.merge.MergeProfileRequest;
import de.ascendro.f4m.service.profile.model.merge.ProfileMergeEvent;
import de.ascendro.f4m.service.profile.util.ProfileMerger.MergeType;
import de.ascendro.f4m.service.util.EventServiceClient;

public class ProfileUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProfileUtil.class);

	public static final String PROFILE_DELETE_EVENT_TOPIC = "profile/delete-user";
	protected final Config config;
	
	private final ProfileAerospikeDao profileAerospikeDao;
	private final EventServiceClient eventServiceClient;
	private final JsonMessageUtil jsonUtil;
	private final ProfilePrimaryKeyUtil profilePrimaryKeyUtil;
	private final  CommonProfileAerospikeDao commonProfileAerospikeDao;

	@Inject
	public ProfileUtil(Config config,ProfileAerospikeDao profileAerospikeDao,
					   EventServiceClient eventServiceClient, JsonMessageUtil jsonUtil, ProfilePrimaryKeyUtil profilePrimaryKeyUtil,
					   CommonProfileAerospikeDao commonProfileAerospikeDao) {
		
		this.config = config;
		this.profileAerospikeDao = profileAerospikeDao;
		this.eventServiceClient = eventServiceClient;
		this.jsonUtil = jsonUtil;
		this.profilePrimaryKeyUtil = profilePrimaryKeyUtil;
		this.commonProfileAerospikeDao = commonProfileAerospikeDao;
		
	}

	public void deleteProfile(String userId) {
		profileAerospikeDao.deleteProfile(userId);
		publishProfileDeleteEvent(userId);
	}

	public void publishProfileDeleteEvent(String userId) {
		final JsonElement deleteProfileEventContent = jsonUtil.toJsonElement(new DeleteProfileEvent(userId));
		eventServiceClient.publish(PROFILE_DELETE_EVENT_TOPIC, deleteProfileEventContent);
	}

	public void publishProfileMergeEvent(MergeProfileRequest mergeProfileRequest) {
		eventServiceClient.publish(ProfileMergeEvent.PROFILE_MERGE_EVENT_TOPIC, true, new ProfileMergeEvent(mergeProfileRequest).getJsonObject());
	}

	@SuppressWarnings("unchecked")
	public String createProfile(final String countryCode, final Pair<ProfileIdentifierType, String>... profileIdentifiers) {
		return createNewProfileByIdentifiers(countryCode, profileIdentifiers);
	}

	@SuppressWarnings("unchecked")
	private String createNewProfileByIdentifiers(final String countryCode, final Pair<ProfileIdentifierType, String>... profileIdentifiers) {
		String userId;
		boolean created = false;
		int i = 0;
		do {
			userId = profilePrimaryKeyUtil.generateId();
			final Profile profile = Profile.create(userId, countryCode, profileIdentifiers);
			try {
				profileAerospikeDao.createProfile(userId, profile);
				created = true;
			} catch (AerospikeException ae) {
				i++;
				if (ae.getResultCode() != ResultCode.KEY_EXISTS_ERROR) {
					throw ae;
				} else {
					if (i >= 5) {
						throw ae;
					} else {
						LOGGER.warn(
								"Failed to create profile as record with attempted primary key already exists: attempt"
										+ (i + 1), ae);
					}
				}
			}
		} while (!created);

		return userId;
	}

	public Pair<ProfileUpdateInfo, Profile> updateProfile(String userId, Profile profile, boolean verifyIfGlobalAttributesCanBeChanged, boolean verifyIfUserAttributesCanBeChanged,
														  boolean updateVerifiedData, boolean isServiceResultEngineOrGamEngine) throws F4MException {
		final Profile actualDbProfile = getActiveProfile(userId);
		if (actualDbProfile != null) {
			if (verifyIfGlobalAttributesCanBeChanged) {
				verifyThatNonUpdateablePropertiesAreNotChanged(actualDbProfile, profile);
			}
			if (verifyIfUserAttributesCanBeChanged) {
				verifyThatNonUpdateablePropertiesByUserAreNotChanged(actualDbProfile, profile);
			}
			if (!updateVerifiedData && actualDbProfile.isPhonesVerified()) {
				profile.removePhones();
			}

			ProfileUpdateInfo result = new ProfileUpdateInfo();
			checkAndGenerateNickname(userId, profile, actualDbProfile);

			Profile updatedProfile = profileAerospikeDao.updateProfile(userId, profile, actualDbProfile, isServiceResultEngineOrGamEngine);
			result.setProfile(updatedProfile);
			if (StringUtils.isEmpty(actualDbProfile.getRecommendedBy())
					&& !StringUtils.isEmpty(profile.getRecommendedBy())) {
				result.setRecommendedByAdded(true);
			}
			return new ImmutablePair<>(result, actualDbProfile);
		} else {
			throw new F4MEntryNotFoundException();
		}
	}
	
	/**
	 * checks if such nickname exists and regenerates if not.
	 * 
	 * @param profile- changed profile delta
	 * @param userId- user Id
	 * @param actualDbProfile- current profile information from database 
	 * @throws F4MFatalErrorException 
	 */
	protected void checkAndGenerateNickname(String userId, Profile profile, Profile actualDbProfile)
			throws F4MFatalErrorException {
		// we need to update only if any person data has changed
		if (profile.getPersonWrapper() != null) {

			String firstName = getProfileUserField(profile,actualDbProfile,a -> a.getFirstName());
			String lastName = getProfileUserField(profile,actualDbProfile,a -> a.getLastName());
			String nickname = getProfileUserField(profile,actualDbProfile,a -> a.getNickname());
			
			if (StringUtils.isEmpty(nickname) &&
					(StringUtils.isNotEmpty(firstName) && StringUtils.isNotEmpty(lastName) )) {
				createNickname(userId, profile, firstName, lastName);
			}
		}
	}

	
	protected String getProfileUserField(Profile profile, Profile actualDbProfile,
			Function<ProfileUser, String> getter) {

		String value = getter.apply(profile.getPersonWrapper());
		if (StringUtils.isEmpty(value) && actualDbProfile.getPersonWrapper() != null) {
			value = getter.apply(actualDbProfile.getPersonWrapper());
		}
		return value;
	}
	
	private void createNickname(String userId, Profile profile, String firstName, String lastName)
			throws F4MFatalErrorException {
		String proposedNickname = profile.getPersonWrapper().getNickname();
		if (profile.getPersonWrapper().getNickname() == null) {
			proposedNickname = generateNickname(firstName, lastName);
		}
		String foundUserId;
		boolean foundNickname = false;

		int tryCount = config.getPropertyAsInteger(ProfileConfig.PROFILE_NICKNAME_GENERATE_TRY_COUNT);

		for (int i = 0; i < tryCount; i++) {
			foundUserId = profileAerospikeDao.findUserIdByIdentifierWithCleanup(ProfileIdentifierType.NICKNAME,
					proposedNickname);

			if (foundUserId != null && (!userId.equals(foundUserId))) {
				proposedNickname = generateNickname(firstName, lastName) + RandomUtils.nextInt(1, 100);
			} else {
				profile.getPersonWrapper().setNickname(proposedNickname);
				foundNickname = true;
				break;
			}
		}

		if (!foundNickname) {
			throw new F4MFatalErrorException("Cannot generate unique nickname");
		}
	}

	public static String generateNickname(String firstName, String lastName) {
		return StringUtils.removeAll(firstName, "\\s") + StringUtils.removeAll(lastName, "\\s");
	}

	public Profile mergeProfiles(Profile sourceProfile, Profile targetProfile) {
		ProfileMerger profileMerger = new ProfileMerger(jsonUtil, MergeType.MERGE);
		final Profile mergedProfile = profileMerger.mergeProfileObjects(sourceProfile, targetProfile, false);

		final Set<String> targetBlobNames = copyProfileSubBlobs(sourceProfile, targetProfile);
		mergedProfile.setSubBlobNames(targetBlobNames);

		deleteProfile(sourceProfile.getUserId());
		updateProfile(targetProfile.getUserId(), mergedProfile, false, false, true, false);

		if (sourceProfile.getTenants()!=null) {
			sourceProfile.getTenants().forEach(tenantId -> {
				ProfileStats sourceProfileStats = getProfileStats(sourceProfile.getUserId(), tenantId);
				profileAerospikeDao.updateStats(targetProfile.getUserId(), tenantId, sourceProfileStats);
			});
		}
		
		profileAerospikeDao.saveProfileMergeHistory(sourceProfile.getUserId(), targetProfile.getUserId());
		
		return mergedProfile;
	}

	private Set<String> copyProfileSubBlobs(Profile sourceProfile, Profile targetProfile) {
		final Set<String> sourceBlobNames = sourceProfile.getSubBlobNames();

		final Set<String> mergedBlobNames;
		if (sourceBlobNames != null && !sourceBlobNames.isEmpty()) {
			mergedBlobNames = new HashSet<>(sourceBlobNames);

			final Set<String> targetBlobNames = targetProfile.getSubBlobNames();
			if (targetBlobNames != null) {
				mergedBlobNames.addAll(targetProfile.getSubBlobNames());
			}
			final String sourceUserId = sourceProfile.getUserId();
			final String targetUserId = targetProfile.getUserId();

			for (String blobName : sourceBlobNames) {
				final JsonElement sourceBlobValue = getProfileBlob(sourceUserId, blobName);
				final JsonElement targetBlobValue = getProfileBlob(targetUserId, blobName);
				if (sourceBlobValue != null && !sourceBlobValue.isJsonNull()) {
					mergePorfileSubBlobs(mergedBlobNames, targetUserId, blobName, sourceBlobValue, targetBlobValue);
				}
			}
		} else {
			mergedBlobNames = null;
		}

		return mergedBlobNames;
	}

	/**
	 * Merge two profile two sub records (blobs).
	 * 
	 * @param mergedBlobNames
	 *            - set of both profile sub record (blob) names set(sourceProfile.blobs.name + targetProfile.blobs.name)
	 * @param targetUserId
	 *            - target profile user id
	 * @param blobName
	 *            - sub record(blob) name within both profiles
	 * @param sourceBlobValue
	 *            - source profile blob value (not null)
	 * @param targetBlobValue
	 *            - target profile blob value (null if not present)
	 */
	private void mergePorfileSubBlobs(final Set<String> mergedBlobNames, final String targetUserId, String blobName,
			final JsonElement sourceBlobValue, final JsonElement targetBlobValue) {
		if (sourceBlobValue.isJsonObject() && !sourceBlobValue.getAsJsonObject().entrySet().isEmpty()) {
			updateProfileBlob(targetUserId, blobName, sourceBlobValue.toString());
		} else if (sourceBlobValue.isJsonArray() && sourceBlobValue.getAsJsonArray().size() > 0) {
			if (targetBlobValue != null && targetBlobValue.isJsonArray() && targetBlobValue.getAsJsonArray().size() > 0) {
				final JsonArray targetJsonArray = targetBlobValue.getAsJsonArray();
				sourceBlobValue.getAsJsonArray().forEach(s -> {
					if (!targetJsonArray.contains(s)) {
						targetJsonArray.add(s);
					}
				});
			}
			updateProfileBlob(targetUserId, blobName, targetBlobValue != null ? targetBlobValue.toString()
					: sourceBlobValue.toString());
		} else {
			mergedBlobNames.remove(blobName);
		}

	}
	
	protected void verifyThatNonUpdateablePropertiesAreNotChanged(Profile actualDbProfile, Profile changedProfile) {
		//currently all NEVER_UPDATABLE_PROPERTIES are in root.
		//No advanced comparison is implemented, since it is unclear, how subattributes may be specified in list (and the comparison is more complicated than implementing directly).
		for (int i = 0; i < Profile.NEVER_UPDATABLE_PROPERTIES.length; i++) {
			String property = Profile.NEVER_UPDATABLE_PROPERTIES[i];
			JsonElement deltaElement = changedProfile.getJsonObject().get(property);
			if (deltaElement != null && !deltaElement.isJsonNull() 
					//hack/shortcut for testing - property can be empty (means - no change) or the same value (means - don't change) 
					&& !areJsonValuesEqual(actualDbProfile.getJsonObject(), changedProfile.getJsonObject(), property)) {
				throw new F4MValidationFailedException("Property " + property + " cannot be updated");
			}
		}
	}
	
	protected void verifyThatNonUpdateablePropertiesByUserAreNotChanged(Profile actualDbProfile, Profile changedProfile) {
		//currently all NEVER_UPDATABLE_PROPERTIES are in root.
		//No advanced comparison is implemented, since it is unclear, how subattributes may be specified in list (and the comparison is more complicated than implementing directly).
		verifyJsonArraysEqualOrNotSpecified(actualDbProfile, changedProfile, Profile.ROLES_PROPERTY);
		verifyJsonArraysEqualOrNotSpecified(actualDbProfile, changedProfile, Profile.APPLICATIONS_PROPERTY_NAME);
		verifyJsonArraysEqualOrNotSpecified(actualDbProfile, changedProfile, Profile.TENANTS_PROPERTY_NAME);
		verifyJsonArraysEqualOrNotSpecified(actualDbProfile, changedProfile, Profile.PHONES_PROPERTY);
		verifyJsonArraysEqualOrNotSpecified(actualDbProfile, changedProfile, Profile.DEVICES_PROPERTY_NAME);
	}

	private boolean areJsonValuesEqual(JsonObject actual, JsonObject changed, String property) {
		JsonElement originalValue = actual.get(property);
		JsonElement changedValue = changed.get(property);
		return Objects.equals(originalValue, changedValue);
	}

	private void verifyJsonArraysEqualOrNotSpecified(Profile actualDbProfile, Profile changedProfile, String property) {
		if (changedProfile.getJsonObject().has(property) && !actualDbProfile.getJsonObject().get(property).equals(changedProfile.getJsonObject().get(property))) {
			throw new F4MValidationFailedException("Property " + property + " cannot be updated");
		}
	}
	
	public String findByIdentifier(ProfileIdentifierType profileIdType, String identifier) throws F4MException {
		return profileAerospikeDao.findUserIdByIdentifierWithCleanup(profileIdType, identifier);
	}

	public List<JsonObject> findListByIdentifiers(List<FindListParameter> identifiers) {
		Map<String, JsonObject> profilesMap = new LinkedHashMap<>();
		identifiers.forEach(it -> {
			Profile profile = profileAerospikeDao.findByIdentifierWithCleanup(it.getIdentifierType(), it.getIdentifier());
			if (profile != null) {
				profilesMap.put(profile.getUserId(),profile.getJsonObject());
			}
		});
		return new ArrayList<>(profilesMap.values());
	}

	public Profile getActiveProfile(String userId) {
		return profileAerospikeDao.getActiveProfile(userId);
	}

	public Profile getProfile(String userId) {
		return profileAerospikeDao.getProfile(userId);
	}

	public ProfileStats getProfileStats(String userId, String tenantId) {
		return commonProfileAerospikeDao.getStatsFromBlob(userId, tenantId);
	}
	
	public List<JsonObject> getProfileListByIds(List<String> profilesIds) {
		Map<String, JsonObject> profilesMap = new LinkedHashMap<>();
		profilesIds.forEach(profileId -> {
			Profile profile = profileAerospikeDao.getActiveProfile(profileId);
			if (profile != null) {
				profilesMap.put(profile.getUserId(),profile.getJsonObject());
			}
		});
		return new ArrayList<>(profilesMap.values());
	}

	/**
	 * Update user profile devices information
	 * 
	 * @param userId
	 *            - user profile id
	 * @param deviceUUID
	 *            - User device UUID
	 * @param deviceProperties
	 *            - User device properties
	 * @param oneSignalDeviceId
	 * @return added tenantIds if any
	 * @throws F4MException
	 */
	public List<String> updateProfileStatistics(String userId, String deviceUUID, JsonElement deviceProperties, String appId,
			String tenantId, String IMEI, String oneSignalDeviceId) throws F4MInsufficientRightsException {
		final Profile profile = getActiveProfile(userId);
		boolean changed = false;
		List<String> newTenantIds = new ArrayList<>(1);
		if (profile != null) {
			String oneSignalDeviceIdUserId = findByIdentifier(ProfileIdentifierType.ONE_SIGNAL_ID, oneSignalDeviceId);

			if(oneSignalDeviceIdUserId != null && !userId.equals(oneSignalDeviceIdUserId)) {
				// @TODO : device handover workflow #12449
			}

			final Profile profileFromDb = new Profile(JsonUtil.deepCopy(profile.getJsonObject()));
			if ((deviceUUID != null || IMEI != null) && deviceProperties != null) {
				changed = profile.mergeDeviceIntoDevicesArray(deviceUUID, deviceProperties, IMEI, oneSignalDeviceId) || changed;
			}

			if (appId != null) {
				changed = profile.addApplication(appId) || changed;
			}

			if (tenantId != null) {
				boolean newTenant = profile.addTenant(tenantId);
				if (newTenant) {
					changed = true;
					newTenantIds.add(tenantId);
				}
			}

			if (changed) {
				profileAerospikeDao.overwriteProfile(userId, profile, profileFromDb);
			}
			return newTenantIds;
		} else {
			throw new F4MInsufficientRightsException("User profile is not found");
		}
	}

	public JsonElement getProfileBlob(String userId, String blobName) {
		final String blobAsString = profileAerospikeDao.getSubBlob(userId, blobName);
		final JsonElement blobElement;
		if (blobAsString != null) {
			blobElement = jsonUtil.fromJson(blobAsString, JsonElement.class);
		} else {
			blobElement = new JsonObject();
		}
		return blobElement;
	}

	public JsonElement updateProfileBlob(String userId, String blobName, String blobAsString) {
		profileAerospikeDao.updateSubBlob(userId, blobName, blobAsString);
		return jsonUtil.fromJson(blobAsString, JsonElement.class);
	}
	
	public static class ProfileUpdateInfo {
		private Profile profile;
		private boolean recommendedByAdded;

		public Profile getProfile() {
			return profile;
		}

		public void setProfile(Profile profile) {
			this.profile = profile;
		}

		public boolean isRecommendedByAdded() {
			return recommendedByAdded;
		}

		public void setRecommendedByAdded(boolean recommendedByAdded) {
			this.recommendedByAdded = recommendedByAdded;
		}
	}

	public int queueResync(String userId) {
		return profileAerospikeDao.queueResync(userId);
	}

}
