package de.ascendro.f4m.service.usermessage.dao;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.aerospike.client.policy.WritePolicy;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;

import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.usermessage.config.UserMessageConfig;
import de.ascendro.f4m.service.usermessage.dao.model.UserMessage;

public class UserMessageAerospikeDao {
	public static final String USERMESSAGES_BIN_NAME = "usermessages";
	private static final Type USER_MESSAGE_LIST_TYPE = new TypeToken<List<UserMessage>>() {}.getType();

	private AerospikeDao aerospikeDao;
	private JsonMessageUtil jsonUtil;
	private Config config;
	private PrimaryKeyUtil<String> primaryKeyUtil;

	@Inject
	public UserMessageAerospikeDao(AerospikeDao aerospikeDao, JsonMessageUtil jsonUtil, Config config,
			PrimaryKeyUtil<String> primaryKeyUtil) {
		this.aerospikeDao = aerospikeDao;
		this.jsonUtil = jsonUtil;
		this.config = config;
		this.primaryKeyUtil = primaryKeyUtil;
	}
	
	public String storeNewDirectMessage(String userId, UserMessage userMessage, Integer timeout) {
		userMessage.setMessageId(primaryKeyUtil.generateId());
		aerospikeDao.createOrUpdateJson(getSet(), primaryKeyUtil.createPrimaryKey(userId), USERMESSAGES_BIN_NAME,
				(result, policy) -> update(result, policy, userMessage, timeout));
		return userMessage.getMessageId();
	}
	
	private String update(String readResult, WritePolicy writePolicy, UserMessage newUserMessage, Integer timeout) {
		if (timeout != null && timeout > 0) {
			writePolicy.expiration = timeout; 
		}
		List<UserMessage> list = getUserMessageListFromJsonString(readResult);
		list.add(newUserMessage);
		return convertUserMessagesToJsonString(list);
	}

	private String convertUserMessagesToJsonString(List<UserMessage> list) {
		JsonArray array = new JsonArray();
		for (UserMessage listMessage : list) {
			array.add(jsonUtil.toJsonElement(listMessage));
		}
		return array.toString();
	}

	private List<UserMessage> getUserMessageListFromJsonString(String contents) {
		List<UserMessage> list;
		if (StringUtils.isNotEmpty(contents)) {
			list = jsonUtil.fromJson(contents, USER_MESSAGE_LIST_TYPE);
		} else {
			list = new ArrayList<>(1);
		}
		return list;
	}
	
	protected String returnRemainingUnreadMessagesByOtherDevicesAndCalculateMessagesReadByThisCall(String messagesInDb,
			String messageId, String deviceUUID, List<UserMessage> returnMessages) {
		List<UserMessage> dbUserMessages = getUserMessageListFromJsonString(messagesInDb);
		List<UserMessage> messagesToStore = returnRemainingUnreadMessagesByOtherDevicesAndCalculateMessagesReadByThisCall(
				messageId, deviceUUID, returnMessages, dbUserMessages);
		return convertUserMessagesToJsonString(messagesToStore);
	}

	protected List<UserMessage> returnRemainingUnreadMessagesByOtherDevicesAndCalculateMessagesReadByThisCall(
			String messageId, String deviceUUID, List<UserMessage> returnMessages, List<UserMessage> dbUserMessages) {
		returnMessages.clear(); //clear list, if callback was called multiple times
		List<UserMessage> messagesToStore = new ArrayList<>();
		for (UserMessage userMessage : dbUserMessages) {
			if (messageId == null || messageId.equals(userMessage.getMessageId())) {
				if (userMessage.getUnreadDeviceUUID() != null) {
					calculateMarkingForMessageWithDeviceUUID(deviceUUID, returnMessages, messagesToStore, userMessage);
				} else {
					returnMessages.add(userMessage); // means - this was a message without deviceUUID - first comes gets it
				}
			} else {
				messagesToStore.add(userMessage);
			}
		}
		return messagesToStore;
	}

	private void calculateMarkingForMessageWithDeviceUUID(String deviceUUID, List<UserMessage> returnMessages, List<UserMessage> messagesToStore,
			UserMessage userMessage) {
		if (userMessage.getUnreadDeviceUUID().contains(deviceUUID)) {
			returnMessages.add(userMessage);
			userMessage.getUnreadDeviceUUID().remove(deviceUUID);
			if (!userMessage.getUnreadDeviceUUID().isEmpty()) {
				messagesToStore.add(userMessage);
			}
		} else {
			messagesToStore.add(userMessage);
		}
	}
	
	public List<UserMessage> getUserMessagesAndMarkRead(String userId, String messageId, String deviceUUID) {
		List<UserMessage> returnMessages = new ArrayList<>();

		aerospikeDao.createOrUpdateJson(getSet(), primaryKeyUtil.createPrimaryKey(userId), USERMESSAGES_BIN_NAME,
				(result, policy) -> returnRemainingUnreadMessagesByOtherDevicesAndCalculateMessagesReadByThisCall(
						result, messageId, deviceUUID, returnMessages));

		return returnMessages;
	}
	
	private String getSet() {
		//shouldn't this be in some common part / interface for all DAOs? 
		return config.getProperty(UserMessageConfig.AEROSPIKE_USERMESSAGE_SET);
	}
}
