var _ = require('lodash');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var ProfileData = require('./../config/profile.data.js');
var DataIds = require('./../config/_id.data.js');

describe('WS ShopUserProfile API', function () {
    this.timeout(10000);
    global.wsHelper.series().forEach(function (serie) {

        describe('[' + serie + '] ' + 'shopping cart', function () {
            it('[' + serie + '] ' + 'set shopping cart', function (done) {
                var reqContent = {
                    articleId: DataIds.ARTICLE_ID,
                };
                global.wsHelper.initProfiles();
                global.wsHelper.apiSecureCall(serie, 'shop/articleSetSelect', ProfileData.CLIENT_INFO, reqContent, function (err, message) {
                    try {
                        var content = message.getContent();
                        should(err).not.Error;
                        should.not.exist(message.getError());
                        done();
                    } catch (e) {
                        done(e);
                    }
                });
            });
            
            it('[' + serie + '] ' + 'get shopping cart', function (done) {
                var reqContent = {
                    articleId: DataIds.ARTICLE_ID,
                };
                global.wsHelper.initProfiles();
                global.wsHelper.apiSecureCall(serie, 'shop/articleSetSelect', ProfileData.CLIENT_INFO, reqContent, function (err, message) {
                    try {
                        global.wsHelper.apiSecureCall(serie, 'shop/articleGetSelect', ProfileData.CLIENT_INFO, null, function (err, message) {
                            try {
                                var content = message.getContent();
                                should(err).not.Error;
                                should.not.exist(message.getError());
                                should.exist(content);
                                content.should.have.property("articleId", DataIds.ARTICLE_ID);
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
        
        describe('[' + serie + '] ' + 'shipping address', function () {
            it('[' + serie + '] ' + 'set shipping address', function (done) {
                global.wsHelper.initProfiles();
                global.wsHelper.apiSecureCall(serie, 'shop/shippingSet', ProfileData.CLIENT_INFO,
                        ProfileData.SHIPPING_ADDRESS, function (err, message) {
                    try {
                        var content = message.getContent();
                        should(err).not.Error;
                        should.not.exist(message.getError());
                        done();
                    } catch (e) {
                        done(e);
                    }
                });
            });
            
            it('[' + serie + '] ' + 'set shipping address invalid country', function (done) {
                global.wsHelper.initProfiles();
                global.wsHelper.apiSecureCall(serie, 'shop/shippingSet', ProfileData.CLIENT_INFO,
                        ProfileData.SHIPPING_ADDRESS_INVALID_COUNTRY, function (err, message) {
                    try {
                        var content = message.getContent();
                        should(err).not.Error;
                        if (process.env.VALIDATE_FULL_MESSAGE === "true") {
                            message.getError().message.should.exactly(Errors.ValidationFailed);
                        } else {
                            message.getError().message.should.exactly(Errors.FatalError);
                        }
                        done();
                    } catch (e) {
                        done(e);
                    }
                });
            });
            
            it('[' + serie + '] ' + 'set shipping address missing name', function (done) {
                global.wsHelper.initProfiles();
                global.wsHelper.apiSecureCall(serie, 'shop/shippingSet', ProfileData.CLIENT_INFO,
                        ProfileData.SHIPPING_ADDRESS_MISSING_NAME, function (err, message) {
                    try {
                        var content = message.getContent();
                        should(err).not.Error;
                        if (process.env.VALIDATE_FULL_MESSAGE === "true") {
                            message.getError().message.should.exactly(Errors.ValidationFailed);
                        } else {
                            message.getError().message.should.exactly(Errors.FatalError);
                        }
                        done();
                    } catch (e) {
                        done(e);
                    }
                });
            });
            
            it('[' + serie + '] ' + 'get shipping address', function (done) {
                global.wsHelper.initProfiles();
                global.wsHelper.apiSecureCall(serie, 'shop/shippingSet', ProfileData.CLIENT_INFO,
                        ProfileData.SHIPPING_ADDRESS, function (err, message) {
                    try {
                        global.wsHelper.apiSecureCall(serie, 'shop/shippingGet', ProfileData.CLIENT_INFO, null, function (err, message) {
                            try {
                                var content = message.getContent();
                                should(err).not.Error;
                                should.not.exist(message.getError());
                                should.exist(content);
                                content.should.have.property("firstName", ProfileData.SHIPPING_ADDRESS.firstName);
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