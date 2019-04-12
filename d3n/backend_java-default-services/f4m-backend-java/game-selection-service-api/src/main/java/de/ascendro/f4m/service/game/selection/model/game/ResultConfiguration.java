package de.ascendro.f4m.service.game.selection.model.game;

import static de.ascendro.f4m.service.util.F4MEnumUtils.getEnum;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;
import de.ascendro.f4m.service.payment.model.Currency;

public class ResultConfiguration extends JsonObjectWrapper {
	
	// Bonus point calculator properties
	public static final String PROPERTY_POINT_CALCULATOR = "pointCalculator";
	public static final String PROPERTY_ALTERNATIVE_BONUS_POINTS_PER_CORRECT_ANSWER = "alternativeBonusPointsPerCorrectAnswer";
	
	public static final String PROPERTY_BONUS_POINTS_PER_CORRECT_ANSWER_FOR_UNPAID = "bonusPointsPerCorrectAnswerForUnpaid";
	public static final String PROPERTY_BONUS_POINTS_PER_GAME_POINT_FOR_PAID = "bonusPointsPerGamePointForPaid";
	public static final String PROPERTY_TREAT_PAID_LIKE_UNPAID = "treatPaidLikeUnpaid";

	public static final String PROPERTY_BONUS_POINTS_FOR_ALL_CORRECT_ANSWERS = "bonusPointsForAllCorrectAnswers";
	
	public static final String PROPERTY_BONUS_POINTS_FOR_QUICK_RESPONSE = "quickResponseBonuspointsAmount";
	public static final String PROPERTY_BONUS_POINTS_FOR_QUICK_RESPONSE_MS = "quickResponseBonusPointsMs";
	
	// Game point calculator properties
	public static final String PROPERTY_CORRECT_ANSWER_POINT_CALCULATION_TYPE = "correctAnswerPointCalculationType";
	public static final String PROPERTY_CORRECT_ANSWER_QUESTION_COMPLEXITY_GAME_POINTS = "correctAnswerQuestionComplexityGamePoints";
	public static final String PROPERTY_LEVEL = "level";
	public static final String PROPERTY_POINTS = "points";

	// Duel group winner payout properties
	public static final String PROPERTY_DUEL_GROUP_WINNER_PAYOUT_TYPE = "duelGroupWinnerPayoutType";
	public static final String PROPERTY_DUEL_GROUP_WINNER_PAYOUT_FIXED_VALUE_AMOUNT = "duelGroupWinnerPayoutFixedValueAmount";
	public static final String PROPERTY_DUEL_GROUP_WINNER_PAYOUT_FIXED_VALUE_CURRENCY = "duelGroupWinnerPayoutFixedValueCurrency";
	public static final String PROPERTY_DUEL_GROUP_WINNER_PAYOUT_JACKPOT_PERCENT = "duelGroupWinnerPayoutJackpotPercent";
	
	// Duel group prize payout properties
	public static final String PROPERTY_DUEL_GROUP_PRIZE_PAYOUT_TYPE = "duelGroupPrizePayoutType";
	public static final String PROPERTY_DUEL_GROUP_PRIZE_PAID_OUT_TO_EVERY_X_WINNER = "duelGroupPrizePaidOutToEveryXWinner";
	
	// Tournament game and payout structure properties
	public static final String PROPERTY_JACKPOT_GAME = "jackpotGame";
	public static final String PROPERTY_MINIMUM_JACKPOT_AMOUNT = "minimumJackpotAmount";
	public static final String PROPERTY_TARGET_JACKPOT_AMOUNT = "targetJackpotAmount";
	public static final String PROPERTY_TARGET_JACKPOT_CURRENCY = "targetJackpotCurrency";
	public static final String PROPERTY_TOURNAMENT_HANDICAP_STRUCTURE = "tournamentHandicapStructure";
	public static final String PROPERTY_TOURNAMENT_PAYOUT_STRUCTURE = "tournamentPayoutStructure";
	public static final String PROPERTY_TOURNAMENT_JACKPOT_CALCULATE_BY_ENTRY_FEE = "jackpotCalculateByEntryFee";

	// Winning component configuration properties
	public static final String PROPERTY_PAID_COMPONENT_STRUCTURE = "paidWinningComponentPayoutStructure";
	public static final String PROPERTY_FREE_COMPONENT_STRUCTURE = "freeWinningComponentPayoutStructure";

	// Special prize
	public static final String PROPERTY_SPECIAL_PRIZE = "specialPrize";
	public static final String PROPERTY_SPECIAL_PRIZE_CORRECT_ANSWERS_PERCENT = "specialPrizeCorrectAnswersPercent";
	public static final String PROPERTY_SPECIAL_PRIZE_VOUCHER_ID = "specialPrizeVoucherId";
	public static final String PROPERTY_SPECIAL_PRIZE_WINNING_RULE = "specialPrizeWinningRule";
	public static final String PROPERTY_SPECIAL_PRIZE_WINNING_RULE_AMOUNT = "specialPrizeWinningRuleAmount";
	
	public ResultConfiguration() {
		// Initialize with empty Json object
	}

	public ResultConfiguration(JsonObject resultConfiguration) {
		super(resultConfiguration);
	}

	public Boolean isPointCalculator() {
		return getPropertyAsBoolean(PROPERTY_POINT_CALCULATOR);
	}

	public Integer getAlternativeBonusPointsPerCorrectAnswer() {
		return getPropertyAsInteger(PROPERTY_ALTERNATIVE_BONUS_POINTS_PER_CORRECT_ANSWER);
	}

	public Integer getBonusPointsPerCorrectAnswerForUnpaid() {
		return getPropertyAsInteger(PROPERTY_BONUS_POINTS_PER_CORRECT_ANSWER_FOR_UNPAID);
	}

	public Integer getBonusPointsPerGamePointForPaid() {
		return getPropertyAsInteger(PROPERTY_BONUS_POINTS_PER_GAME_POINT_FOR_PAID);
	}

	public Boolean isTreatPaidLikeUnpaid() {
		return getPropertyAsBoolean(PROPERTY_TREAT_PAID_LIKE_UNPAID);
	}

	public Integer getBonusPointsForAllCorrectAnswers() {
		return getPropertyAsInteger(PROPERTY_BONUS_POINTS_FOR_ALL_CORRECT_ANSWERS);
	}
	
	public Integer getBonusPointsForQuickAnswer() {
		return getPropertyAsInteger(PROPERTY_BONUS_POINTS_FOR_QUICK_RESPONSE);
	}
	
	public Integer getBonusPointsForQuickAnswerMs() {
		return getPropertyAsInteger(PROPERTY_BONUS_POINTS_FOR_QUICK_RESPONSE_MS);
	}
	
	public CorrectAnswerPointCalculationType getCorrectAnswerPointCalculationType() {
		return CorrectAnswerPointCalculationType.fromString(getPropertyAsString(PROPERTY_CORRECT_ANSWER_POINT_CALCULATION_TYPE));
	}
	
	public Map<Integer, Integer> getCorrectAnswerQuestionComplexityGamePoints() {
		JsonArray complexityArray = getArray(PROPERTY_CORRECT_ANSWER_QUESTION_COMPLEXITY_GAME_POINTS);
		if (complexityArray == null) {
			return Collections.emptyMap();
		}
		Map<Integer, Integer> result = new HashMap<>();
		complexityArray.forEach(c -> {
			if (c != null && ! c.isJsonNull()) {
				JsonObject complexity = c.getAsJsonObject();
				result.put(complexity.get(PROPERTY_LEVEL).getAsInt(), 
						complexity.get(PROPERTY_POINTS).getAsInt());
			}
		});
		return result;
	}

	public DuelGroupWinnerPayoutType getDuelGroupWinnerPayoutType() {
		return DuelGroupWinnerPayoutType.fromString(getPropertyAsString(PROPERTY_DUEL_GROUP_WINNER_PAYOUT_TYPE));
	}
	
	public Double getDuelGroupWinnerPayoutFixedValueAmount() {
		return getPropertyAsDouble(PROPERTY_DUEL_GROUP_WINNER_PAYOUT_FIXED_VALUE_AMOUNT);
	}
	
	public Currency getDuelGroupWinnerPayoutFixedValueCurrency() {
		return getEnum(Currency.class, getPropertyAsString(PROPERTY_DUEL_GROUP_WINNER_PAYOUT_FIXED_VALUE_CURRENCY));
	}
	
	public Double getDuelGroupWinnerPayoutJackpotPercent() {
		return getPropertyAsDouble(PROPERTY_DUEL_GROUP_WINNER_PAYOUT_JACKPOT_PERCENT);
	}
	
	public GroupPrizePaidOutToType getDuelGroupPrizePayoutType() {
		return GroupPrizePaidOutToType.fromString(getPropertyAsString(PROPERTY_DUEL_GROUP_PRIZE_PAYOUT_TYPE));
	}

	public Integer getDuelGroupPrizePaidOutToEveryXWinner() {
		return getPropertyAsInteger(PROPERTY_DUEL_GROUP_PRIZE_PAID_OUT_TO_EVERY_X_WINNER);
	}

	public Boolean isJackpotGame() {
		Boolean jackpotGame = getPropertyAsBoolean(PROPERTY_JACKPOT_GAME);
		return jackpotGame == null ? false : jackpotGame;
	}

	public Boolean isJackpotCalculateByEntryFee(){
        Boolean jackpotCalculateByEntryFee = getPropertyAsBoolean(PROPERTY_TOURNAMENT_JACKPOT_CALCULATE_BY_ENTRY_FEE);
        return jackpotCalculateByEntryFee == null ? false : jackpotCalculateByEntryFee;
    }

	public BigDecimal getMinimumJackpotAmount() {
		return getPropertyAsBigDecimal(PROPERTY_MINIMUM_JACKPOT_AMOUNT);
	}
	
	public BigDecimal getTargetJackpotAmount() {
		return getPropertyAsBigDecimal(PROPERTY_TARGET_JACKPOT_AMOUNT);
	}
	
	public Currency getTargetJackpotCurrency() {
		return getEnum(Currency.class, getPropertyAsString(PROPERTY_TARGET_JACKPOT_CURRENCY));
	}

	public List<HandicapRange> getTournamentHandicapStructure() {
		JsonArray handicapArray = getArray(PROPERTY_TOURNAMENT_HANDICAP_STRUCTURE);
		if (handicapArray == null) {
			return Collections.emptyList();
		}
		List<HandicapRange> result = new ArrayList<>();
		handicapArray.forEach(l -> {
			if (l != null && ! l.isJsonNull()) {
				result.add(new HandicapRange(l.getAsJsonObject()));
			}
		});
		return result;
	}
	
	public List<Integer> getTournamentPayoutStructure() {
		JsonArray payoutArray = getArray(PROPERTY_TOURNAMENT_PAYOUT_STRUCTURE);
		if (payoutArray == null) {
			return Collections.emptyList();
		}
		List<Integer> result = new ArrayList<>(payoutArray.size());
		payoutArray.forEach(p -> {
			if (p != null && p.isJsonPrimitive()) {
				result.add(p.getAsInt());
			}
		});
		return result;
	}
	
	

	public boolean isSpecialPrizeEnabled() {
		Boolean result = getPropertyAsBoolean(PROPERTY_SPECIAL_PRIZE);
		return result == null ? false : result;
	}
	
	public int getSpecialPrizeCorrectAnswersPercent() {
		Integer result = getPropertyAsInteger(PROPERTY_SPECIAL_PRIZE_CORRECT_ANSWERS_PERCENT);
		return result == null ? 100 : result;
	}
	
	public SpecialPrizeWinningRule getSpecialPrizeWinningRule() {
		return SpecialPrizeWinningRule.fromString(getPropertyAsString(PROPERTY_SPECIAL_PRIZE_WINNING_RULE));
	}

	public Integer getSpecialPrizeWinningRuleAmount() {
		return getPropertyAsInteger(PROPERTY_SPECIAL_PRIZE_WINNING_RULE_AMOUNT);
	}
	
	public String getSpecialPrizeVoucherId() {
		return getPropertyAsString(PROPERTY_SPECIAL_PRIZE_VOUCHER_ID);
	}

}
