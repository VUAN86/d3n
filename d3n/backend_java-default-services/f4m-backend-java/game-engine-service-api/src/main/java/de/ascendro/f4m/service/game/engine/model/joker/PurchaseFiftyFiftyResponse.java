package de.ascendro.f4m.service.game.engine.model.joker;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class PurchaseFiftyFiftyResponse implements JsonMessageContent {

	private String[] removedAnswers;

	public PurchaseFiftyFiftyResponse(String[] removedAnswers) {
		this.removedAnswers = removedAnswers;
	}

	public String[] getRemovedAnswers() {
		return removedAnswers;
	}

	public void setRemovedAnswers(String[] removedAnswers) {
		this.removedAnswers = removedAnswers;
	}
}
