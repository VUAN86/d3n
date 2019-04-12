package de.ascendro.f4m.server.game;

import java.util.Objects;

import de.ascendro.f4m.service.game.selection.model.game.Game;

public class GameResponseSanitizer {

	public Game removeExcessInfo(Game game) {
		removePoolInfo(game);
		return game;
	}
	
	public boolean needsRemovingExcessInfo(Game game) {
		return needsPoolDataRemoval(game);
	}

	private boolean needsPoolDataRemoval(Game game) {
		return Objects.equals(game.getHideCategories(), Boolean.TRUE);
	}

	private void removePoolInfo(Game game) {
		if (needsPoolDataRemoval(game)) {
			game.setAssignedPools(null);
			game.setAssignedPoolsNames(null);
			game.setAssignedPoolsIcons(null);
			game.setAssignedPoolsColors(null);
			game.setQuestionPools(null);
		}
	}
}
