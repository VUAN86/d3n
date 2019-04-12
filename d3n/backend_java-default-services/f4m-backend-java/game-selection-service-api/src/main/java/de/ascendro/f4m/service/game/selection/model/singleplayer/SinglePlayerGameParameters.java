package de.ascendro.f4m.service.game.selection.model.singleplayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameParametersBase;
import de.ascendro.f4m.service.game.selection.model.game.validate.CustomParameterRule;
import de.ascendro.f4m.service.game.selection.model.game.validate.NumberOfQuestionsRule;
import de.ascendro.f4m.service.game.selection.model.game.validate.PoolsRule;

public class SinglePlayerGameParameters extends GameParametersBase {
	private Boolean trainingMode;

	public SinglePlayerGameParameters() {
	}

	public SinglePlayerGameParameters(boolean trainingMode) {
		this.trainingMode = trainingMode;
	}

	public Boolean getTrainingMode() {
		return trainingMode;
	}

	public void setTrainingMode(Boolean trainingMode) {
		this.trainingMode = trainingMode;
	}

	public boolean isTrainingMode() {
		return trainingMode == Boolean.TRUE;
	}

	/**
	 * Validate parameters according to {@link Game} configuration
	 * @param game Game
	 */
	public void validate(Game game) {
		List<CustomParameterRule<? super SinglePlayerGameParameters>> rules = new ArrayList<>();
		rules.add(new PoolsRule());
		rules.add(new NumberOfQuestionsRule());

		rules.forEach(r -> r.validate(this, game));
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SinglePlayerGameParameters [trainingMode=");
		builder.append(trainingMode);
		builder.append(", getPoolIds()=");
		builder.append(Arrays.toString(getPoolIds()));
		builder.append(", getNumberOfQuestions()=");
		builder.append(getNumberOfQuestions());
		builder.append("]");
		return builder.toString();
	}

}
