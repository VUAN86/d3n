package de.ascendro.f4m.service.game.selection.model.game.validate;

import de.ascendro.f4m.service.game.selection.exception.F4MInvalidGameModeException;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.singleplayer.SinglePlayerGameParameters;

public class SinglePlayerModeRule implements CustomParameterRule<SinglePlayerGameParameters> {

	private static final String INVALID = "Forbidden to play paid game in training mode; game [%s]; params [%s]";

	@Override
	public void validate(SinglePlayerGameParameters params, Game game) {
		if (params.isTrainingMode() && !game.isFree()) {
			throw new F4MInvalidGameModeException(String.format(INVALID, game.getGameId(), params));
		}
	}

}
