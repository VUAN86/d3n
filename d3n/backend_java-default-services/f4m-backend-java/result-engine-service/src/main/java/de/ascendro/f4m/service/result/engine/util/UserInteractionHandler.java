package de.ascendro.f4m.service.result.engine.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import de.ascendro.f4m.server.result.ResultItem;
import de.ascendro.f4m.server.result.ResultType;
import de.ascendro.f4m.server.result.Results;
import de.ascendro.f4m.server.result.UserInteractionType;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.selection.model.game.ResultConfiguration;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.result.engine.client.ServiceCommunicator;
import de.ascendro.f4m.service.session.SessionWrapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.math.BigDecimal;

public class UserInteractionHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(UserInteractionHandler.class);

	private final ServiceCommunicator serviceCommunicator;
	private final ResultEngineUtil resultEngineUtil;
	
	@Inject
	public UserInteractionHandler(ServiceCommunicator serviceCommunicator, ResultEngineUtil resultEngineUtil) {
		this.serviceCommunicator = serviceCommunicator;
		this.resultEngineUtil = resultEngineUtil;
	}
	
	/**
	 * @return <code>true</code> if we have to wait for answer from other service
	 */
	public boolean invokeUserInteractions(Results results, JsonMessage<? extends JsonMessageContent> request, SessionWrapper session) {
		if (results.getUserInteractions().contains(UserInteractionType.SPECIAL_PRIZE)) {
			// At the moment there is no interaction in UI for actually using special price, so we always assume that user responds with "yes"
			return respondToUserInteraction(UserInteractionType.SPECIAL_PRIZE, results, new JsonPrimitive(true), request, session); 
		} else if (results.getUserInteractions().contains(UserInteractionType.HANDICAP_ADJUSTMENT)) {
			// At the moment there is no interaction in UI for including handicap in user profile, so we always assume that user responds with "yes"
			return respondToUserInteraction(UserInteractionType.HANDICAP_ADJUSTMENT, results, new JsonPrimitive(true), request, session);
		} else if (results.getUserInteractions().contains(UserInteractionType.BONUS_POINT_TRANSFER)) {
			// This is a fake user interaction for getting bonus point totals, so we always continue
			return respondToUserInteraction(UserInteractionType.BONUS_POINT_TRANSFER, results, null, request, session);
		} else if (results.getUserInteractions().contains(UserInteractionType.BONUS_POINT_ENQUIRY)) {
			// This is a fake user interaction for getting bonus point totals, so we always continue
			return respondToUserInteraction(UserInteractionType.BONUS_POINT_ENQUIRY, results, null, request, session);
		}

		return false;
	}

	/**
	 * @return Should we wait for another message to finish.
	 */
	public boolean respondToUserInteraction(UserInteractionType type, Results results, JsonElement response, 
			JsonMessage<? extends JsonMessageContent> request, SessionWrapper session) {
		switch (type) {
		case SPECIAL_PRIZE:
			return onSpecialPrize(type, results, response, request, session);
		case HANDICAP_ADJUSTMENT:
	    	return onHandicapAdjustment(type, results, response, request, session);
		case BONUS_POINT_TRANSFER:
			return onBonusPointTransfer(results, request, session);
		case BONUS_POINT_ENQUIRY:
			return onBonusPointEnquiry(results, request, session);
		case BUY_WINNING_COMPONENT:
			return onBuyWinningComponent(results);
		default:
			throw new F4MValidationFailedException("Unknown interaction type [" + type + "]");
		}
	}

	public boolean releaseUserVoucher(GameInstance gameInstance, Results results, JsonMessage<? extends JsonMessageContent> request,
			SessionWrapper session) {
		boolean waitForOtherServiceResponse = false;
		ResultConfiguration resultConfiguration = gameInstance.getGame().getResultConfiguration();
		if (resultConfiguration.isSpecialPrizeEnabled() &&
				StringUtils.isNotBlank(resultConfiguration.getSpecialPrizeVoucherId()) &&
				!results.getUserInteractions().contains(UserInteractionType.SPECIAL_PRIZE)) {
			serviceCommunicator.requestUserVoucherRelease(gameInstance, request, session);
			waitForOtherServiceResponse = true;
		}
		return waitForOtherServiceResponse;
	}

	private boolean onSpecialPrize(UserInteractionType type, Results results, JsonElement response,
			JsonMessage<? extends JsonMessageContent> request, SessionWrapper session) {
		if (retrieveBooleanResponseValue(type, response)) {
			Validate.notBlank(results.getSpecialPrizeVoucherId(), "Results do not have a special prize voucher");
			serviceCommunicator.requestUserVoucherAssign(results, request, session);
			return true; // We have to wait for answer from voucher service
		}
		return false;
	}

	private boolean onHandicapAdjustment(UserInteractionType type, Results results, JsonElement response,
			JsonMessage<? extends JsonMessageContent> request, SessionWrapper session) {
		if (retrieveBooleanResponseValue(type, response)) {
			Validate.notNull(results.getNewUserHandicap(), "Results do not have a new handicap value");
			serviceCommunicator.requestUpdateProfileHandicap(results, request, session);
			return true; // We have to wait for answer from profile service
		}
		return false;
	}

	private boolean onBonusPointTransfer(Results results, JsonMessage<? extends JsonMessageContent> request, SessionWrapper session) {
		LOGGER.debug("onBonusPointTransfer results {} ", results); // 1
		ResultItem bonusPointsResult = results.getResultItems().get(ResultType.BONUS_POINTS);
		if (bonusPointsResult == null) {
			resultEngineUtil.removeUserInteraction(results.getGameInstanceId(), UserInteractionType.BONUS_POINT_TRANSFER);
			return false;
		}
		int bonusPoints = (int) bonusPointsResult.getAmount();
		// Initiate payout
		serviceCommunicator.requestSinglePlayerGamePaymentTransfer(results.getUserId(), results.getGameId(), results.getMultiplayerGameInstanceId(), results
				.getGameInstanceId(), BigDecimal.valueOf(bonusPoints), Currency.BONUS, request, session);
		return true;
	}

	private boolean onBonusPointEnquiry(Results results, JsonMessage<? extends JsonMessageContent> request,
			SessionWrapper session) {
		serviceCommunicator.requestTotalBonusPoints(results, request, session);
		return true;
	}

	private boolean onBuyWinningComponent(Results results) {
		LOGGER.debug("onBuyWinningComponent results {} ", results); // 0
		// Simply remove the interaction - at this point it should be already handled by winning service.
		resultEngineUtil.removeUserInteraction(results.getGameInstanceId(), UserInteractionType.BUY_WINNING_COMPONENT);
		return false;
	}

	private boolean retrieveBooleanResponseValue(UserInteractionType type, JsonElement response) {
		Boolean result = response == null || ! response.isJsonPrimitive() || ! (response.getAsJsonPrimitive().isBoolean()) ? null : response.getAsBoolean();
		Validate.notNull(result, "Response to [" + type + "] has to be specified as boolean");
		return result;
	}

}
