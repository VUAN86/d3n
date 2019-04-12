package de.ascendro.f4m.service.achievement.model.get.user;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.ascendro.f4m.server.achievement.model.UserBadge;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.ListResult;

public class UserBadgeInstanceListResponse extends ListResult<UserBadge> implements JsonMessageContent {

    public UserBadgeInstanceListResponse(int limit, long offset) {
        super(limit, offset, 0, Collections.emptyList());
    }

    public UserBadgeInstanceListResponse(int limit, long offset, long total) {
        super(limit, offset, total, new ArrayList<>());
    }

    public UserBadgeInstanceListResponse(int limit, long offset, long total, List<UserBadge> items) {
        super(limit, offset, total, items);
    }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UserBadgeInstanceListResponse [");
        builder.append("items=").append(getItems());
        builder.append(", limit=").append(getLimit());
        builder.append(", offset=").append(getOffset());
        builder.append(", total=").append(getTotal());
        builder.append("]");
        return builder.toString();
    }
}
