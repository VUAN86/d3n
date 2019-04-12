package de.ascendro.f4m.server.multiplayer.move.dao;

public interface MoveMultiplayerGameInstanceDao {

	void moveUserMgiData(String tenantId, String appId, String sourceUserId, String targetUserId);

}
