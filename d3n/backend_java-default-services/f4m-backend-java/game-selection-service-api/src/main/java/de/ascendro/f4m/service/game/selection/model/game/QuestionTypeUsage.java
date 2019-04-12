package de.ascendro.f4m.service.game.selection.model.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum QuestionTypeUsage {
	RANDOM("random"),
	ORDERED("ordered");

	private static final Logger LOGGER = LoggerFactory.getLogger(QuestionTypeUsage.class);

	private final String text;

	private QuestionTypeUsage(String text) {
		this.text = text;
	}

	public static QuestionTypeUsage getByText(String text) {
		try {
			return QuestionTypeUsage.valueOf(text.toUpperCase());
		} catch (IllegalArgumentException | NullPointerException e) {
			LOGGER.error("Invalid QuestionTypeUsage [{}]", text, e);
			return null;
		}
	}

	public String getText() {
		return text;
	}
}
