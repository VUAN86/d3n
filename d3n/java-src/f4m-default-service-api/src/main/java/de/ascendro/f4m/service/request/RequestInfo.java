package de.ascendro.f4m.service.request;

import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.MessageSource;

public interface RequestInfo {
	default void setSourceSession(Object sourceSession) {
        System.out.println("It will be deleted");
    };

	Object getSourceSession();

	void setSourceMessageSource(MessageSource sourceMessageSource);

	MessageSource getSourceMessageSource();

	JsonMessage<? extends JsonMessageContent> getSourceMessage();

	public void setSourceMessage(JsonMessage<? extends JsonMessageContent> sourceMessage);

}
