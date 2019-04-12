package de.ascendro.f4m.service.game.engine.client.voucher;

import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.session.SessionWrapper;

public interface VoucherCommunicator {

	/**
	 * Request to reserve a voucher for later assignation.
	 */
	void requestUserVoucherReserve(String voucherId, JsonMessage<? extends JsonMessageContent> sourceMessage,
			SessionWrapper session);

	/**
	 * Request to release a voucher reservation.
	 */
	void requestUserVoucherRelease(String voucherId, JsonMessage<? extends JsonMessageContent> sourceMessage,
			SessionWrapper session);
}
