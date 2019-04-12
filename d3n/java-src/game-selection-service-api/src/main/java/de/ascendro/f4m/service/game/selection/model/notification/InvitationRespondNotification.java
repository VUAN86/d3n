package de.ascendro.f4m.service.game.selection.model.notification;

import de.ascendro.f4m.service.usermessage.model.WebsocketMessageType;

public class InvitationRespondNotification extends InvitationNotification {
	private boolean accept;
	private String inviteeId;

	public InvitationRespondNotification(String multiplayerGameInstanceId, String inviteeId, boolean accept,
			String inviterId) {
		super(multiplayerGameInstanceId, inviterId);
		this.accept = accept;
		this.inviteeId = inviteeId;
	}

	@Override
	public WebsocketMessageType getType() {
		return WebsocketMessageType.INVITATION_RESPOND_NOTIFICATION;
	}

	public boolean isAccept() {
		return accept;
	}

	public void setAccept(boolean accept) {
		this.accept = accept;
	}

	public String getInviteeId() {
		return inviteeId;
	}

	public void setInviteeId(String inviteeId) {
		this.inviteeId = inviteeId;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (accept ? 1231 : 1237);
		result = prime * result + ((inviteeId == null) ? 0 : inviteeId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof InvitationRespondNotification))
			return false;
		InvitationRespondNotification other = (InvitationRespondNotification) obj;
		if (accept != other.accept)
			return false;
		if (inviteeId == null) {
			if (other.inviteeId != null)
				return false;
		} else if (!inviteeId.equals(other.inviteeId))
			return false;
		return super.equals(obj);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder("InvitationRespondNotification [getMultiplayerGameInstanceId()=");
		builder.append(getMultiplayerGameInstanceId());
		builder.append(", getInviterId()=");
		builder.append(getInviterId());
		builder.append(", accept=");
		builder.append(accept);
		builder.append(", inviteeId=");
		builder.append(inviteeId);
		builder.append("]");
		return builder.toString();
	}

}
