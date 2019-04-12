package de.ascendro.f4m.service.game.selection.model.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum QuestionSelection {
	RANDOM_PER_PLAYER("random_per_player");

	private static final Logger LOGGER = LoggerFactory.getLogger(QuestionSelection.class);

	private final String text;

	private QuestionSelection(String text) {
		this.text = text;
	}

	public static QuestionSelection getByText(String text) {
		try {
			return QuestionSelection.valueOf(text.toUpperCase());
		} catch (IllegalArgumentException | NullPointerException e) {
			LOGGER.error("Invalid QuestionSelection [{}]", text, e);
			return null;
		}
	}

	public String getText() {
		return text;
	}
}
