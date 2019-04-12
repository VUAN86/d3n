package de.ascendro.f4m.server.analytics;


import java.lang.reflect.InvocationTargetException;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.analytics.model.base.BaseEvent;

public class EventContent {
	
    public static final String LOCAL_IP = "0.0.0.0";

    private String eventType;
    private String userId;
    private String tenantId;
    private String appId;
    private String sessionIp;
    private String countryCode;
    private JsonObject eventData;
    private long eventTimestamp;

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    @SuppressWarnings("unchecked")
	public <T extends BaseEvent> T getEventData() {
    	if (eventData != null) {
    		JsonObject data = eventData;
    		if (data.has("jsonObject")) {
    			data = data.get("jsonObject").getAsJsonObject();
    		}
    		try {
				return (T) Class.forName(getEventType()).getConstructor(JsonObject.class).newInstance(data);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException
					| ClassNotFoundException e) {
				throw new IllegalArgumentException("Could not instantiate correct event type", e);
			}
    	} else {
    		return null;
    	}
    }

    public void setEventData(BaseEvent eventData) {
    	if (eventData == null) {
    		this.eventData = null;
    	} else {
	    	JsonObject jsonObject = new JsonObject();
	    	jsonObject.add("jsonObject", eventData.getJsonObject());
	        this.eventData = jsonObject;
    	}
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getSessionIp() {
        return sessionIp;
    }

    public void setSessionIp(String sessionIp) {
        this.sessionIp = sessionIp;
    }

    public boolean isOfType(Class<?> eventClass) {
        return eventClass.getCanonicalName().equals(this.getEventType());
    }

    public long getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(long time) {
        this.eventTimestamp = time;
    }
}
