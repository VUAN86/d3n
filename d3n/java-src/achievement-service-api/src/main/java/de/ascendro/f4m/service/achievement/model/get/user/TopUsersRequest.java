package de.ascendro.f4m.service.achievement.model.get.user;

import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonRequiredNullable;

public class TopUsersRequest extends FilterCriteria implements JsonMessageContent {
	
	@JsonRequiredNullable
	private Boolean friendsOnly = Boolean.FALSE;

    private String badgeType;

    public static final int MAX_LIST_LIMIT = 100;

    public TopUsersRequest() {
        this(MAX_LIST_LIMIT, 0, null);
    }

    public TopUsersRequest(long offset) {
        this(MAX_LIST_LIMIT, offset, null);
    }

    public TopUsersRequest(int limit, long offset) {
        this(limit, offset, null);
    }

    public TopUsersRequest(int limit, long offset, String badgeType) {
        setLimit(limit);
        setOffset(offset);
        setBadgeType(badgeType);
    }



    public Boolean getFriendsOnly() {
        return friendsOnly;
    }

    public void setFriendsOnly(Boolean friendsOnly) {
        this.friendsOnly = friendsOnly;
    }

    public String getBadgeType() {
        return badgeType;
    }

    public void setBadgeType(String badgeType) {
        this.badgeType = badgeType;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TopUsersRequest [").append("badgeType=").append(badgeType);
        builder.append(", friendsOnly=").append(friendsOnly);
        builder.append(", limit=").append(getLimit());
        builder.append(", offset=").append(getOffset());
        builder.append(", orderBy=").append(getOrderBy());
        builder.append(", searchBy=").append(getSearchBy());
        builder.append("]");
        return builder.toString();
    }
}
