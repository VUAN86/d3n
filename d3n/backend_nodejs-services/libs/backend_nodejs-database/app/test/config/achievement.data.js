var _ = require('lodash');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./_id.data.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('./../../../index.js').getInstance(Config);
var DatabaseErrors = Database.Errors;
var RdbmsService = Database.RdbmsService;
var Achievement = RdbmsService.Models.AchievementManager.Achievement;
var Badge = RdbmsService.Models.AchievementManager.Badge;
var AchievementHasBadge = RdbmsService.Models.AchievementManager.AchievementHasBadge;
var BadgeHasBadge = RdbmsService.Models.AchievementManager.BadgeHasBadge;

module.exports = {
    ACHIEVEMENT_1: {
        $id: DataIds.ACHIEVEMENT_1_ID,
        name: 'Achievement one',
        description: 'Description 1',
        status: 'inactive',
        imageId: 'test.jpg',
        isTimeLimit: 1,
        timePeriod: 100,
        isReward: 1,
        bonusPointsReward: 15,
        creditReward: 0,
        paymentMultiplier: 1.1,
        accessRules: "[\"PRO\"]",
        messaging: "[\"SMS\",\"EMAIL\"]",
        createDate: DateUtils.isoNow(),
        $tenantId: DataIds.TENANT_1_ID
    },
    ACHIEVEMENT_2: {
        $id: DataIds.ACHIEVEMENT_2_ID,
        name: 'Achievement two',
        description: 'Description 2',
        status: 'inactive',
        imageId: 'test.jpg',
        isTimeLimit: 1,
        timePeriod: 200,
        isReward: 1,
        bonusPointsReward: 0,
        creditReward: 3,
        paymentMultiplier: 1.1,
        accessRules: "[\"PREMIUM\"]",
        messaging: "[\"SMS\",\"EMAIL\"]",
        createDate: DateUtils.isoNow(),
        $tenantId: DataIds.TENANT_2_ID
    },

    BADGE_1: {
        $id: DataIds.BADGE_1_ID,
        name: 'Badge one',
        description: 'Description 1',
        type: 'Game',
        status: 'inactive',
        time: 'Permanent',
        timePeriod: 0,
        imageId: 'test.jpg',
        usage: 'Single',
        isReward: 1,
        bonusPointsReward: 15,
        creditReward: 0,
        paymentMultiplier: 1.1,
        accessRules: "[\"PRO\"]",
        earningRules: "[{\"rule\":\"RULE_QUESTION_TRANSLATED\",\"limit\":\100},{\"rule\":\"RULE_GAME_WON_GAMES\",\"limit\":\10}]",
        messaging: "[\"SMS\",\"EMAIL\"]",
        createDate: DateUtils.isoNow(),
        $tenantId: DataIds.TENANT_1_ID
    },
    BADGE_2: {
        $id: DataIds.BADGE_2_ID,
        name: 'Badge two',
        description: 'Description 2',
        type: 'Battle',
        status: 'inactive',
        time: 'Temporary',
        timePeriod: 100,
        imageId: 'test.jpg',
        usage: 'Multiple',
        isReward: 1,
        bonusPointsReward: 0,
        creditReward: 10,
        paymentMultiplier: 1.1,
        accessRules: "[\"PREMIUM\"]",
        earningRules: "[{\"rule\":\"RULE_QUESTION_TRANSLATED\",\"limit\":\200},{\"rule\":\"RULE_GAME_WON_GAMES\",\"limit\":\20}]",
        messaging: "[\"SMS\",\"EMAIL\"]",
        createDate: DateUtils.isoNow(),
        $tenantId: DataIds.TENANT_2_ID
    },

    ACHIEVEMENT_1_HAS_BADGE_1: {
        $achievementId: DataIds.ACHIEVEMENT_1_ID,
        $badgeId: DataIds.BADGE_1_ID
    },
    ACHIEVEMENT_2_HAS_BADGE_2: {
        $achievementId: DataIds.ACHIEVEMENT_2_ID,
        $badgeId: DataIds.BADGE_2_ID
    },

    BADGE_2_HAS_BADGE_1: {
        $badgeId: DataIds.BADGE_2_ID,
        $relatedBadgeId: DataIds.BADGE_1_ID
    },

    loadClassifiers: function (testSet) {
        return testSet
            .createSeries(Badge, [this.BADGE_1, this.BADGE_2])
            .createSeries(Achievement, [this.ACHIEVEMENT_1, this.ACHIEVEMENT_2])
    },

    loadManyToMany: function (testSet) {
        return testSet
            .createSeries(AchievementHasBadge, [this.ACHIEVEMENT_1_HAS_BADGE_1, this.ACHIEVEMENT_2_HAS_BADGE_2])
            .createSeries(BadgeHasBadge, [this.BADGE_2_HAS_BADGE_1])
    }

};
