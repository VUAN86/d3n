package de.ascendro.f4m.server.achievement.config;

import com.google.common.io.Resources;

import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.config.ElasticConfigImpl;
import de.ascendro.f4m.service.config.F4MConfigImpl;

public class AchievementConfig extends F4MConfigImpl {

    public static String AEROSPIKE_ACHIEVEMENT_SET = "achievement.aerospike.set";
    public static String AEROSPIKE_ACHIEVEMENT_LIST_SET = "achievementList.aerospike.set";
    public static String AEROSPIKE_USER_ACHIEVEMENT_SET = "userAchievement.aerospike.set";
    public static String AEROSPIKE_USER_ACHIEVEMENT_LIST_SET = "userAchievementList.aerospike.set";
    public static String AEROSPIKE_ACHIEVEMENT_LIST_BY_BADGE_SET = "achievementListByBadge.aerospike.set";

    public static String AEROSPIKE_BADGE_SET = "badge.aerospike.set";
    public static String AEROSPIKE_BADGE_LIST_SET = "badgeList.aerospike.set";
    public static String AEROSPIKE_USER_BADGE_SET = "userBadge.aerospike.set";
    public static String AEROSPIKE_USER_BADGE_LIST_SET = "userBadgeList.aerospike.set";
    public static String AEROSPIKE_BADGE_LIST_BY_RULE_TYPE_SET = "badgeListByRuleType.aerospike.set";
    
    public static String AEROSPIKE_TENANTS_WITH_ACHIEVEMENTS_SET = "tenantsWithAchievements.aerospike.set";

	public static final String ELASTIC_MAPPING_INDEX_ACHIEVEMENT = "elastic.mapping.index.achievement";
    public static String ELASTIC_MAPPING_TYPE_ACHIEVEMENT = "elastic.mapping.type.achievement";

	public static final String ELASTIC_INDEX_ACHIEVEMENT = "elastic.index.achievement";
	public static final String ELASTIC_TYPE_ACHIEVEMENT_PREFIX = "elastic.type.achievement";

    public AchievementConfig() {
        super(new AerospikeConfigImpl(), new ElasticConfigImpl());
        // achievement related
        setProperty(AEROSPIKE_ACHIEVEMENT_SET, "achievement");
        setProperty(AEROSPIKE_ACHIEVEMENT_LIST_SET, "achievementList");
        setProperty(AEROSPIKE_USER_ACHIEVEMENT_SET, "userAchievement");
        setProperty(AEROSPIKE_USER_ACHIEVEMENT_LIST_SET, "userAchievementList");
        setProperty(AEROSPIKE_ACHIEVEMENT_LIST_BY_BADGE_SET, "achievementListByBadge");

        // badge related
        setProperty(AEROSPIKE_BADGE_SET, "badge");
        setProperty(AEROSPIKE_BADGE_LIST_SET, "badgeList");
        setProperty(AEROSPIKE_USER_BADGE_SET, "userBadge");
        setProperty(AEROSPIKE_USER_BADGE_LIST_SET, "userBadgeList");
        setProperty(AEROSPIKE_BADGE_LIST_BY_RULE_TYPE_SET, "badgeListByRuleType");        

        setProperty(AEROSPIKE_TENANTS_WITH_ACHIEVEMENTS_SET, "tenantsWithAchievements");

		setProperty(ELASTIC_INDEX_ACHIEVEMENT, "achievement");

		setProperty(ELASTIC_TYPE_ACHIEVEMENT_PREFIX, "achievement");

		setProperty(ELASTIC_MAPPING_INDEX_ACHIEVEMENT, Resources.getResource(getClass(), "AchievementIndexESMapping.json"));
        setProperty(ELASTIC_MAPPING_TYPE_ACHIEVEMENT, Resources.getResource(getClass(), "AchievementESMapping.json"));

        loadProperties();
    }
}
