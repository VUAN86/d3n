package de.ascendro.f4m.service.payment.client;

import de.ascendro.f4m.service.auth.AuthMessageTypes;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.event.model.NotifySubscriberMessageContent;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.handler.JsonAuthenticationMessageMQHandler;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.exception.F4MTentantNotFoundException;
import de.ascendro.f4m.service.payment.manager.PaymentManager;
import de.ascendro.f4m.service.payment.manager.UserAccountManager;
import de.ascendro.f4m.service.payment.model.config.MergeUsersRequest;
import de.ascendro.f4m.service.profile.ProfileMessageTypes;
import de.ascendro.f4m.service.profile.model.merge.ProfileMergeEvent;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.util.EventServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class PaymentServiceClientMessageHandler extends JsonAuthenticationMessageMQHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(PaymentServiceClientMessageHandler.class);

//	private final EventServiceClient eventServiceClient;
	private PaymentManager paymentManager;
	private UserAccountManager userAccountManager;
	private AdminEmailForwarder adminEmailForwarder;

	public PaymentServiceClientMessageHandler(//EventServiceClient eventServiceClient,
											  PaymentManager paymentManager,
			UserAccountManager userAccountManager, AdminEmailForwarder adminEmailForwarder) {
//		this.eventServiceClient = eventServiceClient;
		this.paymentManager = paymentManager;
		this.userAccountManager = userAccountManager;
		this.adminEmailForwarder = adminEmailForwarder;
	}

	@Override
	public JsonMessageContent onUserMessage(JsonMessage<? extends JsonMessageContent> message) {
		EventMessageTypes eventServiceType;
		if ((eventServiceType  = message.getType(EventMessageTypes.class)) != null) {
			onEventServiceMessage(message, eventServiceType );
		} else if (message.getType(ProfileMessageTypes.class) != null) {
			LOGGER.debug("Received a success response from profile service {}", message);
		} else if (message.getType(AuthMessageTypes.class) != null) {
			LOGGER.debug("Received a success response from auth service {}", message);
		} else if (message.getType(UserMessageMessageTypes.class) != null) {
			LOGGER.debug("Received a success response from user message service {}", message);
		}  else if (message.getType(PaymentMessageTypes.class) != null) {
			LOGGER.debug("Received a success response from payment message service {}", message);
		} else {
			throw new F4MValidationFailedException("Unrecognized message " + message.getName());
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private void onEventServiceMessage(JsonMessage<? extends JsonMessageContent> message, EventMessageTypes type) {
		if (type == EventMessageTypes.NOTIFY_SUBSCRIBER) {
			onNotifyMessage((JsonMessage<NotifySubscriberMessageContent>) message);
		} else if (type == EventMessageTypes.SUBSCRIBE_RESPONSE) {
			LOGGER.debug("Received SUBSCRIBE_RESPONSE message: {}", message);
		} else {
			LOGGER.error("Incorrect message type {}", type);
		}
	}

	private void onNotifyMessage(JsonMessage<NotifySubscriberMessageContent> message) {
		NotifySubscriberMessageContent notifySubscriberMessageContent = message.getContent();
		if (ProfileMergeEvent.PROFILE_MERGE_EVENT_TOPIC.equals(notifySubscriberMessageContent.getTopic())) {
			ProfileMergeEvent event = new ProfileMergeEvent(
					notifySubscriberMessageContent.getNotificationContent().getAsJsonObject());

			String sourceUserId = event.getSourceUserId();
			String targetUserId = event.getTargetUserId();

			Set<String> tenantIds = userAccountManager.getProfileTenants(sourceUserId);
			if (tenantIds == null) {
				tenantIds = userAccountManager.getProfileTenants(targetUserId);
			}

			if (tenantIds != null) {
				tenantIds.stream().forEach(tenantId -> mergeProfilesByTenant(sourceUserId, targetUserId, tenantId, message));
			} else {
				LOGGER.warn("Cannot merge users. No one tenant defined for both users: {}, {}",sourceUserId, targetUserId);
			}
		} else {
			LOGGER.error("Incorrect message topic: {}", notifySubscriberMessageContent.getTopic());
		}
	}

	private void mergeProfilesByTenant(String sourceUserId, String targetUserId, String tenantId, JsonMessage<NotifySubscriberMessageContent> message) {
		try {
			paymentManager.mergeUserAccounts(new MergeUsersRequest(tenantId, sourceUserId, targetUserId));
		} catch (Exception e) {
			String details = "Profile merge error. From sourceUser " + sourceUserId + " to targetUser " + targetUserId
					+ ", tenant " + tenantId;
			LOGGER.error(details, e);
			try {
				if (shouldForwardToAdmin(e)) { //forward to admin also client-related errors
					adminEmailForwarder.forwardErrorToAdmin(message, e, details);
				}
			} catch (Exception ef) {
				LOGGER.error("Could not forward error to admin via email", ef);
			}
		}
	}

	private boolean shouldForwardToAdmin(Exception e) {
		//There are a lot of known cases, when profiles contain tentantIds which are not defined in Payment system.
		//So don't send emails to admin about this known issue.
		return !(e instanceof F4MTentantNotFoundException);
	}

//	@Override
//	protected void onEventServiceRegistered() {
//		eventServiceClient.subscribe(true, PaymentMessageTypes.SERVICE_NAME,
//	TODO			ProfileMergeEvent.PROFILE_MERGE_EVENT_TOPIC);
//	}

}
