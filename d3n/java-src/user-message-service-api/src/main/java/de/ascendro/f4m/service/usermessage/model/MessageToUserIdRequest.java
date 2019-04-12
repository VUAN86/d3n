package de.ascendro.f4m.service.usermessage.model;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public interface MessageToUserIdRequest extends JsonMessageContent {
	String getUserId();

	String getMessage();
}
