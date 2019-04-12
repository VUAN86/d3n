package de.ascendro.f4m.service.tombola.model.events;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class TombolaEventRequest implements JsonMessageContent {

    private String tombolaId;

    public String getTombolaId() {
        return tombolaId;
    }

    public void setTombolaId(String tombolaId) {
        this.tombolaId = tombolaId;
    }
}
