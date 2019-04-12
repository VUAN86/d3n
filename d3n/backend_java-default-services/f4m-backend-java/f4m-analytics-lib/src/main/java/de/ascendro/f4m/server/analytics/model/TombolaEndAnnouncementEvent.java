package de.ascendro.f4m.server.analytics.model;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.analytics.model.base.BaseEvent;

public class TombolaEndAnnouncementEvent extends BaseEvent {

    public static final String TOMBOLA_ID = "tombolaId";
    public static final String MINUTES_TO_END = "minutes";

    public TombolaEndAnnouncementEvent() {
        //default constructor
    }

    public TombolaEndAnnouncementEvent(JsonObject baseGameJsonObject) {
        super(baseGameJsonObject);
    }

    public void setTombolaId(Long tombolaId) {
        setProperty(TOMBOLA_ID, tombolaId);
    }

    public Long getTombolaId() {
        return getPropertyAsLong(TOMBOLA_ID);
    }

    public Integer getMinutesToEnd() {
        return getPropertyAsInteger(MINUTES_TO_END);
    }

    public void setMinutesToEnd(Integer minutes) {
        setProperty(MINUTES_TO_END, minutes);
    }

}
