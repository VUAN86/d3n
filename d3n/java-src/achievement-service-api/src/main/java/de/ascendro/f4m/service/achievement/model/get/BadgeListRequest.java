package de.ascendro.f4m.service.achievement.model.get;

import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonRequiredNullable;

public class BadgeListRequest extends FilterCriteria implements JsonMessageContent {
	@JsonRequiredNullable
	private String badgeType;

	public static final int MAX_LIST_LIMIT = 100;

	public BadgeListRequest() {
		this(MAX_LIST_LIMIT, 0, null);
	}

	public BadgeListRequest(long offset) {
		this(MAX_LIST_LIMIT, offset, null);
	}

	public BadgeListRequest(int limit, long offset) {
		this(limit, offset, null);
	}

	public BadgeListRequest(int limit, long offset, String badgeType) {
		setLimit(limit);
		setOffset(offset);
		setBadgeType(badgeType);
	}

	public void setBadgeType(String badgeType) {
		this.badgeType = badgeType;
	}

	public String getBadgeType() {
		return badgeType;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BadgeListRequest [");
		builder.append("badgeType=").append(getBadgeType());
		builder.append(", limit=").append(Integer.toString(getLimit()));
		builder.append(", offset=").append(Long.toString(getOffset()));
		builder.append(", orderBy=").append(getOrderBy());
		builder.append(", searchBy=").append(getSearchBy());
		builder.append("]");
		return builder.toString();
	}

}