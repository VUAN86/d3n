package de.ascendro.f4m.service.usermessage.model;

import static org.junit.Assert.*;

import org.junit.Test;

import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;

public class ModelMessageTypeNameTest {
	@Test
	public void testMessageTypeEnumNames() throws Exception {
		assertEquals("sendUserPush",
				UserMessageMessageTypes.SEND_USER_PUSH.getShortName());
		assertEquals("userMessage/sendUserPush",
				UserMessageMessageTypes.SEND_USER_PUSH.getMessageName());
		assertEquals("sendUserPushResponse",
				UserMessageMessageTypes.SEND_USER_PUSH_RESPONSE.getShortName());
		assertEquals("userMessage/sendUserPushResponse",
				UserMessageMessageTypes.SEND_USER_PUSH_RESPONSE.getMessageName());
	}
}
