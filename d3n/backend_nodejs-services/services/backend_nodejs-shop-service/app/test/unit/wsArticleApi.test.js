var _ = require('lodash');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var ProfileData = require('./../config/profile.data.js');
var DataIds = require('./../config/_id.data.js');
var ShopData = require('./../config/shop.data.js');

describe('WS ShopArticle API', function () {
    this.timeout(10000);
    global.wsHelper.series().forEach(function (serie) {
        
        describe('[' + serie + '] ' + 'articleList', function () {
            
            it('[' + serie + '] ' + 'get first page articles', function (done) {
                var reqContent = {
                    limit: 10,
                    offset: 0,
                    searchBy: {
                        categoryId: DataIds.CATEGORY_ID,
                        text: "a"
                    },
                    orderBy: {
                        field: "title",
                        direction: "asc"
                    },
                    isActive: "1"
                };
                global.wsHelper.apiSecureCall(serie, 'shop/articleList', ProfileData.CLIENT_INFO, reqContent, function (err, message) {
                    try {
                        var content = message.getContent();
                        should(err).not.Error;
                        should.not.exist(message.getError());
                        should.exist(content);
                        content.should.have.property("limit");
                        content.should.have.property("offset");
                        content.should.have.property("items");
                        done();
                    } catch (e) {
                        done(e);
                    }
                });
            });
            
            it('[' + serie + '] ' + 'get articles error mapping order field', function (done) {
                var reqContent = {
                    limit: 10,
                    offset: 0,
                    orderBy: {
                        field: "invalid",
                        direction: "asc"
                    },
                    searchBy: {
                        categoryId: "",
                        text: ""
                    }
                };
                global.wsHelper.apiSecureCall(serie, 'shop/articleList', ProfileData.CLIENT_INFO, reqContent, function (err, message) {
                    try {
                        should(err).not.Error;
                        message.getError().should.have.property("message", Errors.RequestMapping);
                        done();
                    } catch (e) {
                        done(e);
                    }
                });
            });
        });
        
        describe('[' + serie + '] ' + 'article', function () {
            
            it('[' + serie + '] ' + 'get article', function (done) {
                var reqContent = {
                    id: DataIds.ARTICLE_ID,
                };
                global.wsHelper.apiSecureCall(serie, 'shop/articleGet', ProfileData.CLIENT_INFO, reqContent, function (err, message) {
                    try {
                        var content = message.getContent();
                        should(err).not.Error;
                        should.not.exist(message.getError());
                        should.exist(content);
                        content.should.have.property("id");
                        content.id.should.exactly(reqContent.id);
                        done();
                    } catch (e) {
                        done(e);
                    }
                });
            });
            
            it('[' + serie + '] ' + 'get non existent article', function (done) {
                var reqContent = {
                    id: DataIds.ARTICLE_NON_EXISTING_ID,
                };
                global.wsHelper.apiSecureCall(serie, 'shop/articleGet', ProfileData.CLIENT_INFO, reqContent, function (err, message) {
                    try {
                        var content = message.getContent();
                        should(err).not.Error;
                        message.getError().message.should.exactly(Errors.NotFound);
                        done();
                    } catch (e) {
                        done(e);
                    }
                });
            });
            
            it('[' + serie + '] ' + 'add article', function (done) {
                global.wsHelper.apiSecureCall(serie, 'shop/articleDelete', ProfileData.CLIENT_INFO, 
                ShopData.ARTICLE_DELETE, function (err, message) {
                    try {
                        //disregard response - if not exists is error when deleting - just make sure article does not exist before adding it
                        global.wsHelper.apiSecureCall(serie, 'shop/articleAdd', ProfileData.CLIENT_INFO,
                        ShopData.ARTICLE_ADD, function (err, message) {
                            try {
                                var content = message.getContent();
                                should(err).not.Error;
                                should.not.exist(message.getError());
                                should.exist(content);
                                content.should.have.property("id");
                                content.id.should.exactly(ShopData.ARTICLE_ADD.id);
                                content.should.have.property("fullMoneyPrice");
                                content.fullMoneyPrice.should.exactly(ShopData.ARTICLE_ADD.fullMoneyPrice);
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
            
            it('[' + serie + '] ' + 'add article invalid field', function (done) {
                global.wsHelper.apiSecureCall(serie, 'shop/articleAdd', ProfileData.CLIENT_INFO,
                ShopData.ARTICLE_ADD_INVALID, function (err, message) {
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
            
            it('[' + serie + '] ' + 'update article', function (done) {
                global.wsHelper.apiSecureCall(serie, 'shop/articleAdd', ProfileData.CLIENT_INFO, 
                ShopData.ARTICLE_ADD, function (err, message) {
                    try {
                        //disregard response - if exists is error when adding - just make sure article exists before updating it
                        global.wsHelper.apiSecureCall(serie, 'shop/articleUpdate', ProfileData.CLIENT_INFO,
                        ShopData.ARTICLE_UPDATE, function (err, message) {
                            try {
                                var content = message.getContent();
                                should(err).not.Error;
                                should.not.exist(message.getError());
                                should.exist(content);
                                content.should.have.property("id");
                                content.id.should.exactly(ShopData.ARTICLE_UPDATE.id);
                                content.should.have.property("fullMoneyPrice");
                                content.fullMoneyPrice.should.exactly(ShopData.ARTICLE_UPDATE.fullMoneyPrice);
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

            it('[' + serie + '] ' + 'update non existing article', function (done) {
                global.wsHelper.apiSecureCall(serie, 'shop/articleDelete', ProfileData.CLIENT_INFO, 
                ShopData.ARTICLE_DELETE, function (err, message) {
                    try {
                        //disregard response - if not exists is error when deleting - just make sure article does not exist before updating it
                        global.wsHelper.apiSecureCall(serie, 'shop/articleUpdate', ProfileData.CLIENT_INFO,
                        ShopData.ARTICLE_UPDATE, function (err, message) {
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
            
            it('[' + serie + '] ' + 'delete article', function (done) {
                global.wsHelper.apiSecureCall(serie, 'shop/articleAdd', ProfileData.CLIENT_INFO, 
                ShopData.ARTICLE_ADD, function (err, message) {
                    try {
                        //disregard response - if exists is error when adding - just make sure article exists before deleting it
                        global.wsHelper.apiSecureCall(serie, 'shop/articleDelete', ProfileData.CLIENT_INFO,
                        ShopData.ARTICLE_DELETE, function (err, message) {
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
            
            it('[' + serie + '] ' + 'delete non existing article', function (done) {
                global.wsHelper.apiSecureCall(serie, 'shop/articleDelete', ProfileData.CLIENT_INFO, 
                ShopData.ARTICLE_DELETE, function (err, message) {
                    try {
                        //disregard response - if not exists is error when deleting - just make sure article does not exist before deleting it
                        global.wsHelper.apiSecureCall(serie, 'shop/articleDelete', ProfileData.CLIENT_INFO,
                        ShopData.ARTICLE_DELETE, function (err, message) {
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