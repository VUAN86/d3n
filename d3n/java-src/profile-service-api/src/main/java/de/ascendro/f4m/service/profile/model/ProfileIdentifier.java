package de.ascendro.f4m.service.profile.model;

public class ProfileIdentifier {

    private ProfileIdentifierType identifierType;
    private String identifier;

    public ProfileIdentifierType getIdentifierType() {
        return identifierType;
    }

    public void setIdentifierType(ProfileIdentifierType type) {
        this.identifierType = type;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
}
