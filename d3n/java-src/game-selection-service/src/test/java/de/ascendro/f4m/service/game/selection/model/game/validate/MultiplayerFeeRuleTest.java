package de.ascendro.f4m.service.game.selection.model.game.validate;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;

import org.junit.Test;

public class MultiplayerFeeRuleTest {

	MultiplayerFeeRule rule = new MultiplayerFeeRule();

	@Test
	public void testIsValidEntryFeeAmount() {
		assertTrue(isValidEntryFeeAmount("1.60", "1.60", "2.00", "0.20"));
		assertTrue(isValidEntryFeeAmount("1.60", "1.00", "1.60", "0.20"));
		assertTrue(isValidEntryFeeAmount("1.60", "1.00", "2.00", "0.20"));
		assertTrue(isValidEntryFeeAmount("1.66", "1.00", "2.00", "0.33"));

		assertFalse(isValidEntryFeeAmount("0.80", "1.00", "2.00", "0.20"));
		assertFalse(isValidEntryFeeAmount("2.10", "1.00", "2.00", "0.20"));
		assertFalse(isValidEntryFeeAmount("1.50", "1.00", "2.00", "0.20"));
	}

	private boolean isValidEntryFeeAmount(String actual, String min, String max, String step) {
		return rule.isValidEntryFeeAmount(new BigDecimal(actual), new BigDecimal(min), new BigDecimal(max), new BigDecimal(step));
	}

}
