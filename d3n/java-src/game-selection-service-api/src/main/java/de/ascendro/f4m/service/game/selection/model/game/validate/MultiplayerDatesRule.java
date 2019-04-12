package de.ascendro.f4m.service.game.selection.model.game.validate;

import java.time.Period;
import java.time.ZonedDateTime;

import org.apache.commons.lang3.ObjectUtils;

import de.ascendro.f4m.service.game.selection.exception.F4MCustomDatesNotValidException;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.multiplayer.MultiplayerGameParameters;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class MultiplayerDatesRule implements CustomParameterRule<MultiplayerGameParameters> {

	private static final int MAX_USER_LIVE_TOURNAMENT_DAYS = 7;
	private static final int MIN_USER_LIVE_TOURNAMENT_HOURS = 2; 
//	private static final int MIN_USER_LIVE_TOURNAMENT_MINUTES = 1; //FIXME: testing value
	
	private static final String NOT_CONFIGURABLE = "Start and end dates are not configurable; params [%s]";
	private static final String INVALID = "Start and end dates are not valid; params [%s]";
	private static final String PLAY_DATETIME_BEFORE_VALID_INTERVAL = "Play date time for user live tournament  is before interval, need to be at least 2 hours from current time; params [%s]";
	private static final String PLAY_DATETIME_AFTER_VALID_INTERVAL = "Play date time for user live tournament  is after interval, need to be at maximum 7 days from current time; params [%s]";

	@Override
	public void validate(MultiplayerGameParameters params, Game game) {
		if (ObjectUtils.allNotNull(params.getStartDateTime(), params.getEndDateTime()) || params.getStartDateTime() != null) {
			validateDates(params, game);
		} else if (params.getEndDateTime() != null) {
			throw new F4MCustomDatesNotValidException(String.format(INVALID, params));
		}
		
		if (GameType.USER_LIVE_TOURNAMENT.equals(game.getType())) {
			validateUserGameTournamentDates(params);
		}

	}

	private void validateDates(MultiplayerGameParameters params, Game game) {
		ZonedDateTime gameFrom = game.getStartDateTime();
		ZonedDateTime gameTo = game.getEndDateTime();
		
		ZonedDateTime configFrom = params.getStartDateTime();
		ZonedDateTime configTo = ObjectUtils.firstNonNull(params.getEndDateTime(), gameTo);
		
		if (game.isDuel()) {
			throw new F4MCustomDatesNotValidException(String.format(NOT_CONFIGURABLE, params));
		} else if (configFrom.isBefore(gameFrom) || configTo.isAfter(gameTo) || configFrom.isAfter(configTo) || configFrom.isEqual(configTo)) {
			throw new F4MCustomDatesNotValidException(String.format(INVALID, params));
		}
	}
	
	private void validateUserGameTournamentDates(MultiplayerGameParameters params) {
		if (params.getPlayDateTime() != null) {
			//FIXME: removed validatoin for test purposes
//			if (params.getPlayDateTime().isBefore(getMinDate()) ){
//				throw new F4MCustomDatesNotValidException(
//						String.format(PLAY_DATETIME_BEFORE_VALID_INTERVAL, params.getPlayDateTime().toString()));
//			}
//			if (params.getPlayDateTime().isAfter(getMaxDate()) ){
//				throw new F4MCustomDatesNotValidException(
//						String.format(PLAY_DATETIME_AFTER_VALID_INTERVAL, params.getPlayDateTime().toString()));
//			}
		} else {
			throw new F4MCustomDatesNotValidException("Play date time is mandatory for user live tournaments");
		}
	}
	
	private ZonedDateTime getMinDate(){
		//FIXME: for testing purposes
	    return DateTimeUtil.getCurrentDateTime().plusHours(MIN_USER_LIVE_TOURNAMENT_HOURS);
		
	}
	
	private ZonedDateTime getMaxDate(){
	    return DateTimeUtil.getCurrentDateTime().plus(Period.ofDays(MAX_USER_LIVE_TOURNAMENT_DAYS));
	}
}
