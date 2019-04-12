package de.ascendro.f4m.server.analytics.model;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.analytics.model.base.BaseEvent;

public class NewTombolaEvent extends BaseEvent {
    public static final String TOMBOLA_ID = "tombolaId";

    public NewTombolaEvent() {
        //default constructor
    }

    public NewTombolaEvent(JsonObject baseGameJsonObject) {
        super(baseGameJsonObject);
    }

    public void setTombolaId(Long tombolaId) {
        setProperty(TOMBOLA_ID, tombolaId);
    }

    public Long getTombolaId() {
        return getPropertyAsLong(TOMBOLA_ID);
    }
}
