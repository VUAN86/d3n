package de.ascendro.f4m.service.usermessage.direct;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.usermessage.client.DependencyServicesCommunicator;
import de.ascendro.f4m.service.usermessage.dao.UserMessageAerospikeDao;
import de.ascendro.f4m.service.usermessage.dao.model.UserMessage;
import de.ascendro.f4m.service.usermessage.model.ListUnreadWebsocketMessagesRequest;
import de.ascendro.f4m.service.usermessage.model.ListUnreadWebsocketMessagesResponse;
import de.ascendro.f4m.service.usermessage.model.MessageInfo;
import de.ascendro.f4m.service.usermessage.model.NewWebsocketMessageRequest;
import de.ascendro.f4m.service.usermessage.model.SendWebsocketMessageRequest;
import de.ascendro.f4m.service.usermessage.model.SendWebsocketMessageResponse;
import de.ascendro.f4m.service.usermessage.model.SendWebsocketMessageToAllUserDevicesRequest;
import de.ascendro.f4m.service.usermessage.model.SendWebsocketMessageToAllUserDevicesResponse;
import de.ascendro.f4m.service.usermessage.translation.TranslationPlaceholderReplacer;
import de.ascendro.f4m.service.usermessage.translation.Translator;
import de.ascendro.f4m.service.util.F4MBeanUtils;

/**
 * Coordinates necessary interactions between Aerospike, Gateway and other parties related to direct websocket messages.
 */
public class DirectWebsocketMessageCoordinator {
	
	private UserMessageAerospikeDao userMessageAerospikeDao;
	private CommonProfileAerospikeDao profileAerospikeDao;
	private DependencyServicesCommunicator profileServiceCommunicator;
	private Translator translator;
	private TranslationPlaceholderReplacer placeholderReplacer;

	@Inject
	public DirectWebsocketMessageCoordinator(UserMessageAerospikeDao userMessageAerospikeDao,
			CommonProfileAerospikeDao profileAerospikeDao, DependencyServicesCommunicator profileServiceCommunicator,
			Translator translator, TranslationPlaceholderReplacer placeholderReplacer) {
		this.userMessageAerospikeDao = userMessageAerospikeDao;
		this.profileAerospikeDao = profileAerospikeDao;
		this.profileServiceCommunicator = profileServiceCommunicator;
		this.translator = translator;
		this.placeholderReplacer = placeholderReplacer;
	}
	
	public SendWebsocketMessageResponse sendDirectMessage(JsonMessage<SendWebsocketMessageRequest> message,
			SessionWrapper sessionWrapper) {
		Profile profile = null;
		if (message.getContent().isLanguageAuto()) {
			profile = profileAerospikeDao.getProfile(message.getContent().getUserId());
		}
		SendWebsocketMessageRequest translatedMessage = translate(message, profile);
		String messageId = storeDirectMessage(translatedMessage, null);
		SendWebsocketMessageResponse response = new SendWebsocketMessageResponse();
		response.setMessageId(messageId);
		profileServiceCommunicator.initiateDirectMessageSendingToUserViaGateway(translatedMessage, messageId, sessionWrapper);
		return response;
	}
	
	public SendWebsocketMessageToAllUserDevicesResponse sendDirectMessageList(
			JsonMessage<SendWebsocketMessageToAllUserDevicesRequest> message, SessionWrapper sessionWrapper) {
		SendWebsocketMessageToAllUserDevicesResponse response = new SendWebsocketMessageToAllUserDevicesResponse();
		List<String> unreadDeviceUUIDs = new ArrayList<>();
		Profile profile = profileAerospikeDao.getProfile(message.getContent().getUserId());
		for (JsonElement deviceJson : profile.getDevicesAsJsonArray()) {
			JsonObject deviceObject = (JsonObject)deviceJson;
			String deviceUUID = deviceObject.get(Profile.DEVICE_UUID_PROPERTY_NAME).getAsString();
			//last 3 lines actually are copy paste from UserMessageServiceClientMessageHandler.sendMobilePush... refactor somehow!
			unreadDeviceUUIDs.add(deviceUUID);
		}
		SendWebsocketMessageRequest translatedMessage = translate(message, profile);
		String messageId = storeDirectMessage(translatedMessage, unreadDeviceUUIDs);
		response.setMessageId(messageId);
		profileServiceCommunicator.initiateDirectMessageSendingToUserViaGateway(translatedMessage, messageId, sessionWrapper);
		return response;
	}
	
	private SendWebsocketMessageRequest translate(JsonMessage<? extends SendWebsocketMessageRequest> message, Profile profile) {
		SendWebsocketMessageRequest result = new SendWebsocketMessageRequest();
		SendWebsocketMessageRequest source = message.getContent();
		F4MBeanUtils.copyProperties(result, source);
		if (source.isLanguageAuto()) {
			result.setLanguageAuto();
		} else {
			result.setISOLanguage(source.getISOLanguage());
		}
		translator.updateLanguageFromProfile(profile, result);
		//No placeholder replacing - in future users might be allowed to send messages. If placeholders are allowed here, a separate call without placeholders should be provided to users. 
		String translatedString = translator.translate(result);
		translatedString = placeholderReplacer.replaceProfilePlaceholders(translatedString, profile);
		result.setMessage(translatedString);
		return result;
	}

	private String storeDirectMessage(SendWebsocketMessageRequest request, List<String> unreadDeviceUUIDs) {
		String userId = request.getUserId();
		UserMessage userMessage = new UserMessage();
		userMessage.setMessage(request.getMessage());
		userMessage.setUnreadDeviceUUID(unreadDeviceUUIDs);
		userMessage.setType(request.getType());
		userMessage.setPayload(request.getPayload());
		return userMessageAerospikeDao.storeNewDirectMessage(userId, userMessage, request.getTimeout());
	}
	
	public void markMessageRead(NewWebsocketMessageRequest request, String deviceUUID) {
		userMessageAerospikeDao.getUserMessagesAndMarkRead(request.getUserId(), request.getMessageId(), deviceUUID);
	}
	
	public ListUnreadWebsocketMessagesResponse listUnreadMessage(JsonMessage<ListUnreadWebsocketMessagesRequest> message) {
		ListUnreadWebsocketMessagesResponse response = new ListUnreadWebsocketMessagesResponse();
		String userId = message.getContent().getUserId();
		String deviceUUID = message.getContent().getDeviceUUID();
		List<UserMessage> userMessages = userMessageAerospikeDao.getUserMessagesAndMarkRead(userId, null, deviceUUID);
		response.setMessages(new ArrayList<>(userMessages.size()));
		for (UserMessage userMessage : userMessages) {
			MessageInfo info = MessageInfoUtil.copyMessageInfo(userMessage, new MessageInfo());
			response.getMessages().add(info);
		}
		return response;
	}
}