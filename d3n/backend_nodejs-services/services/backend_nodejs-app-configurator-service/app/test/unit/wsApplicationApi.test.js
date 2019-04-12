var _ = require('lodash');
var should = require('should');
var assert = require('chai').assert;
var async = require('async');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var Database = require('nodejs-database').getInstance(Config);
var DataIds = require('./../config/_id.data.js');
var DataApplication = require('./../config/application.data.js');
var DataTenant = require('./../config/tenant.data.js');
var ProfileData = require('./../config/profile.data.js');
var Data = require('./../config/application.data.js');
var Application = Database.RdbmsService.Models.Application.Application;
var ShouldHelper = require('nodejs-automapper').getInstance(Config).ShouldHelper;
var tenantService = require('../../services/tenantService.js');

describe('WS Application API', function () {
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
        
        describe('[' + serie + '] ' + 'applicationUpdate', function () {
            it('[' + serie + '] ' + 'SUCCESS: applicationUpdate create', function (done) {
                var content = _.clone(Data.APPLICATION_TEST);
                content.id = null;
                global.wsHelper.apiSecureSucc(serie, 'application/applicationCreate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.APPLICATION_TEST);
                    shouldResponseContent.paymentTypesIds = ShouldHelper.treatAsList(Data.APPLICATION_TEST.paymentTypesIds);
                    shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(Data.APPLICATION_TEST.regionalSettingsIds);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.application, ['id', 'tenantId', 
                        'createDate', 'releaseDate', 'activeAnimationsIds', 'activeCharactersIds', 'gameModulesIds', 
                        'gamesIds', 'deployment',
                        'stat_adsViewed', 'stat_bettingGames', 'stat_bonusPointsWon', 'stat_creditsPurchased',
                        'stat_creditsWon', 'stat_deinstallations', 'stat_downloads', 'stat_friendsInvited',
                        'stat_games', 'stat_gamesPlayed', 'stat_installations', 'stat_moneyCharged', 
                        'stat_moneyGames', 'stat_moneyWon', 'stat_players', 'stat_quizGames', 'stat_voucherWon'
                    ]);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: applicationUpdate update', function (done) {
                var content = {
                    id: DataIds.APPLICATION_1_ID,
                    description: 'updated',
                    configuration: {
                        menu_color: { appArea: '999999' }
                    },
                };
                global.wsHelper.apiSecureSucc(serie, 'application/applicationUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should(responseContent.application.description).be.equal(content.description);
                    should(responseContent.application.configuration).be.deepEqual(content.configuration);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE: applicationUpdate create - unique title per tenant', function (done) {
                var content = _.clone(Data.APPLICATION_TEST);
                content.id = null;
                content.title = 'Application one title';
                global.wsHelper.apiSecureFail(serie, 'application/applicationCreate', ProfileData.CLIENT_INFO_1, content, Errors.ApplicationApi.AppNameNotUnquePerTenant, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: applicationUpdate update', function (done) {
                var content = { id: DataIds.APPLICATION_TEST_ID, description: 'updated' };
                global.wsHelper.apiSecureFail(serie, 'application/applicationUpdate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'applicationDelete', function () {
            it('[' + serie + '] ' + 'SUCCESS: applicationDelete', function (done) {
                var content = { id: DataIds.APPLICATION_NO_DEPENDENCIES_ID };
                global.wsHelper.apiSecureSucc(serie, 'application/applicationDelete', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureFail(serie, 'application/applicationGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_QAPI_FATAL_ERROR: applicationDelete', function (done) {
                var content = { id: DataIds.APPLICATION_1_ID };
                global.wsHelper.apiSecureFail(serie, 'application/applicationDelete', ProfileData.CLIENT_INFO_1, content, Errors.QuestionApi.ForeignKeyConstraintViolation, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: applicationDelete', function (done) {
                var content = { id: DataIds.APPLICATION_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'application/applicationDelete', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'applicationList', function () {
            it('[' + serie + '] ' + 'SUCCESS: applicationList - 1 item', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: {
                        tenantId: DataIds.TENANT_1_ID,
                        creatorResourceId: '$user'
                    },
                    orderBy: [{ field: 'id', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'application/applicationList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.APPLICATION_1);
                    shouldResponseContent.activeAnimationsIds = ShouldHelper.treatAsList(Data.APPLICATION_1.activeAnimationsIds);
                    shouldResponseContent.activeCharactersIds = ShouldHelper.treatAsList(Data.APPLICATION_1.activeCharactersIds);
                    shouldResponseContent.gameModulesIds = ShouldHelper.treatAsList(Data.APPLICATION_1.gameModulesIds);
                    shouldResponseContent.gamesIds = ShouldHelper.treatAsList(Data.APPLICATION_1.gamesIds);
                    shouldResponseContent.paymentTypesIds = ShouldHelper.treatAsList(Data.APPLICATION_1.paymentTypesIds);
                    shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(Data.APPLICATION_1.regionalSettingsIds);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.items[0], ['tenantId',
                        'stat_adsViewed', 'stat_bettingGames', 'stat_bonusPointsWon', 'stat_creditsPurchased',
                        'stat_creditsWon', 'stat_deinstallations', 'stat_downloads', 'stat_friendsInvited',
                        'stat_games', 'stat_gamesPlayed', 'stat_installations', 'stat_moneyCharged', 
                        'stat_moneyGames', 'stat_moneyWon', 'stat_players', 'stat_quizGames', 'stat_voucherWon'
                    ]);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: applicationList - no item', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.APPLICATION_TEST_ID },
                    orderBy: [{ field: 'id', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'application/applicationList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent).has.property('items').which.has.length(0);
                }, done);
            });
        });

        describe('[' + serie + '] ' + 'applicationGet', function () {
            it('[' + serie + '] ' + 'SUCCESS: applicationGet', function (done) {
                var content = { id: DataIds.APPLICATION_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'application/applicationGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.APPLICATION_1);;
                    shouldResponseContent.activeAnimationsIds = ShouldHelper.treatAsList(Data.APPLICATION_1.activeAnimationsIds);
                    shouldResponseContent.activeCharactersIds = ShouldHelper.treatAsList(Data.APPLICATION_1.activeCharactersIds);
                    shouldResponseContent.gameModulesIds = ShouldHelper.treatAsList(Data.APPLICATION_1.gameModulesIds);
                    shouldResponseContent.gamesIds = ShouldHelper.treatAsList(Data.APPLICATION_1.gamesIds);
                    shouldResponseContent.paymentTypesIds = ShouldHelper.treatAsList(Data.APPLICATION_1.paymentTypesIds);
                    shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(Data.APPLICATION_1.regionalSettingsIds);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.application, ['tenantId',
                        'stat_adsViewed', 'stat_bettingGames', 'stat_bonusPointsWon', 'stat_creditsPurchased',
                        'stat_creditsWon', 'stat_deinstallations', 'stat_downloads', 'stat_friendsInvited',
                        'stat_games', 'stat_gamesPlayed', 'stat_installations', 'stat_moneyCharged', 
                        'stat_moneyGames', 'stat_moneyWon', 'stat_players', 'stat_quizGames', 'stat_voucherWon'
                    ]);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: applicationGet', function (done) {
                var content = { id: DataIds.APPLICATION_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'application/applicationGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'applicationActivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: applicationActivate', function (done) {
                var content = { id: DataIds.APPLICATION_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'application/applicationActivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'application/applicationGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.application.status).be.equal(Application.constants().STATUS_ACTIVE);
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: applicationActivate', function (done) {
                var content = { id: DataIds.APPLICATION_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'application/applicationActivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'applicationDeactivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: applicationDeactivate', function (done) {
                var content = { id: DataIds.APPLICATION_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'application/applicationDeactivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'application/applicationGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.application.status).be.equal(Application.constants().STATUS_INACTIVE);
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: applicationDeactivate', function (done) {
                var content = { id: DataIds.APPLICATION_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'application/applicationDeactivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'applicationArchive', function () {
            it('[' + serie + '] ' + 'SUCCESS: applicationArchive', function (done) {
                var content = { id: DataIds.APPLICATION_INACTIVE_ID };
                global.wsHelper.apiSecureSucc(serie, 'application/applicationArchive', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureFail(serie, 'application/applicationActivate', ProfileData.CLIENT_INFO_1, content, Errors.ApplicationApi.AppIsArchived, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: applicationArchive', function (done) {
                var content = { id: DataIds.APPLICATION_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'application/applicationArchive', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'applicationAnimationList', function () {
            it('[' + serie + '] ' + 'SUCCESS: applicationAnimationList - 1 item', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.APPLICATION_ANIMATION_1_ID },
                    orderBy: [{ field: 'id', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'application/applicationAnimationList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.APPLICATION_ANIMATION_1);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.items[0], []);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: applicationAnimationList - no item', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.APPLICATION_ANIMATION_TEST_ID },
                    orderBy: [{ field: 'id', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'application/applicationAnimationList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent).has.property('items').which.has.length(0);
                }, done);
            });
        });

        describe('[' + serie + '] ' + 'applicationCharacterList', function () {
            it('[' + serie + '] ' + 'SUCCESS: applicationCharacterList - 1 item', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.APPLICATION_CHARACTER_1_ID },
                    orderBy: [{ field: 'id', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'application/applicationCharacterList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.APPLICATION_CHARACTER_1);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.items[0], []);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: applicationCharacterList - no item', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.APPLICATION_CHARACTER_TEST_ID },
                    orderBy: [{ field: 'id', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'application/applicationCharacterList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent).has.property('items').which.has.length(0);
                }, done);
            });
        });

        describe('[' + serie + '] ' + 'applicationAnimationActivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: applicationAnimationActivate', function (done) {
                var content = { applicationId: DataIds.APPLICATION_1_ID, applicationAnimationId: DataIds.APPLICATION_ANIMATION_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'application/applicationAnimationActivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        content = { id: DataIds.APPLICATION_1_ID };
                        global.wsHelper.apiSecureSucc(serie, 'application/applicationGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.application.activeAnimationsIds.total).be.equal(1);
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: applicationAnimationActivate', function (done) {
                var content = { applicationId: DataIds.APPLICATION_TEST_ID, applicationAnimationId: DataIds.APPLICATION_ANIMATION_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'application/applicationAnimationActivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'applicationAnimationDeactivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: applicationAnimationDeactivate', function (done) {
                var content = { applicationId: DataIds.APPLICATION_1_ID, applicationAnimationId: DataIds.APPLICATION_ANIMATION_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'application/applicationAnimationDeactivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        content = { id: DataIds.APPLICATION_1_ID };
                        global.wsHelper.apiSecureSucc(serie, 'application/applicationGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.application.activeAnimationsIds.total).be.equal(0);
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: applicationAnimationDeactivate', function (done) {
                var content = { applicationId: DataIds.APPLICATION_TEST_ID, applicationAnimationId: 1231 };
                global.wsHelper.apiSecureFail(serie, 'application/applicationAnimationDeactivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'applicationCharacterActivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: applicationCharacterActivate', function (done) {
                var content = { applicationId: DataIds.APPLICATION_1_ID, applicationCharacterId: DataIds.APPLICATION_CHARACTER_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'application/applicationCharacterActivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        content = { id: DataIds.APPLICATION_1_ID };
                        global.wsHelper.apiSecureSucc(serie, 'application/applicationGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.application.activeCharactersIds.total).be.equal(1);
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: applicationCharacterActivate', function (done) {
                var content = { applicationId: DataIds.APPLICATION_TEST_ID, applicationCharacterId: DataIds.APPLICATION_CHARACTER_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'application/applicationCharacterActivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'applicationCharacterDeactivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: applicationCharacterDeactivate', function (done) {
                var content = { applicationId: DataIds.APPLICATION_1_ID, applicationCharacterId: DataIds.APPLICATION_CHARACTER_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'application/applicationCharacterDeactivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        content = { id: DataIds.APPLICATION_1_ID };
                        global.wsHelper.apiSecureSucc(serie, 'application/applicationGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.application.activeCharactersIds.total).be.equal(0);
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: applicationCharacterDeactivate', function (done) {
                var content = { applicationId: DataIds.APPLICATION_TEST_ID, applicationCharacterId: DataIds.APPLICATION_CHARACTER_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'application/applicationCharacterDeactivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'applicationGameAdd', function () {
            it('[' + serie + '] ' + 'SUCCESS: applicationGameAdd', function (done) {
                var content = { applicationId: DataIds.APPLICATION_1_ID, gameId: DataIds.GAME_2_ID };
                global.wsHelper.apiSecureSucc(serie, 'application/applicationGameAdd', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        content = { id: DataIds.APPLICATION_1_ID };
                        global.wsHelper.apiSecureSucc(serie, 'application/applicationGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            var shouldResponseContent = _.clone(Data.APPLICATION_1);;
                            shouldResponseContent.activeAnimationsIds = ShouldHelper.treatAsList(Data.APPLICATION_1.activeAnimationsIds);
                            shouldResponseContent.activeCharactersIds = ShouldHelper.treatAsList(Data.APPLICATION_1.activeCharactersIds);
                            shouldResponseContent.gameModulesIds = ShouldHelper.treatAsList(Data.APPLICATION_1.gameModulesIds);
                            shouldResponseContent.gamesIds = ShouldHelper.treatAsList([DataIds.GAME_1_ID, DataIds.GAME_2_ID]);
                            shouldResponseContent.paymentTypesIds = ShouldHelper.treatAsList(Data.APPLICATION_1.paymentTypesIds);
                            shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(Data.APPLICATION_1.regionalSettingsIds);
                            ShouldHelper.deepEqual(shouldResponseContent, responseContent.application, ['tenantId',
                                'stat_adsViewed', 'stat_bettingGames', 'stat_bonusPointsWon', 'stat_creditsPurchased',
                                'stat_creditsWon', 'stat_deinstallations', 'stat_downloads', 'stat_friendsInvited',
                                'stat_games', 'stat_gamesPlayed', 'stat_installations', 'stat_moneyCharged', 
                                'stat_moneyGames', 'stat_moneyWon', 'stat_players', 'stat_quizGames', 'stat_voucherWon'
                            ]);
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_FATAL_ERROR: applicationGameAdd', function (done) {
                var content = { applicationId: DataIds.APPLICATION_TEST_ID, gameId: DataIds.GAME_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'application/applicationGameAdd', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.FatalError, done);
            });
        });

        describe('[' + serie + '] ' + 'applicationGameRemove', function () {
            it('[' + serie + '] ' + 'SUCCESS: applicationGameRemove', function (done) {
                var content = { id: DataIds.GAME_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'game/gameActivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        var content = { applicationId: DataIds.APPLICATION_1_ID, gameId: DataIds.GAME_1_ID };
                        global.wsHelper.apiSecureSucc(serie, 'application/applicationGameRemove', ProfileData.CLIENT_INFO_1, content, null, function () {
                            setImmediate(function (done) {
                                content = { id: DataIds.APPLICATION_1_ID };
                                global.wsHelper.apiSecureSucc(serie, 'application/applicationGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                                    var shouldResponseContent = _.clone(Data.APPLICATION_1);
                                    shouldResponseContent.activeAnimationsIds = ShouldHelper.treatAsList(Data.APPLICATION_1.activeAnimationsIds);
                                    shouldResponseContent.activeCharactersIds = ShouldHelper.treatAsList(Data.APPLICATION_1.activeCharactersIds);
                                    shouldResponseContent.gameModulesIds = ShouldHelper.treatAsList(Data.APPLICATION_1.gameModulesIds);
                                    shouldResponseContent.gamesIds = ShouldHelper.treatAsList([]);
                                    shouldResponseContent.paymentTypesIds = ShouldHelper.treatAsList(Data.APPLICATION_1.paymentTypesIds);
                                    shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(Data.APPLICATION_1.regionalSettingsIds);
                                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.application, ['tenantId',
                                        'stat_adsViewed', 'stat_bettingGames', 'stat_bonusPointsWon', 'stat_creditsPurchased',
                                        'stat_creditsWon', 'stat_deinstallations', 'stat_downloads', 'stat_friendsInvited',
                                        'stat_games', 'stat_gamesPlayed', 'stat_installations', 'stat_moneyCharged',
                                        'stat_moneyGames', 'stat_moneyWon', 'stat_players', 'stat_quizGames', 'stat_voucherWon'
                                    ]);
                                }, done);
                            }, done);
                        });
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: applicationGameRemove', function (done) {
                var content = { applicationId: DataIds.APPLICATION_TEST_ID, gameId: DataIds.GAME_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'application/applicationGameRemove', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'gameModuleActivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: gameModuleActivate', function (done) {
                var content = { applicationId: DataIds.APPLICATION_1_ID, gameModuleId: DataIds.GAME_MODULE_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'application/gameModuleActivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        content = { id: DataIds.APPLICATION_1_ID };
                        global.wsHelper.apiSecureSucc(serie, 'application/applicationGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.application.gameModulesIds.total).be.equal(1);
                        }, done);
                    }, done);
                });
            });
        });

        describe('[' + serie + '] ' + 'gameModuleDeactivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: gameModuleDeactivate', function (done) {
                var content = { applicationId: DataIds.APPLICATION_1_ID, gameModuleId: DataIds.GAME_MODULE_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'application/gameModuleDeactivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        content = { id: DataIds.APPLICATION_1_ID };
                        global.wsHelper.apiSecureSucc(serie, 'application/applicationGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.application.gameModulesIds.total).be.equal(0);
                        }, done);
                    }, done);
                });
            });
        });

        describe('[' + serie + '] ' + 'application APIs, tenant specific', function () {
            it('[' + serie + '] ' + 'test application create/update', function (done) {
                testSessionTenantId = DataIds.TENANT_2_ID;
                var content = _.clone(Data.APPLICATION_TEST);
                content.id = null;
                var newItemId;
                
                async.series([
                    // create new item
                    function (next) {
                        global.wsHelper.apiSecureSucc(serie, 'application/applicationCreate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            newItemId = responseContent.application.id;
                            var shouldResponseContent = _.clone(Data.APPLICATION_TEST);
                        }, next);
                    },
                    
                    // check tenantId was set proper
                    function (next) {
                        Application.findOne({ where: { id: newItemId } }).then(function (item) {
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
                    
                    // update succesully
                    function (next) {
                        var content = { id: newItemId, title: 'name test updated' };
                        global.wsHelper.apiSecureSucc(serie, 'application/applicationUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.application.title).be.equal(content.title);
                        }, next);
                    },
                    
                    // change session tenant and try to update an item not belong to session tenant, should return no record found error
                    function (next) {
                        testSessionTenantId = DataIds.TENANT_1_ID;
                        var content = { id: newItemId, title: 'name test' };
                        global.wsHelper.apiSecureFail(serie, 'application/applicationUpdate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, next);
                    },
                    // restore session tenant
                    function (next) {
                        testSessionTenantId = DataIds.TENANT_2_ID;
                        return setImmediate(next);
                    }
                ], done);
            });
            
            it('[' + serie + '] ' + 'test application get/list', function (done) {
                //return done();
                var item = _.clone(Data.APPLICATION_TEST);
                item.id = null;
                var tenant1Items = [_.clone(item), _.clone(item)];
                var tenant2Items = [_.clone(item), _.clone(item)];
                
                async.series([
                    // create tenant 1 items
                    function (next) {
                        testSessionTenantId = DataIds.TENANT_1_ID;
                        async.mapSeries(tenant1Items, function (item, cbItem) {
                            item.title = 'title ' + _.uniqueId();
                            global.wsHelper.apiSecureSucc(serie, 'application/applicationCreate', ProfileData.CLIENT_INFO_1, item, function (responseContent) {
                                item.id = responseContent.application.id;
                            }, cbItem);
                       }, next);
                    },
                    
                    // create tenant 2 items
                    function (next) {
                        testSessionTenantId = DataIds.TENANT_2_ID;
                        async.mapSeries(tenant2Items, function (item, cbItem) {
                            item.title = 'title ' + _.uniqueId();
                            global.wsHelper.apiSecureSucc(serie, 'application/applicationCreate', ProfileData.CLIENT_INFO_1, item, function (responseContent) {
                                item.id = responseContent.application.id;
                            }, cbItem);
                       }, next);
                    },
                    
                    // get success
                    function (next) {
                        testSessionTenantId = DataIds.TENANT_1_ID;
                        var itm = _.clone(tenant1Items[0]);
                        var content = { id: itm.id };
                        global.wsHelper.apiSecureSucc(serie, 'application/applicationGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            assert.strictEqual(responseContent.application.title, itm.title);
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
                        global.wsHelper.apiSecureSucc(serie, 'application/applicationList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            assert.strictEqual(responseContent.items[0].title, itm.title);
                        }, next);
                    },
                    
                    // get error, not belong to session tenant
                    function (next) {
                        testSessionTenantId = DataIds.TENANT_1_ID;
                        var itm = _.clone(tenant2Items[0]);
                        var content = { id: itm.id };
                        global.wsHelper.apiSecureFail(serie, 'application/applicationGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, next);
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
                        global.wsHelper.apiSecureSucc(serie, 'application/applicationList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should.exist(responseContent);
                            should(responseContent).has.property('items').which.has.length(0);
                        }, next);
                    }
                ], done);
            });
        });
        
        
        
    });
});