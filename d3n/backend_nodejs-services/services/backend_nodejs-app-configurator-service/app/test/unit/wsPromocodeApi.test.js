var _ = require('lodash');
var fs = require('fs');
var path = require('path');
var async = require('async');
var should = require('should');
var assert = require('chai').assert;
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var Promocode = Database.RdbmsService.Models.PromocodeManager.Promocode;
var PromocodeCampaign = Database.RdbmsService.Models.PromocodeManager.PromocodeCampaign;
var DataIds = require('./../config/_id.data.js');
var ProfileData = require('./../config/profile.data.js');
var Data = require('./../config/promocode.data.js');
var ShouldHelper = require('nodejs-automapper').getInstance(Config).ShouldHelper;
var TenantService = require('../../services/tenantService.js');
var PromocodeService = require('../../services/promocodeService.js');
var Aerospike = require('nodejs-aerospike').getInstance(Config);
var KeyvalueService = Aerospike.KeyvalueService;
var AerospikePromocodeCounter = KeyvalueService.Models.AerospikePromocodeCounter;

describe('WS Promocode API', function () {
    this.timeout(20000);
    global.wsHelper.series().forEach(function (serie) {
        var stub_getSessionTenantId;
        var stub_promocodeServiceLoadCodes;
        var stub_promocodeServiceDeleteCodes;
        var testSessionTenantId = DataIds.TENANT_1_ID;
        before(function (done) {
            stub_getSessionTenantId = global.wsHelper.sinon.stub(TenantService, 'getSessionTenantId', function (message, clientSession, cb) {
                return cb(false, testSessionTenantId);
            });
            stub_promocodeServiceLoadCodes = global.wsHelper.sinon.stub(PromocodeService, 'loadCodes', function (key, callback) {
                fs.readFile(path.join(__dirname, '../resources', key), 'utf8', function (err, data) {
                    if (err) {
                        return console.log(err);
                    }
                    console.log(data);
                    return callback(false, data);
                });
            });
            stub_promocodeServiceDeleteCodes = global.wsHelper.sinon.stub(PromocodeService, 'deleteCodes', function (key, callback) {
                return setImmediate(callback, false);
            });
            return setImmediate(done);
        });
        after(function (done) {
            stub_getSessionTenantId.restore();
            stub_promocodeServiceLoadCodes.restore();
            stub_promocodeServiceDeleteCodes.restore();
            return setImmediate(done);
        });
        
        describe('[' + serie + '] ' + 'promocodeCampaignUpdate', function () {
            it('[' + serie + '] ' + 'SUCCESS: promocodeCampaignUpdate:create', function (done) {
                var content = _.cloneDeep(Data.PROMOCODE_CAMPAIGN_TEST);
                content.id = null;
                global.wsHelper.apiSecureSucc(serie, 'promocodeManager/promocodeCampaignCreate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.cloneDeep(Data.PROMOCODE_CAMPAIGN_TEST);
                    shouldResponseContent.applicationsIds = ShouldHelper.treatAsList(Data.PROMOCODE_CAMPAIGN_TEST.applicationsIds);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.promocodeCampaign, ['createDate', 'id', 'tenantId', 'status',
                     'stat_bonusPointsPaid', 'stat_creditsPaid', 
                     'stat_inventoryCount', 'stat_moneyPaid', 'stat_used']);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: promocodeCampaignUpdate:update', function (done) {
                var content = {
                    id: DataIds.PROMOCODE_CAMPAIGN_2_ID,
                    campaignName: Data.PROMOCODE_CAMPAIGN_2.campaignName + ' updated'
                };
                global.wsHelper.apiSecureSucc(serie, 'promocodeManager/promocodeCampaignUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.cloneDeep(Data.PROMOCODE_CAMPAIGN_2);
                    shouldResponseContent.applicationsIds = ShouldHelper.treatAsList(Data.PROMOCODE_CAMPAIGN_2.applicationsIds);
                    shouldResponseContent.campaignName = content.campaignName;
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.promocodeCampaign, ['createDate', 'id', 'tenantId', 'status',
                     'stat_bonusPointsPaid', 'stat_creditsPaid', 
                     'stat_inventoryCount', 'stat_moneyPaid', 'stat_used']);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_VALIDATION_FAILED: promocodeCampaignUpdate:update', function (done) {
                var content = { id: DataIds.PROMOCODE_CAMPAIGN_TEST_ID, title: 'updated', startDate: DateUtils.isoFuture(), endDate: DateUtils.isoNow() };
                global.wsHelper.apiSecureFail(serie, 'promocodeManager/promocodeCampaignUpdate', ProfileData.CLIENT_INFO_1, content, Errors.QuestionApi.ValidationFailed, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: promocodeCampaignUpdate:update', function (done) {
                var content = { id: DataIds.PROMOCODE_CAMPAIGN_TEST_ID, title: 'updated' };
                global.wsHelper.apiSecureFail(serie, 'promocodeManager/promocodeCampaignUpdate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'promocodeCampaignActivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: promocodeCampaignActivate', function (done) {
                var content = { id: DataIds.PROMOCODE_CAMPAIGN_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'promocodeManager/promocodeCampaignActivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'promocodeManager/promocodeCampaignGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.promocodeCampaign.status).be.equal('active');
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: promocodeCampaignActivate', function (done) {
                var content = { id: DataIds.PROMOCODE_CAMPAIGN_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'promocodeManager/promocodeCampaignActivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'promocodeCampaignDeactivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: promocodeCampaignDeactivate', function (done) {
                var content = { id: DataIds.PROMOCODE_CAMPAIGN_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'promocodeManager/promocodeCampaignDeactivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'promocodeManager/promocodeCampaignGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.promocodeCampaign.status).be.equal('inactive');
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: promocodeCampaignDeactivate', function (done) {
                var content = { id: DataIds.PROMOCODE_CAMPAIGN_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'promocodeManager/promocodeCampaignDeactivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'promocodeCampaignGenerateFromFile', function () {
            it('[' + serie + '] ' + 'SUCCESS: promocodeCampaignGenerateFromFile', function (done) {
                var content = { id: DataIds.PROMOCODE_CAMPAIGN_1_ID, key: 'promocode.test.csv' };
                global.wsHelper.apiSecureSucc(serie, 'promocodeManager/promocodeCampaignGenerateFromFile', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'promocodeManager/promocodeCampaignGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.promocodeCampaign.status).be.equal('active');
                        }, function () {
                            setImmediate(function (done) {
                                var content = {
                                    limit: Config.rdbms.limit,
                                    offset: 0,
                                    promocodeCampaignId: DataIds.PROMOCODE_CAMPAIGN_1_ID
                                };
                                global.wsHelper.apiSecureSucc(serie, 'promocodeManager/promocodeList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                                    should.exist(responseContent);
                                    should(responseContent)
                                        .has.property('items')
                                        .which.has.length(6);
                                }, done);
                            }, done);
                        });
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: promocodeCampaignGenerateFromFile', function (done) {
                var content = { id: DataIds.PROMOCODE_CAMPAIGN_TEST_ID, key: 'promocode.test.csv' };
                global.wsHelper.apiSecureFail(serie, 'promocodeManager/promocodeCampaignGenerateFromFile', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'promocodeCampaignArchive', function () {
            it('[' + serie + '] ' + 'SUCCESS: promocodeCampaignArchive', function (done) {
                var content = { id: DataIds.PROMOCODE_CAMPAIGN_INACTIVE_ID };
                global.wsHelper.apiSecureSucc(serie, 'promocodeManager/promocodeCampaignArchive', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureFail(serie, 'promocodeManager/promocodeCampaignActivate', ProfileData.CLIENT_INFO_1, content, Errors.PromocodeApi.PromocodeCampaignIsArchived, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: promocodeCampaignArchive', function (done) {
                var content = { id: DataIds.PROMOCODE_CAMPAIGN_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'promocodeManager/promocodeCampaignArchive', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'promocodeCampaignDelete', function () {
            it('[' + serie + '] ' + 'SUCCESS: promocodeCampaignDelete', function (done) {
                var content = { id: DataIds.PROMOCODE_CAMPAIGN_INACTIVE_ID };
                global.wsHelper.apiSecureSucc(serie, 'promocodeManager/promocodeCampaignDelete', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureFail(serie, 'promocodeManager/promocodeCampaignDelete', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: promocodeCampaignDelete', function (done) {
                var content = { id: DataIds.PROMOCODE_CAMPAIGN_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'promocodeManager/promocodeCampaignDelete', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
            it('[' + serie + '] ' + 'FAILURE: ERR_DATABASE_EXISTING_DEPENDECIES promocodeCampaignDelete', function (done) {
                var content = { id: DataIds.PROMOCODE_CAMPAIGN_1_ID };
                global.wsHelper.apiSecureFail(serie, 'promocodeManager/promocodeCampaignDelete', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.SequelizeForeignKeyConstraintError, done);
            });
        });

        describe('[' + serie + '] ' + 'promocodeCampaignGet', function () {
            it('[' + serie + '] ' + 'SUCCESS: promocodeCampaignGet', function (done) {
                var content = { id: DataIds.PROMOCODE_CAMPAIGN_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'promocodeManager/promocodeCampaignGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                   should(responseContent.promocodeCampaign.title).be.equal(Data.PROMOCODE_CAMPAIGN_1.title);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: promocodeCampaignGet', function (done) {
                var content = { id: DataIds.PROMOCODE_CAMPAIGN_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'promocodeManager/promocodeCampaignGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'promocodeCampaignList', function () {
            it('[' + serie + '] ' + 'SUCCESS: promocodeCampaignList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.PROMOCODE_CAMPAIGN_1_ID },
                    orderBy: [{ field: 'campaignName', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'promocodeManager/promocodeCampaignList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                   should(responseContent.items[0].title).be.equal(Data.PROMOCODE_CAMPAIGN_1.title);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: promocodeCampaignList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.PROMOCODE_CAMPAIGN_TEST_ID },
                    orderBy: [{ field: 'campaignName', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'promocodeManager/promocodeCampaignList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent)
                        .has.property('items')
                        .which.has.length(0);
                }, done);
            });
        });

        describe('[' + serie + '] ' + 'promocodeCreate', function () {
            it('[' + serie + '] ' + 'SUCCESS: promocodeCreate two unique codes, code is null', function (done) {
                var content = {
                    promocodeCampaignId: DataIds.PROMOCODE_CAMPAIGN_1_ID,
                    number: 2,
                    isUnique: 1,
                    code: null,
                    isQRCodeRequired: 1,
                    usage: Promocode.constants().USAGE_MULTI_PER_PLAYER,
                    numberOfUses: 3
                };
                content.id = null;
                global.wsHelper.apiSecureSucc(serie, 'promocodeManager/promocodeCreate', ProfileData.CLIENT_INFO_1, content, null, function (responseContent) {
                    setImmediate(function (done) {
                        var content = {
                            limit: Config.rdbms.limit,
                            offset: 0,
                            promocodeCampaignId: DataIds.PROMOCODE_CAMPAIGN_1_ID
                        };
                        global.wsHelper.apiSecureSucc(serie, 'promocodeManager/promocodeList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should.exist(responseContent);
                            should(responseContent)
                                .has.property('items')
                                .which.has.length(3);
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'SUCCESS: promocodeCreate two non-unique codes, code is null', function (done) {
                var content = {
                    promocodeCampaignId: DataIds.PROMOCODE_CAMPAIGN_1_ID,
                    number: 2,
                    isUnique: 0,
                    code: null,
                    isQRCodeRequired: 1,
                    usage: Promocode.constants().USAGE_MULTI_PER_PLAYER,
                    numberOfUses: 3
                };
                content.id = null;
                global.wsHelper.apiSecureSucc(serie, 'promocodeManager/promocodeCreate', ProfileData.CLIENT_INFO_1, content, null, function (responseContent) {
                    setImmediate(function (done) {
                        var content = {
                            limit: Config.rdbms.limit,
                            offset: 0,
                            promocodeCampaignId: DataIds.PROMOCODE_CAMPAIGN_1_ID
                        };
                        global.wsHelper.apiSecureSucc(serie, 'promocodeManager/promocodeList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should.exist(responseContent);
                            should(responseContent)
                                .has.property('items')
                                .which.has.length(3);
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'SUCCESS: promocodeCreate two unique codes', function (done) {
                var content = {
                    promocodeCampaignId: DataIds.PROMOCODE_CAMPAIGN_1_ID,
                    number: 2,
                    isUnique: 1,
                    code: 'REBATE_10_PERCENT',
                    isQRCodeRequired: 1,
                    usage: Promocode.constants().USAGE_MULTI_PER_PLAYER,
                    numberOfUses: 3
                };
                content.id = null;
                global.wsHelper.apiSecureSucc(serie, 'promocodeManager/promocodeCreate', ProfileData.CLIENT_INFO_1, content, null, function (responseContent) {
                    setImmediate(function (done) {
                        var content = {
                            limit: Config.rdbms.limit,
                            offset: 0,
                            promocodeCampaignId: DataIds.PROMOCODE_CAMPAIGN_1_ID
                        };
                        global.wsHelper.apiSecureSucc(serie, 'promocodeManager/promocodeList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should.exist(responseContent);
                            should(responseContent)
                                .has.property('items')
                                .which.has.length(3);
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'SUCCESS: promocodeCreate two non-unique codes', function (done) {
                var content = {
                    promocodeCampaignId: DataIds.PROMOCODE_CAMPAIGN_1_ID,
                    number: 2,
                    isUnique: 0,
                    code: 'REBATE_10_PERCENT',
                    isQRCodeRequired: 1,
                    usage: Promocode.constants().USAGE_MULTI_PER_PLAYER,
                    numberOfUses: 3
                };
                content.id = null;
                global.wsHelper.apiSecureSucc(serie, 'promocodeManager/promocodeCreate', ProfileData.CLIENT_INFO_1, content, null, function (responseContent) {
                    setImmediate(function (done) {
                        var content = {
                            limit: Config.rdbms.limit,
                            offset: 0,
                            promocodeCampaignId: DataIds.PROMOCODE_CAMPAIGN_1_ID
                        };
                        global.wsHelper.apiSecureSucc(serie, 'promocodeManager/promocodeList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should.exist(responseContent);
                            should(responseContent)
                                .has.property('items')
                                .which.has.length(3);
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: promocodeCreate', function (done) {
                var content = {
                    promocodeCampaignId: DataIds.PROMOCODE_CAMPAIGN_TEST_ID,
                    number: 2,
                    isUnique: 0,
                    code: 'REBATE_10_PERCENT',
                    isQRCodeRequired: 1,
                    usage: Promocode.constants().USAGE_MULTI_PER_PLAYER,
                    numberOfUses: 3
                };
                global.wsHelper.apiSecureFail(serie, 'promocodeManager/promocodeCreate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'promocodeList', function () {
            it('[' + serie + '] ' + 'SUCCESS: promocodeList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    promocodeCampaignId: DataIds.PROMOCODE_CAMPAIGN_1_ID
                };
                global.wsHelper.apiSecureSucc(serie, 'promocodeManager/promocodeList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent)
                        .has.property('items')
                        .which.has.length(2);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: promocodeList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    promocodeCampaignId: DataIds.PROMOCODE_CAMPAIGN_TEST_ID
                };
                global.wsHelper.apiSecureSucc(serie, 'promocodeManager/promocodeList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent)
                        .has.property('items')
                        .which.has.length(0);
                }, done);
            });
        });

        describe('[' + serie + '] ' + 'promocodeInstanceList', function () {
            it('[' + serie + '] ' + 'SUCCESS: promocodeInstanceList', function (done) {
                var content = { id: DataIds.PROMOCODE_CAMPAIGN_1_ID };
                var filter = {
                    limit: 2,
                    offset: 0,
                    code: Data.PROMOCODE_UNIQUE.code
                };
                var INSTANCE = {
                    code: Data.PROMOCODE_UNIQUE.code,
                    userId: DataIds.LOCAL_USER_ID,
                    expirationDate: '2017-01-01T09:00:00Z',
                    usedOnDate: '2018-01-01T09:00:00Z',
                    moneyValue: 2,
                    creditValue: 0,
                    bonuspointsValue: 0,
                    moneyTransactionId: '22222',
                    creditTransactionId: 0,
                    bonuspointsTransactionId: 0,
                };
                async.series([
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'promocodeManager/promocodeCampaignActivate', ProfileData.CLIENT_INFO_1, content, null, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'promocodeManager/promocodeInstanceList', ProfileData.CLIENT_INFO_1, filter, function (responseContent) {
                            should.exist(responseContent);
                            should(responseContent).has.property('items').which.has.length(0);
                            should(responseContent).has.property('total').equal(0);
                        }, next);
                    },
                    function (next) {
                        var instance = new AerospikePromocodeCounter();
                        instance.copyInstance(INSTANCE);
                        instance.instanceId = 0;
                        instance.saveInstance(function (err) {
                            try {
                                assert.ifError(err);
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        });
                    },
                    function (next) {
                        var instance = new AerospikePromocodeCounter();
                        instance.copyInstance(INSTANCE);
                        instance.instanceId = 1;
                        instance.saveInstance(function (err) {
                            try {
                                assert.ifError(err);
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        });
                    },
                    function (next) {
                        AerospikePromocodeCounter.incrementUsed(filter, function (err, amount) {
                            try {
                                assert.ifError(err);
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        });
                    },
                    function (next) {
                        AerospikePromocodeCounter.incrementUsed(filter, function (err, amount) {
                            try {
                                assert.ifError(err);
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        });
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'promocodeManager/promocodeInstanceList', ProfileData.CLIENT_INFO_1, filter, function (responseContent) {
                            should.exist(responseContent);
                            should(responseContent).has.property('items').which.has.length(filter.limit);
                            should(responseContent).has.property('total').equal(filter.limit);
                        }, next);
                    },
                ], done);
            });
        });

    });
});
