package de.ascendro.f4m.service.result.engine.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;

/**
 * Comparator ordering results by their places.
 */
public class UserResultsComparator implements Comparator<UserResults> {

	@Override
	public int compare(UserResults r1, UserResults r2) {
		// First, compare by determining if game was finished at all
		if ((r1 == null || !r1.isGameFinished()) && (r2 == null || !r2.isGameFinished())) {
			// Both did not finish => equal
			return 0;
		} else if (r1 == null || !r1.isGameFinished()) {
			// First did not finish => put him at the bottom
			return 1;
		} else if (r2 == null || !r2.isGameFinished()) {
			// Second did not finish => put him at the bottom
			return -1;
		} else {
			// Both finished => compare by correct answers first
			int result = compareByCorrectAnswers(r1, r2);
			if (result != 0) {
				return result;
			}
			// Then by game points
			return compareByGamePoints(r1, r2);
		}
	}

	/**
	 * Compare two results by correct answers (more correct answers on top).
	 */
	private int compareByCorrectAnswers(UserResults r1, UserResults r2) {
		// Put the one with more correct answers at the top
		int correctAnswers1 = r1.getCorrectAnswerCount();
		int correctAnswers2 = r2.getCorrectAnswerCount();
		if (correctAnswers1 == correctAnswers2) {
			return 0;
		} else {
			return correctAnswers1 > correctAnswers2 ? -1 : 1;
		}
	}

	/**
	 * Compare two results by game points (more points on top).
	 */
	private int compareByGamePoints(UserResults r1, UserResults r2) {
		// Put the one with more game points at the top
		BigDecimal gamePoints1 = roundDownToOneDecimalPlace(r1.getGamePointsWithBonus());
		BigDecimal gamePoints2 = roundDownToOneDecimalPlace(r2.getGamePointsWithBonus());
		return -gamePoints1.compareTo(gamePoints2);
	}

	private BigDecimal roundDownToOneDecimalPlace(double value) {
		return BigDecimal.valueOf(value).setScale(1, RoundingMode.DOWN);
	}

}
