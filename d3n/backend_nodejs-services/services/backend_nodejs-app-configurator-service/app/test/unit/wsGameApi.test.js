var _ = require('lodash');
var should = require('should');
var assert = require('chai').assert;
var async = require('async');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var Game = Database.RdbmsService.Models.Game.Game;
var DataIds = require('./../config/_id.data.js');
var ProfileData = require('./../config/profile.data.js');
var Data = require('./../config/game.data.js');
var ShouldHelper = require('nodejs-automapper').getInstance(Config).ShouldHelper;
var tenantService = require('../../services/tenantService.js');

describe('WS Game API', function () {
    this.timeout(20000);
    global.wsHelper.series().forEach(function (serie) {
        var stub_getSessionTenantId;
        var testSessionTenantId = DataIds.TENANT_1_ID;
        before(function (done) {
            stub_getSessionTenantId = global.wsHelper.sinon.stub(tenantService, 'getSessionTenantId', function (message, clientSession, cb) {
                return cb(false, testSessionTenantId);
            });

            return setImmediate(done);
        });
        after(function (done) {
            stub_getSessionTenantId.restore();
            return setImmediate(done);
        });
        
        
        describe('[' + serie + '] ' + 'gameUpdate', function () {
            it('[' + serie + '] ' + 'SUCCESS: gameUpdate:create', function (done) {
                var content = _.cloneDeep(Data.GAME_TEST);
                content.id = null;
                global.wsHelper.apiSecureSucc(serie, 'game/gameCreate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.cloneDeep(Data.GAME_TEST);
                    shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(Data.GAME_TEST.regionalSettingsIds);
                    shouldResponseContent.poolsIds = ShouldHelper.treatAsList(Data.GAME_TEST.poolsIds);
                    shouldResponseContent.applicationsIds = ShouldHelper.treatAsList(Data.GAME_TEST.applicationsIds);
                    shouldResponseContent.questionTemplates = ShouldHelper.treatAsList(Data.GAME_TEST.questionTemplates);
                    shouldResponseContent.winningComponents = ShouldHelper.treatAsList(Data.GAME_TEST.winningComponents);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.game, ['regionalSettingsIds', 'createDate', 'id', 'tenantId',
                    'stat_adsCount', 'stat_adsViewed', 'stat_averageAnswerSpeed', 'stat_bonusPointsPaid',
                    'stat_bonusPointsWon','stat_creditsPayed', 'stat_creditsWon', 'stat_gamesPlayed', 
                    'stat_moneyPayed', 'stat_moneyWon', 'stat_players', 'stat_poolsCount', 'stat_questionsAnswered',
                    'stat_questionsAnsweredRight', 'stat_questionsAnsweredWrong', 'stat_voucherWon',
                    'playerEntryFeeType', 'playerEntryFeeSettings', 'playerEntryFeeValues'
                    ]);
                }, done);
            });
            
            it('[' + serie + '] ' + 'SUCCESS: gameUpdate:create, default regionalSettingsIds', function (done) {
                var content = _.cloneDeep(Data.GAME_TEST);
                content.id = null;
                content.regionalSettingsIds = [];
                global.wsHelper.apiSecureSucc(serie, 'game/gameCreate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.cloneDeep(Data.GAME_TEST);
                    shouldResponseContent.poolsIds = ShouldHelper.treatAsList(Data.GAME_TEST.poolsIds);
                    shouldResponseContent.applicationsIds = ShouldHelper.treatAsList(Data.GAME_TEST.applicationsIds);
                    shouldResponseContent.questionTemplates = ShouldHelper.treatAsList(Data.GAME_TEST.questionTemplates);
                    shouldResponseContent.winningComponents = ShouldHelper.treatAsList(Data.GAME_TEST.winningComponents);
                    should(responseContent.game.regionalSettingsIds.total).be.greaterThan(0);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.game, ['regionalSettingsIds', 'createDate', 'id', 'tenantId',
                    'stat_adsCount', 'stat_adsViewed', 'stat_averageAnswerSpeed', 'stat_bonusPointsPaid',
                    'stat_bonusPointsWon','stat_creditsPayed', 'stat_creditsWon', 'stat_gamesPlayed', 
                    'stat_moneyPayed', 'stat_moneyWon', 'stat_players', 'stat_poolsCount', 'stat_questionsAnswered',
                    'stat_questionsAnsweredRight', 'stat_questionsAnsweredWrong', 'stat_voucherWon'
                    ]);
                }, done);
            });
            
            it('[' + serie + '] ' + 'SUCCESS: gameUpdate:update', function (done) {
                var content = {
                    id: DataIds.GAME_2_ID,
                    title: Data.GAME_1.title + ' updated',
                    regionalSettingsIds: Data.GAME_1.regionalSettingsIds,
                    questionComplexity: [
                        { level: 1, number: 20 },
                        { level: 2, number: 50 },
                        { level: 3, number: 100 }
                    ],
                    questionTemplates: [
                        {
                            questionTemplateId: DataIds.QUESTION_TEMPLATE_APPROVED_ID,
                            amount: 1,
                            order: 2
                        }
                    ],
                    winningComponents: [
                        {
                            winningComponentId: DataIds.WINNING_COMPONENT_2_ID,
                            isFree: 0,
                            rightAnswerPercentage: 88,
                            entryFeeAmount: 3.5,
                            entryFeeCurrency: 'MONEY'
                        },
                    ],
                };
                global.wsHelper.apiSecureSucc(serie, 'game/gameUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.cloneDeep(Data.GAME_2);
                    shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(content.regionalSettingsIds);
                    shouldResponseContent.applicationsIds = ShouldHelper.treatAsList(Data.GAME_2.applicationsIds);
                    shouldResponseContent.poolsIds = ShouldHelper.treatAsList(Data.GAME_2.poolsIds);
                    shouldResponseContent.questionComplexity = content.questionComplexity;
                    shouldResponseContent.questionTemplates = ShouldHelper.treatAsList(content.questionTemplates);
                    shouldResponseContent.winningComponents = ShouldHelper.treatAsList(content.winningComponents);
                    shouldResponseContent.title = content.title;
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.game, ['createDate', 'id', 'tenantId',
                    'stat_adsCount', 'stat_adsViewed', 'stat_averageAnswerSpeed', 'stat_bonusPointsPaid',
                    'stat_bonusPointsWon','stat_creditsPayed', 'stat_creditsWon', 'stat_gamesPlayed', 
                    'stat_moneyPayed', 'stat_moneyWon', 'stat_players', 'stat_poolsCount', 'stat_questionsAnswered',
                    'stat_questionsAnsweredRight', 'stat_questionsAnsweredWrong', 'stat_voucherWon'
                    ]);
                }, done);
            });
            
            it('[' + serie + '] ' + 'FAILURE ERR_VALIDATION_FAILED: gameUpdate:update', function (done) {
                var content = { id: DataIds.GAME_TEST_ID, title: 'updated', startDate: DateUtils.isoFuture(), endDate: DateUtils.isoNow() };
                global.wsHelper.apiSecureFail(serie, 'game/gameUpdate', ProfileData.CLIENT_INFO_1, content, Errors.QuestionApi.ValidationFailed, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_VALIDATION_FAILED: gameUpdate:update, promotion fields', function (done) {
                var content = _.cloneDeep(Data.GAME_1);
                content.isPromotion = 1;
                delete content.promotionStartDate;
                delete content.promotionEndDate;
                
                global.wsHelper.apiSecureFail(serie, 'game/gameUpdate', ProfileData.CLIENT_INFO_1, content, Errors.QuestionApi.ValidationFailed, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: gameUpdate:update', function (done) {
                var content = { id: DataIds.GAME_TEST_ID, title: 'updated' };
                global.wsHelper.apiSecureFail(serie, 'game/gameUpdate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });
        
        describe('[' + serie + '] ' + 'gameActivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: gameActivate', function (done) {
                var content = { id: DataIds.GAME_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'game/gameActivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'game/gameGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.game.status).be.equal('active');
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: gameActivate', function (done) {
                var content = { id: DataIds.GAME_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'game/gameActivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'gameDeactivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: gameDeactivate', function (done) {
                var content = { id: DataIds.GAME_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'game/gameDeactivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'game/gameGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.game.status).be.equal('inactive');
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: gameDeactivate', function (done) {
                var content = { id: DataIds.GAME_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'game/gameDeactivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'gameArchive', function () {
            it('[' + serie + '] ' + 'SUCCESS: gameArchive', function (done) {
                var content = { id: DataIds.GAME_INACTIVE_ID };
                global.wsHelper.apiSecureSucc(serie, 'game/gameArchive', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureFail(serie, 'game/gameActivate', ProfileData.CLIENT_INFO_1, content, Errors.GameApi.GameIsArchived, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: gameArchive', function (done) {
                var content = { id: DataIds.GAME_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'game/gameArchive', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'gameClose', function () {
            it('[' + serie + '] ' + 'SUCCESS: gameClose', function (done) {
                var content = { id: DataIds.GAME_NO_DEPENDENCIES_ID };
                global.wsHelper.apiSecureSucc(serie, 'game/gameClose', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'game/gameGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.game.isClosed).be.equal(1);
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: gameClose', function (done) {
                var content = { id: DataIds.GAME_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'game/gameClose', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'gameDelete', function () {
            it('[' + serie + '] ' + 'SUCCESS: gameDelete', function (done) {
                var content = { id: DataIds.GAME_NO_DEPENDENCIES_ID };
                global.wsHelper.apiSecureSucc(serie, 'game/gameDelete', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureFail(serie, 'game/gameDelete', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: gameDelete', function (done) {
                var content = { id: DataIds.GAME_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'game/gameDelete', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
            it('[' + serie + '] ' + 'FAILURE: ERR_DATABASE_EXISTING_DEPENDECIES gameDelete', function (done) {
                var content = { id: DataIds.GAME_1_ID };
                global.wsHelper.apiSecureFail(serie, 'game/gameDelete', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.SequelizeForeignKeyConstraintError, done);
            });
        });

        describe('[' + serie + '] ' + 'gameGet', function () {
            it('[' + serie + '] ' + 'SUCCESS: gameGet', function (done) {
                var content = { id: DataIds.GAME_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'game/gameGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                   should(responseContent.game.title).be.equal(Data.GAME_1.title);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: gameGet', function (done) {
                var content = { id: DataIds.GAME_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'game/gameGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'gameList', function () {
            it('[' + serie + '] ' + 'SUCCESS: gameList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.GAME_1_ID, questionTemplates: [{ questionTemplateId: DataIds.QUESTION_TEMPLATE_NEW_ID, amount: 1, order: 2 }] },
                    orderBy: [{ field: 'title', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'game/gameList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                   should(responseContent.items[0].title).be.equal(Data.GAME_1.title);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: gameList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.GAME_TEST_ID },
                    orderBy: [{ field: 'title', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'game/gameList', ProfileData.CLIENT_INFO_1, content, function (content) {
                    should.exist(content);
                    should(content)
                        .has.property('items')
                        .which.has.length(0);
                }, done);
            });
        });

        describe('[' + serie + '] ' + 'gameModuleList', function () {
            it('[' + serie + '] ' + 'SUCCESS: gameModuleList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.GAME_MODULE_1_ID },
                    orderBy: [{ field: 'name', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'game/gameModuleList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.GAME_MODULE_1);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.items[0], []);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: gameModuleList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.GAME_MODULE_TEST_ID },
                    orderBy: [{ field: 'name', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'game/gameModuleList', ProfileData.CLIENT_INFO_1, content, function (content) {
                    should.exist(content);
                    should(content)
                        .has.property('items')
                        .which.has.length(0);
                }, done);
            });
        });

        describe('[' + serie + '] ' + 'gameValidateById', function () {
            it('[' + serie + '] ' + 'SUCCESS: gameValidateById - game ' + DataIds.GAME_1_ID, function (done) {
                var content = { id: DataIds.GAME_1_ID, tenantId: DataIds.TENANT_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'game/gameValidateById', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    responseContent.warnings.should.be.empty();
                    responseContent.errors.should.have.length(2);
                    //responseContent.errors.should.be.empty(); pool2 has question2
                }, done);
            });

            it('[' + serie + '] ' + 'ERROR: gameValidateById - game ' + DataIds.GAME_2_ID, function (done) {
                var content = { id: DataIds.GAME_2_ID, tenantId: DataIds.TENANT_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'game/gameValidateById', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    responseContent.warnings.should.be.empty();
                    responseContent.errors.should.have.length(2);
                }, done);
            });
        });

        describe('[' + serie + '] ' + 'gameValidate', function () {
            it('[' + serie + '] ' + 'SUCCESS: gameValidate', function (done) {
                var content = {
                    poolIds: [DataIds.POOL_1_ID],
                    languageIds: [1,2], 
                    questionTemplateIds: [1], 
                    regionalSettingIds: [1],
                    isInternationalGame: 1, 
                    complexityLevel: { "1":1, "2":0, "3":0 },
                    complexitySpread: "fixed", 
                 };
                global.wsHelper.apiSecureSucc(serie, 'game/gameValidate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    responseContent.warnings.should.be.empty();
                    responseContent.errors.should.be.empty();
                }, done);
            });

            it('[' + serie + '] ' + 'ERROR: gameValidate', function (done) {
                var content = { 
                    poolIds: [DataIds.POOL_2_ID],
                    languageIds: [1,2], 
                    questionTemplateIds: [2], 
                    regionalSettingIds: [2],
                    isInternationalGame: 2, 
                    complexityLevel: { "1":1, "2":0, "3":0},
                    complexitySpread: "percentage", 
                 };
                global.wsHelper.apiSecureSucc(serie, 'game/gameValidate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    responseContent.warnings.should.be.empty();
                    responseContent.errors.should.have.length(2);
                }, done);
            });
        });

        
        describe('[' + serie + '] ' + 'game APIs, tenant specific', function () {
            it('[' + serie + '] ' + 'test game create/update', function (done) {
                testSessionTenantId = DataIds.TENANT_2_ID;
                var content = _.clone(Data.GAME_TEST);
                content.id = null;
                var newItemId;
                
                async.series([
                    // create new item
                    function (next) {
                        global.wsHelper.apiSecureSucc(serie, 'game/gameCreate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            newItemId = responseContent.game.id;
                        }, next);
                    },
                    
                    // check tenantId was set proper
                    function (next) {
                        Game.findOne({ where: { id: newItemId } }).then(function (item) {
                            try {
                                assert.strictEqual(item.get({plain: true}).tenantId, testSessionTenantId);
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        }).catch(function (err) {
                            return next(err);
                        });
                    },
                    
                    // deactivate so i can update
                    function (next) {
                        var content = { id: newItemId };
                        global.wsHelper.apiSecureSucc(serie, 'game/gameDeactivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                            setImmediate(function (done) {
                                global.wsHelper.apiSecureSucc(serie, 'game/gameGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                                    should(responseContent.game.status).be.equal('inactive');
                                }, next);
                            }, next);
                        });
                    },
                    
                    // update succesully
                    function (next) {
                        var content = { id: newItemId, title: 'name test updated' };
                        console.log('#######contnt:', content);
                        global.wsHelper.apiSecureSucc(serie, 'game/gameUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.game.title).be.equal(content.title);
                        }, next);
                    },
                    
                    // change session tenant and try to update an item not belong to session tenant, should return no record found error
                    function (next) {
                        testSessionTenantId = DataIds.TENANT_1_ID;
                        var content = { id: newItemId, title: 'name test' };
                        global.wsHelper.apiSecureFail(serie, 'game/gameUpdate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, next);
                    },
                    // restore session tenant
                    function (next) {
                        testSessionTenantId = DataIds.TENANT_2_ID;
                        return setImmediate(next);
                    }
                ], done);
            });
            
            it('[' + serie + '] ' + 'test question template get/list', function (done) {
                //return done();
                var item = _.clone(Data.GAME_TEST);
                item.id = null;
                var tenant1Items = [_.clone(item), _.clone(item)];
                var tenant2Items = [_.clone(item), _.clone(item)];
                
                async.series([
                    // create tenant 1 items
                    function (next) {
                        testSessionTenantId = DataIds.TENANT_1_ID;
                        async.mapSeries(tenant1Items, function (item, cbItem) {
                            global.wsHelper.apiSecureSucc(serie, 'game/gameCreate', ProfileData.CLIENT_INFO_1, item, function (responseContent) {
                                item.id = responseContent.game.id;
                            }, cbItem);
                       }, next);
                    },
                    
                    // create tenant 2 items
                    function (next) {
                        testSessionTenantId = DataIds.TENANT_2_ID;
                        async.mapSeries(tenant2Items, function (item, cbItem) {
                            global.wsHelper.apiSecureSucc(serie, 'game/gameCreate', ProfileData.CLIENT_INFO_1, item, function (responseContent) {
                                item.id = responseContent.game.id;
                            }, cbItem);
                       }, next);
                    },
                    
                    // get success
                    function (next) {
                        testSessionTenantId = DataIds.TENANT_1_ID;
                        var itm = _.clone(tenant1Items[0]);
                        var content = { id: itm.id };
                        global.wsHelper.apiSecureSucc(serie, 'game/gameGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            assert.strictEqual(responseContent.game.title, itm.title);
                        }, next);
                    },
                    
                    // list success
                    function (next) {
                        testSessionTenantId = DataIds.TENANT_1_ID;
                        var itm = tenant1Items[0];
                        var content = {
                            limit: Config.rdbms.limit,
                            offset: 0,
                            searchBy: { id: itm.id },
                            orderBy: [{ field: 'title', direction: 'asc' }]
                        };
                        global.wsHelper.apiSecureSucc(serie, 'game/gameList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            assert.strictEqual(responseContent.items[0].title, itm.title);
                        }, next);
                    },
                    
                    // get error, not belong to session tenant
                    function (next) {
                        testSessionTenantId = DataIds.TENANT_1_ID;
                        var itm = _.clone(tenant2Items[0]);
                        var content = { id: itm.id };
                        global.wsHelper.apiSecureFail(serie, 'game/gameGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, next);
                    },
                    
                    // list error
                    function (next) {
                        testSessionTenantId = DataIds.TENANT_1_ID;
                        var itm = tenant2Items[0];
                        var content = {
                            limit: Config.rdbms.limit,
                            offset: 0,
                            searchBy: { id: itm.id },
                            orderBy: [{ field: 'title', direction: 'asc' }]
                        };
                        global.wsHelper.apiSecureSucc(serie, 'game/gameList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should.exist(responseContent);
                            should(responseContent).has.property('items').which.has.length(0);
                        }, next);
                    }
                ], done);
            });
        });
        
        describe('[' + serie + '] ' + 'gameValidityCheck', function () {
            it('[' + serie + '] ' + 'SUCCESS: gameValidityCheck as CronMasterJob', function (done) {
                var content = { id: DataIds.GAME_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'game/gamePublish', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        var tickCompleteSpy = global.wsHelper.sinon.spy();
                        var gameValidityCheckJob = require('./../../jobs/gameValidityCheckJob.js');
                        gameValidityCheckJob.on(global.wsHelper.cmaster.EVENTS.TICK_COMPLETE, tickCompleteSpy);
                        gameValidityCheckJob.start();
                        setTimeout(function () {
                            should(tickCompleteSpy.called).be.true;
                            gameValidityCheckJob.stop();
                            return done();
                        }, 3000); 
                    }, done);
                });
            });
        });
        
    });
});
