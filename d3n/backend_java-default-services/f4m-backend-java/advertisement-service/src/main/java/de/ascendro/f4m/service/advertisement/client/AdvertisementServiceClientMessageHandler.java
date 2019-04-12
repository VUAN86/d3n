package de.ascendro.f4m.service.advertisement.client;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.google.inject.Inject;

import de.ascendro.f4m.server.analytics.model.RewardEvent;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.winning.CommonUserWinningAerospikeDao;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.model.TransactionId;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.winning.model.UserWinning;
import de.ascendro.f4m.service.winning.model.UserWinningType;

public class AdvertisementServiceClientMessageHandler extends DefaultJsonMessageHandler {

    private static final String UNEXPECTED_MESSAGE_TYPE = "Unexpected message type";
    private static final String UNRECOGNIZED_MESSAGE_TYPE = "Unrecognized message type";
    private static final String ERROR_EMAIL_SUBJECT = "Advertisement reward payment error";
    private static final String ERROR_EMAIL_TEXT = "The following error occurred when assigning a reward for advertisement: {0}\\nFyber advertisement details:{1}\\n";

    private static final String WINNING_NAME = "Fyber advertisement payout";
    private final DependencyServicesCommunicator dependencyServicesCommunicator;
    private final CommonUserWinningAerospikeDao userWinningDao;
    private final Tracker tracker;

    @Inject
    public AdvertisementServiceClientMessageHandler(DependencyServicesCommunicator dependencyServicesCommunicator,
                                                    Tracker tracker,
                                                    CommonUserWinningAerospikeDao userWinningDao){
        this.dependencyServicesCommunicator = dependencyServicesCommunicator;
        this.tracker = tracker;
        this.userWinningDao = userWinningDao;
    }

    @Override
    public JsonMessageContent onUserMessage(RequestContext context) {

        JsonMessage<? extends JsonMessageContent> message = context.getMessage();
        RequestInfo originalRequestInfo = context.getOriginalRequestInfo();
        if (PaymentMessageTypes.isTransferFundsResponseWithTransactionId(message.getType(PaymentMessageTypes.class)) &&
                originalRequestInfo != null) {
            if (originalRequestInfo instanceof RewardPayoutRequestInfo) {
                processReceivedTransactionIdForRewardPayout(message, context);
            } else {
                throw new F4MValidationFailedException(UNEXPECTED_MESSAGE_TYPE);
            }
        } else {
            throw new F4MValidationFailedException(UNRECOGNIZED_MESSAGE_TYPE);
        }
        return null;
    }

    @Override
    public void onUserErrorMessage(RequestContext context) {
        try{
            RequestInfo originalRequestInfo = context.getOriginalRequestInfo();
            JsonMessage<? extends JsonMessageContent> originalMessageDecoded = context.getMessage();
            if(PaymentMessageTypes.isTransferFundsResponseWithTransactionId(
                    originalMessageDecoded.getType(PaymentMessageTypes.class)) &&
                originalRequestInfo instanceof RewardPayoutRequestInfo) {
                    onReceivedTransactionIdWithErrorForRewardPayout(context.getOriginalRequestInfo(),
                            originalMessageDecoded.getError().getCode());
            }
        } finally {
            super.onUserErrorMessage(context);
        }
    }

    private void processReceivedTransactionIdForRewardPayout(JsonMessage<? extends JsonMessageContent> message,
                                                            RequestContext context) {
        RewardPayoutRequestInfo originalRequestInfo = context.getOriginalRequestInfo();

        TransactionId transactionId = (TransactionId) message.getContent();
        if (StringUtils.isNotEmpty(transactionId.getTransactionId())) {
            processRewardPayoutReceivedTransactionId(message, originalRequestInfo);
        } else {
            onReceivedTransactionIdWithErrorForRewardPayout(context.getOriginalRequestInfo(),
                    "Transaction ID empty on response");
        }
    }

    private void processRewardPayoutReceivedTransactionId(JsonMessage<? extends JsonMessageContent> message,
                                                         RewardPayoutRequestInfo originalRequestInfo) {
        UserWinning userWinning = new UserWinning(WINNING_NAME, UserWinningType.AFFILIATE, originalRequestInfo.getAmount(), originalRequestInfo.getCurrency(),
                originalRequestInfo.getFyberTransactionId(), originalRequestInfo.getImageId(), null);
        userWinningDao.saveUserWinning(originalRequestInfo.getAppId(), originalRequestInfo.getUserId(), userWinning);

        RewardEvent rewardEvent = new RewardEvent();
        rewardEvent.setPrizeWon(originalRequestInfo.getAmount(), originalRequestInfo.getCurrency());
        ClientInfo clientInfo = Optional.ofNullable(message.getClientInfo()).orElse(new ClientInfo());
        clientInfo.setTenantId(originalRequestInfo.getTenantId());
        clientInfo.setUserId(originalRequestInfo.getUserId());
        tracker.addEvent(clientInfo, rewardEvent);
    }

    public void onReceivedTransactionIdWithErrorForRewardPayout(RewardPayoutRequestInfo prizePayoutRequestInfo, String errorMessage) {
        dependencyServicesCommunicator.sendEmailToAdmin(ERROR_EMAIL_SUBJECT, null,
                ERROR_EMAIL_TEXT, new String[] {errorMessage, prizePayoutRequestInfo.toString()});
    }
}
