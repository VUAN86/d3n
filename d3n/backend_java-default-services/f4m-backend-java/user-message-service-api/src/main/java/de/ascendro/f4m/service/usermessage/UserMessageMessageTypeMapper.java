package de.ascendro.f4m.service.usermessage;

import com.google.gson.reflect.TypeToken;

import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMapImpl;
import de.ascendro.f4m.service.usermessage.model.CancelUserPushRequest;
import de.ascendro.f4m.service.usermessage.model.ListUnreadWebsocketMessagesRequest;
import de.ascendro.f4m.service.usermessage.model.ListUnreadWebsocketMessagesResponse;
import de.ascendro.f4m.service.usermessage.model.NewWebsocketMessageRequest;
import de.ascendro.f4m.service.usermessage.model.NewWebsocketMessageResponse;
import de.ascendro.f4m.service.usermessage.model.SendEmailWrapperRequest;
import de.ascendro.f4m.service.usermessage.model.SendEmailWrapperResponse;
import de.ascendro.f4m.service.usermessage.model.SendUserPushResponse;
import de.ascendro.f4m.service.usermessage.model.SendSmsRequest;
import de.ascendro.f4m.service.usermessage.model.SendSmsResponse;
import de.ascendro.f4m.service.usermessage.model.SendTopicPushRequest;
import de.ascendro.f4m.service.usermessage.model.SendUserPushRequest;
import de.ascendro.f4m.service.usermessage.model.SendWebsocketMessageRequest;
import de.ascendro.f4m.service.usermessage.model.SendWebsocketMessageResponse;
import de.ascendro.f4m.service.usermessage.model.SendWebsocketMessageToAllUserDevicesRequest;
import de.ascendro.f4m.service.usermessage.model.SendWebsocketMessageToAllUserDevicesResponse;

public class UserMessageMessageTypeMapper extends JsonMessageTypeMapImpl {
	private static final long serialVersionUID = 6431097041746435463L;

	public UserMessageMessageTypeMapper() {
		init();
	}

	protected void init() {
		this.register(UserMessageMessageTypes.SEND_SMS, new TypeToken<SendSmsRequest>() {});
		this.register(UserMessageMessageTypes.SEND_SMS_RESPONSE, new TypeToken<SendSmsResponse>() {});
		this.register(UserMessageMessageTypes.SEND_EMAIL, new TypeToken<SendEmailWrapperRequest>() {});
		this.register(UserMessageMessageTypes.SEND_EMAIL_RESPONSE, new TypeToken<SendEmailWrapperResponse>() {});
		this.register(UserMessageMessageTypes.SEND_USER_PUSH,
				new TypeToken<SendUserPushRequest>() {});
		this.register(UserMessageMessageTypes.SEND_USER_PUSH_RESPONSE,
				new TypeToken<SendUserPushResponse>() {});
		this.register(UserMessageMessageTypes.SEND_TOPIC_PUSH, new TypeToken<SendTopicPushRequest>() {});
		this.register(UserMessageMessageTypes.SEND_TOPIC_PUSH_RESPONSE, new TypeToken<SendUserPushResponse>() {});
		this.register(UserMessageMessageTypes.SEND_WEBSOCKET_MESSAGE, new TypeToken<SendWebsocketMessageRequest>() {});
		this.register(UserMessageMessageTypes.SEND_WEBSOCKET_MESSAGE_RESPONSE, new TypeToken<SendWebsocketMessageResponse>() {});
		this.register(UserMessageMessageTypes.SEND_WEBSOCKET_MESSAGE_TO_ALL_USER_DEVICES,
				new TypeToken<SendWebsocketMessageToAllUserDevicesRequest>() {});
		this.register(UserMessageMessageTypes.SEND_WEBSOCKET_MESSAGE_TO_ALL_USER_DEVICES_RESPONSE,
				new TypeToken<SendWebsocketMessageToAllUserDevicesResponse>() {});
		this.register(UserMessageMessageTypes.LIST_UNREAD_WEBSOCKET_MESSAGES, new TypeToken<ListUnreadWebsocketMessagesRequest>() {});
		this.register(UserMessageMessageTypes.LIST_UNREAD_WEBSOCKET_MESSAGES_RESPONSE,
				new TypeToken<ListUnreadWebsocketMessagesResponse>() {});
		this.register(UserMessageMessageTypes.NEW_WEBSOCKET_MESSAGE, new TypeToken<NewWebsocketMessageRequest>() {});
		this.register(UserMessageMessageTypes.NEW_WEBSOCKET_MESSAGE_RESPONSE,
				new TypeToken<NewWebsocketMessageResponse>() {});
		this.register(UserMessageMessageTypes.CANCEL_USER_PUSH,
				new TypeToken<CancelUserPushRequest>() {});
		this.register(UserMessageMessageTypes.CANCEL_USER_PUSH_RESPONSE,
				new TypeToken<EmptyJsonMessageContent>() {});
	}
}
