package de.ascendro.f4m.service.profile.model.find;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.profile.model.ProfileIdentifierType;

public class FindProfileRequest implements JsonMessageContent {
	private ProfileIdentifierType identifierType;
	private String identifier;

	public FindProfileRequest() {
	}

	public FindProfileRequest(ProfileIdentifierType identifierType) {
		this.identifierType = identifierType;
	}

	public FindProfileRequest(ProfileIdentifierType identifierType, String identifier) {
		this(identifierType);
		this.identifier = identifier;
	}

	public ProfileIdentifierType getIdentifierType() {
		return identifierType;
	}

	public void setIdentifierType(ProfileIdentifierType identifierType) {
		this.identifierType = identifierType;
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

}
