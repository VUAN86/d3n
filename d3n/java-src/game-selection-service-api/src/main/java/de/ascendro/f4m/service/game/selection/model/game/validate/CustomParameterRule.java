package de.ascendro.f4m.service.game.selection.model.game.validate;

import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameParametersBase;

public interface CustomParameterRule<T extends GameParametersBase> {
	void validate(T params, Game game);
}
