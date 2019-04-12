package de.ascendro.f4m.service.tombola.model.get;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.tombola.model.TombolaDrawing;

public class TombolaWinnerListResponse implements JsonMessageContent {

    private TombolaDrawing drawing;

    public TombolaWinnerListResponse(TombolaDrawing drawing) {
        this.drawing = drawing;
    }

    public TombolaDrawing getDrawing() {
        return drawing;
    }

    public void setDrawing(TombolaDrawing drawing) {
        this.drawing = drawing;
    }
}

