package de.ascendro.f4m.service.friend.util;

import javax.inject.Inject;

import de.ascendro.f4m.service.config.Config;

public class BuddyPrimaryKeyUtil extends FriendPrimaryKeyUtil {

	private static final String KEY_PREFIX_BUDDY = KEY_ITEM_SEPARATOR + "buddy" + KEY_ITEM_SEPARATOR;

	@Inject
	public BuddyPrimaryKeyUtil(Config config) {
		super(config);
	}

	/**
	 * @param userId
	 * @param buddyId
	 * @return primary key as
	 *         friend:user:[userId]:buddy:[buddyId]
	 */
	public String createBuddyPrimaryKey(String userId, String buddyId) {
		return getServiceName() + KEY_PREFIX_USER + userId + KEY_PREFIX_BUDDY + buddyId;
	}

}
