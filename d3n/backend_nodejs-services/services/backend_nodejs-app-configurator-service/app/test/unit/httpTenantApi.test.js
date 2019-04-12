var _ = require('lodash');
var async = require('async');
var should = require('should');
var assert = require('chai').assert;
var request = require('supertest');
var Config = require('./../../config/config.js');
var StandardErrors = require('nodejs-errors');
var Data = require('./../config/tenant.data.js');
var ProfileData = require('./../config/profile.data.js');
var DataIds = require('./../config/_id.data.js');
var ShouldHelper = require('nodejs-automapper').getInstance(Config).ShouldHelper;
var AppConfiguratorService = require('./../../../index.js');
var appService = new AppConfiguratorService();
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikeTenant = KeyvalueService.Models.AerospikeTenant;
var AerospikeApp = KeyvalueService.Models.AerospikeApp;
var AerospikeCountryVat = KeyvalueService.Models.AerospikeCountryVat;
var AerospikeEndCustomerInvoiceList = KeyvalueService.Models.AerospikeEndCustomerInvoiceList;
var Database = require('nodejs-database').getInstance(Config);
var TenantInvoice = Database.RdbmsService.Models.Tenant.TenantInvoice;
var Tenant = Database.RdbmsService.Models.Tenant.Tenant;
var ProfileHasRole = Database.RdbmsService.Models.ProfileManager.ProfileHasRole;
var tmConnectorApiFactory = require('../../factories/tenantManagementConnectorApiFactory.js');

describe('HTTP TENANT API', function () {
    this.timeout(20000);

    describe('Security', function () {
        it('401 - Unauthorized for missing Authorization header', function (done) {
            global.httpHelper.securityCheck('/tenantManagementConnector/tenantGet', {}, 401, function(responseContent) {
                responseContent.should.eql('Unauthorized');
            }, done);
        });

        it('401 - Unauthorized for missing Authorization header', function (done) {
            global.httpHelper.securityCheck('/tenantManagementConnector/tenantGet', 
                {'Authorization': global.httpHelper.buildAuthHeader('dummyUsername', 'dummyPassword')}, 
                401, function(responseContent) {
                responseContent.should.eql('Unauthorized');
            }, done);
        });

// Disable IP check functionality and test per request from integration team to be able to access the
// test machine from different IP addresses

        // it('403 - Forbidden for not allowed IP address', function (done) {
        //     global.httpHelper.securityIpCheck('/tenantManagementConnector/tenantGet', 403, function(responseContent) {
        //         responseContent.should.eql('Forbidden');
        //     }, done);
        // });

    });

    describe('tenantGet', function () {
        it('200 for existing tenant', function (done) {
            global.httpHelper.getSuccess('/tenantManagementConnector/tenantGet', { 'id': DataIds.TENANT_1_ID } , function(responseContent) {
                var shouldResponseContent = _.clone(Data.TENANT_1);
                ShouldHelper.deepEqual(shouldResponseContent, responseContent.tenant);
            }, done);
        });
        it('404 for tenant id not specified', function (done) {
             global.httpHelper.getError('/tenantManagementConnector/tenantGet', 404, _.clone(StandardErrors['ERR_ENTRY_NOT_FOUND']), done);
        });
        it('404 for tenant id not found in database', function (done) {
            global.httpHelper.getError('/tenantManagementConnector/tenantGet?id=' + DataIds.TENANT_TEST_ID, 404, _.clone(StandardErrors['ERR_ENTRY_NOT_FOUND']), done);
        });
   });

    describe('tenantList', function () {
        it('list one tenant', function (done) {
            var queryParams = {
                limit: Config.rdbms.limit,
                offset: 0,
                searchBy: { id: DataIds.TENANT_1_ID },
            };
            global.httpHelper.postSuccess('/tenantManagementConnector/tenantList', queryParams, function(responseContent) {
                should.exist(responseContent);
                should(responseContent).has.property('items').which.has.length(1);
                ShouldHelper.deepEqual(Data.TENANT_1, responseContent.items[0]);
            }, done);
        });

        it('list all', function (done) {
            var queryParams = {
                limit: Config.rdbms.limit,
                offset: 0
            };
            global.httpHelper.postSuccess('/tenantManagementConnector/tenantList', queryParams, function(responseContent) {
                should.exist(responseContent);
                should(responseContent).has.property('items').which.has.length(2);
            }, done);
        });

        it('list all with default params', function (done) {
            var queryParams = {};
            global.httpHelper.postSuccess('/tenantManagementConnector/tenantList', queryParams, function(responseContent) {
                should.exist(responseContent);
                should.equal(responseContent.limit, 10);
                should.equal(responseContent.offset, 0);
                should(responseContent).has.property('items').which.has.length(2);
            }, done);
        });

        it('list all with limit and offset set to 0', function (done) {
            var queryParams = {
                limit: 0,
                offset: 0
            };
            global.httpHelper.postSuccess('/tenantManagementConnector/tenantList', queryParams, function(responseContent) {
                should.exist(responseContent);
                should.equal(responseContent.limit, 0);
                should.equal(responseContent.offset, 0);
                should(responseContent).has.property('items').which.has.length(2);
            }, done);
        });

        it('empty list for not record found', function (done) {
            var queryParams = {
                limit: Config.rdbms.limit,
                offset: 0,
                searchBy: { id: DataIds.TENANT_TEST_ID },
                // Due to a bug in the supertest/node query string generator/parser use this format to
                // send an array of parameters to the service
                orderBy: [{field:'id', direction:'asc'}, {field:'name',direction:'asc'}]
            };
            global.httpHelper.postSuccess('/tenantManagementConnector/tenantList', queryParams, function(responseContent) {
                should.exist(responseContent);
                should.equal(responseContent.limit, Config.rdbms.limit);
                should.equal(responseContent.offset, 0);
                should(responseContent).has.property('items').which.has.length(0);
            }, done);
        });

    });

    describe('tenantTrailList', function () {
        var baseUrl = '/tenantManagementConnector/tenantTrailList';

        it('list one trail item', function (done) {
            var queryParams = {
                limit: Config.rdbms.limit,
                offset: 0,
                searchBy: { "id": DataIds.TENANT_AUDIT_LOG_1_ID, "tenantId": DataIds.TENANT_1_ID },
            };
            global.httpHelper.postSuccess('/tenantManagementConnector/tenantTrailList', queryParams, function(responseContent) {
                should.exist(responseContent);
                should(responseContent).has.property('items').which.has.length(1);
                ShouldHelper.deepEqual(Data.TENANT_1_AUDIT_LOG_1, responseContent.items[0]);
            }, done);
        });

        it('list one trail item for wrong tenant', function (done) {
            var queryParams = {
                limit: Config.rdbms.limit,
                offset: 0,
                searchBy: { "id": DataIds.TENANT_AUDIT_LOG_1_ID, "tenantId": DataIds.TENANT_2_ID },
            };
            global.httpHelper.postSuccess('/tenantManagementConnector/tenantTrailList', queryParams, function(responseContent) {
                should.exist(responseContent);
                should(responseContent).has.property('items').which.has.length(0);
            }, done);
        });

        it('list all trail items for a given tenant', function (done) {
            var queryParams = {
                limit: Config.rdbms.limit,
                offset: 0,
                searchBy: { "tenantId": DataIds.TENANT_1_ID },
            };
            global.httpHelper.postSuccess('/tenantManagementConnector/tenantTrailList', queryParams, function(responseContent) {
                should.exist(responseContent);
                should.equal(responseContent.limit, Config.rdbms.limit);
                should.equal(responseContent.offset, 0);
                should(responseContent).has.property('items').which.has.length(2);
            }, done);
        });

        it('list all trail items using default params', function (done) {
            var queryParams = { };
            global.httpHelper.postSuccess('/tenantManagementConnector/tenantTrailList', queryParams, function(responseContent) {
                should.exist(responseContent);
                should.equal(responseContent.limit, 10);
                should.equal(responseContent.offset, 0);
                should(responseContent).has.property('items').which.has.length(2);
            }, done);
        });

        it('list all trail items with limit and offset set to 0', function (done) {
            var queryParams = { limit: 0, offset: 0 };
            global.httpHelper.postSuccess('/tenantManagementConnector/tenantTrailList', queryParams, function(responseContent) {
                should.exist(responseContent);
                should.equal(responseContent.limit, 0);
                should.equal(responseContent.offset, 0);
                should(responseContent).has.property('items').which.has.length(2);
            }, done);
        });

        it('empty list for no record found', function (done) {
            var queryParams = {
                limit: Config.rdbms.limit,
                offset: 0,
                searchBy: { id: DataIds.TENANT_AUDIT_LOG_TEST_ID },
                // Due to a bug in the supertest/node query string generator/parser use this format to
                // send an array of parameters to the service
                orderBy: [{field:'id',direction:'asc'}]
            };
            global.httpHelper.postSuccess('/tenantManagementConnector/tenantTrailList', queryParams, function(responseContent) {
                should.exist(responseContent);
                should(responseContent).has.property('items').which.has.length(0);
            }, done);
        });
    });

    describe('tenantCreate', function () {
        it('create a new tenant', function(done) {
            var postParams = _.omit(Data.TENANT_TEST, ['id', 'createDate', 'updateDate']);

            global.httpHelper.postSuccess('/tenantManagementConnector/tenantCreate', postParams, function(responseContent) {
                should.exist(responseContent);
                ShouldHelper.deepEqual(postParams, responseContent.tenant, ['id', 'createDate', 'updateDate']);
            }, done);
        });
    });

    // Update a single tenant
    describe('tenantUpdate', function () {
        it('update tenant', function(done) {
            var postParams = _.clone(Data.TENANT_1);
            postParams.name = "New Name";
            postParams.country = "New Country";
            postParams.city = "New City";

            global.httpHelper.postSuccess('/tenantManagementConnector/tenantUpdate', postParams, function(responseContent) {
                should.exist(responseContent);
                ShouldHelper.deepEqual(postParams, responseContent.tenant, ['updateDate']);
            }, done);
        });
    });

    // Update a single tenant configuration
    describe('tenantConfigurationUpdate', function () {
        it('update configuration', function(done) {
            var postParams = _.cloneDeep(Data.TENANT_CONFIG_1);

            global.httpHelper.postSuccess('/tenantManagementConnector/tenantConfigurationUpdate', postParams, function(responseContent) {
                should.exist(responseContent);
                ShouldHelper.deepEqual(postParams, responseContent.tenant);
            }, done);
        });
    });

    describe('tenantPaydentApiKeyUpdate', function () {
        it ('create new tenant configuration api key', function (done) {
            AerospikeTenant.remove({tenantId: Data.TENANT_CONFIG_1.tenantId}, function (err){
                if (err && err != StandardErrors.ERR_ENTRY_NOT_FOUND.message) {
                    return done(err);
                }
                
                var postParams = {
                    'tenantId': Data.TENANT_CONFIG_1.tenantId,
                    'apiId': 'test-api-id'
                };

                global.httpHelper.postSuccess('/tenantManagementConnector/tenantPaydentApiKeyUpdate', postParams, function(responseContent) {
                    should.exist(responseContent);
                    postParams.exchangeRates = [];
                    postParams.mainCurrency = 'EUR';
                    ShouldHelper.deepEqual(postParams, responseContent.tenant);
                }, done);
            });
        });

        it ('update existing tenant configuration api key', function (done) {
                var postParams = {
                    'tenantId': Data.TENANT_CONFIG_1.tenantId,
                    'apiId': Data.TENANT_CONFIG_1.apiId
                };

                global.httpHelper.postSuccess('/tenantManagementConnector/tenantPaydentApiKeyUpdate', postParams, function(responseContent) {
                    should.exist(responseContent);
                    postParams.exchangeRates = [];
                    postParams.mainCurrency = 'EUR';
                    ShouldHelper.deepEqual(postParams, responseContent.tenant);
                }, done);
        })
    });

    describe('tenantExchangeCurrencyUpdate', function () {
        it ('create new exchange and currency', function (done) {
            AerospikeTenant.remove({tenantId: Data.TENANT_CONFIG_1.tenantId}, function (err){
                if (err && err != StandardErrors.ERR_ENTRY_NOT_FOUND.message) {
                    return done(err);
                }
                
                var postParams = {
                    'tenantId': Data.TENANT_CONFIG_1.tenantId,
                    'mainCurrency': Data.TENANT_CONFIG_1.mainCurrency,
                    'exchangeRates': Data.TENANT_CONFIG_1.exchangeRates
                };

                global.httpHelper.postSuccess('/tenantManagementConnector/tenantExchangeCurrencyUpdate', postParams, function(responseContent) {
                    should.exist(responseContent);
                    postParams.apiId = '';
                    ShouldHelper.deepEqual(postParams, responseContent.tenant);
                }, done);
            });
        });

        it ('update existing tenant exchange and currency data', function (done) {
            var postParams = {
                'tenantId': Data.TENANT_CONFIG_1.tenantId,
                'mainCurrency': 'RON',
                'exchangeRates': [Data.TENANT_CONFIG_1.exchangeRates[0]]
            };

            global.httpHelper.postSuccess('/tenantManagementConnector/tenantExchangeCurrencyUpdate', postParams, function(responseContent) {
                should.exist(responseContent);
                postParams.apiId = '';
                ShouldHelper.deepEqual(postParams, responseContent.tenant);
            }, done);
        });

        it ('update just apiId', function (done) {
            var postParams = {
                'tenantId': Data.TENANT_CONFIG_1.tenantId,
                'apiId': 'test-api-id',
                'mainCurrency': 'garbage',
                'exchangeRates': 'Hello world'
            };

            global.httpHelper.postSuccess('/tenantManagementConnector/tenantPaydentApiKeyUpdate', postParams, function(responseContent) {
                should.exist(responseContent);
                postParams.mainCurrency = 'RON';
                postParams.exchangeRates = [Data.TENANT_CONFIG_1.exchangeRates[0]]
                ShouldHelper.deepEqual(postParams, responseContent.tenant);
            }, done);
        });
    });

    // Update many tenants
    describe('tenantBulkUpdate', function () {
        it('bulk update tenants', function(done) {
            var postParams = _.clone(Data.TENANT_1);
            postParams.name = "New Name";
            postParams.country = "New Country";
            postParams.city = "New City";
            postParams.contactFirstName = "New FirstName";
            postParams.contactLastName = "New LastName";
            postParams.ids = [DataIds.TENANT_1_ID, DataIds.TENANT_2_ID];

            global.httpHelper.postSuccess('/tenantManagementConnector/tenantBulkUpdate', postParams, function(responseContent) {
                should.exist(responseContent);
                should.equal(responseContent.tenant.updates, 2);
            }, function () {
                global.httpHelper.getSuccess('/tenantManagementConnector/tenantGet', { 'id': DataIds.TENANT_2_ID } , function(getResponseContent) {
                    var shouldResponseContent = _.assign(_.clone(Data.TENANT_2), postParams);
                    shouldResponseContent.id = DataIds.TENANT_2_ID;
                    ShouldHelper.deepEqual(shouldResponseContent, getResponseContent.tenant, ['ids', 'updateDate']);
                }, done);
            });
        });
    });

    describe('tenant/contractGet', function () {
        var baseUrl = '/tenantManagementConnector/tenant/' + DataIds.TENANT_1_ID + '/contractGet';
        it('200 for existing tenant', function (done) {
            global.httpHelper.getSuccess(baseUrl, { 'id': DataIds.TENANT_CONTRACT_1_ID } , function(responseContent) {
                var shouldResponseContent = _.clone(Data.TENANT_1_CONTRACT_1);
                ShouldHelper.deepEqual(shouldResponseContent, responseContent.contract);
            }, done);
        });
        it('404 for tenant id not specified', function (done) {
             global.httpHelper.getError(baseUrl, 404, _.clone(StandardErrors['ERR_ENTRY_NOT_FOUND']), done);
        });
        it('404 for tenant id not found in database', function (done) {
            global.httpHelper.getError(baseUrl + '?id=' + DataIds.TENANT_TEST_ID, 404, _.clone(StandardErrors['ERR_ENTRY_NOT_FOUND']), done);
        });
   });

    describe('tenant/contractList', function () {
        var baseUrl = '/tenantManagementConnector/tenant/' + DataIds.TENANT_1_ID + '/contractList';
        it('list one contract', function (done) {
            var queryParams = {
                limit: Config.rdbms.limit,
                offset: 0,
                searchBy: { id: DataIds.TENANT_CONTRACT_1_ID },
            };
            global.httpHelper.postSuccess(baseUrl, queryParams, function(responseContent) {
                should.exist(responseContent);
                should(responseContent).has.property('items').which.has.length(1);
                ShouldHelper.deepEqual(Data.TENANT_1_CONTRACT_1, responseContent.items[0]);
            }, done);
        });

        it('list all', function (done) {
            var queryParams = {
                limit: Config.rdbms.limit,
                offset: 0
            };
            global.httpHelper.postSuccess(baseUrl, queryParams, function(responseContent) {
                should.exist(responseContent);
                should(responseContent).has.property('items').which.has.length(2);
            }, done);
        });

        it('empty list for no record found', function (done) {
            var queryParams = {
                limit: Config.rdbms.limit,
                offset: 0,
                searchBy: { id: DataIds.TENANT_CONTRACT_TEST_ID },
                // Due to a bug in the supertest/node query string generator/parser use this format to
                // send an array of parameters to the service
                orderBy: [{field:'id',direction:'asc'}, {field:'name',direction:'asc'}]
            };
            global.httpHelper.postSuccess(baseUrl, queryParams, function(responseContent) {
                should.exist(responseContent);
                should(responseContent).has.property('items').which.has.length(0);
            }, done);
        });

        it('empty list for a contract which bellong to a different tenant', function (done) {
            var queryParams = {
                limit: Config.rdbms.limit,
                offset: 0,
                searchBy: { id: DataIds.TENANT_CONTRACT_1_ID },
                // Due to a bug in the supertest/node query string generator/parser use this format to
                // send an array of parameters to the service
                orderBy: [{field: 'id',direction:'asc'}, {field:'name',direction: 'asc'}]
            };
            global.httpHelper.postSuccess('/tenantManagementConnector/tenant/' + DataIds.TENANT_2_ID + '/contractList', queryParams, function(responseContent) {
                should.exist(responseContent);
                should.equal(responseContent.limit, Config.rdbms.limit);
                should.equal(responseContent.offset, 0);
                should(responseContent).has.property('items').which.has.length(0);
            }, done);
        });

        it('list all with default params', function (done) {
            var queryParams = { };
            global.httpHelper.postSuccess(baseUrl, queryParams, function(responseContent) {
                should.exist(responseContent);
                should.equal(responseContent.limit, 10);
                should.equal(responseContent.offset, 0);
                should(responseContent).has.property('items').which.has.length(2);
            }, done);
        });

        it('list all with limit and offset set to 0', function (done) {
            var queryParams = { limit: 0, offset: 0 };
            global.httpHelper.postSuccess(baseUrl, queryParams, function(responseContent) {
                should.exist(responseContent);
                should.equal(responseContent.limit, 0);
                should.equal(responseContent.offset, 0);
                should(responseContent).has.property('items').which.has.length(2);
            }, done);
        });
    });


    describe('tenant/contractCreate', function () {
        var baseUrl = '/tenantManagementConnector/tenant/' + DataIds.TENANT_1_ID + '/contractCreate';
        it('create a new tenant contract', function(done) {
            var postParams = _.omit(Data.TENANT_1_CONTRACT_TEST, ['id', 'createDate', 'updateDate']);

            global.httpHelper.postSuccess(baseUrl, postParams, function(responseContent) {
                should.exist(responseContent);
                ShouldHelper.deepEqual(postParams, responseContent.contract, ['id', 'createDate', 'updateDate']);
            }, done);
        });
    });

    describe('tenant/profileGet', function () {
        var baseUrl = '/tenantManagementConnector/profileGet';
        it('200 for existing profile', function (done) {
            global.httpHelper.postSuccess(baseUrl, { 'userId': ProfileData.PROFILE_1.userId } , function(responseContent) {
                var shouldResponseContent = _.pick(ProfileData.PROFILE_1, [
                    'userId', 'firstName', 'lastName', 'nickname', 'birthDate', 'sex', 'addressStreet', 'addressStreetNumber',
                    'addressCity', 'addressPostalCode', 'addressCountry', 'emails', 'phones'
                ]);
                ShouldHelper.deepEqual(shouldResponseContent, responseContent.profile);
            }, done);
        });
        it('404 validation error, userId not provided', function (done) {
             global.httpHelper.postError(baseUrl, {}, 404, _.clone(StandardErrors['ERR_VALIDATION_FAILED']), done);
        });
   });

    describe('tenant/invoiceCreate', function () {
        var baseUrl = '/tenantManagementConnector/tenant/' + DataIds.TENANT_1_ID + '/invoiceCreate';
        it('success', function(done) {
            var postParams = _.omit(Data.TENANT_1_INVOICE_TEST, ['id']);
            
            async.series([
                function (next) {
                    global.httpHelper.postSuccess(baseUrl, postParams, function(responseContent) {
                        should.exist(responseContent);
                    }, next);
                },
                function (next) {
                    TenantInvoice.findAll().then(function (dbItems) {
                        try {
                            var dbInvoice = _.omit(dbItems[2].get({plain: true}), 'id');
                            var sampleInvoice = _.cloneDeep(postParams);
                            dbInvoice.date = dbInvoice.date.toISOString();
                            dbInvoice.paymentDate = dbInvoice.paymentDate.toISOString();
                            assert.deepEqual(dbInvoice, sampleInvoice);
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    }).catch(next);
                }
            ], done);
            
        });
        it('error', function(done) {
            var postParams = _.omit(Data.TENANT_1_INVOICE_TEST, ['id']);
            postParams.status = 'st1'; // invalid status
            global.httpHelper.postError(baseUrl, postParams, 404, _.clone(StandardErrors['ERR_VALIDATION_FAILED']), done);
        });
    });
    
    describe('tenant/invoiceEndCustomerCreate', function () {
        var baseUrl = '/tenantManagementConnector/tenant/' + DataIds.TENANT_1_ID + '/invoiceEndCustomerCreate';
        it('success', function (done) {
            var postParams = _.omit(Data.TENANT_1_INVOICE_END_CUSTOMER_TEST, ['id']);

            async.series([
                // Make 1 invoice
                function (next) {
                    global.httpHelper.postSuccess(baseUrl, postParams, function (responseContent) {
                        should.exist(responseContent);
                    }, next);
                },
                function (next) {
                    AerospikeEndCustomerInvoiceList.findOne(postParams, function (err, item) {
                        try {
                            var dbInvoice = _.omit(JSON.parse(item.data[0]), 'id');
                            var sampleInvoice = _.omit(Data.TENANT_1_INVOICE_END_CUSTOMER_TEST, 'id');
                            sampleInvoice.tenantId = sampleInvoice.tenantId.toString();
                            assert.deepEqual(dbInvoice, sampleInvoice);
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    });
                },
                // Make 2 invoice
                function (next) {
                    global.httpHelper.postSuccess(baseUrl, postParams, function (responseContent) {
                        should.exist(responseContent);
                    }, next);
                },
                function (next) {
                    AerospikeEndCustomerInvoiceList.findOne(postParams, function (err, item) {
                        try {
                            var dbInvoice = _.omit(JSON.parse(item.data[1]), 'id');
                            var sampleInvoice = _.omit(Data.TENANT_1_INVOICE_END_CUSTOMER_TEST, 'id');
                            sampleInvoice.tenantId = sampleInvoice.tenantId.toString();
                            assert.deepEqual(dbInvoice, sampleInvoice);
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    });
                },
            ], done);

        });
        it('error', function (done) {
            var postParams = _.omit(Data.TENANT_1_INVOICE_END_CUSTOMER_TEST, ['id']);
            postParams.invoiceSeriesNumber = null; // required
            global.httpHelper.postError(baseUrl, postParams, 404, _.clone(StandardErrors['ERR_VALIDATION_FAILED']), done);
        });
    });

    describe('tenant/vatAdd', function () {
        var baseUrl = '/tenantManagementConnector/vatAdd';
        it('success', function(done) {
            var country = Data.VAT_1.country;
            var vat1 = _.assign({}, Data.VAT_1);
            var vat2 = _.assign({}, Data.VAT_2);
            async.series([
                function (next) {
                    AerospikeCountryVat.remove({country: country}, function (err) {
                        if (err && err !== StandardErrors.ERR_ENTRY_NOT_FOUND.message) {
                            return next(err);
                        }
                        
                        return next();
                    });
                },
                
                // add vat
                function (next) {
                    global.httpHelper.postSuccess(baseUrl, vat1, function(responseContent) {
                        should.exist(responseContent);
                    }, next);
                },
                
                // check added vat
                function (next) {
                    AerospikeCountryVat.findOne({country: country}, function (err, result) {
                        try {
                            assert.ifError(err);
                            assert.deepEqual(result.vats, [
                                {
                                    percent: vat1.percent,
                                    startDate: parseInt(Date.parse(vat1.startDate)/1000),
                                    endDate: null
                                }
                            ]);
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    });
                },
                
                // ad a new vat
                function (next) {
                    global.httpHelper.postSuccess(baseUrl, vat2, function(responseContent) {
                        should.exist(responseContent);
                    }, next);
                },
                
                // check added vat, previous period must be closed
                function (next) {
                    AerospikeCountryVat.findOne({country: country}, function (err, result) {
                        try {
                            assert.ifError(err);
                            assert.deepEqual(result.vats, [
                                {
                                    percent: vat1.percent,
                                    startDate: parseInt(Date.parse(vat1.startDate)/1000),
                                    endDate: parseInt(Date.parse(vat2.startDate)/1000)-1
                                },
                                {
                                    percent: vat2.percent,
                                    startDate: parseInt(Date.parse(vat2.startDate)/1000),
                                    endDate: null
                                }
                            ]);
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    });
                }
            ], done);
            
        });
        it('error, validation error', function(done) {
            var vat1 = _.assign({}, Data.VAT_1);
            delete vat1.startDate;
            global.httpHelper.postError(baseUrl, vat1, 404, _.clone(StandardErrors['ERR_VALIDATION_FAILED']), done);
        });
    });
    
    describe('tenant/contractUpdate', function () {
        var baseUrl = '/tenantManagementConnector/tenant/' + DataIds.TENANT_1_ID + '/contractUpdate';
        it('update tenant a tenant contract', function(done) {
            var postParams = _.clone(Data.TENANT_1_CONTRACT_1);
            postParams.name = "New Name";

            global.httpHelper.postSuccess(baseUrl, postParams, function(responseContent) {
                should.exist(responseContent);
                ShouldHelper.deepEqual(postParams, responseContent.contract, ['updateDate']);
            }, done);
        });
    });

    describe('tenant/contractTrailList', function () {
        it('list one trail item', function (done) {
            var queryParams = {
                limit: Config.rdbms.limit,
                offset: 0,
                searchBy: { id: DataIds.TENANT_CONTRACT_AUDIT_LOG_1_ID, contractId: DataIds.TENANT_CONTRACT_1_ID },
            };
            global.httpHelper.postSuccess('/tenantManagementConnector/contractTrailList', queryParams, function(responseContent) {
                should.exist(responseContent);
                should(responseContent).has.property('items').which.has.length(1);
                ShouldHelper.deepEqual(Data.CONTRACT_1_AUDIT_LOG_1, responseContent.items[0]);
            }, done);
        });

        it('list one trail item for the wrong contract', function (done) {
            var queryParams = {
                limit: Config.rdbms.limit,
                offset: 0,
                searchBy: { id: DataIds.TENANT_CONTRACT_AUDIT_LOG_1_ID, contractId: DataIds. TENANT_CONTRACT_2_ID },
            };
            global.httpHelper.postSuccess('/tenantManagementConnector/contractTrailList', queryParams, function(responseContent) {
                should.exist(responseContent);
                should(responseContent).has.property('items').which.has.length(0);
            }, done);
        });

        it('list all trail items for a contract', function (done) {
            var queryParams = {
                limit: Config.rdbms.limit,
                offset: 0,
                searchBy: { contractId: DataIds. TENANT_CONTRACT_1_ID },
            };
            global.httpHelper.postSuccess('/tenantManagementConnector/contractTrailList', queryParams, function(responseContent) {
                should.exist(responseContent);
                should(responseContent).has.property('items').which.has.length(2);
            }, done);
        });

        it('list all trail items for all contracts', function (done) {
            var queryParams = {
                limit: Config.rdbms.limit,
                offset: 0
            };
            global.httpHelper.postSuccess('/tenantManagementConnector/contractTrailList', queryParams, function(responseContent) {
                should.exist(responseContent);
                should(responseContent).has.property('items').which.has.length(2);
            }, done);
        });

        it('empty list for no record found', function (done) {
            var queryParams = {
                limit: Config.rdbms.limit,
                offset: 0,
                searchBy: { id: DataIds.TENANT_CONTRACT_AUDIT_LOG_TEST_ID },
                // Due to a bug in the supertest/node query string generator/parser use this format to
                // send an array of parameters to the service
                orderBy: [{field: 'id', direction: 'asc'}]
            };
            global.httpHelper.postSuccess('/tenantManagementConnector/contractTrailList', queryParams, function(responseContent) {
                should.exist(responseContent);
                should.equal(responseContent.limit, Config.rdbms.limit);
                should.equal(responseContent.offset, 0);
                should(responseContent).has.property('items').which.has.length(0);
            }, done);
        });
        it('list all trail items using default params', function (done) {
            var queryParams = {};
            global.httpHelper.postSuccess('/tenantManagementConnector/contractTrailList', queryParams, function(responseContent) {
                should.exist(responseContent);
                should.equal(responseContent.limit, 10);
                should.equal(responseContent.offset, 0);
                should(responseContent).has.property('items').which.has.length(2);
            }, done);
        });

        it('list all trail items with limit and offset set to 0', function (done) {
            var queryParams = {
                limit: 0,
                offset: 0
            };
            global.httpHelper.postSuccess('/tenantManagementConnector/contractTrailList', queryParams, function(responseContent) {
                should.exist(responseContent);
                should.equal(responseContent.limit, 0);
                should.equal(responseContent.offset, 0);
                should(responseContent).has.property('items').which.has.length(2);
            }, done);
        });
        
    });

    describe('tenant/administratorGet', function () {
        var baseUrl = '/tenantManagementConnector/tenant/' + DataIds.TENANT_1_ID + '/administratorGet';
        it('200 for existing administrator', function (done) {
            global.httpHelper.getSuccess(baseUrl, { 'email': ProfileData.PROFILE_EMAIL_1.email } , function(responseContent) {
                ShouldHelper.deepEqual({
                    'id': DataIds.LOCAL_USER_ID,
                    'tenantId': DataIds.TENANT_1_ID,
                    'email': ProfileData.PROFILE_EMAIL_1.email,
                    'firstName': ProfileData.PROFILE_1.firstName,
                    'lastName': ProfileData.PROFILE_1.lastName,
                    'phone': ProfileData.PROFILE_PHONE_1.phone
                }, responseContent.administrator);
            }, done);
        });
        it('404 for tenant id not specified', function (done) {
             global.httpHelper.getError(baseUrl, 404, _.clone(StandardErrors['ERR_ENTRY_NOT_FOUND']), done);
        });
        it('404 for tenant id not found in database', function (done) {
            global.httpHelper.getError(baseUrl + '?email=' + ProfileData.PROFILE_EMAIL_2.email, 404, _.clone(StandardErrors['ERR_ENTRY_NOT_FOUND']), done);
        });
   });

    describe('tenant/administratorCreate', function () {
        var baseUrl = '/tenantManagementConnector/tenant/' + DataIds.TENANT_1_ID + '/administratorCreate';
        var postData = { 
            'email': Data.TENANT_1.email,
            'tenantId': Data.TENANT_1.id,
            'firstName': 'firstname new', 
            'lastName': 'lastname new',
            'phone': '12312122-12'
        };

        it('200 update existing administrator', function (done) {
            global.httpHelper.postSuccess(baseUrl, postData , function(responseContent) {
                var result = _.clone(postData);
                result.id = DataIds.LOCAL_USER_ID;
                ShouldHelper.deepEqual(result, responseContent.administrator);
            }, done);
        });

        it('200 update existing administrator existing phone of another user', function (done) {
            var newPostData = _.clone(postData);
            newPostData.phone = ProfileData.PROFILE_PHONE_2.phone;
            newPostData.id = ProfileData.PROFILE_1.userId;
            global.httpHelper.postSuccess(baseUrl, newPostData , function(responseContent) {
                ShouldHelper.deepEqual(newPostData, responseContent.administrator);
            }, done);
        });

        it('200 create new phone entry for the existing user', function (done) {
            var newPostData = _.clone(postData);
            newPostData.phone = '735791212';
            newPostData.id = ProfileData.PROFILE_1.userId;
            global.httpHelper.postSuccess(baseUrl, newPostData , function(responseContent) {
                ShouldHelper.deepEqual(newPostData, responseContent.administrator);
            }, done);
        });

        // Creation of new admin user can not be tested, requires updates for the mocks and a
        // reimplementation of the functionality in the auth service
        // it('200 create new administrator entry', function (done) {
        //     var newPostData = _.clone(postData);
        //     newPostData.email = 'email-test@ascendro.de';
        //     global.httpHelper.postSuccess(baseUrl, newPostData , function(responseContent) {
        //         newPostData.id = 'dummy-user-id-mocked-response';
        //         ShouldHelper.deepEqual(newPostData, responseContent.administrator);
        //     }, done);
        // });
        

        it('404 and FATAL_ERROR for tenant id not specified', function (done) {
            global.httpHelper.postError(baseUrl, { 'tenantId': DataIds.TENANT_1_ID }, 404,_.clone(StandardErrors['ERR_FATAL_ERROR']), done);
        });
        it('404 and FATAL_ERROR for email not specified', function (done) {
            global.httpHelper.postError(baseUrl, { 'email': ProfileData.PROFILE_EMAIL_1.email }, 404,_.clone(StandardErrors['ERR_FATAL_ERROR']), done);
        });
   });

    describe('administratorTrailList', function () {
        it('list one entry', function (done) {
            var queryParams = {
                limit: Config.rdbms.limit,
                offset: 0,
                searchBy: { id: Data.TENANT_ADMIN_1_AUDIT_LOG_1.id },
            };
            global.httpHelper.postSuccess('/tenantManagementConnector/administratorTrailList', queryParams, function(responseContent) {
                should.exist(responseContent);
                should.equal(responseContent.limit, Config.rdbms.limit);
                should.equal(responseContent.offset, 0);
                should(responseContent).has.property('items').which.has.length(1);
                ShouldHelper.deepEqualList(responseContent.items, [Data.TENANT_ADMIN_1_AUDIT_LOG_1], ["createDate"]);
            }, done);
        });

        it('list all entries with default params', function (done) {
            var queryParams = { };
            global.httpHelper.postSuccess('/tenantManagementConnector/administratorTrailList', queryParams, function(responseContent) {
                should.exist(responseContent);
                should.equal(responseContent.limit, 10);
                should.equal(responseContent.offset, 0);
                should(responseContent).has.property('items').which.has.length(2);
                ShouldHelper.deepEqualList(responseContent.items, [Data.TENANT_ADMIN_1_AUDIT_LOG_1, Data.TENANT_ADMIN_1_AUDIT_LOG_2], ["createDate"]);
            }, done);
        });

        it('list all entries with limit and offset set to 0', function (done) {
            var queryParams = { limit: 0, offset: 0 };
            global.httpHelper.postSuccess('/tenantManagementConnector/administratorTrailList', queryParams, function(responseContent) {
                should.exist(responseContent);
                should.equal(responseContent.limit, 0);
                should.equal(responseContent.offset, 0);
                should(responseContent).has.property('items').which.has.length(2);
                ShouldHelper.deepEqualList(responseContent.items, [Data.TENANT_ADMIN_1_AUDIT_LOG_1, Data.TENANT_ADMIN_1_AUDIT_LOG_2], ["createDate"]);
            }, done);
        });
   });

    describe('tenant/administratorDelete', function () {
        var baseUrl = '/tenantManagementConnector/administratorDelete';

        it('200 delete admin role by tenantId', function (done) {
            var postData = { 
                'email': ProfileData.PROFILE_EMAIL_1.email,
                'tenantIds': [DataIds.TENANT_1_ID]
            };

            global.httpHelper.postSuccess(baseUrl, postData , function(responseContent) {
                ShouldHelper.deepEqual(ProfileData.PROFILE_1.userId, responseContent.administrator.id);
            }, done);
        });

        it('200 delete admin for all tenants ', function (done) {
            var postData = { 
                'email': ProfileData.PROFILE_EMAIL_1.email,
                'allTenants': true
            };
            global.httpHelper.postSuccess(baseUrl, postData , function(responseContent) {
                ShouldHelper.deepEqual(ProfileData.PROFILE_1.userId, responseContent.administrator.id);
            }, done);
        });

        it('200 delete admin for all tenants ', function (done) {
            var postData = { 
                'email': ProfileData.PROFILE_EMAIL_2.email,
                'allTenants': true
            };
            global.httpHelper.postSuccess(baseUrl, postData , function(responseContent) {
                ShouldHelper.deepEqual(ProfileData.PROFILE_2.userId, responseContent.administrator.id);
            }, done);
        });

        it('404 and FATAL_ERROR for email not specified', function (done) {
            var postData = { 
                'allTenants': true
            };
            global.httpHelper.postError(baseUrl, postData, 404,_.clone(StandardErrors['ERR_FATAL_ERROR']), done);
        });
   });

    describe('tenant/administratorImpersonate', function () {
        var baseUrl = '/tenantManagementConnector/tenant/' + DataIds.TENANT_1_ID + '/administratorImpersonate';
        it('200 for existing admin', function (done) {
            global.httpHelper.getSuccess(baseUrl, { 'email': 'admin@test.com' } , function(responseContent) {
                responseContent.token.should.eql('dummy-token');
            }, done);
        });
        it('404 for tenant id not specified', function (done) {
             global.httpHelper.getError(baseUrl, 404, _.clone(StandardErrors['ERR_ENTRY_NOT_FOUND']), done);
        });
        it('404 for invalid user', function (done) {
            global.httpHelper.getError(baseUrl + '?email=test', 404, _.clone(StandardErrors['ERR_ENTRY_NOT_FOUND']), done);
        });
   });

    describe('f4mConfigurationUpdate', function () {
        var baseUrl = '/tenantManagementConnector/f4mConfigurationUpdate';
        var postData = {
            name: 'friends4media GmbH',
            address: 'Waffnergasse 8',
            city: '93047 Regensburg',
            country: 'Germany',
            vat: 'DE295946616',
            url: 'http://www.f4m.tv/',
            email: 'kontakt@f4m.tv',
            description: 'F4M Description Text',
            termsAndConditions: 'Some terms and conditions',
            privacyStatement: 'Some privacy statement',
            gameRules: 'F4M Game Rules',
            copyright: 'F4M Copyright',
            phone: '12341234',
            logoUrl: 'http://www.f4m.tv/'
        };
        it('200 for updating correctly', function (done) {
            global.httpHelper.postSuccess(baseUrl, postData , function(responseContent) {
                responseContent.should.eql(postData);
                // Updating data should work as well
            }, function(err) {
                if (err) {
                    return done();
                }
                var updatedItem = _.cloneDeep(postData);
                updatedItem.name = 'gabi';
                global.httpHelper.postSuccess(baseUrl, updatedItem, function (updatedResponseContent){
                    updatedResponseContent.should.eql(updatedItem);
                }, done);
            });
        });
        it('404 for invalid payload', function (done) {
            var invalidPost = { name: "test" };
            global.httpHelper.postError(baseUrl, invalidPost, 404, _.clone(StandardErrors['ERR_VALIDATION_FAILED']), done);
        });
   });
   
   
   describe('tenantAdminContractCreate', function () {
       
       it('tenantAdminContractCreate success, update existing administrator', function (done) {
           async.series([
               function (next) {
                    try {
                        var tenant = _.omit(Data.TENANT_TEST, ['id', 'createDate', 'updateDate']);
                        var contract = _.omit(Data.TENANT_1_CONTRACT_TEST, ['id', 'tenantId', 'createDate', 'updateDate']);
                        var administrator = {
                            'email': Data.TENANT_1.email,
                            'firstName': 'firstname new', 
                            'lastName': 'lastname new',
                            'phone': '12312122-12'
                        };
                        var postData = {
                            tenant: tenant,
                            contract: contract,
                            administrator: administrator
                        };
                        
                        global.httpHelper.postSuccess('/tenantManagementConnector/tenantAdminContractCreate', postData , function(responseContent) {
                            assert.isDefined(responseContent.tenant.id);
                            assert.isDefined(responseContent.administrator.id);
                            assert.isDefined(responseContent.contract.id);
                        }, next);
                    } catch (e) {
                        return setImmediate(next, e);
                    }
               }
           ], done);
       });
       
       it('tenantAdminContractCreate success, update existing administrator existing phone of another user', function (done) {
           async.series([
               function (next) {
                    try {
                        var tenant = _.omit(Data.TENANT_TEST, ['id', 'createDate', 'updateDate']);
                        var contract = _.omit(Data.TENANT_1_CONTRACT_TEST, ['id', 'tenantId', 'createDate', 'updateDate']);
                        var administrator = {
                            'email': Data.TENANT_1.email,
                            'firstName': 'firstname new', 
                            'lastName': 'lastname new',
                            'phone': '12312122-12'
                        };
                        
                        administrator.phone = ProfileData.PROFILE_PHONE_2.phone;
                        administrator.id = ProfileData.PROFILE_1.userId;
                        
                        var postData = {
                            tenant: tenant,
                            contract: contract,
                            administrator: administrator
                        };
                        
                        global.httpHelper.postSuccess('/tenantManagementConnector/tenantAdminContractCreate', postData , function(responseContent) {
                            assert.isDefined(responseContent.tenant.id);
                            assert.isDefined(responseContent.administrator.id);
                            assert.isDefined(responseContent.contract.id);
                        }, next);
                    } catch (e) {
                        return setImmediate(next, e);
                    }
               }
           ], done);
       });
       it('tenantAdminContractCreate success, create new phone entry for the existing user', function (done) {
           async.series([
               function (next) {
                    try {
                        var tenant = _.omit(Data.TENANT_TEST, ['id', 'createDate', 'updateDate']);
                        var contract = _.omit(Data.TENANT_1_CONTRACT_TEST, ['id', 'tenantId', 'createDate', 'updateDate']);
                        var administrator = {
                            'email': Data.TENANT_1.email,
                            'firstName': 'firstname new', 
                            'lastName': 'lastname new',
                            'phone': '12312122-12'
                        };
                        
                        administrator.phone = '735791212';
                        administrator.id = ProfileData.PROFILE_1.userId;
                        
                        var postData = {
                            tenant: tenant,
                            contract: contract,
                            administrator: administrator
                        };
                        
                        global.httpHelper.postSuccess('/tenantManagementConnector/tenantAdminContractCreate', postData , function(responseContent) {
                            assert.isDefined(responseContent.tenant.id);
                            assert.isDefined(responseContent.administrator.id);
                            assert.isDefined(responseContent.contract.id);
                        }, next);
                    } catch (e) {
                        return setImmediate(next, e);
                    }
               }
           ], done);
       });
       
       
        it('tenantAdminContractCreateRollback, _contractUpdate fail', function (done) {
            var _contractUpdateStub = global.wsHelper.sinonSandbox.stub(tmConnectorApiFactory, '_contractUpdate', function (args, cb) {
                return setImmediate(cb, 'ERR_FATAL_ERROR');
            });
            var _tenantUpdateSpy = global.wsHelper.sinonSandbox.spy(tmConnectorApiFactory, '_tenantUpdate');
            var _administratorCreateSpy = global.wsHelper.sinonSandbox.spy(tmConnectorApiFactory, '_administratorCreate');
            var _tenantAdminContractCreateRollbackSpy = global.wsHelper.sinonSandbox.spy(tmConnectorApiFactory, '_tenantAdminContractCreateRollback');
            var AerospikeAppSpy = global.wsHelper.sinonSandbox.spy(AerospikeApp, 'unpublishApplication');
            
            var tenantsCnt = 0;
            var profileHasRoleCnt = 0;
            async.series([
                // count tenants, should be same after rollback
                function (next) {
                    try {
                        Tenant.count().then(function (cnt) {
                            tenantsCnt = cnt;
                            return next();
                        }).catch(next);
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                },
                
                function (next) {
                    try {
                        ProfileHasRole.count().then(function (cnt) {
                            profileHasRoleCnt = cnt;
                            return next();
                        }).catch(next);
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                },

                function (next) {
                    try {
                        var tenant = _.omit(Data.TENANT_TEST, ['id', 'createDate', 'updateDate']);
                        var contract = _.omit(Data.TENANT_1_CONTRACT_TEST, ['id', 'tenantId', 'createDate', 'updateDate']);
                        var administrator = {
                            'email': Data.TENANT_1.email,
                            'firstName': 'firstname new', 
                            'lastName': 'lastname new',
                            'phone': '12312122-12'
                        };
                        var postData = {
                            tenant: tenant,
                            contract: contract,
                            administrator: administrator
                        };
                        
                        
                        global.httpHelper.postError('/tenantManagementConnector/tenantAdminContractCreate', postData, 404, _.clone(StandardErrors['ERR_FATAL_ERROR']), next);
                        
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                },
                
                function (next) {
                    try {
                        Tenant.count().then(function (cnt) {
                            try {
                                assert.strictEqual(cnt, tenantsCnt);
                                return next();
                                
                            } catch (e) {
                                return next(e);
                            }
                        }).catch(next);
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                },
                function (next) {
                    try {
                        ProfileHasRole.count().then(function (cnt) {
                            try {
                                assert.isBelow(cnt, profileHasRoleCnt);
                                return next();
                                
                            } catch (e) {
                                return next(e);
                            }
                        }).catch(next);
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                },
                
                function (next) {
                    try {
                        assert.strictEqual(_tenantUpdateSpy.callCount, 1);
                        assert.strictEqual(_administratorCreateSpy.callCount, 1);
                        assert.strictEqual(_tenantAdminContractCreateRollbackSpy.callCount, 1);
                        assert.strictEqual(AerospikeAppSpy.callCount, 1);
                        assert.isDefined(AerospikeAppSpy.lastCall.args[0].tenant.id);
                        return setImmediate(next);
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                },

                
            ], done);
        });
        
        it('tenantAdminContractCreateRollback, _administratorCreate fail', function (done) {
            var _administratorCreateStub = global.wsHelper.sinonSandbox.stub(tmConnectorApiFactory, '_administratorCreate', function (args, cb) {
                return setImmediate(cb, 'ERR_FATAL_ERROR');
            });
            async.series([
                function (next) {
                    try {
                        var tenant = _.omit(Data.TENANT_TEST, ['id', 'createDate', 'updateDate']);
                        var contract = _.omit(Data.TENANT_1_CONTRACT_TEST, ['id', 'tenantId', 'createDate', 'updateDate']);
                        var administrator = {
                            'email': Data.TENANT_1.email,
                            'firstName': 'firstname new', 
                            'lastName': 'lastname new',
                            'phone': '12312122-12'
                        };
                        var postData = {
                            tenant: tenant,
                            contract: contract,
                            administrator: administrator
                        };
                        
                        
                        global.httpHelper.postError('/tenantManagementConnector/tenantAdminContractCreate', postData, 404, _.clone(StandardErrors['ERR_FATAL_ERROR']), next);
                        
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                }
            ], done);
        });
        
        
        it('tenantAdminContractCreateRollback, _tenantUpdate fail', function (done) {
            var _tenantUpdateStub = global.wsHelper.sinonSandbox.stub(tmConnectorApiFactory, '_tenantUpdate', function (args, cb) {
                return setImmediate(cb, 'ERR_FATAL_ERROR');
            });
            async.series([
                function (next) {
                    try {
                        var tenant = _.omit(Data.TENANT_TEST, ['id', 'createDate', 'updateDate']);
                        var contract = _.omit(Data.TENANT_1_CONTRACT_TEST, ['id', 'tenantId', 'createDate', 'updateDate']);
                        var administrator = {
                            'email': Data.TENANT_1.email,
                            'firstName': 'firstname new', 
                            'lastName': 'lastname new',
                            'phone': '12312122-12'
                        };
                        var postData = {
                            tenant: tenant,
                            contract: contract,
                            administrator: administrator
                        };
                        
                        
                        global.httpHelper.postError('/tenantManagementConnector/tenantAdminContractCreate', postData, 404, _.clone(StandardErrors['ERR_FATAL_ERROR']), next);
                        
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                }
            ], done);
        });
        
       it('tenantAdminContractCreate error, validation error, email missing', function (done) {
           async.series([
               function (next) {
                    try {
                        var tenant = _.omit(Data.TENANT_TEST, ['id', 'createDate', 'updateDate']);
                        var contract = _.omit(Data.TENANT_1_CONTRACT_TEST, ['id', 'tenantId', 'createDate', 'updateDate']);
                        var administrator = {
                            //'email': Data.TENANT_1.email,
                            'firstName': 'firstname new', 
                            'lastName': 'lastname new',
                            'phone': '12312122-12'
                        };
                        var postData = {
                            tenant: tenant,
                            contract: contract,
                            administrator: administrator
                        };
                        
                        global.httpHelper.postError('/tenantManagementConnector/tenantAdminContractCreate', postData, 404, _.clone(StandardErrors['ERR_VALIDATION_FAILED']), next);
                        
                    } catch (e) {
                        return setImmediate(next, e);
                    }
               }
           ], done);
       });
       it('tenantAdminContractCreate error, validation error, tenant name missing', function (done) {
           async.series([
               function (next) {
                    try {
                        var tenant = _.omit(Data.TENANT_TEST, ['id', 'createDate', 'updateDate']);
                        delete tenant.name;
                        var contract = _.omit(Data.TENANT_1_CONTRACT_TEST, ['id', 'tenantId', 'createDate', 'updateDate']);
                        var administrator = {
                            'email': Data.TENANT_1.email,
                            'firstName': 'firstname new', 
                            'lastName': 'lastname new',
                            'phone': '12312122-12'
                        };
                        var postData = {
                            tenant: tenant,
                            contract: contract,
                            administrator: administrator
                        };
                        
                        global.httpHelper.postError('/tenantManagementConnector/tenantAdminContractCreate', postData, 404, _.clone(StandardErrors['ERR_VALIDATION_FAILED']), next);
                        
                    } catch (e) {
                        return setImmediate(next, e);
                    }
               }
           ], done);
       });
        
        
   });
});
