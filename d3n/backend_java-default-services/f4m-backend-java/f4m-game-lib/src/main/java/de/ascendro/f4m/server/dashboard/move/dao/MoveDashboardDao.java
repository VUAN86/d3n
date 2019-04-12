package de.ascendro.f4m.server.dashboard.move.dao;

public interface MoveDashboardDao {
	
	void moveLastPlayedGame(String tenantId, String sourceUserId, String targetUserId);

}
