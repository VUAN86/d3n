package de.ascendro.f4m.service.achievement.model.get.user;

import de.ascendro.f4m.server.achievement.model.BadgeType;
import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonRequiredNullable;

public class UserBadgeListRequest extends FilterCriteria implements JsonMessageContent {

    public static final int MAX_LIST_LIMIT = 100;
    
    @JsonRequiredNullable
    private BadgeType type;

    public UserBadgeListRequest() {
        this(MAX_LIST_LIMIT, 0);
    }

    public UserBadgeListRequest(int limit, long offset) {
        setLimit(limit);
        setOffset(offset);
    }

    public UserBadgeListRequest(int limit, long offset, BadgeType type) {
        this(limit, offset);
        setType(type);
    }

	public BadgeType getType() {
		return type;
	}

	public void setType(BadgeType type) {
		this.type = type;
	}

	@Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UserBadgeListRequest [");
        builder.append("type=").append(type);
        builder.append(", limit=").append(Integer.toString(getLimit()));
        builder.append(", offset=").append(Long.toString(getOffset()));
        builder.append(", orderBy=").append(getOrderBy());
        builder.append(", searchBy=").append(getSearchBy());
        builder.append("]");
        return builder.toString();
    }
}