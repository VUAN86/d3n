package de.ascendro.f4m.service.game.selection.client.communicator;

import javax.inject.Inject;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.friend.FriendManagerMessageTypes;
import de.ascendro.f4m.service.friend.model.api.BuddyRelationType;
import de.ascendro.f4m.service.friend.model.api.buddy.BuddyAddForUserRequest;
import de.ascendro.f4m.service.friend.model.api.buddy.BuddyListAllIdsRequest;
import de.ascendro.f4m.service.friend.model.api.group.GroupListPlayersRequest;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.InviteGroupToGameRequest;
import de.ascendro.f4m.service.game.selection.request.InviteRequestInfoImpl;
import de.ascendro.f4m.service.game.selection.request.RequestInfoWithUserIdImpl;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.MessageSource;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;

/**
 * Class for sending requests to other services
 *
 */
public class FriendManagerCommunicator {

	private final ServiceRegistryClient serviceRegistryClient;
	private final JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;
	private final JsonMessageUtil jsonMessageUtil;

	@Inject
	public FriendManagerCommunicator(ServiceRegistryClient serviceRegistryClient,
			JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool, JsonMessageUtil jsonMessageUtil) {
		this.serviceRegistryClient = serviceRegistryClient;
		this.jsonWebSocketClientSessionPool = jsonWebSocketClientSessionPool;
		this.jsonMessageUtil = jsonMessageUtil;
	}

	public void sendGetFriendsRequest(JsonMessage<? extends JsonMessageContent> originalMessage, String userId,
			SessionWrapper sessionWrapper) {
		BuddyListAllIdsRequest requestContent = new BuddyListAllIdsRequest(userId, 
				new BuddyRelationType[] { BuddyRelationType.BUDDY }, 
				new BuddyRelationType[] { BuddyRelationType.BLOCKED });
		JsonMessage<BuddyListAllIdsRequest> message = jsonMessageUtil.createNewMessage(FriendManagerMessageTypes.BUDDY_LIST_ALL_IDS,
				requestContent);

		RequestInfoWithUserIdImpl requestInfo = new RequestInfoWithUserIdImpl(originalMessage, sessionWrapper, userId);

		sendMessage(message, requestInfo);
	}

	public void requestGroupListPlayers(JsonMessage<InviteGroupToGameRequest> sourceMessage,
			Object sourceSession, String mgiId, String gameInstanceId) {
		GroupListPlayersRequest requestContent = new GroupListPlayersRequest(sourceMessage.getContent().getGroupId());
		requestContent.setExcludeGroupOwner(true);
		JsonMessage<GroupListPlayersRequest> message = jsonMessageUtil
				.createNewMessage(FriendManagerMessageTypes.GROUP_LIST_PLAYERS, requestContent);
		
		InviteRequestInfoImpl requestInfo = new InviteRequestInfoImpl(sourceMessage, (MessageSource) sourceSession, mgiId);
		requestInfo.setGameInstanceId(gameInstanceId);
		
		sendMessage(message, requestInfo);
	}
	
	public void requestAddBuddyForUser(InviteRequestInfoImpl requestInfo, String inviterUserId, String userId) {
		if (inviterUserId != null) {
			BuddyAddForUserRequest buddyAddRequest = new BuddyAddForUserRequest(inviterUserId, null, userId);
			JsonMessage<BuddyAddForUserRequest> message = jsonMessageUtil
					.createNewMessage(FriendManagerMessageTypes.BUDDY_ADD_FOR_USER, buddyAddRequest);
			sendMessage(message, requestInfo);
		}
	}

	private void sendMessage(JsonMessage<?> message, RequestInfo requestInfo) {
		try {
			ServiceConnectionInformation friendManagerConnInfo = serviceRegistryClient
					.getServiceConnectionInformation(FriendManagerMessageTypes.SERVICE_NAME);
			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(friendManagerConnInfo, message, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send " + message.getContent().getClass().getSimpleName() + " to Friend Manager Service", e);
		}
	}

}
