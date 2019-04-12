package de.ascendro.f4m.service.usermessage.dao;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.ascendro.f4m.service.usermessage.dao.model.UserMessage;

public class UserMessageAerospikeDaoTest {
	private UserMessageAerospikeDao userMessageAerospikeDao;
	private final String messageIdX = "X";
	private final String messageIdY = "Y";
	private final String deviceUuid = "uuid1";
	private final String deviceUuidOther = "uuidOther";
	private List<UserMessage> messagesToBeShownToUser;
	private List<UserMessage> messagesAlreadyInDB;

	@Before
	public void setUp() {
		userMessageAerospikeDao = new UserMessageAerospikeDao(null, null, null, null);
		messagesToBeShownToUser = new ArrayList<>();
		messagesAlreadyInDB = new ArrayList<>();
	}

	private void prepareTwoMessagesWithBothDeviceUuids() {
		messagesAlreadyInDB.add(buildUserMessage(messageIdX, deviceUuidOther, deviceUuid));
		messagesAlreadyInDB.add(buildUserMessage(messageIdY, deviceUuid, deviceUuidOther));
	}

	@Test
	public void testMessageMarkingAsReadWithDeviceUUIDsByMessageId() throws Exception {
		prepareTwoMessagesWithBothDeviceUuids();
		// checks marking one message, used for gateway/sendUserMessage
		List<UserMessage> messagesToBeSavedBackToDB = userMessageAerospikeDao
				.returnRemainingUnreadMessagesByOtherDevicesAndCalculateMessagesReadByThisCall(messageIdX, deviceUuid,
						messagesToBeShownToUser, messagesAlreadyInDB);
		assertEquals(2, messagesToBeSavedBackToDB.size());
		UserMessage dbMessage = messagesToBeSavedBackToDB.get(0);
		assertEquals(1, dbMessage.getUnreadDeviceUUID().size());
		assertEquals(deviceUuidOther, dbMessage.getUnreadDeviceUUID().get(0));
		dbMessage = messagesToBeSavedBackToDB.get(1);
		assertEquals(2, dbMessage.getUnreadDeviceUUID().size());
		
		assertEquals(1, messagesToBeShownToUser.size());
		assertEquals(messageIdX, messagesToBeShownToUser.get(0).getMessageId());
	}

	@Test
	public void testMessageMarkingAsReadWithDeviceUUIDsWithoutMessageId() throws Exception {
		prepareTwoMessagesWithBothDeviceUuids();
		// checks marking the whole list, used for userMessage/listUnreadWebsocketMessages
		List<UserMessage> messagesToBeSavedBackToDB = userMessageAerospikeDao
				.returnRemainingUnreadMessagesByOtherDevicesAndCalculateMessagesReadByThisCall(null, deviceUuid,
						messagesToBeShownToUser, messagesAlreadyInDB);
		assertEquals(2, messagesToBeSavedBackToDB.size());
		for (int i = 0; i < messagesToBeSavedBackToDB.size(); i++) {
			UserMessage dbMessage = messagesToBeSavedBackToDB.get(i);
			assertEquals("Unexpected size at " + i, 1, dbMessage.getUnreadDeviceUUID().size());
			assertEquals("Unexpected uuid at " + i, deviceUuidOther, dbMessage.getUnreadDeviceUUID().get(0));
		}
		
		assertEquals(2, messagesToBeShownToUser.size());
		assertEquals(messageIdX, messagesToBeShownToUser.get(0).getMessageId());
		assertEquals(messageIdY, messagesToBeShownToUser.get(1).getMessageId());
	}

	@Test
	public void testMessageMarkingAsReadForFirstComesGetsIt() throws Exception {
		messagesAlreadyInDB.add(buildUserMessageWithoutDeviceUUIDs(messageIdX));
		messagesAlreadyInDB.add(buildUserMessageWithoutDeviceUUIDs(messageIdY));

		checkAllMessagesMarkedAsRead();

		checkOnlyYNotRead();
	}

	private void checkOnlyYNotRead() {
		List<UserMessage> messagesToBeSavedBackToDB;
		messagesToBeSavedBackToDB = userMessageAerospikeDao
				.returnRemainingUnreadMessagesByOtherDevicesAndCalculateMessagesReadByThisCall(messageIdX, deviceUuid,
						messagesToBeShownToUser, messagesAlreadyInDB);
		assertEquals(1, messagesToBeSavedBackToDB.size());
		assertEquals(messageIdY, messagesToBeSavedBackToDB.get(0).getMessageId());
	}

	@Test
	public void testMessageMarkingAsReadForMessagesForSingleAndAnyDevice() throws Exception {
		messagesAlreadyInDB.add(buildUserMessageWithoutDeviceUUIDs(messageIdX));
		messagesAlreadyInDB.add(buildUserMessage(messageIdY, deviceUuid));

		checkAllMessagesMarkedAsRead();

		checkOnlyYNotRead();
	}

	private void checkAllMessagesMarkedAsRead() {
		List<UserMessage> messagesToBeSavedBackToDB = userMessageAerospikeDao
				.returnRemainingUnreadMessagesByOtherDevicesAndCalculateMessagesReadByThisCall(null, deviceUuid,
						messagesToBeShownToUser, messagesAlreadyInDB);
		assertEquals(0, messagesToBeSavedBackToDB.size());
	}
	// testu, ka ar 1 deviceUUID sho izmet aaraa

	private UserMessage buildUserMessageWithoutDeviceUUIDs(String messageId) {
		UserMessage message = new UserMessage();
		message.setMessageId(messageId);
		return message;
	}

	private UserMessage buildUserMessage(String messageId, String... unreadDeviceUUID) {
		UserMessage message = buildUserMessageWithoutDeviceUUIDs(messageId);
		message.setUnreadDeviceUUID(new ArrayList<>(Arrays.asList(unreadDeviceUUID)));
		return message;
	}
}
