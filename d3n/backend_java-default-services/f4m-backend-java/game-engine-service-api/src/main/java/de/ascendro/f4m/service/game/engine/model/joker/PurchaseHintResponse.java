package de.ascendro.f4m.service.game.engine.model.joker;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class PurchaseHintResponse implements JsonMessageContent {

	private String hint;

	public PurchaseHintResponse(String hint) {
		this.hint = hint;
	}

	public String getHint() {
		return hint;
	}

	public void setHint(String hint) {
		this.hint = hint;
	}
}
