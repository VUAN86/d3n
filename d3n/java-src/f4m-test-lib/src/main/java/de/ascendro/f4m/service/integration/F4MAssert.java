package de.ascendro.f4m.service.integration;

import static de.ascendro.f4m.service.integration.RetriedAssert.assertWithWait;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.type.MessageType;

public class F4MAssert {
	
	private F4MAssert() {
	}

	public static void assertSize(int expected, Collection<?> collection) {
		int actualSize = collection.size();
		if (actualSize != expected) {
			StringBuilder sb = new StringBuilder("Collection size expected:<");
			sb.append(expected);
			sb.append("> but was:<");
			sb.append(actualSize);
			sb.append(">, actual values:");
			sb.append(collection.stream().map(obj -> "[" + obj.toString() + "]").collect(Collectors.joining(",")));
			fail(sb.toString());
		}
	}
	
	/**
	 * Method for checking expected messages in list in any order.
	 * Similar to F4MIntegrationTestBase.assertReceivedMessagesWithWait, and to assertSize just with better messages like:
	 * "iterable containing [hasProperty("name", "pushServiceStatistics")] but: item 0: property 'name' was "event/subscribe""
	 * 
	 * @param messageList
	 * @param messageTypes
	 */
	public static void assertReceivedMessagesAnyOrderWithWait(List<JsonMessage<? extends JsonMessageContent>> messageList, MessageType... messageTypes) {
		assertWithWait(() -> assertThat(messageList, containsMessageTypes(messageTypes)));
	}
	
	public static Matcher<? super List<JsonMessage<? extends JsonMessageContent>>> containsMessageTypes(
			MessageType... messageTypes) {
		List<Matcher<? super JsonMessage<? extends JsonMessageContent>>> nameMatchers = new ArrayList<>();
		for (int i = 0; i < messageTypes.length; i++) {
			nameMatchers.add(Matchers.hasProperty("name", equalTo(messageTypes[i].getMessageName())));
		}
		return Matchers.<JsonMessage<? extends JsonMessageContent>>contains(nameMatchers);
	}
}
