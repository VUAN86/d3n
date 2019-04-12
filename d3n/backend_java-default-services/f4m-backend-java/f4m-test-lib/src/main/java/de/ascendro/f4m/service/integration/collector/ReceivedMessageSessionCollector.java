package de.ascendro.f4m.service.integration.collector;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.session.SessionWrapper;

public class ReceivedMessageSessionCollector extends DefaultJsonMessageHandler {

	private Set<SessionWrapper> receivedMessageSessionSet = new CopyOnWriteArraySet<>();

	@Override
	public JsonMessageContent onUserMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded) {
		receivedMessageSessionSet.add(getSessionWrapper());
		return null;
	}

	public Set<SessionWrapper> getReceivedMessageSessionSet() {
		return receivedMessageSessionSet;
	}

	public void clearReceivedMessageSessionSet() {
		receivedMessageSessionSet.clear();
	}
}
