package de.ascendro.f4m.service.friend.client;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.server.analytics.model.InviteEvent;
import de.ascendro.f4m.server.analytics.model.RewardEvent;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.service.auth.AuthMessageTypes;
import de.ascendro.f4m.service.auth.model.register.InviteUserByEmailResponse;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.event.model.NotifySubscriberMessageContent;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.friend.BuddyManager;
import de.ascendro.f4m.service.friend.FriendManagerMessageTypes;
import de.ascendro.f4m.service.friend.config.request.InviteRequestInfoImpl;
import de.ascendro.f4m.service.friend.model.Contact;
import de.ascendro.f4m.service.friend.model.api.AppRatedResponse;
import de.ascendro.f4m.service.friend.model.api.AppSharedResponse;
import de.ascendro.f4m.service.friend.model.api.DailyLoginResponse;
import de.ascendro.f4m.service.friend.model.api.contact.ContactInviteNewResponse;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.model.TransactionId;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.merge.ProfileMergeEvent;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.EventServiceClient;

public class FriendManagerServiceClientMessageHandler extends DefaultJsonMessageHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(FriendManagerServiceClientMessageHandler.class);
	
	private final BuddyManager buddyManager;
	private final EventServiceClient eventServiceClient;
	private final CommonProfileAerospikeDao profileDao;
	private final Tracker tracker;
    private final  CommonProfileAerospikeDao commonProfileAerospikeDao;

	public FriendManagerServiceClientMessageHandler(BuddyManager buddyManager, EventServiceClient eventServiceClient,
			CommonProfileAerospikeDao profileDao, Tracker tracker,
                                                    CommonProfileAerospikeDao commonProfileAerospikeDao) {
		this.buddyManager = buddyManager;
		this.eventServiceClient = eventServiceClient;
		this.profileDao = profileDao;
		this.tracker = tracker;
		this.commonProfileAerospikeDao = commonProfileAerospikeDao;
	}

	@Override
	protected void onEventServiceRegistered() {
		eventServiceClient.subscribe(true, FriendManagerMessageTypes.SERVICE_NAME, ProfileMergeEvent.PROFILE_MERGE_EVENT_TOPIC);
	}

	@Override
	public JsonMessageContent onUserMessage(RequestContext context) {
		JsonMessage<? extends JsonMessageContent> message = context.getMessage();
		RequestInfo originalRequestInfo = context.getOriginalRequestInfo();
		AuthMessageTypes authMessageType;
		EventMessageTypes eventMessageType;
		if ((eventMessageType = message.getType(EventMessageTypes.class)) != null) {
			return onEventMessage(message, eventMessageType);
		} else if ((authMessageType = message.getType(AuthMessageTypes.class)) != null) {
			onAuthMessage(authMessageType, context);
		} else if (message.getType(UserMessageMessageTypes.class) != null) {
			LOGGER.debug("Received a success response from user message service {}", message);
		} else if(PaymentMessageTypes.isTransferFundsResponseWithTransactionId(
				message.getType(PaymentMessageTypes.class))) {
			if (originalRequestInfo instanceof UserActionPayoutRequestInfo) {
				processReceivedTransactionIdForUserActionPayout(message, context);
			}
		}
		return null;
	}

    private void processReceivedTransactionIdForUserActionPayout(JsonMessage<? extends JsonMessageContent> message,
                                                                 RequestContext context) {
        UserActionPayoutRequestInfo originalRequestInfo = context.getOriginalRequestInfo();

        TransactionId transactionId = (TransactionId) message.getContent();
        if (StringUtils.isNotEmpty(transactionId.getTransactionId())) {
            processUserActionPayoutReceivedTransactionId(message, originalRequestInfo);
        }
    }

    private void processUserActionPayoutReceivedTransactionId(JsonMessage<? extends JsonMessageContent> message,
			UserActionPayoutRequestInfo originalRequestInfo) {
        RewardEvent rewardEvent = new RewardEvent();
		rewardEvent.setPrizeWon(originalRequestInfo.getAmount(), originalRequestInfo.getCurrency());
        ClientInfo clientInfo = new ClientInfo();
        clientInfo.setTenantId(originalRequestInfo.getTenantId());
        clientInfo.setUserId(originalRequestInfo.getUserId());
        clientInfo.setAppId(originalRequestInfo.getAppId());
        tracker.addEvent(clientInfo, rewardEvent);
        JsonMessageContent response;
		FriendManagerMessageTypes originalMessageType = originalRequestInfo.getSourceMessage()
				.getType(FriendManagerMessageTypes.class);
		if (FriendManagerMessageTypes.DAILY_LOGIN.equals(originalMessageType)) {
            commonProfileAerospikeDao.setLastLoginTimestampInBlob(originalRequestInfo.getUserId(),
					originalRequestInfo.getAppId(), DateTimeUtil.getCurrentTimestampInISOFormat());
            response = new DailyLoginResponse(originalRequestInfo.getAmount(), originalRequestInfo.getCurrency(), false);
        } else if (FriendManagerMessageTypes.APP_RATED.equals(originalMessageType)) {
			response = new AppRatedResponse(originalRequestInfo.getAmount(), originalRequestInfo.getCurrency());
		} else if (FriendManagerMessageTypes.APP_SHARED.equals(originalMessageType)) {
			response = new AppSharedResponse(originalRequestInfo.getAmount(), originalRequestInfo.getCurrency());
		} else  {
			throw new F4MValidationFailedException("Unrecognized message type " + message.getTypeName());
		}
		JsonMessage<? extends JsonMessageContent> original = originalRequestInfo.getSourceMessage();
		this.sendResponse(original, response, originalRequestInfo.getSourceSession());
	}

    private void onAuthMessage(AuthMessageTypes messageType, RequestContext context) {
		if (AuthMessageTypes.INVITE_USER_BY_EMAIL_RESPONSE == messageType) {
			onInviteUserByEmail(context);
		} else {
			throw new F4MValidationFailedException("Unsupported message type[" + messageType + "]");
		}
	}

	private void onInviteUserByEmail(RequestContext context) {
		final InviteUserByEmailResponse inviteUserByEmailResponse = (InviteUserByEmailResponse) context.getMessage().getContent();
		final InviteRequestInfoImpl requestInfo = (InviteRequestInfoImpl) context.getOriginalRequestInfo();
		final ClientInfo clientInfo = requestInfo.getSourceMessage().getClientInfo();

		InviteEvent inviteEvent = new InviteEvent();
		inviteEvent.setFriendsInvited(1);
		inviteEvent.setBonusInvite(true);
		tracker.addEvent(clientInfo, inviteEvent);

		String invitedUserId = inviteUserByEmailResponse.getUserId();
		Contact contact = buddyManager.getContact(clientInfo.getUserId(), requestInfo.getContactId());
		contact.setUserId(invitedUserId);
		contact.setSentInvitationTextAndGroup(clientInfo.getAppId(), requestInfo.getInvitationText(), requestInfo.getGroupId());

		buddyManager.createOrUpdateContact(contact);
		buddyManager.addBuddies(clientInfo.getUserId(), clientInfo.getTenantId(), null, false, invitedUserId);
		if (StringUtils.isNotBlank(requestInfo.getGroupId())) {
			buddyManager.addPlayersToGroup(clientInfo.getUserId(), clientInfo.getTenantId(), 
					requestInfo.getGroupId(), new String[] { invitedUserId }, clientInfo);
		}
		
		ContactInviteNewResponse response = new ContactInviteNewResponse(invitedUserId);
		this.sendResponse(requestInfo.getSourceMessage(), response, requestInfo.getSourceSession());
	}

	private JsonMessageContent onEventMessage(JsonMessage<? extends JsonMessageContent> message,
			EventMessageTypes eventMessageType) {
		if (eventMessageType == EventMessageTypes.NOTIFY_SUBSCRIBER) {
			onNotifyMessage((NotifySubscriberMessageContent) message.getContent());
		} else if (eventMessageType == EventMessageTypes.SUBSCRIBE_RESPONSE) {
			LOGGER.debug("Received SUBSCRIBE_RESPONSE message: {}", message);
		} else {
			LOGGER.error("Unexpected message with type {}", eventMessageType);
		}
		return null;
	}

	private void onNotifyMessage(NotifySubscriberMessageContent notifySubscriberMessageContent) {
		if (ProfileMergeEvent.PROFILE_MERGE_EVENT_TOPIC.equals(notifySubscriberMessageContent.getTopic())) {
			ProfileMergeEvent event = new ProfileMergeEvent(notifySubscriberMessageContent.getNotificationContent().getAsJsonObject());
			Profile sourceUser = profileDao.getProfile(event.getSourceUserId());
			Profile targetUser = profileDao.getProfile(event.getTargetUserId());
			Collection<String> tenantIds = (sourceUser == null ? targetUser : sourceUser).getTenants();
			buddyManager.moveBuddies(event.getSourceUserId(), event.getTargetUserId());
			buddyManager.moveContacts(event.getSourceUserId(), targetUser, tenantIds);
			buddyManager.moveGroups(event.getSourceUserId(), targetUser, tenantIds);
		} else {
			LOGGER.error("Unexpected message with topic {}", notifySubscriberMessageContent.getTopic());
		}
	}

}
