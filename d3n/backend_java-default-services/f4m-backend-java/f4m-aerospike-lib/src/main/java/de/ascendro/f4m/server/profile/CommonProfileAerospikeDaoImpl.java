package de.ascendro.f4m.server.profile;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeOperateDaoImpl;
import de.ascendro.f4m.server.UpdateCall;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.profile.util.ProfilePrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileIdentifierType;
import de.ascendro.f4m.service.profile.model.ProfileStats;
import de.ascendro.f4m.service.profile.model.api.ApiProfileBasicInfo;
import de.ascendro.f4m.service.profile.model.api.ApiProfileExtendedBasicInfo;
import de.ascendro.f4m.service.voucher.model.UserVoucherReferenceEntry;

public class CommonProfileAerospikeDaoImpl extends AerospikeOperateDaoImpl<ProfilePrimaryKeyUtil> implements CommonProfileAerospikeDao {

	private static final Logger LOGGER = LoggerFactory.getLogger(CommonProfileAerospikeDaoImpl.class);
	private static final int MERGED_PROFILE_MAX_LOOKUP_DEPTH = 10; //naive approach to avoid infinite cycles
	private static final Type MAP_STRING_TO_LIST_OF_STRING_TYPE = new TypeToken<Map<String, List<String>>>(){}.getType();

	@Inject
	public CommonProfileAerospikeDaoImpl(Config config, ProfilePrimaryKeyUtil profilePrimaryKeyUtil,
			AerospikeClientProvider aerospikeClientProvider, JsonUtil jsonUtil) {
		super(config, profilePrimaryKeyUtil, jsonUtil, aerospikeClientProvider);
	}
	
	@Override
	public Profile getProfile(String userId) {
		String profileJson = getProfileOrMergedSource(userId, MERGED_PROFILE_MAX_LOOKUP_DEPTH);
		return jsonUtil.toJsonObjectWrapper(profileJson, Profile::new);
	}
	
	@Override
	public Profile getActiveProfile(String userId) {
		String profileJson = getProfileOrMergedSource(userId, 0);
		return jsonUtil.toJsonObjectWrapper(profileJson, Profile::new);
	}
	
	private String getProfileOrMergedSource(String userId, int lookupDepth) {
		final String key = primaryKeyUtil.createPrimaryKey(userId);
		final String profileJson = readJson(getSet(), key, BLOB_BIN_NAME);
		if (profileJson == null && lookupDepth > 0) {
			String mergedProfileId = getMergedProfileId(userId);
			if (mergedProfileId != null) {
				return getProfileOrMergedSource(mergedProfileId, lookupDepth - 1);
			}
		}
		return profileJson;
	}
	
	private String getMergedProfileId(String sourceUserId) {
		final String key = primaryKeyUtil.createPrimaryKey(sourceUserId);
		return readString(getMergedProfilesSet(), key, BLOB_BIN_NAME);
	}

	@Override
	public boolean exists(String userId) {
		final String key = primaryKeyUtil.createPrimaryKey(userId);
		return exists(getSet(), key);
	}

	@Override
	public List<Profile> getProfiles(List<String> usersIds) {
		Function<String, Profile> stringToJsonFunc = s -> jsonUtil.toJsonObjectWrapper(s, Profile::new);
		return getJsonObjects(getSet(), BLOB_BIN_NAME, usersIds, primaryKeyUtil::createPrimaryKey, stringToJsonFunc, false);
	}
	
	@Override
	public ApiProfileBasicInfo getProfileBasicInfo(String userId) {
		Map<String, ApiProfileBasicInfo> infoMap = getProfileBasicInfo(Arrays.asList(userId));
		return Optional.ofNullable(infoMap.get(userId))
				.orElse(new ApiProfileBasicInfo());
	}

	@Override
	public ApiProfileExtendedBasicInfo getProfileExtendedBasicInfo(String userId) {
		Map<String, ApiProfileExtendedBasicInfo> infoMap = getProfileExtendedBasicInfo(Arrays.asList(userId));
		return Optional.ofNullable(infoMap.get(userId))
				.orElse(new ApiProfileExtendedBasicInfo());
	}

	@Override
	public Map<String, ApiProfileBasicInfo> getProfileBasicInfo(List<String> userIds) {
		return getProfiles(userIds).stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(Profile::getUserId, ApiProfileBasicInfo::new));
	}

	@Override
	public Map<String, ApiProfileExtendedBasicInfo> getProfileExtendedBasicInfo(List<String> userIds) {
		return getProfiles(userIds).stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(Profile::getUserId, ApiProfileExtendedBasicInfo::new));
	}

	@Override
	public Profile findByIdentifierWithCleanup(ProfileIdentifierType identifierType, String identifier) throws F4MException {
		final String userId = findUserIdByIdentifierWithCleanup(identifierType, identifier);
		return userId == null ? null : getActiveProfile(userId);
	}

	@Override
	public String findUserIdByIdentifierWithCleanup(ProfileIdentifierType identifierType, String identifier) throws F4MException {
		String userId = findUserIdByIdentifier(identifierType, identifier);
		if (StringUtils.isNotEmpty(userId) && !exists(userId)) {
			deleteIdentifierSilently(identifierType, identifier);
			userId = null;
		}
		return userId;
	}

	public void deleteIdentifierSilently(ProfileIdentifierType identifierType, String identifier) {
		deleteSilently(getSet(), primaryKeyUtil.createPrimaryKey(identifierType, identifier));
	}
	
	public String findUserIdByIdentifier(ProfileIdentifierType identifierType, String identifier) {
		final String profileKey = readString(getSet(),
				primaryKeyUtil.createPrimaryKey(identifierType, identifier), PROFILE_ID_BIN_NAME);
		if (profileKey == null) {
			return null;
		} else {
			final String userId = primaryKeyUtil.parseId(profileKey);
			if (userId != null) {
				return userId;
			} else {
				throw new F4MFatalErrorException("Cannot parse user id from profile primary key");
			}
		}
	}

	@Override
	public String getSubBlob(String userId, String subRecordName) {
		final String subRecordKey = primaryKeyUtil.createSubRecordKeyByUserId(userId, subRecordName);
		return readJson(getSet(), subRecordKey, BLOB_BIN_NAME);
	}

	@Override
	public String getSubBlobMapEntry(String userId, String subRecordName, String mapKey) {
		final String subRecordKey = primaryKeyUtil.createSubRecordKeyByUserId(userId, subRecordName);
		return getByKeyFromMap(getSet(), subRecordKey, BLOB_BIN_NAME, mapKey);
	}


	@Override
	public void deleteSubBlob(String userId, String subRecordName) {
		final String subRecordKey = primaryKeyUtil.createSubRecordKeyByUserId(userId, subRecordName);
		deleteSilently(getSet(), subRecordKey);
	}

	@Override
	public JsonArray getVoucherReferencesArrayFromBlob(String userId) {
		String voucherReferencesString = getSubBlob(userId, USER_VOUCHER_BLOB_NAME);
		return getReferenceListFromString(voucherReferencesString);
	}

	@Override
	public String getLastLoginTimestampFromBlob(String userId, String appId) {
		return getSubBlobMapEntry(userId, USER_LOGIN_TIMESTAMP_BLOB_NAME, appId);
	}

	@Override
	public void setLastLoginTimestampInBlob(String userId, String appId, String timestamp) {
		updateSubBlobMapEntry(userId, USER_LOGIN_TIMESTAMP_BLOB_NAME, appId, timestamp);
	}

	@Override
	public ProfileStats getStatsFromBlob(String userId, String tenantId) {
		String statsString = getSubBlob(userId, getSubRecordName(tenantId, USER_STATS_BLOB_NAME));
		return getStatsFromString(statsString);
	}

	protected ProfileStats getStatsFromString(String statsString) {
		final ProfileStats profileStats;
		if (statsString != null) {
			profileStats = jsonUtil.fromJson(statsString, ProfileStats.class);
		} else {
			profileStats = new ProfileStats();
		}
		return profileStats;
	}

	protected JsonArray getReferenceListFromString(String referencesString) {
		JsonArray references;
		if (referencesString != null) {
			try {
				references = jsonUtil.fromJson(referencesString, JsonArray.class);
			} catch (JsonSyntaxException e) {
				if (StringUtils.deleteWhitespace(referencesString).equals("{}")) {
					// This case occurred in real environment, found "{}" in Aerospike
					LOGGER.warn("Syntax Exception when reading user voucher reference array, expected a JsonArray " +
							"but got an empty json object. Deleting current value ({}) and re-initializing array",
							referencesString);
					references = new JsonArray();
				} else {
					LOGGER.warn("Syntax Exception when reading user voucher reference array: {} ", referencesString);
					throw e;
				}
			}

		} else {
			references = new JsonArray();
		}
		return references;
	}

	protected Map<String, List<String>> getOpponentsMapFromString(String mapString) {
		Map<String, List<String>> mapResult;
		if (StringUtils.isNotBlank(mapString)) {
			try {
				mapResult = jsonUtil.fromJson(mapString, MAP_STRING_TO_LIST_OF_STRING_TYPE);
			} catch (JsonSyntaxException e) {
				LOGGER.warn("Syntax Exception when reading opponents map from aerospike, " +
								"expected a map of string -> List of strings but got an invalid object. " +
								"Deleting current value ({}) and re-initializing array",
						mapString);
				mapResult = new HashMap<>();
			}
		} else {
			mapResult = new HashMap<>();
		}
		return mapResult;
	}

	@Override
	public String getSubRecordName(String tenantId, String blobName) {
		return primaryKeyUtil.createSubRecordPrimaryKey(tenantId, blobName);
	}

	@Override
	public void updateSubBlob(String userId, String subRecordName, String blobValue) {
		updateSubBlob(userId, subRecordName, (readResult, writePolicy) ->
            blobValue //FIXME: potential sub blob data loss as blob operations are performed within separate service
        );
	}

	@Override
	public void updateSubBlobMapEntry(String userId, String subRecordName, String mapKey, String blobValue) {
		updateSubBlobMap(userId, subRecordName, mapKey, (readResult, writePolicy) ->
				blobValue //FIXME: potential sub blob data loss as blob operations are performed within separate service
		);
	}

	private String updateSubBlob(String userId, String blobName, UpdateCall<String> updateCall) {
		final String subRecordKey = primaryKeyUtil.createSubRecordKeyByUserId(userId, blobName);

		String originalSubRecordValue = getSubBlob(userId, blobName);
		if (originalSubRecordValue == null) {
			addSubBlobName(userId, blobName);
		}
		return createOrUpdateJson(getSet(), subRecordKey, BLOB_BIN_NAME, updateCall);
	}

	private String updateSubBlobMap(String userId, String blobName, String mapKey, UpdateCall<String> updateCall) {
		final String subRecordKey = primaryKeyUtil.createSubRecordKeyByUserId(userId, blobName);

		Integer originalSubRecordSize = getMapSize(getSet(), subRecordKey, BLOB_BIN_NAME);;
		if (originalSubRecordSize == null || originalSubRecordSize == 0) {
			addSubBlobName(userId, blobName);
		}
		return createOrUpdateMapValueByKey(getSet(), subRecordKey, BLOB_BIN_NAME, mapKey, updateCall);
	}

	private void addSubBlobName(String userId, String subRecordName) {
		updateJson(getSet(), primaryKeyUtil.createPrimaryKey(userId), BLOB_BIN_NAME, (originalProfileAsString, writePolicy) -> {
			if (originalProfileAsString != null) {
				final JsonElement originalProfileElement = jsonUtil.fromJson(originalProfileAsString, JsonElement.class);
				final Profile originalProfile = new Profile(originalProfileElement);
				originalProfile.addSubBlobName(subRecordName);
				return originalProfile.getAsString();
			} else {
				throw new F4MEntryNotFoundException();
			}
		});
	}

	@Override
	public void vouchersEntryAdd(String userId, String voucherId, String userVoucherId) {
		updateSubBlob(userId, USER_VOUCHER_BLOB_NAME,
				(readResult, writePolicy) -> {
					JsonArray voucherReferences = getReferenceListFromString(readResult);
					addVoucherReferenceToArray(voucherReferences, voucherId, userVoucherId);
					return jsonUtil.toJson(voucherReferences);
				});
	}

	@Override
	public void setLastInvitedDuelOponents(String inviterId, String appId, List<String> invitedUsers) {
		updateSubBlob(inviterId, LAST_INVITED_DUEL_OPPONENTS_BLOB_NAME,
				(readResult, writePolicy) -> setOpponentsForApp(appId, invitedUsers, readResult));
	}

	@Override
	public void removeUsersFromLastInvitedDuelOponents(String ownerId, List<String> userIds) {
		if (userIds != null && !userIds.isEmpty()) {
			updateSubBlob(ownerId, LAST_INVITED_DUEL_OPPONENTS_BLOB_NAME,
					(readResult, writePolicy) -> removeOpponentsFromAllApps(userIds, readResult));
		}
	}

	@Override
	public List<String> getLastInvitedDuelOpponents(String userId, String appId) {
		String lastInvitedOpponentsMapString = getSubBlob(userId, LAST_INVITED_DUEL_OPPONENTS_BLOB_NAME);
		return Optional.ofNullable(
				getOpponentsMapFromString(lastInvitedOpponentsMapString)
						.get(appId)
		).orElse(Collections.emptyList());
	}

	@Override
	public void setPausedDuelOponents(String ownerId, String appId, List<String> pausedUsers) {
		updateSubBlob(ownerId, PAUSED_DUEL_OPPONENTS_BLOB_NAME,
				(readResult, writePolicy) -> setOpponentsForApp(appId, pausedUsers, readResult));
	}

	@Override
	public void removeUsersFromPausedDuelOponents(String ownerId, List<String> userIds) {
		if (userIds != null && !userIds.isEmpty()) {
			updateSubBlob(ownerId, PAUSED_DUEL_OPPONENTS_BLOB_NAME,
					(readResult, writePolicy) -> removeOpponentsFromAllApps(userIds, readResult));
		}
	}

	@Override
	public List<String> getPausedDuelOpponents(String userId, String appId) {
		String pausedOpponentsMapString = getSubBlob(userId, PAUSED_DUEL_OPPONENTS_BLOB_NAME);
		return Optional.ofNullable(
				getOpponentsMapFromString(pausedOpponentsMapString)
						.get(appId)
		).orElse(Collections.emptyList());
	}

	@Override
	public void updateStats(String userId, String tenantId,  ProfileStats profileStats) {
		updateSubBlob(userId, getSubRecordName(tenantId, USER_STATS_BLOB_NAME),
				(readResult, writePolicy) -> {
					ProfileStats oldProfileStats = getStatsFromString(readResult);
					oldProfileStats.add(profileStats);
					return jsonUtil.toJson(oldProfileStats);
				});
	}

	private String setOpponentsForApp(String appId, List<String> opponents, String opponentsMapJson) {
		Map<String, List<String>> opponentsMap = getOpponentsMapFromString(opponentsMapJson);
		if (opponents == null || opponents.isEmpty()) {
			opponentsMap.remove(appId);
		} else {
			opponentsMap.put(appId, opponents);
		}
		return jsonUtil.toJson(opponentsMap);
	}

	private String removeOpponentsFromAllApps(List<String> opponents, String opponentsMapJson) {
		Map<String, List<String>> lastInvitedOpponentsMap = getOpponentsMapFromString(opponentsMapJson);
		lastInvitedOpponentsMap.forEach((appId, invitedUsers) -> {
			invitedUsers.removeAll(opponents);
		});
		return jsonUtil.toJson(lastInvitedOpponentsMap);
	}

	private void addVoucherReferenceToArray(JsonArray voucherReferences, String voucherId, String userVoucherId) {
		UserVoucherReferenceEntry userVoucherReferenceEntry = new UserVoucherReferenceEntry();
		userVoucherReferenceEntry.setVoucherId(voucherId);
		userVoucherReferenceEntry.setUserVoucherId(userVoucherId);
		voucherReferences.add(jsonUtil.toJsonElement(userVoucherReferenceEntry));
	}

	protected String getSet() {
		return config.getProperty(AerospikeConfigImpl.AEROSPIKE_PROFILE_SET);
	}

	protected String getMergedProfilesSet() {
		return config.getProperty(AerospikeConfigImpl.AEROSPIKE_MERGED_PROFILE_SET);
	}

}
