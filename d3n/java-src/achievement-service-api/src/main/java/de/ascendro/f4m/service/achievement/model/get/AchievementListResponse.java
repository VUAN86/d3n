package de.ascendro.f4m.service.achievement.model.get;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.ascendro.f4m.server.achievement.model.Achievement;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.ListResult;

public class AchievementListResponse extends ListResult<Achievement> implements JsonMessageContent {

    public AchievementListResponse(int limit, long offset) {
        super(limit, offset, 0, Collections.emptyList());
    }

    public AchievementListResponse(int limit, long offset, long total) {
        super(limit, offset, total, new ArrayList<>());
    }

    public AchievementListResponse(int limit, long offset, long total, List<Achievement> items) {
        super(limit, offset, total, items);
    }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AchievementListResponse [");
        builder.append("items=").append(getItems());
        builder.append(", limit=").append(getLimit());
        builder.append(", offset=").append(getOffset());
        builder.append(", total=").append(getTotal());
        builder.append("]");
        return builder.toString();
    }
}
