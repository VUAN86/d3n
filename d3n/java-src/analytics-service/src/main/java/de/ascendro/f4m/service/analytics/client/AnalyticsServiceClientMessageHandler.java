package de.ascendro.f4m.service.analytics.client;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.server.analytics.model.RewardEvent;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.service.analytics.module.achievement.processor.AchievementsLoader;
import de.ascendro.f4m.service.analytics.notification.NotificationCommon;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.event.model.NotifySubscriberMessageContent;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.TransactionId;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.usermessage.model.SendEmailWrapperResponse;
import de.ascendro.f4m.service.usermessage.model.SendWebsocketMessageResponse;
import de.ascendro.f4m.service.usermessage.translation.Messages;
import de.ascendro.f4m.service.util.EventServiceClient;

public class AnalyticsServiceClientMessageHandler extends DefaultJsonMessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsServiceClientMessageHandler.class);
    
	private static final String SERVICE_NAME = "analytics";
	private static final String ACHIEVEMENTS_REFRESH_EVENT_TOPIC = "analytics.achievements.refresh";
	
	
    private final NotificationCommon notificationCommon;
    private final Tracker tracker;
	private final EventServiceClient eventServiceClient;
    private final AchievementsLoader achievementsLoader;

    public AnalyticsServiceClientMessageHandler(NotificationCommon notificationCommon,
                                                Tracker tracker, EventServiceClient eventServiceClient,
                                                AchievementsLoader achievementsLoader) {
        this.notificationCommon = notificationCommon;
        this.tracker = tracker;
		this.eventServiceClient = eventServiceClient;
		this.achievementsLoader = achievementsLoader;
    }

	@Override
	protected void onEventServiceRegistered() {
		eventServiceClient.subscribe(true, SERVICE_NAME, ACHIEVEMENTS_REFRESH_EVENT_TOPIC);
	}

    @Override
    public JsonMessageContent onUserMessage(RequestContext context) {
        JsonMessage<? extends JsonMessageContent> message = context.getMessage();
        RequestInfo originalRequestInfo = context.getOriginalRequestInfo();
		EventMessageTypes eventMessageType;
		if ((eventMessageType = message.getType(EventMessageTypes.class)) != null) {
			return onEventMessage(message, eventMessageType);
		} else if (originalRequestInfo != null) {
            if (PaymentMessageTypes.isTransferFundsResponseWithTransactionId(message.getType(PaymentMessageTypes.class))) {
                if (originalRequestInfo instanceof PaymentTransferRequestInfo) {
                	onPaymentTransfer(message, (PaymentTransferRequestInfo) originalRequestInfo);
                }
            } else if (message.getContent() instanceof SendEmailWrapperResponse) {
                // Nothing to do here
            } else if (message.getContent() instanceof SendWebsocketMessageResponse) {
                // Nothing to do here
            }
        } else {
            LOGGER.error("OriginalRequestInfo not found for {}", message.getContent());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
	private JsonMessageContent onEventMessage(JsonMessage<? extends JsonMessageContent> message,
			EventMessageTypes eventMessageType) {
    	if (eventMessageType == EventMessageTypes.NOTIFY_SUBSCRIBER) {
    		NotifySubscriberMessageContent messageContent = ((JsonMessage<NotifySubscriberMessageContent>) message).getContent();
	    	if (ACHIEVEMENTS_REFRESH_EVENT_TOPIC.equals(messageContent.getTopic())) {
	    		LOGGER.info("received event to trigger achievement mappings refresh.");
	    		achievementsLoader.loadTenants();
	    	} else {
	    		LOGGER.error("Unexpected message with topic {}", messageContent.getTopic());
			}
    	} else if (eventMessageType == EventMessageTypes.SUBSCRIBE_RESPONSE) {
			LOGGER.debug("Received SUBSCRIBE_RESPONSE message: {}", message);
		} else {
    		LOGGER.error("Unexpected message with type {}", eventMessageType);
    	}
    	return null;
	}

	private void onPaymentTransfer(JsonMessage<? extends JsonMessageContent> message, PaymentTransferRequestInfo originalRequestInfo) {
        TransactionId transactionId = (TransactionId) message.getContent();
        boolean isMonthlyInvitesPayoutRequest = originalRequestInfo instanceof MonthlyInvitesPayoutRequestInfo;
        if (StringUtils.isNotEmpty(transactionId.getTransactionId())) {
        	processPaymentSucceeded(originalRequestInfo);
        } else {
        	notifyPaymentError(isMonthlyInvitesPayoutRequest, originalRequestInfo);
        }
    }

    private void processPaymentSucceeded(PaymentTransferRequestInfo originalRequestInfo) {
        ClientInfo clientInfo = new ClientInfo();
        clientInfo.setTenantId(originalRequestInfo.getTenantId());
        clientInfo.setUserId(originalRequestInfo.getUserId());
        clientInfo.setAppId(originalRequestInfo.getAppId());

        if (originalRequestInfo instanceof MonthlyInvitesPayoutRequestInfo) {
            notificationCommon.pushMessageToUser(originalRequestInfo.getUserId(),
                    Messages.MONTHLY_INVITES_PRIZE_PAYOUT_PUSH, 
                    new String[]{
                            String.valueOf(((MonthlyInvitesPayoutRequestInfo) originalRequestInfo).getTargetInvites()),
                            originalRequestInfo.getAmount().setScale(2).toPlainString(),
                            originalRequestInfo.getCurrency().getFullName(),
                    }, 
                    clientInfo);
        } else if (originalRequestInfo instanceof BonusPayoutRequestInfo) {
            notificationCommon.pushMessageToUser(originalRequestInfo.getUserId(),
                    ((BonusPayoutRequestInfo) originalRequestInfo).isFullRegistration() ? Messages.REGISTRATION_BONUS_PRIZE_PAYOUT_PUSH_FULL : Messages.REGISTRATION_BONUS_PRIZE_PAYOUT_PUSH_INITIAL, 
                    new String[]{
                            originalRequestInfo.getAmount().setScale(0).toPlainString(),
                            originalRequestInfo.getCurrency().getFullName(),
                    }, 
                    clientInfo);
        }

        RewardEvent rewardEvent = new RewardEvent();
        if (Currency.CREDIT == originalRequestInfo.getCurrency()) {
            rewardEvent.setCreditWon(originalRequestInfo.getAmount().longValue());
        } else if (Currency.BONUS == originalRequestInfo.getCurrency()) {
            rewardEvent.setBonusPointsWon(originalRequestInfo.getAmount().longValue());
        }
        tracker.addEvent(clientInfo, rewardEvent);
	}

	private void notifyPaymentError(boolean isMonthlyInvitesPayoutRequest, PaymentTransferRequestInfo originalRequestInfo) {
        notificationCommon.sendEmailToAdmin(
        		isMonthlyInvitesPayoutRequest ? Messages.MONTHLY_INVITES_PRIZE_PAYOUT_ERROR_TO_ADMIN_SUBJECT : Messages.REGISTRATION_BONUS_PRIZE_PAYOUT_ERROR_TO_ADMIN_SUBJECT,
        		null,
                isMonthlyInvitesPayoutRequest ? Messages.MONTHLY_INVITES_PRIZE_PAYOUT_ERROR_TO_ADMIN_CONTENT : Messages.REGISTRATION_BONUS_PRIZE_PAYOUT_ERROR_TO_ADMIN_CONTENT,
        		new String[]{
                        Optional.ofNullable("Transaction ID empty on response").orElse("-"),
                        Optional.ofNullable(originalRequestInfo.getTransactionLogId()).orElse("-"),
                        originalRequestInfo.getAmount() != null ? originalRequestInfo.getAmount().setScale(2).toPlainString() : "-",
                        Optional.ofNullable(originalRequestInfo.getCurrency().name()).orElse("-"),
                        Optional.ofNullable(originalRequestInfo.getTenantId()).orElse("-"),
                        Optional.ofNullable(originalRequestInfo.getUserId()).orElse("-")
                });
	}

}
