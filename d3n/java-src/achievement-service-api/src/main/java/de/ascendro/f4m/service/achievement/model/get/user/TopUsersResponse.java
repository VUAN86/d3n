package de.ascendro.f4m.service.achievement.model.get.user;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.ascendro.f4m.server.achievement.model.UserScore;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.ListResult;

public class TopUsersResponse extends ListResult<UserScore> implements JsonMessageContent {

    public TopUsersResponse(int limit, long offset) {
        super(limit, offset, 0, Collections.emptyList());
    }

    public TopUsersResponse(int limit, long offset, long total) {
        super(limit, offset, total, new ArrayList<>());
    }

    public TopUsersResponse(int limit, long offset, long total, List<UserScore> items) {
        super(limit, offset, total, items);
    }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TopUsersResponse [");
        builder.append("items=").append(getItems());
        builder.append(", limit=").append(getLimit());
        builder.append(", offset=").append(getOffset());
        builder.append(", total=").append(getTotal());
        builder.append("]");
        return builder.toString();
    }
}
