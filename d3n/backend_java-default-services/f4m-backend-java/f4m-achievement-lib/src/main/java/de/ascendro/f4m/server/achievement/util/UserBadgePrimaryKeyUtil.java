package de.ascendro.f4m.server.achievement.util;

import javax.inject.Inject;

import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.server.achievement.model.BadgeType;
import de.ascendro.f4m.service.config.Config;

public class UserBadgePrimaryKeyUtil extends PrimaryKeyUtil<String> {

    private static final String USER_BADGE_PK_PREFIX = "userBadge";
	private static final String USER_BADGE_PROGRESS_PK_SUFFIX = "progress";
    private static final String USER_BADGE_LIST_PK_PREFIX = "userBadgeList";

    @Inject
    public UserBadgePrimaryKeyUtil(Config config) {
        super(config);
    }

	public String createPrimaryKey(String badgeId, String userId) {
		return USER_BADGE_PK_PREFIX + KEY_ITEM_SEPARATOR + badgeId + KEY_ITEM_SEPARATOR + userId + KEY_ITEM_SEPARATOR
				+ USER_BADGE_PROGRESS_PK_SUFFIX;
	}

	public String createListPrimaryKey(String tenantId, String userId, BadgeType type) {
		return USER_BADGE_LIST_PK_PREFIX + KEY_ITEM_SEPARATOR + tenantId + KEY_ITEM_SEPARATOR + userId
				+ KEY_ITEM_SEPARATOR + type;
	}

    public String createInstancePrimaryKey(String badgeId, String userId, int instanceId) {
        return USER_BADGE_PK_PREFIX + KEY_ITEM_SEPARATOR + badgeId + KEY_ITEM_SEPARATOR + userId + KEY_ITEM_SEPARATOR + instanceId;
    }
}
