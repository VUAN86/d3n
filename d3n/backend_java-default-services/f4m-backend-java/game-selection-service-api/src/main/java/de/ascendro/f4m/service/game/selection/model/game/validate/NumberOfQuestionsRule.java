package de.ascendro.f4m.service.game.selection.model.game.validate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import de.ascendro.f4m.service.game.selection.exception.F4MNumberOfQuestionsNotValidException;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameParametersBase;

public class NumberOfQuestionsRule implements CustomParameterRule<GameParametersBase> {

	//FIXME: temp fix - should be 3, 5, 7 for duels and quizes; 10,15, 20 for tournaments, should be changed later 
	public static final List<Integer> VALID_NUMBER_OF_QUESTIONS_DEFAULT = Collections.unmodifiableList(Arrays.asList(3, 5, 7,10, 15, 20));
	public static final List<Integer> VALID_NUMBER_OF_QUESTIONS_TOURNAMENT = Collections.unmodifiableList(Arrays.asList(3, 5, 7,10, 15, 20));

	private static final String NOT_SET = "Number of questions should be set; params [%s]";
	private static final String NOT_CONFIGURABLE = "Number of questions is not configurable; params [%s]";
	private static final String INVALID = "Invalid number of questions [%d]; params [%s]";

	@Override
	public void validate(GameParametersBase params, Game game) {
		validateNumberOfQuestions(params, game);
	}

	private void validateNumberOfQuestions(GameParametersBase params, Game game) {
		Integer defaultValue = game.getNumberOfQuestions();
		Integer providedValue = params.getNumberOfQuestions();
		
		if (game.canCustomizeNumberOfQuestions()) {
			if (!isProvided(providedValue)) {
				throw new F4MNumberOfQuestionsNotValidException(String.format(NOT_SET, params));
			} else if ( (game.getType().isTournament() && !VALID_NUMBER_OF_QUESTIONS_TOURNAMENT.contains(providedValue)) || 
					(!VALID_NUMBER_OF_QUESTIONS_DEFAULT.contains(providedValue)) ) {
				
				throw new F4MNumberOfQuestionsNotValidException(String.format(INVALID, providedValue, params));
			}
		} else if (isProvided(providedValue) && !Objects.equals(providedValue, defaultValue)) {
			throw new F4MNumberOfQuestionsNotValidException(String.format(NOT_CONFIGURABLE, params));
		}
	}

	private boolean isProvided(Integer providedValue) {
		return providedValue != null && !providedValue.equals(0);
	}

}
