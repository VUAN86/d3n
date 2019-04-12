package de.ascendro.f4m.service.game.selection.model.game;

import java.util.Arrays;

public abstract class GameParametersBase {

	private String[] poolIds;
	private Integer numberOfQuestions;

	public String[] getPoolIds() {
		return poolIds;
	}

	public void setPoolIds(String[] poolIds) {
		this.poolIds = poolIds;
	}

	public Integer getNumberOfQuestions() {
		return numberOfQuestions;
	}

	public void setNumberOfQuestions(Integer numberOfQuestions) {
		this.numberOfQuestions = numberOfQuestions;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CustomGameParameters [poolIds=");
		builder.append(Arrays.toString(poolIds));
		builder.append(", numberOfQuestions=");
		builder.append(numberOfQuestions);
		builder.append("]");
		return builder.toString();
	}

}
