package de.ascendro.f4m.service.game.selection.model.game.validate;

import de.ascendro.f4m.service.game.selection.exception.F4MMaxParticipantCountNotValidException;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.multiplayer.MultiplayerGameParameters;

public class MultiplayerParticipantRule implements CustomParameterRule<MultiplayerGameParameters> {

	private static final String INVALID = "Duel participant count must be [%d]; params [%s]";

	@Override
	public void validate(MultiplayerGameParameters params, Game game) {
		if (game.isDuel() && params.getMaxNumberOfParticipants() != null) {
			validateMaxNumberOfParticipants(params);
		}
	}

	private void validateMaxNumberOfParticipants(MultiplayerGameParameters params) {
		Integer maxParticipants = params.getMaxNumberOfParticipants();
		if (Game.DUEL_PARTICIPANT_COUNT != maxParticipants) {
			throw new F4MMaxParticipantCountNotValidException(String.format(INVALID, Game.DUEL_PARTICIPANT_COUNT, params));
		}
	}

}
