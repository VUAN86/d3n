package de.ascendro.f4m.service.game.selection.model.notification;

import de.ascendro.f4m.service.usermessage.model.WebsocketMessageType;
import de.ascendro.f4m.service.usermessage.notification.MobilePushJsonNotification;

public class InvitationNotification extends MobilePushJsonNotification {
	private String multiplayerGameInstanceId;
	private String inviterId;

	public InvitationNotification(String multiplayerGameInstanceId, String inviterId) {
		this.multiplayerGameInstanceId = multiplayerGameInstanceId;
		this.inviterId = inviterId;
	}
	
	@Override
	public WebsocketMessageType getType() {
		return WebsocketMessageType.INVITATION_NOTIFICATION;
	}

	public String getMultiplayerGameInstanceId() {
		return multiplayerGameInstanceId;
	}

	public void setMultiplayerGameInstanceId(String multiplayerGameInstanceId) {
		this.multiplayerGameInstanceId = multiplayerGameInstanceId;
	}

	public String getInviterId() {
		return inviterId;
	}

	public void setInviterId(String inviterId) {
		this.inviterId = inviterId;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((inviterId == null) ? 0 : inviterId.hashCode());
		result = prime * result + ((multiplayerGameInstanceId == null) ? 0 : multiplayerGameInstanceId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InvitationNotification other = (InvitationNotification) obj;
		if (inviterId == null) {
			if (other.inviterId != null)
				return false;
		} else if (!inviterId.equals(other.inviterId))
			return false;
		if (multiplayerGameInstanceId == null) {
			if (other.multiplayerGameInstanceId != null)
				return false;
		} else if (!multiplayerGameInstanceId.equals(other.multiplayerGameInstanceId))
			return false;
		return super.equals(obj);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder("InvitationNotification [multiplayerGameInstanceId=");
		builder.append(multiplayerGameInstanceId);
		builder.append(", inviterId=");
		builder.append(inviterId);
		builder.append("]");
		return builder.toString();
	}
}
