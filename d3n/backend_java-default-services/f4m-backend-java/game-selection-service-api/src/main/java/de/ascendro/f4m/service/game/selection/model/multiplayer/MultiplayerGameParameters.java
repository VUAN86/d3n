package de.ascendro.f4m.service.game.selection.model.multiplayer;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.ascendro.f4m.service.game.selection.model.game.EntryFee;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameParametersBase;
import de.ascendro.f4m.service.game.selection.model.game.validate.CustomParameterRule;
import de.ascendro.f4m.service.game.selection.model.game.validate.MultiplayerDatesRule;
import de.ascendro.f4m.service.game.selection.model.game.validate.MultiplayerFeeRule;
import de.ascendro.f4m.service.game.selection.model.game.validate.MultiplayerParticipantRule;
import de.ascendro.f4m.service.game.selection.model.game.validate.NumberOfQuestionsRule;
import de.ascendro.f4m.service.game.selection.model.game.validate.PoolsRule;
import de.ascendro.f4m.service.payment.model.Currency;

public class MultiplayerGameParameters extends GameParametersBase implements EntryFee {
	
	private String gameId;
	private Integer maxNumberOfParticipants;
	private BigDecimal entryFeeAmount;
	private Currency entryFeeCurrency;
	/**
	 * Multiplayer game visibility start date-time
	 */
	private ZonedDateTime startDateTime;
	/**
	 * Multiplayer game visibility end date-time
	 */
	private ZonedDateTime endDateTime;
	
	/**
	 * Expected date-time when multiplayer game going to be started (used for Live Tournaments)
	 */
	private ZonedDateTime playDateTime;

	public MultiplayerGameParameters() {
	}

	public MultiplayerGameParameters(String gameId) {
		this.gameId = gameId;
	}

	public String getGameId() {
		return gameId;
	}

	public void setGameId(String gameId) {
		this.gameId = gameId;
	}

	public Integer getMaxNumberOfParticipants() {
		return maxNumberOfParticipants;
	}

	public void setMaxNumberOfParticipants(Integer maxNumberOfParticipants) {
		this.maxNumberOfParticipants = maxNumberOfParticipants;
	}

	@Override
	public BigDecimal getEntryFeeAmount() {
		return entryFeeAmount;
	}

	public void setEntryFeeAmount(BigDecimal entryFeeAmount) {
		this.entryFeeAmount = entryFeeAmount;
	}

	@Override
	public Currency getEntryFeeCurrency() {
		return entryFeeCurrency;
	}

	public void setEntryFeeCurrency(Currency entryFeeCurrency) {
		this.entryFeeCurrency = entryFeeCurrency;
	}

	/**
	 * @return visibility start date-time
	 */
	public ZonedDateTime getStartDateTime() {
		return startDateTime;
	}

	/**
	 * @param startDateTime - visibility start date-time
	 */
	public void setStartDateTime(ZonedDateTime startDateTime) {
		this.startDateTime = startDateTime;
	}

	/**
	 * @return visibility end date-time
	 */
	public ZonedDateTime getEndDateTime() {
		return endDateTime;
	}

	/**
	 * @param endDateTime - visibility end date-time
	 */
	public void setEndDateTime(ZonedDateTime endDateTime) {
		this.endDateTime = endDateTime;
	}
	
	public ZonedDateTime getPlayDateTime() {
		return playDateTime;
	}
	
	public void setPlayDateTime(ZonedDateTime playDateTime) {
		this.playDateTime = playDateTime;
	}
	
	@Override
	public boolean isFree() {
		return entryFeeAmount == null || entryFeeCurrency == null;
	}
	
    /**
     * Validate parameters according to {@link Game} configuration
     * @param game Game
     */
    public void validate(Game game) {
        List<CustomParameterRule<? super MultiplayerGameParameters>> rules = new ArrayList<>();
        rules.add(new PoolsRule());
        rules.add(new NumberOfQuestionsRule());
        rules.add(new MultiplayerParticipantRule());
        rules.add(new MultiplayerFeeRule());
        rules.add(new MultiplayerDatesRule());
        
        rules.forEach(r -> r.validate(this, game));
    }

	@Override
	public String toString() {
		return "MultiplayerGameParameters [gameId=" +
				gameId +
				", maxNumberOfParticipants=" +
				maxNumberOfParticipants +
				", entryFeeAmount=" +
				entryFeeAmount +
				", entryFeeCurrency=" +
				entryFeeCurrency +
				", startDateTime=" +
				startDateTime +
				", endDateTime=" +
				endDateTime +
				", playDateTime=" +
				playDateTime +
				", getPoolIds()=" +
				Arrays.toString(getPoolIds()) +
				", getNumberOfQuestions()=" +
				getNumberOfQuestions() +
				"]";
	}

}
