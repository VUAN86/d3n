var _ = require('lodash');
var fs = require('fs');
var path = require('path');
var async = require('async');
var should = require('should');
var assert = require('chai').assert;
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./../config/_id.data.js');
var ProfileData = require('./../config/profile.data.js');
var Data = require('./../config/achievement.data.js');
var ShouldHelper = require('nodejs-automapper').getInstance(Config).ShouldHelper;
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var Achievement = Database.RdbmsService.Models.AchievementManager.Achievement;
var AchievementHasBadge = Database.RdbmsService.Models.AchievementManager.AchievementHasBadge;
var Badge = Database.RdbmsService.Models.AchievementManager.Badge;
var TenantService = require('../../services/tenantService.js');
var AchievementService = require('../../services/achievementService.js');

describe('WS Achievement API', function () {
    this.timeout(20000);
    global.wsHelper.series().forEach(function (serie) {
        var stub_getSessionTenantId;
        var stub_achievementServiceImport;
        var stub_achievementServiceExport;
        var stub_achievementServiceDelete;
        var testSessionTenantId = DataIds.TENANT_1_ID;
        before(function (done) {
            stub_getSessionTenantId = global.wsHelper.sinon.stub(TenantService, 'getSessionTenantId', function (message, clientSession, cb) {
                return cb(false, testSessionTenantId);
            });
            stub_achievementServiceImport = global.wsHelper.sinon.stub(AchievementService, 'load', function (key, callback) {
                fs.readFile(path.join(__dirname, '../resources', key), 'utf8', function (err, data) {
                    if (err) {
                        return console.log(err);
                    }
                    console.log(data);
                    return callback(false, data);
                });
            });
            stub_achievementServiceExport = global.wsHelper.sinon.stub(AchievementService, 'export', function (key, object, progress, callback) {
                return setImmediate(callback, false);
            });
            stub_achievementServiceDelete = global.wsHelper.sinon.stub(AchievementService, 'delete', function (key, callback) {
                return setImmediate(callback, false);
            });
            return setImmediate(done);
        });
        after(function (done) {
            stub_getSessionTenantId.restore();
            stub_achievementServiceImport.restore();
            stub_achievementServiceExport.restore();
            stub_achievementServiceDelete.restore();
            return setImmediate(done);
        });

        describe('[' + serie + '] ' + 'achievementUpdate', function () {
            it('[' + serie + '] ' + 'SUCCESS: achievementUpdate:create', function (done) {
                var content = _.clone(Data.ACHIEVEMENT_TEST);
                content.id = null;
                global.wsHelper.apiSecureSucc(serie, 'achievementManager/achievementCreate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.ACHIEVEMENT_TEST);
                    should.exist(responseContent.achievement.badges);
                    should(responseContent.achievement.badges.items.length).be.equal(shouldResponseContent.badgesIds.length);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.achievement, ['id', 'tenantId', 'createDate', 'badgesIds', 'badges',
                        'stat_archieved', 'stat_inventoryCount', 'stat_specialPrizes', 'stat_won'
                    ]);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: achievementUpdate:update', function (done) {
                var content = { id: DataIds.ACHIEVEMENT_1_ID, name: 'name test' };
                global.wsHelper.apiSecureSucc(serie, 'achievementManager/achievementUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should(responseContent.achievement.name).be.equal(content.name);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_ACHIEVEMENT_IS_ACTIVATED: achievementUpdate:update', function (done) {
                var content = { id: DataIds.ACHIEVEMENT_1_ID, name: 'name test' };
                global.wsHelper.apiSecureSucc(serie, 'achievement/achievementActivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureFail(serie, 'achievement/achievementUpdate', ProfileData.CLIENT_INFO_1, content, Errors.AchievementApi.AchievementIsActivated, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: achievementUpdate:update', function (done) {
                var content = { id: DataIds.ACHIEVEMENT_TEST_ID, name: 'name test' };
                global.wsHelper.apiSecureFail(serie, 'achievementManager/achievementUpdate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'achievementActivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: achievementActivate', function (done) {
                var content = { id: DataIds.ACHIEVEMENT_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'achievementManager/achievementActivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'achievementManager/achievementGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.achievement.status).be.equal('active');
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: achievementActivate', function (done) {
                var content = { id: DataIds.ACHIEVEMENT_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'achievementManager/achievementActivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'achievementDeactivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: achievementDeactivate', function (done) {
                var content = { id: DataIds.ACHIEVEMENT_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'achievementManager/achievementActivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'achievementManager/achievementDeactivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                            setImmediate(function (done) {
                                global.wsHelper.apiSecureSucc(serie, 'achievementManager/achievementGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                                    should(responseContent.achievement.status).be.equal('inactive');
                                }, done);
                            }, done);
                        });
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: achievementDeactivate', function (done) {
                var content = { id: DataIds.ACHIEVEMENT_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'achievementManager/achievementDeactivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'achievementArchive', function () {
            it('[' + serie + '] ' + 'SUCCESS: achievementArchive', function (done) {
                var content = { id: DataIds.ACHIEVEMENT_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'achievementManager/achievementArchive', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureFail(serie, 'achievementManager/achievementActivate', ProfileData.CLIENT_INFO_1, content, Errors.AchievementApi.AchievementIsArchived, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: achievementArchive', function (done) {
                var content = { id: DataIds.ACHIEVEMENT_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'achievementManager/achievementArchive', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'achievementDelete', function () {
            it('[' + serie + '] ' + 'SUCCESS: achievementDelete', function (done) {
                var content = { id: DataIds.ACHIEVEMENT_INACTIVE_ID };
                global.wsHelper.apiSecureSucc(serie, 'achievement/achievementDelete', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureFail(serie, 'achievement/achievementGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_ACHIEVEMENT_IS_ACTIVATED: achievementDelete', function (done) {
                var content = { id: DataIds.ACHIEVEMENT_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'achievement/achievementActivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureFail(serie, 'achievement/achievementDelete', ProfileData.CLIENT_INFO_1, content, Errors.AchievementApi.AchievementIsActivated, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: achievementDelete', function (done) {
                var content = { id: DataIds.ACHIEVEMENT_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'achievement/achievementDelete', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'achievementGet', function () {
            it('[' + serie + '] ' + 'SUCCESS: achievementGet', function (done) {
                var content = { id: DataIds.ACHIEVEMENT_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'achievementManager/achievementGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.ACHIEVEMENT_1);
                    should.exist(responseContent.achievement.badges);
                    should(responseContent.achievement.badges.items.length).be.equal(shouldResponseContent.badgesIds.length);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.achievement, ['tenantId', 'createDate', 'badges', 'badgesIds',
                        'stat_archieved', 'stat_inventoryCount', 'stat_specialPrizes', 'stat_won'
                    ]);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: achievementGet', function (done) {
                var content = { userId: DataIds.ACHIEVEMENT_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'achievementManager/achievementGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'achievementList', function () {
            it('[' + serie + '] ' + 'SUCCESS: achievementList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: {},
                    orderBy: [{ field: 'name', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'achievementManager/achievementList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.ACHIEVEMENT_1);
                    should.exist(responseContent.items[0].badges);
                    should(responseContent.items[0].badges.items.length).be.equal(shouldResponseContent.badgesIds.length);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.items[0], ['tenantId', 'createDate', 'badges', 'badgesIds',
                        'stat_archieved', 'stat_inventoryCount', 'stat_specialPrizes', 'stat_won'
                    ]);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: achievementList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.ACHIEVEMENT_TEST_ID },
                    orderBy: [{ field: 'name', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'achievementManager/achievementList', ProfileData.CLIENT_INFO_1, content, function (content) {
                    should.exist(content);
                    should(content).has.property('items').which.has.length(0);
                }, done);
            });
        });

        describe('[' + serie + '] ' + 'badgeUpdate', function () {
            it('[' + serie + '] ' + 'SUCCESS: badgeUpdate:create', function (done) {
                var content = _.clone(Data.BADGE_TEST);
                content.id = null;
                global.wsHelper.apiSecureSucc(serie, 'achievementManager/badgeCreate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.BADGE_TEST);
                    shouldResponseContent.relatedBadgesIds = ShouldHelper.treatAsList(Data.BADGE_TEST.relatedBadgesIds, 0, 0);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.badge, ['id', 'tenantId', 'createDate']);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: badgeUpdate:update', function (done) {
                var content = { id: DataIds.BADGE_1_ID, name: 'name test' };
                global.wsHelper.apiSecureSucc(serie, 'achievementManager/badgeUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should(responseContent.badge.name).be.equal(content.name);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_BADGE_IS_ACTIVATED: badgeUpdate:update', function (done) {
                var content = { id: DataIds.BADGE_1_ID, name: 'name test' };
                global.wsHelper.apiSecureSucc(serie, 'achievement/badgeActivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureFail(serie, 'achievement/badgeUpdate', ProfileData.CLIENT_INFO_1, content, Errors.AchievementApi.BadgeIsActivated, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: badgeUpdate:update', function (done) {
                var content = { id: DataIds.BADGE_TEST_ID, name: 'name test' };
                global.wsHelper.apiSecureFail(serie, 'achievementManager/badgeUpdate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'badgeActivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: badgeActivate', function (done) {
                var content = { id: DataIds.BADGE_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'achievementManager/badgeActivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'achievementManager/badgeGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.badge.status).be.equal('active');
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: badgeActivate', function (done) {
                var content = { id: DataIds.BADGE_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'achievementManager/badgeActivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'badgeDeactivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: badgeDeactivate', function (done) {
                var content = { id: DataIds.BADGE_2_ID };
                global.wsHelper.apiSecureSucc(serie, 'achievementManager/badgeActivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'achievementManager/badgeDeactivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                            setImmediate(function (done) {
                                global.wsHelper.apiSecureSucc(serie, 'achievementManager/badgeGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                                    should(responseContent.badge.status).be.equal('inactive');
                                }, done);
                            }, done);
                        });
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: badgeDeactivate', function (done) {
                var content = { id: DataIds.BADGE_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'achievementManager/badgeDeactivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'badgeArchive', function () {
            it('[' + serie + '] ' + 'SUCCESS: badgeArchive', function (done) {
                var content = { id: DataIds.BADGE_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'achievementManager/badgeArchive', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureFail(serie, 'achievementManager/badgeActivate', ProfileData.CLIENT_INFO_1, content, Errors.AchievementApi.BadgeIsArchived, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: badgeArchive', function (done) {
                var content = { id: DataIds.BADGE_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'achievementManager/badgeArchive', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'badgeDelete', function () {
            it('[' + serie + '] ' + 'SUCCESS: badgeDelete', function (done) {
                var content = { id: DataIds.BADGE_INACTIVE_ID };
                global.wsHelper.apiSecureSucc(serie, 'achievement/badgeDelete', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureFail(serie, 'achievement/badgeGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_BADGE_IS_ACTIVATED: badgeDelete', function (done) {
                var content = { id: DataIds.BADGE_INACTIVE_ID };
                global.wsHelper.apiSecureSucc(serie, 'achievement/badgeActivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureFail(serie, 'achievement/badgeDelete', ProfileData.CLIENT_INFO_1, content, Errors.AchievementApi.BadgeIsActivated, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_FOREIGN_KEY_CONSTRAINT_VIOLATION: badgeDelete', function (done) {
                var content = { id: DataIds.BADGE_1_ID };
                global.wsHelper.apiSecureFail(serie, 'achievement/badgeDelete', ProfileData.CLIENT_INFO_1, content, Errors.QuestionApi.ForeignKeyConstraintViolation, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: badgeDelete', function (done) {
                var content = { id: DataIds.BADGE_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'achievement/badgeDelete', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'badgeGet', function () {
            it('[' + serie + '] ' + 'SUCCESS: badgeGet', function (done) {
                var content = { id: DataIds.BADGE_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'achievementManager/badgeGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.BADGE_1);
                    shouldResponseContent.relatedBadgesIds = ShouldHelper.treatAsList(Data.BADGE_1.relatedBadgesIds, 0, 0);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.badge, ['tenantId']);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: badgeGet', function (done) {
                var content = { userId: DataIds.BADGE_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'achievementManager/badgeGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'badgeList', function () {
            it('[' + serie + '] ' + 'SUCCESS: badgeList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.BADGE_1_ID },
                    orderBy: [{ field: 'name', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'achievementManager/badgeList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.BADGE_1);
                    shouldResponseContent.relatedBadgesIds = ShouldHelper.treatAsList(Data.BADGE_1.relatedBadgesIds, 0, 0);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.items[0], ['tenantId']);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: badgeList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.BADGE_TEST_ID },
                    orderBy: [{ field: 'name', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'achievementManager/badgeList', ProfileData.CLIENT_INFO_1, content, function (content) {
                    should.exist(content);
                    should(content).has.property('items').which.has.length(0);
                }, done);
            });
        });
        
        describe('[' + serie + '] ' + 'badgeImport', function () {
            it('[' + serie + '] ' + 'SUCCESS: badgeImport', function (done) {
                var content = { key: 'achievement.test.csv' };
                global.wsHelper.apiSecureSucc(serie, 'achievementManager/badgeImport', ProfileData.CLIENT_INFO_1, content, null, done);
            });
        });

        describe('[' + serie + '] ' + 'badgeExport', function () {
            it('[' + serie + '] ' + 'SUCCESS: badgeExport', function (done) {
                var content = { key: 'achievement.test.csv' };
                global.wsHelper.apiSecureSucc(serie, 'achievementManager/badgeExport', ProfileData.CLIENT_INFO_1, content, null, done);
            });
        });

    });
});