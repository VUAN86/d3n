package de.ascendro.f4m.service.game.usergameaccess;

import de.ascendro.f4m.service.game.selection.model.game.Game;

public interface UserGameAccessDao {

	void create(Game game,String userId);

	Long getUserGameCountLeft(Game game, String userId);
	
	void decrementGamePlayedCount(Game game, String userId);
	
	boolean exists(String gameId, String userId);
}
