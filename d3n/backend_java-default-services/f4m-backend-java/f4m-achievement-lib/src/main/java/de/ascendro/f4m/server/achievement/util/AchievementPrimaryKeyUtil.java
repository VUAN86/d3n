package de.ascendro.f4m.server.achievement.util;

import javax.inject.Inject;

import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.service.config.Config;

public class AchievementPrimaryKeyUtil extends PrimaryKeyUtil<String> {

    private static final String ACHIEVEMENT_PK_PREFIX = "achievement";
    private static final String ACHIEVEMENT_LIST_PK_PREFIX = "achievementList";

    @Inject
    public AchievementPrimaryKeyUtil(Config config) {
        super(config);
    }


    @Override
    public String createPrimaryKey(String id) {
        return ACHIEVEMENT_PK_PREFIX + KEY_ITEM_SEPARATOR + id;
    }

    public String createListPrimaryKey(String tenantId) {
        return ACHIEVEMENT_LIST_PK_PREFIX + KEY_ITEM_SEPARATOR + tenantId;
    }
}
