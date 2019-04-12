package de.ascendro.f4m.service.game.usergameaccess;

import java.util.Optional;

import com.google.inject.Inject;

import de.ascendro.f4m.service.game.selection.model.game.Game;

public class UserGameAccessService {

	private UserGameAccessDao userSpecialGameDao;

	@Inject
	public UserGameAccessService(UserGameAccessDao userSpecialGameDao) {
		this.userSpecialGameDao = userSpecialGameDao;
	}

	public boolean havePlaysLeft(Game game, String userId) {
		return getRemainingPlayTimes(game, userId) > 0;
	}

	public void grantAccess(Game game, String userId) {
		Long playCountLeft = userSpecialGameDao.getUserGameCountLeft(game, userId);
		if (playCountLeft == null || game.isMultiplePurchaseAllowed()) {
			userSpecialGameDao.create(game, userId);
		}
	}

	/**
	 * Checks if user can play user access game
	 * Game can be played if 
	 * */
	public boolean canPlay(Game game, String userId) {
		boolean canPlayGame = false;
		Long playCountLeft = userSpecialGameDao.getUserGameCountLeft(game, userId);
		if (playCountLeft == null || playCountLeft > 0) {
			canPlayGame = true;
		}
		return canPlayGame;
	}

	public boolean isVisible(Game game, String userId) {
		return game.isMultiplePurchaseAllowed() || canPlay(game, userId);
	}

	public int getRemainingPlayTimes(Game game, String userId) {
		return Optional.ofNullable(userSpecialGameDao.getUserGameCountLeft(game, userId)).map(Long::intValue).orElse(0);
	}
	
	public void registerAccess(Game game, String userId) {
		userSpecialGameDao.decrementGamePlayedCount(game, userId);
	}
}
