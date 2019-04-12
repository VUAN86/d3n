var _ = require('lodash');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var ProfileData = require('./../config/profile.data.js');
var DataIds = require('./../config/_id.data.js');
var ShopData = require('./../config/shop.data.js');

describe('WS ShopTenant API', function () {
    this.timeout(10000);
    global.wsHelper.series().forEach(function (serie) {
        
        describe('[' + serie + '] ' + 'tenant', function () {
            
            it('[' + serie + '] ' + 'add tenant', function (done) {
                global.wsHelper.apiSecureCall(serie, 'shop/tenantDelete', ProfileData.CLIENT_INFO, 
                ShopData.TENANT_DELETE, function (err, message) {
                    try {
                        //disregard response - if not exists is error when deleting - just make sure tenant does not exist before adding it
                        global.wsHelper.apiSecureCall(serie, 'shop/tenantAdd', ProfileData.CLIENT_INFO,
                        ShopData.TENANT_ADD, function (err, message) {
                            try {
                                var content = message.getContent();
                                should(err).not.Error;
                                should.not.exist(message.getError());
                                should.exist(content);
                                content.should.have.property("id");
                                content.id.should.exactly(ShopData.TENANT_ADD.id);
                                content.should.have.property("streetNumber");
                                content.streetNumber.should.exactly(ShopData.TENANT_ADD.streetNumber);
                                done();
                            } catch (e) {
                                done(e);
                            }
                        });
                    } catch (e) {
                        done(e);
                    }
                });
            });
            
            it('[' + serie + '] ' + 'add tenant invalid field', function (done) {
                global.wsHelper.apiSecureCall(serie, 'shop/tenantAdd', ProfileData.CLIENT_INFO,
                ShopData.TENANT_ADD_INVALID, function (err, message) {
                    try {
                        var content = message.getContent();
                        should(err).not.Error;
                        if (process.env.VALIDATE_FULL_MESSAGE === "true") {
                            message.getError().message.should.exactly(Errors.ValidationFailed);
                        } else {
                            message.getError().message.should.exactly(Errors.RequestMapping);
                        }
                        done();
                    } catch (e) {
                        done(e);
                    }
                });
            });
            
            it('[' + serie + '] ' + 'update tenant', function (done) {
                global.wsHelper.apiSecureCall(serie, 'shop/tenantAdd', ProfileData.CLIENT_INFO, 
                ShopData.TENANT_ADD, function (err, message) {
                    try {
                        //disregard response - if exists is error when adding - just make sure tenant exists before updating it
                        global.wsHelper.apiSecureCall(serie, 'shop/tenantUpdate', ProfileData.CLIENT_INFO,
                        ShopData.TENANT_UPDATE, function (err, message) {
                            try {
                                var content = message.getContent();
                                should(err).not.Error;
                                should.not.exist(message.getError());
                                should.exist(content);
                                content.should.have.property("id");
                                content.id.should.exactly(ShopData.TENANT_UPDATE.id);
                                content.should.have.property("streetNumber");
                                content.streetNumber.should.exactly(ShopData.TENANT_UPDATE.streetNumber);
                                done();
                            } catch (e) {
                                done(e);
                            }
                        });
                    } catch (e) {
                        done(e);
                    }
                });
            });

            it('[' + serie + '] ' + 'update non existing tenant', function (done) {
                global.wsHelper.apiSecureCall(serie, 'shop/tenantDelete', ProfileData.CLIENT_INFO, 
                ShopData.TENANT_DELETE, function (err, message) {
                    try {
                        //disregard response - if not exists is error when deleting - just make sure tenant does not exist before updating it
                        global.wsHelper.apiSecureCall(serie, 'shop/tenantUpdate', ProfileData.CLIENT_INFO,
                        ShopData.TENANT_UPDATE, function (err, message) {
                            try {
                                var content = message.getContent();
                                should(err).not.Error;
                                message.getError().message.should.exactly(Errors.NotFound);
                                done();
                            } catch (e) {
                                done(e);
                            }
                        });
                    } catch (e) {
                        done(e);
                    }
                });
            });
            
            it('[' + serie + '] ' + 'delete tenant', function (done) {
                global.wsHelper.apiSecureCall(serie, 'shop/tenantAdd', ProfileData.CLIENT_INFO, 
                ShopData.TENANT_ADD, function (err, message) {
                    try {
                        //disregard response - if exists is error when adding - just make sure tenant exists before deleting it
                        global.wsHelper.apiSecureCall(serie, 'shop/tenantDelete', ProfileData.CLIENT_INFO,
                        ShopData.TENANT_DELETE, function (err, message) {
                            try {
                                var content = message.getContent();
                                should(err).not.Error;
                                should.not.exist(message.getError());
                                done();
                            } catch (e) {
                                done(e);
                            }
                        });
                    } catch (e) {
                        done(e);
                    }
                });
            });
            
            it('[' + serie + '] ' + 'delete non existing tenant', function (done) {
                global.wsHelper.apiSecureCall(serie, 'shop/tenantDelete', ProfileData.CLIENT_INFO, 
                ShopData.TENANT_DELETE, function (err, message) {
                    try {
                        //disregard response - if not exists is error when deleting - just make sure tenant does not exist before deleting it
                        global.wsHelper.apiSecureCall(serie, 'shop/tenantDelete', ProfileData.CLIENT_INFO,
                        ShopData.TENANT_DELETE, function (err, message) {
                            try {
                                var content = message.getContent();
                                should(err).not.Error;
                                message.getError().message.should.exactly(Errors.NotFound);
                                done();
                            } catch (e) {
                                done(e);
                            }
                        });
                    } catch (e) {
                        done(e);
                    }
                });
            });
        });
    });
});