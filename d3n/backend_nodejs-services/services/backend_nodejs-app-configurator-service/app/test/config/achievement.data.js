var _ = require('lodash');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./_id.data.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var DatabaseErrors = Database.Errors;
var RdbmsService = Database.RdbmsService;
var Achievement = RdbmsService.Models.AchievementManager.Achievement;
var Badge = RdbmsService.Models.AchievementManager.Badge;
var AchievementHasBadge = RdbmsService.Models.AchievementManager.AchievementHasBadge;
var BadgeHasBadge = RdbmsService.Models.AchievementManager.BadgeHasBadge;

module.exports = {
    ACHIEVEMENT_1: {
        id: DataIds.ACHIEVEMENT_1_ID,
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
        tenantId: DataIds.TENANT_1_ID,
        badgesIds: [DataIds.BADGE_1_ID]
    },
    ACHIEVEMENT_2: {
        id: DataIds.ACHIEVEMENT_2_ID,
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
        tenantId: DataIds.TENANT_1_ID,
        badgesIds: [DataIds.BADGE_2_ID]
    },
    ACHIEVEMENT_TEST: {
        id: DataIds.ACHIEVEMENT_TEST_ID,
        name: 'Achievement test',
        description: 'Description test',
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
        tenantId: DataIds.TENANT_1_ID,
        badgesIds: [DataIds.BADGE_2_ID]
    },
    ACHIEVEMENT_INACTIVE: {
        id: DataIds.ACHIEVEMENT_INACTIVE_ID,
        name: 'Achievement inactive',
        description: 'Description inactive',
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
        tenantId: DataIds.TENANT_1_ID
    },

    BADGE_1: {
        id: DataIds.BADGE_1_ID,
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
        relatedBadgesIds: [DataIds.BADGE_2_ID],
        messaging: "[\"SMS\",\"EMAIL\"]",
        createDate: DateUtils.isoNow(),
        tenantId: DataIds.TENANT_1_ID
    },
    BADGE_2: {
        id: DataIds.BADGE_2_ID,
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
        relatedBadgesIds: [],
        messaging: "[\"SMS\",\"EMAIL\"]",
        createDate: DateUtils.isoNow(),
        tenantId: DataIds.TENANT_1_ID
    },
    BADGE_TEST: {
        id: DataIds.BADGE_TEST_ID,
        name: 'Badge test',
        description: 'Description test',
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
        relatedBadgesIds: [DataIds.BADGE_2_ID],
        messaging: "[\"SMS\",\"EMAIL\"]",
        createDate: DateUtils.isoNow(),
        tenantId: DataIds.TENANT_1_ID
    },
    BADGE_INACTIVE: {
        id: DataIds.BADGE_INACTIVE_ID,
        name: 'Badge inactive',
        description: 'Description inactive',
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
        relatedBadgesIds: [],
        messaging: "[\"SMS\",\"EMAIL\"]",
        createDate: DateUtils.isoNow(),
        tenantId: DataIds.TENANT_1_ID
    },

    ACHIEVEMENT_1_HAS_BADGE_1: {
        badgeId: DataIds.BADGE_1_ID,
        achievementId: DataIds.ACHIEVEMENT_1_ID
    },
    ACHIEVEMENT_2_HAS_BADGE_2: {
        badgeId: DataIds.BADGE_2_ID,
        achievementId: DataIds.ACHIEVEMENT_2_ID
    },

    BADGE_1_HAS_BADGE_2: {
        badgeId: DataIds.BADGE_1_ID,
        relatedBadgeId: DataIds.BADGE_2_ID
    },

    cleanClassifiers: function (done) {
        var self = this;
        RdbmsService.load()
            .removeSeries(Achievement, [self.ACHIEVEMENT_1, self.ACHIEVEMENT_2, self.ACHIEVEMENT_INACTIVE, self.ACHIEVEMENT_TEST])
            .removeSeries(Badge, [self.BADGE_1, self.BADGE_2, self.BADGE_INACTIVE])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },

    loadClassifiers: function (done) {
        var self = this;
        RdbmsService.load()
            .createSeries(Badge, [self.BADGE_1, self.BADGE_2, self.BADGE_INACTIVE])
            .createSeries(Achievement, [self.ACHIEVEMENT_1, self.ACHIEVEMENT_2, self.ACHIEVEMENT_INACTIVE])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },

    cleanManyToMany: function (done) {
        var self = this;
        RdbmsService.load()
            .removeSeries(AchievementHasBadge, [self.ACHIEVEMENT_1_HAS_BADGE_1, self.ACHIEVEMENT_2_HAS_BADGE_2], 'badgeId')
            .removeSeries(BadgeHasBadge, [self.BADGE_1_HAS_BADGE_2], 'badgeId')
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },

    loadManyToMany: function (done) {
        var self = this;
        RdbmsService.load()
            .createSeries(AchievementHasBadge, [self.ACHIEVEMENT_1_HAS_BADGE_1, self.ACHIEVEMENT_2_HAS_BADGE_2], 'badgeId')
            .createSeries(BadgeHasBadge, [self.BADGE_1_HAS_BADGE_2], 'badgeId')
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },
};
