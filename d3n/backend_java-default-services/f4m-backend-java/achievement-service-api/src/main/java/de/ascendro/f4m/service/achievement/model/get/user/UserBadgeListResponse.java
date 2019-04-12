package de.ascendro.f4m.service.achievement.model.get.user;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.ascendro.f4m.server.achievement.model.UserBadge;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.ListResult;

public class UserBadgeListResponse extends ListResult<UserBadge> implements JsonMessageContent {

    public UserBadgeListResponse(int limit, long offset) {
        super(limit, offset, 0, Collections.emptyList());
    }

    public UserBadgeListResponse(int limit, long offset, long total) {
        super(limit, offset, total, new ArrayList<>());
    }

    public UserBadgeListResponse(int limit, long offset, long total, List<UserBadge> items) {
        super(limit, offset, total, items);
    }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UserBadgeListResponse [");
        builder.append("items=").append(getItems());
        builder.append(", limit=").append(getLimit());
        builder.append(", offset=").append(getOffset());
        builder.append(", total=").append(getTotal());
        builder.append("]");
        return builder.toString();
    }
}