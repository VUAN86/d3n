package de.ascendro.f4m.service.tombola.model.drawing;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class TombolaDrawingGetRequest implements JsonMessageContent {

    private String tombolaId;

    public String getTombolaId() {
        return tombolaId;
    }

    public void setTombolaId(String tombolaId) {
        this.tombolaId = tombolaId;
    }
}
