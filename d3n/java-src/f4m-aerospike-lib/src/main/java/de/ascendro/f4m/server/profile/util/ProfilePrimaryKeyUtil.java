package de.ascendro.f4m.server.profile.util;

import javax.inject.Inject;

import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.profile.ProfileMessageTypes;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileIdentifierType;

/**
 * Profile service aerospike key construct helper
 *
 */
public class ProfilePrimaryKeyUtil extends PrimaryKeyUtil<String> {

	@Inject
	public ProfilePrimaryKeyUtil(Config config) {
		super(config);
	}

	/**
	 * Create unique primary key for profile secondary search keys <<PROFILESERVICE>>_<<UUID>>_<<IDENTIFER TYPE
	 * (PHONE|MAIL|FACEBOOK|GOOGLE)>>_<<IDENTIFIER>> -> UUID
	 *
	 * @param identifierType
	 *            - Profile identifier type
	 * @param identifier
	 *            - profile identifier
	 * @return unique primary key
	 */
	public String createPrimaryKey(ProfileIdentifierType identifierType, String identifier) {
		return ProfileMessageTypes.SERVICE_NAME + KEY_ITEM_SEPARATOR + identifierType.name() + KEY_ITEM_SEPARATOR
				+ Profile.cleanupIdentifierForStorage(identifierType, identifier);
	}

	/**
	 * Create unique primary key based on user id
	 * 
	 * @param userId
	 *            - User id
	 * @return unique primary key
	 */
	@Override
	public String createPrimaryKey(String userId) {
		return ProfileMessageTypes.SERVICE_NAME + KEY_ITEM_SEPARATOR + userId;
	}

	/**
	 * Create profile sub-record key based on user id and sub-record name.
	 * 
	 * @param userId
	 *            - profile user id
	 * @param subRecordName
	 *            - sub-record name
	 * @return sub-record key
	 */
	public String createSubRecordKeyByUserId(String userId, String subRecordName) {
		final String baseRecordKey = createPrimaryKey(userId);
		return createSubRecordPrimaryKey(baseRecordKey, subRecordName);
	}
}
