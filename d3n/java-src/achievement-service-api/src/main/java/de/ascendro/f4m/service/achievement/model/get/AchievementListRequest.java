package de.ascendro.f4m.service.achievement.model.get;

import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class AchievementListRequest extends FilterCriteria implements JsonMessageContent {

	public static final int MAX_LIST_LIMIT = 100;

	public AchievementListRequest() {
		this(MAX_LIST_LIMIT, 0, null);
	}

	public AchievementListRequest(long offset) {
		this(MAX_LIST_LIMIT, offset, null);
	}

	public AchievementListRequest(int limit, long offset) {
		this(limit, offset, null);
	}

	public AchievementListRequest(int limit, long offset, String achievementType) {
		setLimit(limit);
		setOffset(offset);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AchievementListRequest [");
		builder.append("limit=").append(Integer.toString(getLimit()));
		builder.append(", offset=").append(Long.toString(getOffset()));
		builder.append(", orderBy=").append(getOrderBy());
		builder.append(", searchBy=").append(getSearchBy());
		builder.append("]");
		return builder.toString();
	}
}
