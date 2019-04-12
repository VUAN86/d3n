package de.ascendro.f4m.service.payment.notification;

import java.io.Serializable;

import de.ascendro.f4m.service.usermessage.model.WebsocketMessageType;
import de.ascendro.f4m.service.usermessage.notification.MobilePushJsonNotification;

public class IdentificationCallbackMessagePayload extends MobilePushJsonNotification implements Serializable {
	private static final long serialVersionUID = 8484833720603336272L;
	
	private transient WebsocketMessageType type; //won't be serialized to JSON

	public IdentificationCallbackMessagePayload(WebsocketMessageType type) {
		this.type = type;
	}

	@Override
	public WebsocketMessageType getType() {
		return type;
	}

}
