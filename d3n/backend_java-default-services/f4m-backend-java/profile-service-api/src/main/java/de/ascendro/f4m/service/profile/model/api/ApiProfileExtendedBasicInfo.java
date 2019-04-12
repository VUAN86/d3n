package de.ascendro.f4m.service.profile.model.api;

import de.ascendro.f4m.service.profile.model.Profile;

public class ApiProfileExtendedBasicInfo extends ApiProfileBasicInfo {

    private Double handicap;

    public ApiProfileExtendedBasicInfo() {
    }

    public ApiProfileExtendedBasicInfo(Profile profile) {
        super(profile);
        if (profile != null) {
            this.handicap = profile.getHandicap();
        }
    }

    public Double getHandicap() {
        return handicap;
    }

    public void setHandicap(Double handicap) {
        this.handicap = handicap;
    }
}
