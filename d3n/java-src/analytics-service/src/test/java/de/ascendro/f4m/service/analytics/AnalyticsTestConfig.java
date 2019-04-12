package de.ascendro.f4m.service.analytics;

import de.ascendro.f4m.service.analytics.config.AnalyticsConfig;
import de.ascendro.f4m.service.payment.model.Currency;


public class AnalyticsTestConfig extends AnalyticsConfig {
    protected static final String NAMESPACE = "test";
    public static final int MARIADB_PORT = 3307;
    public static String AEROSPIKE_TENANTS_WITH_ACHIEVEMENTS_SET = "tenantsWithAchievements.aerospike.set";
    public static String AEROSPIKE_BADGE_SET = "badge.aerospike.set";
    public static String AEROSPIKE_USER_BADGE_SET = "userBadge.aerospike.set";
    public static String AEROSPIKE_USER_BADGE_LIST_SET = "userBadgeList.aerospike.set";
    public static String AEROSPIKE_ACHIEVEMENT_LIST_SET = "achievementList.aerospike.set";
    public static String AEROSPIKE_BADGE_LIST_SET = "badgeList.aerospike.set";

    public static String AEROSPIKE_USER_ACHIEVEMENT_SET = "userAchievement.aerospike.set";
    public static String AEROSPIKE_USER_ACHIEVEMENT_LIST_SET = "userAchievementList.aerospike.set";
    public static String AEROSPIKE_ACHIEVEMENT_SET = "achievement.aerospike.set";

    public AnalyticsTestConfig() {
        super();

        setProperty(MYSQL_BATCH_SIZE, 1);
        setProperty(MYSQL_DATABASE_URL, "jdbc:mysql://localhost:" + MARIADB_PORT + "/test");
        setProperty(MYSQL_DATABASE_USER, "root");
        setProperty(MYSQL_DATABASE_PASSWORD, "");
        setProperty(MYSQL_BATCH_SIZE, 1);
        setProperty(MYSQL_UPDATE_TRIGGER_IN_SECONDS, 1);
        setProperty(ACTIVE_MQ_PERSISTENT, false);

        setProperty(AEROSPIKE_SCAN_DELAY, 5);

        setProperty(MONTHLY_BONUS_NUMBER_OF_FRIENDS, 10);
        setProperty(MONTHLY_BONUS_VALUE, 100);
        setProperty(MONTHLY_BONUS_CURRENCY, Currency.BONUS.name());

        setProperty(AEROSPIKE_TENANTS_WITH_ACHIEVEMENTS_SET, "tenantsWithAchievements");
        setProperty(AEROSPIKE_BADGE_SET, "badge");
        setProperty(AEROSPIKE_USER_BADGE_SET, "userBadge");
        setProperty(AEROSPIKE_USER_BADGE_LIST_SET, "userBadgeList");

        setProperty(AEROSPIKE_ACHIEVEMENT_SET, "achievement");
        setProperty(AEROSPIKE_USER_ACHIEVEMENT_SET, "userAchievement");
        setProperty(AEROSPIKE_USER_ACHIEVEMENT_LIST_SET, "userAchievementList");
        
        setProperty(AEROSPIKE_ACHIEVEMENT_LIST_SET, "achievementList");
        setProperty(AEROSPIKE_BADGE_LIST_SET, "badgeList");
        loadProperties();
    }
}
