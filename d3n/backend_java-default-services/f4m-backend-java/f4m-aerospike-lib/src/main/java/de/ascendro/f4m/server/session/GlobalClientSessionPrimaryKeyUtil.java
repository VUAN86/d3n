package de.ascendro.f4m.server.session;

import javax.inject.Inject;

import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.service.config.Config;

/**
 * Global client session service aerospike key construct helper
 *
 */
public class GlobalClientSessionPrimaryKeyUtil extends PrimaryKeyUtil<String> {
	private static final String SESSION_KEY_PREFIX = "session" +  PrimaryKeyUtil.KEY_ITEM_SEPARATOR;
	
	@Inject
	public GlobalClientSessionPrimaryKeyUtil(Config config) {
		super(config);
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
		return SESSION_KEY_PREFIX + userId;
	}
	
}
