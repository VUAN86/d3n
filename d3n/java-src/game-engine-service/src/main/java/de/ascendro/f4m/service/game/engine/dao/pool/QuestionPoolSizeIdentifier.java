package de.ascendro.f4m.service.game.engine.dao.pool;

public class QuestionPoolSizeIdentifier {
	private final String type;
	private final int compelxity;

	public QuestionPoolSizeIdentifier(String type, int compelxity) {
		this.type = type;
		this.compelxity = compelxity;
	}

	public String getType() {
		return type;
	}	

	public int getCompelxity() {
		return compelxity;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + compelxity;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof QuestionPoolSizeIdentifier))
			return false;
		QuestionPoolSizeIdentifier other = (QuestionPoolSizeIdentifier) obj;
		if (compelxity != other.compelxity)
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
		builder.append("QuestionPoolSizeIdentifier [type=");
		builder.append(type);
		builder.append(", compelxity=");
		builder.append(compelxity);
		builder.append("]");
		return builder.toString();
	}
}	
