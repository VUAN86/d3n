package de.ascendro.f4m.service.profile.model;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

public class ProfileFacebookId extends JsonObjectWrapper {

    public static final String FACEBOOK_USER_ID_PROPERTY = "facebookUserId";
    public static final String FACEBOOK_APPLICATION_ID_PROPERTY = "facebookAppId";

    public ProfileFacebookId() {
        // Initialize empty object
    }

    public ProfileFacebookId(JsonObject profileJsonObject) {
        this.jsonObject = profileJsonObject;
    }

    public String getFacebookUserId() {
        return getPropertyAsString(FACEBOOK_USER_ID_PROPERTY);
    }

    public void setFacebookUserId(String facebookUserId) {
        setProperty(FACEBOOK_USER_ID_PROPERTY, facebookUserId);
    }

    public String getFacebookAppId() {
        return getPropertyAsString(FACEBOOK_APPLICATION_ID_PROPERTY);
    }

    public void setFacebookAppId(String facebookAppId) {
        setProperty(FACEBOOK_APPLICATION_ID_PROPERTY, facebookAppId);
    }
}
