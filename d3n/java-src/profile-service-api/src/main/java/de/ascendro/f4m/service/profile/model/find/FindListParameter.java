package de.ascendro.f4m.service.profile.model.find;

import de.ascendro.f4m.service.profile.model.ProfileIdentifierType;

/**
 * Almost a copy of #{@link de.ascendro.f4m.service.profile.model.ProfileIdentifier}, just with added FULL_NAME.
 * Actually a poor/temporary design to allow searching by full name, which currently does not do partial searches anyway.
 */
public class FindListParameter {
	private ProfileIdentifierType identifierType;
	private String identifier;

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
