package de.ascendro.f4m.server.dashboard.dao;

import java.util.List;

import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.service.game.selection.model.dashboard.MostPlayedGame;
import de.ascendro.f4m.service.game.selection.model.dashboard.PlayedGameInfo;
import de.ascendro.f4m.service.game.selection.model.game.GameType;

public interface DashboardDao extends AerospikeDao {
	
	void updateLastPlayedGame(String tenantId, String userId, PlayedGameInfo gameInfo);
	
	PlayedGameInfo getLastPlayedGame(String tenantId, String userId, GameType gameType);
	
	MostPlayedGame getMostPlayedGame(List<String> gameIds);
	
}
