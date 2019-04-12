package de.ascendro.f4m.service.friend.util;

import javax.inject.Inject;

import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.friend.FriendManagerMessageTypes;

public class FriendPrimaryKeyUtil extends PrimaryKeyUtil<String> {
	
	protected static final String KEY_PREFIX_TENANT = KEY_ITEM_SEPARATOR + "tenant" + KEY_ITEM_SEPARATOR;
	protected static final String KEY_PREFIX_USER = KEY_ITEM_SEPARATOR + "user" + KEY_ITEM_SEPARATOR;
	protected static final String KEY_PREFIX_NAME = KEY_ITEM_SEPARATOR + "name" + KEY_ITEM_SEPARATOR;
	protected static final String KEY_PREFIX_CONTACT = KEY_ITEM_SEPARATOR + "contact" + KEY_ITEM_SEPARATOR;

	@Inject
	public FriendPrimaryKeyUtil(Config config) {
		super(config);
	}
	
	@Override
	protected String getServiceName() {
		return FriendManagerMessageTypes.SERVICE_NAME;
	}

	/**
	 * @return primary key as friend:tenant:[tenantId]:user:[userId]
	 */
	public String createGroupListPrimaryKey(String tenantId, String userId) {
		return getServiceName() + KEY_PREFIX_TENANT + tenantId + KEY_PREFIX_USER + userId;
	}
	
	public String createGroupUniqueNamePrimaryKey(String tenantId, String userId, String name) {
		return getServiceName() + KEY_PREFIX_TENANT + tenantId + KEY_PREFIX_USER + userId + KEY_PREFIX_NAME + name;
	}
	
	/**
	 * @return primary key as friend:user:[userId]:contact:[contactId]
	 */
	public String createContactPrimaryKey(String userId, String contactId) {
		return getServiceName() + KEY_PREFIX_USER + userId + KEY_PREFIX_CONTACT + contactId;
	}
	
}
