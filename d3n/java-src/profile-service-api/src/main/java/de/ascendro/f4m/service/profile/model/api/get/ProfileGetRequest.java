package de.ascendro.f4m.service.profile.model.api.get;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

/**
 * Publicly available profile getter - request.
 */
public class ProfileGetRequest implements JsonMessageContent {

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("ProfileGetRequest [");
		builder.append("]");
		return builder.toString();
	}

}
