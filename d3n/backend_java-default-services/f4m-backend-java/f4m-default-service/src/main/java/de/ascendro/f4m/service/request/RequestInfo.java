package de.ascendro.f4m.service.request;

import de.ascendro.f4m.service.cache.CachedWithTTL;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.session.SessionWrapper;

public interface RequestInfo extends CachedWithTTL {
	void setSourceSession(SessionWrapper sourceSession);

	SessionWrapper getSourceSession();

	JsonMessage<? extends JsonMessageContent> getSourceMessage();

	public void setSourceMessage(JsonMessage<? extends JsonMessageContent> sourceMessage);

}
