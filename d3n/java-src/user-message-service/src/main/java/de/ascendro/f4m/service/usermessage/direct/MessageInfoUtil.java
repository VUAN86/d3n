package de.ascendro.f4m.service.usermessage.direct;

import de.ascendro.f4m.service.usermessage.model.MessageInfo;
import de.ascendro.f4m.service.usermessage.model.SendWebsocketMessageRequest;

public class MessageInfoUtil {
	
	private MessageInfoUtil() {
		//empty default
	}
	
	public static MessageInfo copyMessageInfo(MessageInfo from, MessageInfo to) {
		to.setMessage(from.getMessage());
		to.setMessageId(from.getMessageId());
		to.setPayload(from.getPayload());
		to.setType(from.getType());
		return to;
	}
	
	public static <T extends MessageInfo> T copyMessageInfo(SendWebsocketMessageRequest from, T to) {
		to.setMessage(from.getMessage());
		to.setPayload(from.getPayload());
		to.setType(from.getType());
		return to;
	}
	
}
