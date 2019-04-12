package de.ascendro.f4m.server.analytics.model;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.analytics.model.base.BaseEvent;

public class TombolaEndEvent extends BaseEvent {

    public static final String TOMBOLA_ID = "tombolaId";

    public TombolaEndEvent() {
        //default constructor
    }

    public TombolaEndEvent(JsonObject baseGameJsonObject) {
        super(baseGameJsonObject);
    }

    public void setTombolaId(Long tombolaId) {
        setProperty(TOMBOLA_ID, tombolaId);
    }

    public Long getTombolaId() {
        return getPropertyAsLong(TOMBOLA_ID);
    }
}
