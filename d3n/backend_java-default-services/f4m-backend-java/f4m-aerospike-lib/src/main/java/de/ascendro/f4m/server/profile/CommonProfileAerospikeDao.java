package de.ascendro.f4m.server.profile;

import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;

import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileIdentifierType;
import de.ascendro.f4m.service.profile.model.ProfileStats;
import de.ascendro.f4m.service.profile.model.api.ApiProfileBasicInfo;
import de.ascendro.f4m.service.profile.model.api.ApiProfileExtendedBasicInfo;

/**
 * Profile Data Access Object interface.
 */
public interface CommonProfileAerospikeDao extends AerospikeDao {

	String BLOB_BIN_NAME = "value";
    String PROFILE_ID_BIN_NAME = "profile";
	String USER_VOUCHER_BLOB_NAME = "voucher";
	String LAST_INVITED_DUEL_OPPONENTS_BLOB_NAME = "lastDuelOpponents";
	String PAUSED_DUEL_OPPONENTS_BLOB_NAME = "pausedDuelOpponents";
	String USER_STATS_BLOB_NAME = "stats";
	String USER_LOGIN_TIMESTAMP_BLOB_NAME = "login.timestamp";

	/**
	 * Get Profile or search for the currently active profile into which userId was merged in.
	 * 
	 * @param userId
	 *            String
	 * @return profile
	 */
	Profile getProfile(String userId);
	
	/**
	 * Get active profile without performing search, if userId was merged into another profile. 
	 * 
	 * @param userId
	 * @return
	 */
	Profile getActiveProfile(String userId);
	
	/**
	 * Determine if profile exists
	 */
	boolean exists(String userId);
	
	/**
	 * Get profiles by IDs.
	 * 
	 * @param usersIds
	 * @return list of profiles
	 */
	List<Profile> getProfiles(List<String> usersIds);
	
	/**
	 * Get basic information of profile
	 * @param userId
	 * @return {@link ApiProfileBasicInfo}
	 */
	ApiProfileBasicInfo getProfileBasicInfo(String userId);

	/**
	 * Get basic information of profiles
	 * @param userIds
	 * @return {@link List} of {@link ApiProfileBasicInfo}s
	 */
	Map<String, ApiProfileBasicInfo> getProfileBasicInfo(List<String> userIds);

	/**
	 * Get basic information of opponent profile (includes handicap)
	 * @param userId
	 * @return {@link ApiProfileBasicInfo}
	 */
	ApiProfileExtendedBasicInfo getProfileExtendedBasicInfo(String userId);

	/**
	 * Get basic information of opponent profile (includes handicap)
	 * @param userId
	 * @return {@link ApiProfileBasicInfo}
	 */
	Map<String, ApiProfileExtendedBasicInfo> getProfileExtendedBasicInfo(List<String> userId);


	/**
	 * Find profile by specified Profile Identifier Type and related value.
	 *
	 * @param type
	 *            {@link ProfileIdentifierType}
	 * @param identifier
	 *            {@link String}
	 * @return profile
	 */
	Profile findByIdentifierWithCleanup(ProfileIdentifierType type, String identifier) throws F4MException;

	/**
	 * Find userId by specified Profile Identifier Type and related value.
	 *
	 * @param type
	 *            {@link ProfileIdentifierType}
	 * @param identifier
	 *            {@link String}
	 * @return profile
	 */
	String findUserIdByIdentifierWithCleanup(ProfileIdentifierType type, String identifier) throws F4MException;
	
	/**
	 * Retrieves profile related sub record identified by its name
	 * @param userId - profile user id
	 * @param blobName - record name to be retrieved
	 * @return value of the blob
	 */
	String getSubBlob(String userId, String blobName);

	/**
	 * Retrieves profile related sub record identified by its name
	 * @param tenantId - profile user id
	 * @param blobName - record name to be retrieved
	 * @return value of the blob
	 */
	String getSubRecordName(String tenantId, String blobName);


	/**
	 * Retrieves profile related sub record map entry identified by its name
	 * @param userId - profile user id
	 * @param blobName - record name to be retrieved
	 * @param mapKey - map key of record to be retrieved
	 * @return value of the blob map entry
	 */
	String getSubBlobMapEntry(String userId, String blobName, String mapKey);

	/**
	 * Deletes profile related sub record identified by its name
	 */
	void deleteSubBlob(String userId, String blobName);
	
	/**
	 * Retrieves vouchers list sub record for a user
	 * @param userId - profile user id
	 * @return value of the vouchers blob
	 */
	JsonArray getVoucherReferencesArrayFromBlob(String userId);

	/**
	 * Retrieves last login timestamp for a user in the specified app
	 * @param userId - profile user id
	 * @param appId - app id
	 * @return value of the timestamp
	 */
	String getLastLoginTimestampFromBlob(String userId, String appId);

	/**
	 * Saves last login timestamp for a user in the specified app
	 * @param userId - profile user id
	 * @param appId - app id
	 * @param timestamp value of the timestamp
	 */
	void setLastLoginTimestampInBlob(String userId, String appId, String timestamp);

	/**
	 * Retrieves profile stats sub record for a user
	 * @param userId - profile user id
	 * @return profile stats
	 */
	ProfileStats getStatsFromBlob(String userId, String tenantId);

    /**
	 * Update or create if does not exist specified profile sub-record
	 * @param userId - profile user id
	 * @param blobName - record name to be updated
	 * @param blobValue - new value of the profile sub-record
	 */
	void updateSubBlob(String userId, String blobName, String blobValue);

	/**
	 * Update or create if does not exist specified profile sub-record map entry
	 * @param userId - profile user id
	 * @param blobName - record name to be updated
	 * @param mapKey - map key of the profile sub-record
	 * @param blobValue - new value of the profile sub-record
	 */
	void updateSubBlobMapEntry(String userId, String blobName, String mapKey, String blobValue);

	/**
	 * Add specified profile vouchers sub-record entry. Operation is performed
	 * by adding entry to the existing list
	 * @param userId - profile user id
	 * @param voucherId - id of Voucher
	 * @param userVoucherId - id of userVoucher
	 */
	void vouchersEntryAdd(String userId, String voucherId, String userVoucherId);

	/**
	 * Set the last invited duel opponents for the specified app id
	 * @param inviterId - profile user id
	 * @param appId - app id
	 * @param invitedUsers - user ids of last invited duel opponents
	 */
	void setLastInvitedDuelOponents(String inviterId, String appId, List<String> invitedUsers);

	void removeUsersFromLastInvitedDuelOponents(String ownerId, List<String> userIds);

	List<String> getLastInvitedDuelOpponents(String userId, String appId);

	void setPausedDuelOponents(String ownerId, String appId, List<String> pausedUsers);

	void removeUsersFromPausedDuelOponents(String ownerId, List<String> userIds);

	List<String> getPausedDuelOpponents(String userId, String appId);

	void updateStats(String userId, String tenantId, ProfileStats profileStats);

}
