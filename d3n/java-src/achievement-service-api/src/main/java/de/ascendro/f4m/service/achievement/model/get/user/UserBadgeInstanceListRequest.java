package de.ascendro.f4m.service.achievement.model.get.user;

import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class UserBadgeInstanceListRequest extends FilterCriteria implements JsonMessageContent {

    public static final int MAX_LIST_LIMIT = 100;
    private String badgeId;

    public UserBadgeInstanceListRequest() {
        this(MAX_LIST_LIMIT, 0);
    }

    public UserBadgeInstanceListRequest(int limit, long offset) {
        setLimit(limit);
        setOffset(offset);
    }

    public String getBadgeId() {
        return badgeId;
    }

    public void setBadgeId(String badgeId) {
        this.badgeId = badgeId;
    }

	@Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UserBadgeInstanceListRequest [");
        builder.append("badgeId=").append(badgeId);
        builder.append(", limit=").append(Integer.toString(getLimit()));
        builder.append(", offset=").append(Long.toString(getOffset()));
        builder.append(", orderBy=").append(getOrderBy());
        builder.append(", searchBy=").append(getSearchBy());
        builder.append("]");
        return builder.toString();
    }
}
