package de.ascendro.f4m.service.friend.notification;

import java.io.Serializable;

import de.ascendro.f4m.service.usermessage.model.WebsocketMessageType;
import de.ascendro.f4m.service.usermessage.notification.MobilePushJsonNotification;

public class PlayerAddedToGroupNotification extends MobilePushJsonNotification implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5577135973248699012L;
	
	private String tenantId;
	private String userId;
	private String groupId;

	public PlayerAddedToGroupNotification(String tenantId, String userId, String groupId) {
		super();
		this.tenantId = tenantId;
		this.userId = userId;
		this.groupId = groupId;
	}

	@Override
	public WebsocketMessageType getType() {
		return WebsocketMessageType.PLAYER_ADDED_TO_GROUP;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder("PlayerAddedToGroupNotification [tenantId=");
		builder.append(tenantId);
		builder.append(", userId=");
		builder.append(userId);
		builder.append(", groupId=");
		builder.append(groupId);
		builder.append("]");
		return builder.toString();
	}

}
