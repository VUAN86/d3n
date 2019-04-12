package de.ascendro.f4m.service.game.engine.model;

import java.util.Arrays;

public class InternationalQuestionKey extends QuestionBaseKey {
	private final String[] playingLanguages;
	
	public InternationalQuestionKey(String id, String type, int complexity, String[] playingLanguages) {
		super(id, type, complexity);
		this.playingLanguages = playingLanguages;
	}

	public InternationalQuestionKey(QuestionBaseKey questionBaseKey, String[] playingLanguages) {
		super(questionBaseKey);
		this.playingLanguages = playingLanguages;
	}
	
	public String[] getPlayingLanguages() {
		return playingLanguages;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("InternationalQuestionKey [playingLanguages=");
		builder.append(Arrays.toString(playingLanguages));
		builder.append(", toString()=");
		builder.append(super.toString());
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(playingLanguages);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		InternationalQuestionKey other = (InternationalQuestionKey) obj;
		if (!Arrays.equals(playingLanguages, other.playingLanguages))
			return false;
		return true;
	}
}
