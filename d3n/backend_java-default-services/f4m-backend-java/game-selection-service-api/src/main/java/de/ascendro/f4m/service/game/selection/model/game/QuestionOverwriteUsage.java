package de.ascendro.f4m.service.game.selection.model.game;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public enum QuestionOverwriteUsage {
	NO_REPEATS("onlyOnce"), /*Player CANNOT get the same question multiple times*/
	REPEATS("repeatQuestions") /*Player CAN get the same question multiple times*/;

	private static final Map<String, QuestionOverwriteUsage> VALUES = new HashMap<>();
	static{
		Arrays.stream(QuestionOverwriteUsage.values())
			.forEach(v -> VALUES.put(v.getText(), v));
	}
	
	private final String text;

	private QuestionOverwriteUsage(String text) {
		this.text = text;
	}

	public static QuestionOverwriteUsage getByText(String text) {
		return VALUES.get(text);
	}
	
	public String getText() {
		return text;
	}
}
