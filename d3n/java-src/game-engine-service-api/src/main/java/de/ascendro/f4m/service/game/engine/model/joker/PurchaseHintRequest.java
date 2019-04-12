package de.ascendro.f4m.service.game.engine.model.joker;

import de.ascendro.f4m.service.game.engine.model.start.game.GameInstanceRequest;
import de.ascendro.f4m.service.game.selection.model.game.JokerType;

public class PurchaseHintRequest extends PurchaseJokerRequest implements GameInstanceRequest {

    @Override
	public JokerType getType() {
		return JokerType.HINT; 
	}

}