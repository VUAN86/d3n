package de.ascendro.f4m.service.friend.model.api;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class ElasticResyncRequest implements JsonMessageContent {

	public ElasticResyncRequest() {
		// Initialize empty object
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("]");
		return builder.toString();
	}

}
