package de.ascendro.f4m.service.game.engine.model;

public class QuestionKey extends QuestionBaseKey {
	private String language;

	public QuestionKey() {
	}
	
	public QuestionKey(String id, String type, int complexity, String language) {
		super(id, type, complexity);
		this.language = language;
	}
	
	public QuestionKey(QuestionBaseKey questionBaseKey, String language) {
		this(questionBaseKey.getPoolId(), questionBaseKey.getType(), questionBaseKey.getComplexity(), language);
	}
	
	public String getLanguage() {
		return language;
	}
	
	public void setLanguage(String language) {
		this.language = language;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("QuestionKey [language=");
		builder.append(language);
		builder.append(", poolId=");
		builder.append(getPoolId());
		builder.append(", type=");
		builder.append(getType());
		builder.append(", complexity=");
		builder.append(getComplexity());
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((language == null) ? 0 : language.hashCode());
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
		QuestionKey other = (QuestionKey) obj;
		if (language == null) {
			if (other.language != null)
				return false;
		} else if (!language.equals(other.language))
			return false;
		return true;
	}
}
