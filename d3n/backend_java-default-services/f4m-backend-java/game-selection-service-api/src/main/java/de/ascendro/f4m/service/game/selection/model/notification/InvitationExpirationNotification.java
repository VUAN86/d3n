package de.ascendro.f4m.service.game.selection.model.notification;

import de.ascendro.f4m.service.usermessage.model.WebsocketMessageType;

public class InvitationExpirationNotification extends InvitationNotification {
	
	private long expirationTimeMillis;

	public InvitationExpirationNotification(String multiplayerGameInstanceId, String inviterId,
			long expirationTimeMillis) {
		super(multiplayerGameInstanceId, inviterId);
		this.expirationTimeMillis = expirationTimeMillis;
	}
	
	@Override
	public WebsocketMessageType getType() {
		return WebsocketMessageType.INVITATION_EXPIRATION_NOTIFICATION;
	}

	public long getExpirationTimeMillis() {
		return expirationTimeMillis;
	}

	public void setExpirationTimeMillis(long expirationTimeMillis) {
		this.expirationTimeMillis = expirationTimeMillis;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (int) (expirationTimeMillis ^ (expirationTimeMillis >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		InvitationExpirationNotification other = (InvitationExpirationNotification) obj;
		if (expirationTimeMillis != other.expirationTimeMillis)
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder("InvitationRespondNotification [getMultiplayerGameInstanceId()=");
		builder.append(getMultiplayerGameInstanceId());
		builder.append(", getInviterId()=");
		builder.append(getInviterId());
		builder.append(", expirationTimeMillis=");
		builder.append(expirationTimeMillis);
		builder.append("]");
		return builder.toString();
	}

}
