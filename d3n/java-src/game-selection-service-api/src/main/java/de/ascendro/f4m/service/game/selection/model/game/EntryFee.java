package de.ascendro.f4m.service.game.selection.model.game;

import java.math.BigDecimal;

import de.ascendro.f4m.service.payment.model.Currency;

public interface EntryFee {
	BigDecimal getEntryFeeAmount();

	Currency getEntryFeeCurrency();

	boolean isFree();
}
