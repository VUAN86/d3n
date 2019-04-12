package de.ascendro.f4m.service.game.engine.model;

public class QuestionBaseKey {
	private String poolId;
	private String type;
	private int complexity;

	public QuestionBaseKey() {
	}
	
	public QuestionBaseKey(String poolId, String type, int complexity) {
		this.poolId = poolId;
		this.type = type;
		this.complexity = complexity;
	}

	public QuestionBaseKey(QuestionBaseKey questionBaseKey) {
		this.poolId = questionBaseKey.getPoolId();
		this.type = questionBaseKey.getType();
		this.complexity = questionBaseKey.getComplexity();
	}
	
	public String getPoolId() {
		return poolId;
	}

	public void setPoolId(String poolId) {
		this.poolId = poolId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getComplexity() {
		return complexity;
	}

	public void setComplexity(int complexity) {
		this.complexity = complexity;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + complexity;
		result = prime * result + ((poolId == null) ? 0 : poolId.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof QuestionBaseKey))
			return false;
		QuestionBaseKey other = (QuestionBaseKey) obj;
		if (complexity != other.complexity)
			return false;
		if (poolId == null) {
			if (other.poolId != null)
				return false;
		} else if (!poolId.equals(other.poolId))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("QuestionBaseKey [poolId=");
		builder.append(poolId);
		builder.append(", type=");
		builder.append(type);
		builder.append(", complexity=");
		builder.append(complexity);
		builder.append("]");
		return builder.toString();
	}

}
