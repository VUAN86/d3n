package de.ascendro.f4m.service.game.selection.model.game.validate;

import java.math.BigDecimal;
import java.util.List;

import de.ascendro.f4m.service.game.selection.model.game.GameEntryFeeValues;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import de.ascendro.f4m.service.game.selection.exception.F4MEntryFeeNotValidException;
import de.ascendro.f4m.service.game.selection.model.game.GameEntryFeeSettings;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.multiplayer.MultiplayerGameParameters;
import de.ascendro.f4m.service.payment.model.Currency;

public class MultiplayerFeeRule implements CustomParameterRule<MultiplayerGameParameters> {

	private static final String NOT_CONFIGURABLE = "Entry fee is not configurable; params [%s]";
	private static final String NOT_PROVIDED = "Entry fee amount and currency must be provided; params [%s]";
	private static final String INVALID = "Entry fee is not valid; min [%s], max [%s], step [%s], currency [%s]; params [%s]";
	private static final String INVALID_FIXED = "Entry fee is not valid; vales [%s], currency [%s]; params [%s]";

	@Override
	public void validate(MultiplayerGameParameters params, Game game) {
		if (ObjectUtils.anyNotNull(params.getEntryFeeAmount(), params.getEntryFeeCurrency())) {
			validateEntryFee(params, game);
		}
	}

	private void validateEntryFee(MultiplayerGameParameters params, Game game) {
		BigDecimal configAmount = params.getEntryFeeAmount();
		Currency configCurrency = params.getEntryFeeCurrency();

		if (!game.isEntryFeeDecidedByPlayer()) {
			throw new F4MEntryFeeNotValidException(String.format(NOT_CONFIGURABLE, params));
		} else if (!ObjectUtils.allNotNull(configAmount, configCurrency)) {
			throw new F4MEntryFeeNotValidException(String.format(NOT_PROVIDED, params));
		} else {
			if (StringUtils.equals(Game.ENTRY_FEE_TYPE_FIXED, game.getEntryFeeType())) {
				//Entry Fee Type FIXED
				CustomEntryFeeValues entryFee = getCustomEntryFeeValues(game.getEntryFeeValues(), configCurrency);
				if (!isValidEntryFeeValue(configAmount, entryFee.getValues())) {
					throw new F4MEntryFeeNotValidException(String.format(INVALID_FIXED, entryFee.toString(), configCurrency, params));
				}
			} else {
				//Entry Fee Type STEP
				CustomEntryFeeSettings entryFee = getCustomEntryFeeSettings(game.getEntryFeeSettings(), configCurrency);
				BigDecimal min = entryFee.getMin();
				BigDecimal max = entryFee.getMax();
				BigDecimal step = entryFee.getStep();
				if (!isValidEntryFeeAmount(configAmount, min, max, step)) {
					throw new F4MEntryFeeNotValidException(String.format(INVALID, min, max, step, configCurrency, params));
				}
			}
		}
	}


	protected boolean isValidEntryFeeValue(BigDecimal amount, List<BigDecimal> values) {
		return values.contains(amount);
	}

	protected boolean isValidEntryFeeAmount(BigDecimal amount, BigDecimal min, BigDecimal max, BigDecimal step) {
		boolean isBoundary = amount.compareTo(min) == 0 || amount.compareTo(max) == 0;
		boolean withinBoundaries = amount.compareTo(min) > 0 && amount.compareTo(max) < 0;

		boolean valid = false;
		if (isBoundary) {
			valid = true;
		} else if (withinBoundaries) {
			valid = isAmountValidByStep(amount, min, max, step);
		}
		return valid;
	}

	private boolean isAmountValidByStep(BigDecimal amount, BigDecimal min, BigDecimal max, BigDecimal step) {
		boolean valid = false;
		for (BigDecimal i = min; !valid && i.compareTo(max) <= 0; i = i.add(step)) {
			if (amount.compareTo(i) == 0) {
				valid = true;
			}
		}
		return valid;
	}

	private CustomEntryFeeSettings getCustomEntryFeeSettings(GameEntryFeeSettings entryFeeSettings, Currency currency) {
		CustomEntryFeeSettings customEntryFee;
		switch (currency) {
			case MONEY:
				customEntryFee = new CustomEntryFeeSettings(entryFeeSettings.getMoneyMin(), entryFeeSettings.getMoneyMax(),
						entryFeeSettings.getMoneyIncrement());
				break;
			case BONUS:
				customEntryFee = new CustomEntryFeeSettings(entryFeeSettings.getBonusMin(),
						entryFeeSettings.getBonusMax(), entryFeeSettings.getBonusIncrement());
				break;
			case CREDIT:
				customEntryFee = new CustomEntryFeeSettings(entryFeeSettings.getCreditsMin(),
						entryFeeSettings.getCreditsMax(), entryFeeSettings.getCreditsIncrement());
				break;
			default:
				throw new F4MEntryFeeNotValidException(String.format("Invalid currency [%s]", currency));
		}
		return customEntryFee;
	}

	private CustomEntryFeeValues getCustomEntryFeeValues(GameEntryFeeValues entryFeeValues, Currency currency) {
		CustomEntryFeeValues customEntryFee;
		switch (currency) {
			case MONEY:
				customEntryFee = new CustomEntryFeeValues(entryFeeValues.getMoney());
				break;
			case BONUS:
				customEntryFee = new CustomEntryFeeValues(entryFeeValues.getBonusPoints());
				break;
			case CREDIT:
				customEntryFee = new CustomEntryFeeValues(entryFeeValues.getCredits());
				break;
			default:
				throw new F4MEntryFeeNotValidException(String.format("Invalid currency [%s]", currency));
		}
		return customEntryFee;
	}

}
