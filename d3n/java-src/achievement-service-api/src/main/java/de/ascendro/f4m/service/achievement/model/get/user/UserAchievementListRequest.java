package de.ascendro.f4m.service.achievement.model.get.user;

import de.ascendro.f4m.server.achievement.model.UserAchievementStatus;
import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonRequiredNullable;

public class UserAchievementListRequest extends FilterCriteria implements JsonMessageContent {

    public static final int MAX_LIST_LIMIT = 100;
    
    @JsonRequiredNullable
    private UserAchievementStatus status;

    public UserAchievementListRequest() {
        this(MAX_LIST_LIMIT, 0);
    }

    public UserAchievementListRequest(int limit, long offset) {
        setLimit(limit);
        setOffset(offset);
    }

    public UserAchievementListRequest(int limit, long offset, UserAchievementStatus status) {
		this(limit, offset);
		this.status = status;
	}

	public UserAchievementStatus getStatus() {
		return status;
	}

	public void setStatus(UserAchievementStatus status) {
		this.status = status;
	}

	@Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UserAchievementListRequest [");
        builder.append("limit=").append(Integer.toString(getLimit()));
        builder.append(", offset=").append(Long.toString(getOffset()));
        builder.append(", orderBy=").append(getOrderBy());
        builder.append(", searchBy=").append(getSearchBy());
        builder.append(", status=").append(getStatus());
        builder.append("]");
        return builder.toString();
    }
}