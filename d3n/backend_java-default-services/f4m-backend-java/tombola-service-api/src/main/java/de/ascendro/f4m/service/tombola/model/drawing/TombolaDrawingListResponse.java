package de.ascendro.f4m.service.tombola.model.drawing;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.ListResult;
import de.ascendro.f4m.service.tombola.model.TombolaDrawing;

public class TombolaDrawingListResponse extends ListResult<TombolaDrawing> implements JsonMessageContent {

    public TombolaDrawingListResponse(int limit, long offset) {
        super(limit, offset, 0, Collections.emptyList());
    }

    public TombolaDrawingListResponse(int limit, long offset, long total) {
        super(limit, offset, total, new ArrayList<>());
    }

    public TombolaDrawingListResponse(int limit, long offset, long total, List<TombolaDrawing> items) {
        super(limit, offset, total, items);
    }
}
