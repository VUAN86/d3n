package de.ascendro.f4m.service.profile.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Bin;
import com.aerospike.client.ResultCode;
import com.aerospike.client.query.IndexType;
import com.aerospike.client.task.IndexTask;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.exception.F4MAerospikeResultCodes;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDaoImpl;
import de.ascendro.f4m.server.profile.util.ProfilePrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.client.F4MEntryAlreadyExistsException;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.profile.config.ProfileConfig;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileIdentifierType;
import de.ascendro.f4m.service.profile.util.ProfileMerger;
import de.ascendro.f4m.service.profile.util.ProfileMerger.MergeType;
import de.ascendro.f4m.service.util.random.RandomUtil;

public class ProfileAerospikeDaoImpl extends CommonProfileAerospikeDaoImpl implements ProfileAerospikeDao {
	
	private static final List<ProfileIdentifierType> UPDATABLE_IDENTIFIERS = Arrays.asList(ProfileIdentifierType.NICKNAME, ProfileIdentifierType.ONE_SIGNAL_ID);
	private static final Logger LOGGER = LoggerFactory.getLogger(ProfileAerospikeDaoImpl.class);
	private final RandomUtil randomUtil;
	private final JsonMessageUtil jsonMessageUtil;

	@Inject
	public ProfileAerospikeDaoImpl(Config config, ProfilePrimaryKeyUtil profilePrimaryKeyUtil,
			AerospikeClientProvider aerospikeClientProvider, JsonMessageUtil jsonMessageUtil, JsonUtil jsonUtil,
			RandomUtil randomUtil) {
		super(config, profilePrimaryKeyUtil, aerospikeClientProvider, jsonUtil);
		this.jsonMessageUtil = jsonMessageUtil;
		this.randomUtil = randomUtil;
	}
	
	@PostConstruct
	public void init() {
		try {
			initSyncSecondaryIndex().waitTillComplete();
		} catch (Exception e) {
			LOGGER.error("Error creating profile sync index on service startup", e);
		}
	}

	@Override
	public void createProfile(String userId, Profile profile) {
		List<Pair<ProfileIdentifierType, String>> identifiers = profile.getIdentifiers();
		executeProfileChangeWithIdentifiersCleanup(userId, identifiers, new ArrayList<>(), () -> {
			String primaryKey = primaryKeyUtil.createPrimaryKey(userId);
			createJson(getSet(), primaryKey, BLOB_BIN_NAME, profile.getAsString());
		});
	}

	@Override
	public Profile updateProfile(String userId, final Profile profile, final Profile alreadyExisting, boolean isServiceResultEngineOrGamEngine) {
		return updateProfile(userId, profile, alreadyExisting, false, isServiceResultEngineOrGamEngine);
	}
	
	@Override
	public Profile overwriteProfile(String userId, final Profile profile, final Profile alreadyExisting) {
		return updateProfile(userId, profile, alreadyExisting, true, false);
	}

	private Profile updateProfile(String userId, final Profile profile, final Profile alreadyExisting, boolean overwrite, boolean isServiceResultEngineOrGamEngine) {
		if (alreadyExisting != null) {
			//no need to validate identifiers - createProfileIdentifierTypeKeys will fail, if they already exist in Aerospike
			//no need to delete old identifiers - there is no use-case, when an identifier is deleted from profile. Also ProfileMerger does not support that.
			List<Pair<ProfileIdentifierType, String>> newIdentifiers = findNewIdentifiers(profile, alreadyExisting);
			List<Pair<ProfileIdentifierType, String>> changedIdentifiers = findOldIdentifiers(profile, alreadyExisting);
			// also no need to recover from exception by deleting already created identifiers in this call, since there should not be more than one.
			executeProfileChangeWithIdentifiersCleanup(userId, newIdentifiers, changedIdentifiers, () -> {
				String primaryKey = primaryKeyUtil.createPrimaryKey(userId);
				if (overwrite) {
					updateJson(getSet(), primaryKey, BLOB_BIN_NAME, (res, policy) -> profile.getAsString());
				} else {
					updateJson(getSet(), primaryKey, BLOB_BIN_NAME, (res, policy) -> updateProfileJson(res, profile, isServiceResultEngineOrGamEngine));
				}
			});
			//profile needs to be synchronized
			addProfileToSyncSet(userId);
			return getActiveProfile(userId);
		} else {
			throw new F4MEntryNotFoundException();
		}
	}

	protected void deleteOldIdentifiers(List<Pair<ProfileIdentifierType, String>> deleteIdentifiers,
			List<Pair<ProfileIdentifierType, String>> deletedIdentifiers) {
		for (Pair<ProfileIdentifierType, String> deleteIdentifier : deleteIdentifiers) {
			deleteIdentifierSilently(deleteIdentifier.getKey(), deleteIdentifier.getValue());
			deletedIdentifiers.add(deleteIdentifier);
		}
	}

	@SuppressWarnings("unchecked")
	protected List<Pair<ProfileIdentifierType, String>> findNewIdentifiers(Profile newProfileData, Profile actualProfile) {
		return ListUtils.subtract(newProfileData.getIdentifiers(), actualProfile.getIdentifiers());
	}

	@SuppressWarnings("unchecked")
	protected List<Pair<ProfileIdentifierType, String>> findOldIdentifiers(Profile newProfileData, Profile actualProfile) {
		List<Pair<ProfileIdentifierType, String>> oldIdentifiers = new ArrayList<>();
		newProfileData.getIdentifiers().forEach(newIdentifier -> {
			if (UPDATABLE_IDENTIFIERS.contains(newIdentifier.getKey())) {
				oldIdentifiers.addAll(actualProfile.getIdentifiersByType(newIdentifier.getKey()));
			}
		});
		return ListUtils.subtract(oldIdentifiers, newProfileData.getIdentifiers());
	}

	private String updateProfileJson(String profileFromDb, Profile profile, boolean isServiceResultEngineOrGamEngine) {
		ProfileMerger profileMerger = new ProfileMerger(jsonMessageUtil, MergeType.UPDATE);
		final JsonElement dbProfileElement = jsonMessageUtil.fromJson(profileFromDb,
				JsonElement.class);
		Profile dbProfile = new Profile(dbProfileElement);
		Profile result = profileMerger.mergeProfileObjects(profile, dbProfile, isServiceResultEngineOrGamEngine);
		return result.getAsString();
	}
	
	protected void addProfileToSyncSet(String profileIdentifier) {
		Integer randomValueBoundary = config.getPropertyAsInteger(ProfileConfig.AEROSPIKE_PROFILE_SYNC_RANDOM_BOUNDARY);
		long randomValue = randomUtil.nextInt(randomValueBoundary == null ? 1000 : randomValueBoundary);
		try {
			createProfileSyncRecord(profileIdentifier, randomValue);
		} catch (AerospikeException ae) {
			// If unique key is violated - don't care, since record already
			// exists
			if (ae.getResultCode() != F4MAerospikeResultCodes.AS_PROTO_RESULT_FAIL_RECORD_EXISTS.getCode()) {
				LOGGER.error("Failed to create Profile synchronization record", ae);
				throw ae;
			}
		}
	}

	private void createProfileSyncRecord(String profileIdentifier, long randomValue) {
		String key = primaryKeyUtil.createPrimaryKey(profileIdentifier);
		Bin profileBin = new Bin(PROFILE_ID_BIN_NAME, profileIdentifier);
		Bin randomValueBin = getLongBin(RANDOM_VALUE_BIN_NAME, randomValue);
		createRecord(getSyncSet(), key, profileBin, randomValueBin);
	}

	protected void createProfileIdentifierTypeKeys(String userId,
			List<Pair<ProfileIdentifierType, String>> profileIdentifiers, List<String> insertedIdentifierKeys) {
		final String profileKey = primaryKeyUtil.createPrimaryKey(userId);
		for (Pair<ProfileIdentifierType, String> profileIdentifier : profileIdentifiers) {
			final String recordKey = primaryKeyUtil.createPrimaryKey(profileIdentifier.getLeft(), profileIdentifier.getRight());
			try {
				createString(getSet(), recordKey, PROFILE_ID_BIN_NAME, profileKey);
				insertedIdentifierKeys.add(recordKey);
			} catch (AerospikeException e) {
				LOGGER.error("Error while creating profile identifier", e);
				if (e.getResultCode() == ResultCode.KEY_EXISTS_ERROR) {
					throw new F4MEntryAlreadyExistsException(
							"Profile with specified identifier " + profileIdentifier.getRight() + " and type "
									+ profileIdentifier.getLeft() + " already exists");
				} else {
					throw e;
				}
			}
		}
	}

	private void executeProfileChangeWithIdentifiersCleanup(String userId,
			List<Pair<ProfileIdentifierType, String>> identifiers,
			List<Pair<ProfileIdentifierType, String>> deleteIdentifiers, Runnable profileChange) {
		List<String> insertedIdentifierKeys = new ArrayList<>(identifiers.size());
		List<Pair<ProfileIdentifierType, String>> deletedIdentifiers = new ArrayList<>(deleteIdentifiers.size());
		try {
			deleteOldIdentifiers(deleteIdentifiers, deletedIdentifiers);
			createProfileIdentifierTypeKeys(userId, identifiers, insertedIdentifierKeys);
			profileChange.run();
		} catch (RuntimeException e) {
			LOGGER.error("Error occurred during profile modification, cleaning up created identifiers", e);
			deleteProfileIdentifiersByKeyList(insertedIdentifierKeys);
			createProfileIdentifierTypeKeys(userId, deletedIdentifiers, new ArrayList<>());
			throw e;
		} catch (Exception e) {
			LOGGER.error("Error occurred during profile modification, cleaning up created identifiers", e);
			deleteProfileIdentifiersByKeyList(insertedIdentifierKeys);
			createProfileIdentifierTypeKeys(userId, deletedIdentifiers, new ArrayList<>());
			throw new F4MFatalErrorException("Could not modify profile data", e);
		}
	}

	@Override
	public void deleteProfile(String userId) {
		final Profile profile = getActiveProfile(userId);

		if (profile != null) {
			deleteProfileIdentifierTypeKeys(profile.getIdentifiers());

			final String profileKey = primaryKeyUtil.createPrimaryKey(userId);
			deleteSubBlobs(profileKey, profile);
			delete(getSet(), profileKey);
			addProfileToSyncSet(userId);
		} else {
			throw new F4MEntryNotFoundException("Profile not found");
		}
	}

	private void deleteProfileIdentifierTypeKeys(final List<Pair<ProfileIdentifierType, String>> profileIdentifiers) {
		for (Pair<ProfileIdentifierType, String> profileIdentifier : profileIdentifiers) {
			deleteIdentifierSilently(profileIdentifier.getLeft(), profileIdentifier.getRight());
		}
	}

	private void deleteProfileIdentifiersByKeyList(List<String> insertedIdentifierKeys) {
		for (String key : insertedIdentifierKeys) {
			deleteSilently(getSet(), key);
		}
	}

	private void deleteSubBlobs(String profileKey, Profile profile) {
		final Set<String> subBlobNames = profile.getSubBlobNames();
		if (subBlobNames != null && !subBlobNames.isEmpty()) {
			subBlobNames.forEach(n -> deleteSilently(getSet(),
					primaryKeyUtil.createSubRecordPrimaryKey(profileKey, n)));
		}
	}
	
	@Override
	public void saveProfileMergeHistory(String sourceProfileId, String targetProfileId) {
		final String key = primaryKeyUtil.createPrimaryKey(sourceProfileId);
		createString(getMergedProfilesSet(), key, BLOB_BIN_NAME, targetProfileId);
	}

	private String getSyncSet() {
		return config.getProperty(ProfileConfig.AEROSPIKE_PROFILE_SYNC_SET);
	}
	
	protected IndexTask initSyncSecondaryIndex() {
		return getAerospikeClient().createIndex(null, getNamespace(), getSyncSet(), getRandomValueIndexName(), RANDOM_VALUE_BIN_NAME, IndexType.NUMERIC);
	}
	
	protected String getRandomValueIndexName(){
		return config.getProperty(ProfileConfig.AEROSPIKE_PROFILE_RANDOM_VALUE_INDEX_NAME);
	}

	@Override
	public int queueResync(String userId) {
		if (StringUtils.isNotBlank(userId)) {
			addProfileToSyncSet(userId);
			return 1;
		} else {
			AtomicInteger scanned = new AtomicInteger();
			getAerospikeClient().scanAll(null, getNamespace(), getSet(), (key, record) -> {
				Object objValue = record.bins.get(BLOB_BIN_NAME);
				if (objValue != null && objValue instanceof byte[]) {
					byte[] value = (byte[]) objValue;
					if (ArrayUtils.isNotEmpty(value)) {
						String profileJson = new String(value, getDefaultCharset());
						try {
							Profile profile = new Profile(jsonUtil.fromJson(profileJson, JsonObject.class));
							addProfileToSyncSet(profile.getUserId());
							scanned.incrementAndGet();
						} catch (Exception e) {
							LOGGER.error("Could not enqueue profile for syncing", e);
							LOGGER.error("Profile data: " + profileJson);
						}
					}
				}
			}, BLOB_BIN_NAME);
			return scanned.get();
		}
	}

}
