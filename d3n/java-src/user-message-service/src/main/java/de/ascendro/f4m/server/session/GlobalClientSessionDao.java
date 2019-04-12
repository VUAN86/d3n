package de.ascendro.f4m.server.session;

public interface GlobalClientSessionDao {
	
	/**
	 * Returns active session information for given user.
	 * null, if user has no active sessions.
	 * @param userId
	 * @return
	 */
	GlobalClientSessionInfo getGlobalClientSessionInfoByUserId(String userId);
}
