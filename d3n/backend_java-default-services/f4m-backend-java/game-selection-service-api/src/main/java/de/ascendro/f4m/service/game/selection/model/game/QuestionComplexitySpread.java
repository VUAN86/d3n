package de.ascendro.f4m.service.game.selection.model.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum QuestionComplexitySpread {
	FIXED("fixed"),
	PERCENTAGE("percentage");

	private static final Logger LOGGER = LoggerFactory.getLogger(QuestionComplexitySpread.class);
	
	private final String text;

	private QuestionComplexitySpread(String text) {
		this.text = text;
	}

	public static QuestionComplexitySpread getByText(String text) {
		try {
			return QuestionComplexitySpread.valueOf(text.toUpperCase());
		} catch (IllegalArgumentException | NullPointerException e) {
			LOGGER.error("Invalid QuestionComplexitySpread [{}]", text, e);
			return null;
		}
	}

	public String getText() {
		return text;
	}
}
