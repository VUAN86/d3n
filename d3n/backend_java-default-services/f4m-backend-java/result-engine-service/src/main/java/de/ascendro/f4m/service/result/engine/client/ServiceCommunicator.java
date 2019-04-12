package de.ascendro.f4m.service.result.engine.client;

import java.math.BigDecimal;

import de.ascendro.f4m.server.result.Results;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.result.engine.model.notification.GameEndNotification;
import de.ascendro.f4m.service.result.engine.util.UserResultsByHandicapRange;
import de.ascendro.f4m.service.session.SessionWrapper;

public interface ServiceCommunicator {

	/**
	 * Request to update user profile
	 */
	void requestUpdateProfileHandicap(Results results, JsonMessage<? extends JsonMessageContent> sourceMessage, SessionWrapper session);

	/**
	 * Request to send mobile push to all devices.
	 * @param clientInfo 
	 */
	void pushMessageToUser(String userId, GameEndNotification gameEndNotification, ClientInfo clientInfo);

	/**
	 * Request to add voucher to profile.
	 */
	void requestUserVoucherAssign(Results results, JsonMessage<? extends JsonMessageContent> sourceMessage, SessionWrapper session);

	/**
	 * Request to release voucher.
	 */
	void requestUserVoucherRelease(GameInstance gameInstance, JsonMessage<? extends JsonMessageContent> sourceMessage, SessionWrapper session);

	/**
	 * Request payout of the winnings.
	 * 
	 * @param userId
	 * @param gameId
	 * @param multiplayerGameInstanceId
	 * @param gameInstanceId
	 * @param amount
	 * @param currency
	 */
	void requestSinglePlayerGamePaymentTransfer(String userId, String gameId, String multiplayerGameInstanceId, String gameInstanceId, 
            BigDecimal amount, Currency currency, JsonMessage<? extends JsonMessageContent> sourceMessage, SessionWrapper session);
	
	/**
	 * Request payout of the winnings or refund.
	 */
	void requestMultiplayerGamePaymentTransfer(UserResultsByHandicapRange results, boolean isTournament);

	/**
	 * Request total bonus point enquiry.
	 */
	void requestTotalBonusPoints(Results results, JsonMessage<? extends JsonMessageContent> sourceMessage, SessionWrapper session);
	
}
