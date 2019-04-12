package de.ascendro.f4m.server.analytics.model;


import com.google.gson.JsonObject;

import de.ascendro.f4m.server.analytics.model.base.GameBaseEvent;

public class AdEvent extends GameBaseEvent {

    public static final String AD_ID_PROPERTY = "adId";
    public static final String AD_KEY_PROPERTY = "adBlobKey";
    public static final String EARN_CREDITS = "earnCredits";
    public static final String FIRST_APP_USAGE = "firstAppUsage";
    public static final String FIRST_GAME_USAGE = "firstGameUsage";

    public static final String ANALYTICS_ADD2APP_SET_NAME = "analytics_add2app";
    public static final String ANALYTICS_ADD2GAME_SET_NAME = "analytics_add2game";


    public AdEvent() {
    }

    public AdEvent(JsonObject adJsonObject) {
    	super(adJsonObject);
    }

    public void setAdId(Long adId) {
        setProperty(AD_ID_PROPERTY, adId);
    }

    public Long getAdId() {
        return getPropertyAsLong(AD_ID_PROPERTY);
    }

    public void setEarnCredits(Long earnCredits) {
        setProperty(EARN_CREDITS, earnCredits);
    }

    public Long getEarnCredits() {
        return getPropertyAsLong(EARN_CREDITS);
    }

    public void setFirstAppUsage(Boolean firstAppUsage) {
        setProperty(FIRST_APP_USAGE, firstAppUsage);
    }

    public Boolean isFirstAppUsage() {
        return getPropertyAsBoolean(FIRST_APP_USAGE);
    }

    public void setFirstGameUsage(Boolean firstGameUsage) {
        setProperty(FIRST_GAME_USAGE, firstGameUsage);
    }

    public Boolean isFirstGameUsage() {
        return getPropertyAsBoolean(FIRST_GAME_USAGE);
    }

    public void setBlobKey(String blobKey) {
        setProperty(AD_KEY_PROPERTY, blobKey);
    }

    public String getBlobKey() {
        return getPropertyAsString(AD_KEY_PROPERTY);
    }

}
