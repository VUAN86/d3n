package de.ascendro.f4m.server.analytics.model;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.analytics.model.base.BaseEvent;

public class UserRegistrationEvent extends BaseEvent {

    public static final String REGISTERED = "registered";
    public static final String FULLY_REGISTERED = "fullyRegistered";

    public UserRegistrationEvent() {
        //default constructor
    }

    public UserRegistrationEvent(JsonObject rewardJsonObject) {
        super(rewardJsonObject);
    }

    public void setRegistered(Boolean registered) {
        setProperty(REGISTERED, registered);
    }

    public boolean isRegistered() {
    	Boolean isFullyRegistered = getPropertyAsBoolean(REGISTERED);
        return isFullyRegistered == null ? false : isFullyRegistered;
    }

    public void setFullyRegistered(Boolean registered) {
        setProperty(FULLY_REGISTERED, registered);
    }

    public boolean isFullyRegistered() {
    	Boolean isFullyRegistered = getPropertyAsBoolean(FULLY_REGISTERED);
        return isFullyRegistered == null ? false : isFullyRegistered;
    }

    @Override
	public boolean isTenantIdRequired() {
        return true;
    }
}
