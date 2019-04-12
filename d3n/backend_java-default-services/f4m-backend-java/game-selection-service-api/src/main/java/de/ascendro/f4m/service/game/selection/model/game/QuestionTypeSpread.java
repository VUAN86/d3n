package de.ascendro.f4m.service.game.selection.model.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum QuestionTypeSpread {
	FIXED("fixed"),
	PERCENTAGE("percentage");

	private static final Logger LOGGER = LoggerFactory.getLogger(QuestionTypeSpread.class);

	private final String text;

	private QuestionTypeSpread(String text) {
		this.text = text;
	}

	public static QuestionTypeSpread getByText(String text) {
		try {
			return QuestionTypeSpread.valueOf(text.toUpperCase());
		} catch (IllegalArgumentException | NullPointerException e) {
			LOGGER.error("Invalid QuestionTypeSpread [{}]", text, e);
			return null;
		}
	}

	public String getText() {
		return text;
	}
}
