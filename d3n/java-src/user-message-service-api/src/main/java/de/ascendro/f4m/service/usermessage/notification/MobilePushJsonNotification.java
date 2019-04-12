package de.ascendro.f4m.service.usermessage.notification;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.usermessage.model.WebsocketMessageType;

/**
 * Base class for push notifications sending JSON content
 */
public abstract class MobilePushJsonNotification implements JsonMessageContent {

	public abstract WebsocketMessageType getType();

	//TODO: consider removing hashCode & equals since they are used only in tests
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getType() == null) ? 0 : getType().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof MobilePushJsonNotification))
			return false;
		MobilePushJsonNotification other = (MobilePushJsonNotification) obj;
		if (getType() == null) {
			if (other.getType() != null)
				return false;
		} else if (!getType().equals(other.getType()))
			return false;
		return true;
	}

}
