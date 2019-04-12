package de.ascendro.f4m.service.game.engine.model;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class QuestionIndex extends QuestionBaseKey {

	private Map<String, Long> index;
	
	public QuestionIndex(){
		setIndex(Collections.emptyMap());
	}

	public Long getIndex(String language) {
		return index.get(language);
	}
	
	public void setIndex(Map<String, Long> index) {
		this.index = Optional.ofNullable(index)
				.orElse(Collections.emptyMap());
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("QuestionIndex [poolId=");
		builder.append(getPoolId());
		builder.append(", type=");
		builder.append(getType());
		builder.append(", complexity=");
		builder.append(getComplexity());
		builder.append(", index=");
		builder.append(index);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((index == null) ? 0 : index.hashCode());
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
		QuestionIndex other = (QuestionIndex) obj;
		if (index == null) {
			if (other.index != null)
				return false;
		} else if (!index.equals(other.index))
			return false;
		return true;
	}
}
