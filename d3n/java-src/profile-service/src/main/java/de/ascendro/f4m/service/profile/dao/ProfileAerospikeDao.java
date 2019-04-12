package de.ascendro.f4m.service.profile.dao;

import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.service.profile.model.Profile;

/**
 * Profile Data Access Object interface.
 */
public interface ProfileAerospikeDao extends CommonProfileAerospikeDao {
	String RANDOM_VALUE_BIN_NAME = "randomValue";

	/**
	 * Create Profile.
	 * @param userId String
	 * @param profile Profile
	 */
	void createProfile(String userId, Profile profile);

	/**
	 * Update Profile with changes given in parameter
	 *
	 * @param userId          {@link String}
	 * @param profile         Json object containing changes in object {@link String}
	 * @param alreadyExisting Json object with data from record already existing in db.
	 * @return profile
	 */
	Profile updateProfile(String userId, Profile profile, Profile alreadyExisting, boolean isServiceResultEngine);

	/**
	 * Overwrites profile information. Should be used only for internal calls.
	 * 
	 * @param userId
	 * @param profile
	 * @param alreadyExisting
	 *            Json object with data from record already existing in db.
	 * @return
	 */
	Profile overwriteProfile(String userId, Profile profile, Profile alreadyExisting);

	/**
	 * Deleted profile by user id
	 * 
	 * @param userId
	 *            {@link String}
	 */
	void deleteProfile(String userId);
	
	/**
	 * Save info, that sourceProfileId was merged into targetProfileId
	 * 
	 * @param sourceProfileId
	 * @param targetProfileId
	 */
	void saveProfileMergeHistory(String sourceProfileId, String targetProfileId);

	/**
	 * Queue profile for synchronization with elastic and mysql.
	 * @param userId User ID to resync. If omitted, all users will be resynced.
	 * @return Number of resynchronized profiles
	 */
	int queueResync(String userId);

}
