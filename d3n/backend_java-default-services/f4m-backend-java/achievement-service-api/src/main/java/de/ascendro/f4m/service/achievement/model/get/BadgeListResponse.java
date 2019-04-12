package de.ascendro.f4m.service.achievement.model.get;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.ascendro.f4m.server.achievement.model.Badge;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.ListResult;

public class BadgeListResponse extends ListResult<Badge> implements JsonMessageContent {

    public BadgeListResponse(int limit, long offset) {
        super(limit, offset, 0, Collections.emptyList());
    }

    public BadgeListResponse(int limit, long offset, long total) {
        super(limit, offset, total, new ArrayList<>());
    }

    public BadgeListResponse(int limit, long offset, long total, List<Badge> items) {
        super(limit, offset, total, items);
    }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BadgeListResponse [");
        builder.append("items=").append(getItems());
        builder.append(", limit=").append(getLimit());
        builder.append(", offset=").append(getOffset());
        builder.append(", total=").append(getTotal());
        builder.append("]");
        return builder.toString();
    }
}