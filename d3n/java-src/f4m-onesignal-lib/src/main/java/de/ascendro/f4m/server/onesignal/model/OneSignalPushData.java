package de.ascendro.f4m.server.onesignal.model;

import java.time.ZonedDateTime;
import java.util.Map;

import de.ascendro.f4m.service.json.model.ISOLanguage;

public class OneSignalPushData {

    private String userId;
    private String type;
    private Map<ISOLanguage, String> messages;
    private String[] appIds;
    private String payload;
    private ZonedDateTime scheduleDateTime;

    public OneSignalPushData(String userId, String type, Map<ISOLanguage, String> messages, String[] appIds,
                             String payload) {
        this.userId = userId;
        this.type = type;
        this.messages = messages;
        this.appIds = appIds;
        this.payload = payload;
    }

    public OneSignalPushData(String userId, String type, Map<ISOLanguage, String> messages, String[] appIds,
                             String payload, ZonedDateTime scheduleDateTime) {
        this(userId, type, messages, appIds, payload);
        this.scheduleDateTime = scheduleDateTime;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<ISOLanguage, String> getMessages() {
        return messages;
    }

    public void setMessages(Map<ISOLanguage, String> messages) {
        this.messages = messages;
    }

    public String[] getAppIds() {
        return appIds;
    }

    public void setAppIds(String[] appId) {
        this.appIds = appId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public ZonedDateTime getScheduleDateTime() {
        return scheduleDateTime;
    }

    public void setScheduleDateTime(ZonedDateTime scheduleDateTime) {
        this.scheduleDateTime = scheduleDateTime;
    }
}
