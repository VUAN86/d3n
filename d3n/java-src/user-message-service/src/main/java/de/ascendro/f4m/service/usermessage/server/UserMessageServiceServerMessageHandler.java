package de.ascendro.f4m.service.usermessage.server;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.ISOLanguage;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.usermessage.aws.AmazonServiceCoordinator;
import de.ascendro.f4m.service.usermessage.direct.DirectWebsocketMessageCoordinator;
import de.ascendro.f4m.service.usermessage.model.CancelUserPushRequest;
import de.ascendro.f4m.service.usermessage.model.ListUnreadWebsocketMessagesRequest;
import de.ascendro.f4m.service.usermessage.model.SendEmailWrapperRequest;
import de.ascendro.f4m.service.usermessage.model.SendSmsRequest;
import de.ascendro.f4m.service.usermessage.model.SendTopicPushRequest;
import de.ascendro.f4m.service.usermessage.model.SendUserPushRequest;
import de.ascendro.f4m.service.usermessage.model.SendUserPushResponse;
import de.ascendro.f4m.service.usermessage.model.SendWebsocketMessageRequest;
import de.ascendro.f4m.service.usermessage.model.SendWebsocketMessageToAllUserDevicesRequest;
import de.ascendro.f4m.server.onesignal.OneSignalCoordinator;
import de.ascendro.f4m.server.onesignal.model.OneSignalPushData;
import de.ascendro.f4m.service.usermessage.onesignal.PushNotificationTypeMessageMapper;
import de.ascendro.f4m.service.usermessage.translation.TranslatableMessage;
import de.ascendro.f4m.service.usermessage.translation.TranslationPlaceholderReplacer;
import de.ascendro.f4m.service.usermessage.translation.Translator;

/**
 * Handles all User Message Service calls, where User Message is server side.
 */
public class UserMessageServiceServerMessageHandler extends DefaultJsonMessageHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserMessageServiceServerMessageHandler.class);
	
	private DirectWebsocketMessageCoordinator directMessageCoordinator;
	private AmazonServiceCoordinator amazonServiceCoordinator;
	private OneSignalCoordinator pushNotificationCoordinator;
	private Translator translator;
	private CommonProfileAerospikeDao profileAerospikeDao;
	private TranslationPlaceholderReplacer placeholderReplacer;
	private PushNotificationTypeMessageMapper pushNotificationTypeMessageMapper;

	public UserMessageServiceServerMessageHandler(DirectWebsocketMessageCoordinator directMessageCoordinator,
			AmazonServiceCoordinator amazonServiceCoordinator,
			OneSignalCoordinator pushNotificationCoordinator,
		  	Translator translator,
		  	CommonProfileAerospikeDao profileAerospikeDao,
		 	TranslationPlaceholderReplacer placeholderReplacer,
			PushNotificationTypeMessageMapper pushNotificationTypeMessageMapper) {
		this.directMessageCoordinator = directMessageCoordinator;
		this.amazonServiceCoordinator = amazonServiceCoordinator;
		this.pushNotificationCoordinator = pushNotificationCoordinator;
		this.translator = translator;
		this.profileAerospikeDao = profileAerospikeDao;
		this.placeholderReplacer = placeholderReplacer;
		this.pushNotificationTypeMessageMapper = pushNotificationTypeMessageMapper;
	}

	@SuppressWarnings("unchecked")
	@Override
	public JsonMessageContent onUserMessage(JsonMessage<? extends JsonMessageContent> message) {
		final JsonMessageContent resultContent;

		final UserMessageMessageTypes type = message.getType(UserMessageMessageTypes.class);
		ClientInfo clientInfo = message.getClientInfo();

		if (type != null) {
			switch (type) {
			case SEND_EMAIL:
				clientInfo = verifyClientInfoExists(clientInfo);
				resultContent = amazonServiceCoordinator
						.sendEmail(((JsonMessage<SendEmailWrapperRequest>) message).getContent(), clientInfo);
				break;
			case SEND_SMS:
				clientInfo = verifyClientInfoExists(clientInfo);
				resultContent = amazonServiceCoordinator.sendSms(((JsonMessage<SendSmsRequest>) message).getContent(),
						clientInfo);
				break;
			case SEND_USER_PUSH:
				resultContent = onSendUserPushRequest(((JsonMessage<SendUserPushRequest>) message).getContent());
				break;
			case SEND_TOPIC_PUSH:
				resultContent = onSendTopicPushRequest(((JsonMessage<SendTopicPushRequest>) message).getContent());
				break;
			case SEND_WEBSOCKET_MESSAGE:
				JsonMessage<SendWebsocketMessageRequest> jsonMessage = (JsonMessage<SendWebsocketMessageRequest>) message;
				resultContent = directMessageCoordinator.sendDirectMessage(jsonMessage, getSessionWrapper());
				sendMobilePushWithWebsocketMessage(jsonMessage, clientInfo);
				break;
			case CANCEL_USER_PUSH:
				JsonMessage<CancelUserPushRequest> cancelJsonMessage = (JsonMessage<CancelUserPushRequest>) message;
				cancelUserPush(cancelJsonMessage);
				resultContent = new EmptyJsonMessageContent();
				break;
			case SEND_WEBSOCKET_MESSAGE_TO_ALL_USER_DEVICES:
				//XXX: SendWebsocketMessageToAllUserDevicesRequest is actually not used anywhere. Remove from API?
				resultContent = directMessageCoordinator.sendDirectMessageList(
						(JsonMessage<SendWebsocketMessageToAllUserDevicesRequest>) message, getSessionWrapper());
				break;
			case LIST_UNREAD_WEBSOCKET_MESSAGES:
				JsonMessage<ListUnreadWebsocketMessagesRequest> listRequest = (JsonMessage<ListUnreadWebsocketMessagesRequest>) message;
				resultContent = directMessageCoordinator.listUnreadMessage(listRequest);
				break;
			default:
				throw new F4MValidationFailedException("User Message Service message type[" + message.getName()
						+ "] not recognized");
			}
		} else {
			throw new F4MValidationFailedException("Unrecognized message " + message.getName());
		}
		return resultContent;
	}

	private void sendMobilePushWithWebsocketMessage(JsonMessage<SendWebsocketMessageRequest> message, ClientInfo clientInfo) {
		String appId = null;
		if (clientInfo == null || (appId = clientInfo.getAppId()) == null) {
			LOGGER.error("Unable to send push notification for [{}] due to missing clientInfo [{}] / appId [{}]", message.getContent(), clientInfo, appId);
			return;
		}
		SendWebsocketMessageRequest websocketMessage = message.getContent();
		SendUserPushRequest pushMessage = new SendUserPushRequest();
		pushMessage.setUserId(websocketMessage.getUserId());
		pushMessage.setMessage(websocketMessage.getMessage());
		pushMessage.setParameters(websocketMessage.getParameters());
		pushMessage.setType(websocketMessage.getType());
        pushMessage.setPayload(websocketMessage.getPayload());
		pushMessage.setAppIds(new String[] {clientInfo.getAppId()});
		OneSignalPushData data = prepareOneSignalPushData(pushMessage);
		pushNotificationCoordinator.sendMobilePush(data);
	}

	private void cancelUserPush(JsonMessage<CancelUserPushRequest> message) {
		CancelUserPushRequest websocketMessage = message.getContent();
		pushNotificationCoordinator.cancelMobilePush(websocketMessage.getMessageIds());
	}

	private ClientInfo verifyClientInfoExists(ClientInfo clientInfo) {
		if (clientInfo == null) {
			return new ClientInfo(); //create default client info for sending emails to admin from a background processes 
			//throw new F4MValidationFailedException("ClientInfo is mandatory");
		}
		return clientInfo;
	}

	private SendUserPushResponse onSendTopicPushRequest(SendTopicPushRequest message) {
		Map<ISOLanguage, String> messages = translator.translateInAllLanguages(prepareMessage(message));
		String[] appIds = calcAppIds(message.getAppIds());

		return convertMessageIdsToPushResponse(pushNotificationCoordinator.sendMobilePush(messages, appIds, message.getTopic()));
	}

	private SendUserPushResponse onSendUserPushRequest(SendUserPushRequest message) {
		LOGGER.debug("Received request to send push notification {}", message);
		OneSignalPushData data = prepareOneSignalPushData(message);
		return convertMessageIdsToPushResponse(pushNotificationCoordinator.sendScheduledMobilePush(data));
	}

	private OneSignalPushData prepareOneSignalPushData(SendUserPushRequest message) {
		Profile profile = profileAerospikeDao.getProfile(message.getUserId());
		Map<ISOLanguage, String> messages = Collections.emptyMap();
		if (pushNotificationTypeMessageMapper.isMessageEnabled(message.getMessage(), profile)) {
			messages = translator.translateInAllLanguages(message);
		}
		for (Map.Entry<ISOLanguage, String> entry : messages.entrySet()) {
			entry.setValue(placeholderReplacer.replaceProfilePlaceholders(entry.getValue(), profile));
		}
		String[] appIds = calcAppIds(message.getAppIds());

		String websocketMessageType = null;
		if (message.getType() != null) {
			websocketMessageType = message.getType().toString();
		}

		String payload = null;
		if (message.getPayload() != null) {
			payload = message.getPayload().toString();
		}
		return new OneSignalPushData(message.getUserId(), websocketMessageType,
				messages, appIds, payload, message.getScheduledDateTime());
	}

	private String[] calcAppIds(String[] appIds) {
		if (appIds == null || appIds.length == 0) {
			return new String[] {null};
		} else {
			return appIds;
		}
	}

	private TranslatableMessage prepareMessage(SendTopicPushRequest request) {
		TranslatableMessage msg = new TranslatableMessage();
		msg.setMessage(request.getMessage());
		msg.setParameters(request.getParameters());
		return msg;
	}

	private SendUserPushResponse convertMessageIdsToPushResponse(List<String> messageIds) {
		SendUserPushResponse f4mResponse = new SendUserPushResponse();
		f4mResponse.setNotificationIds(messageIds.toArray(new String[] {}));
		return f4mResponse;
	}
}
