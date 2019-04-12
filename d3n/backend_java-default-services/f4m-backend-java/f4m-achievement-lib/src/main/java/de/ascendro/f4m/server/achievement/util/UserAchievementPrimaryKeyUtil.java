package de.ascendro.f4m.server.achievement.util;

import javax.inject.Inject;

import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.server.achievement.model.UserAchievementStatus;
import de.ascendro.f4m.service.config.Config;

public class UserAchievementPrimaryKeyUtil extends PrimaryKeyUtil<String> {

    private static final String USER_ACHIEVEMENT_PK_PREFIX = "userAchievement";
    private static final String USER_ACHIEVEMENT_LIST_PK_PREFIX = "userAchievementList";

    @Inject
    public UserAchievementPrimaryKeyUtil(Config config) {
        super(config);
    }

	public String createPrimaryKey(String achievementId, String userId) {
		return USER_ACHIEVEMENT_PK_PREFIX + KEY_ITEM_SEPARATOR + achievementId + KEY_ITEM_SEPARATOR + userId;
	}


	public String createListPrimaryKey(String tenantId, String userId, UserAchievementStatus status) {
        return USER_ACHIEVEMENT_LIST_PK_PREFIX + KEY_ITEM_SEPARATOR + tenantId + KEY_ITEM_SEPARATOR + userId + KEY_ITEM_SEPARATOR +
                status;
	}
}
