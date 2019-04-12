package de.ascendro.f4m.service.integration.collector;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.type.MessageType;

public class ReceivedMessageCollector extends DefaultJsonMessageHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ReceivedMessageCollector.class);

	private List<JsonMessage<? extends JsonMessageContent>> receivedMessageList = new CopyOnWriteArrayList<>();

	@Override
	public JsonMessageContent onProcess(RequestContext context) {
		JsonMessage<? extends JsonMessageContent> originalMessageDecoded = context.getMessage();
		receivedMessageList.add(originalMessageDecoded);
		LOGGER.debug("In session {} collected message {} with content {}", getSessionWrapper().getSessionId(),
				originalMessageDecoded, originalMessageDecoded.getContent());
		return null;
	}

	public List<JsonMessage<? extends JsonMessageContent>> getReceivedMessageList() {
		return receivedMessageList;
	}

	public void clearReceivedMessageList() {
		receivedMessageList.clear();
	}

	public JsonMessage<? extends JsonMessageContent> getMessage(int index) {
		return receivedMessageList.get(index);
	}

	public <T extends JsonMessageContent> JsonMessage<T> getMessageByType(MessageType type) {
		return this.<T>getMessagesByType(type).stream()
			.limit(1)
			.findFirst().orElse(null);
	}
	
	@SuppressWarnings("unchecked")
	public <T extends JsonMessageContent> List<JsonMessage<T>> getMessagesByType(MessageType type){
		return receivedMessageList.stream()
			.filter(m -> m.getName().equalsIgnoreCase(type.getMessageName()))
			.map(m -> (JsonMessage<T>)m)
			.collect(Collectors.toList());
	}

	@Override
	public JsonMessageContent onUserMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded) throws F4MException {
		LOGGER.warn("No user messages are expected {}", originalMessageDecoded);
		return null;
	}

	public void removeMessageByType(MessageType type) {
		receivedMessageList.removeIf(m -> m.getName().equalsIgnoreCase(type.getMessageName()));
	}
}
