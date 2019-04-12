var _ = require('lodash');
var should = require('should');
var sinon = require('sinon');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var ProfileData = require('./../config/profile.data.js');
var DataIds = require('./../config/_id.data.js');
var Timestamp = require('./../../helpers/timestamp.js');
var Validator = require('./../../helpers/validator.js');

describe('WS ShopOrder API', function () {
    this.timeout(10000);
    global.wsHelper.series().forEach(function (serie) {
        
        describe('[' + serie + '] ' + 'intiateOrder', function () {
            
            it('[' + serie + '] ' + 'initiateOrder with billing and shipping address success', function (done) {
                var articleSetSelectRequestContent = {
                    articleId: DataIds.ARTICLE_ID,
                };
                var initiateOrderRequestContent = {
                    isShippingAddress: true,
                };
                global.wsHelper.initProfiles();
                global.wsHelper.apiSecureCall(serie, 'shop/articleSetSelect', ProfileData.CLIENT_INFO, 
                articleSetSelectRequestContent, function (err, message) {
                    try {
                        global.wsHelper.apiSecureCall(serie, 'shop/shippingSet', ProfileData.CLIENT_INFO,
                        ProfileData.SHIPPING_ADDRESS, function (err, message) {
                            try {
                                global.wsHelper.apiSecureCall(serie, 'shop/initiateOrder', ProfileData.CLIENT_INFO,
                                initiateOrderRequestContent, function (err, message) {
                                    try {
                                        var content = message.getContent();
                                        should(err).not.Error;
                                        should.not.exist(message.getError());
                                        should.exist(content);
                                        content.should.be.an.Object;
                                        content.should.have.property("article");
                                        content.article.should.have.property("id", DataIds.ARTICLE_ID);
                                        content.should.have.property("billingAddress");
                                        content.billingAddress.should.have.property("street", ProfileData.TEST_PROFILE.address.street);
                                        content.billingAddress.should.have.property("email", ProfileData.TEST_PROFILE.emails[0].email);
                                        Validator.validateAddress(content.billingAddress);
                                        content.should.have.property("shippingAddress");
                                        content.shippingAddress.should.have.property("street", ProfileData.SHIPPING_ADDRESS.street);
                                        Validator.validateAddress(content.shippingAddress);
                                        done();
                                    } catch (e) {
                                        done(e);
                                    }
                                });
                            } catch (e) {
                                done(e);
                            }
                        });
                    } catch (e) {
                        done(e);
                    }
                });
            });
            
            it('[' + serie + '] ' + 'initiateOrder with missing shipping address success', function (done) {
                var articleSetSelectRequestContent = {
                    articleId: DataIds.ARTICLE_ID,
                };
                var initiateOrderRequestContent = {
                    isShippingAddress: true,
                };
                global.wsHelper.initProfiles();
                global.wsHelper.apiSecureCall(serie, 'shop/articleSetSelect', ProfileData.CLIENT_INFO,
                articleSetSelectRequestContent, function (err, message) {
                    try {
                        global.wsHelper.apiSecureCall(serie, 'shop/initiateOrder', ProfileData.CLIENT_INFO,
                        initiateOrderRequestContent, function (err, message) {
                            try {
                                var content = message.getContent();
                                should(err).not.Error;
                                should.not.exist(message.getError());
                                should.exist(content);
                                content.should.be.an.Object;
                                content.should.have.property("article");
                                content.article.should.have.property("id", DataIds.ARTICLE_ID);
                                content.should.have.property("billingAddress");
                                content.billingAddress.should.have.property("street", ProfileData.TEST_PROFILE.address.street);
                                Validator.validateAddress(content.billingAddress);
                                content.should.have.property("shippingAddress");
                                content.shippingAddress.should.have.property("street", ProfileData.TEST_PROFILE.address.street);
                                Validator.validateAddress(content.shippingAddress);
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
            
            it('[' + serie + '] ' + 'initiateOrder with only billing address success', function (done) {
                var reqContent = {
                    articleId: DataIds.ARTICLE_ID,
                };
                global.wsHelper.initProfiles();
                global.wsHelper.apiSecureCall(serie, 'shop/articleSetSelect', ProfileData.CLIENT_INFO, reqContent, function (err, message) {
                    try {
                        global.wsHelper.apiSecureCall(serie, 'shop/initiateOrder', ProfileData.CLIENT_INFO, {}, function (err, message) {
                            try {
                                var content = message.getContent();
                                should(err).not.Error;
                                should.not.exist(message.getError());
                                should.exist(content);
                                content.should.be.an.Object;
                                content.should.have.property("article");
                                content.article.should.have.property("id", DataIds.ARTICLE_ID);
                                content.should.have.property("billingAddress");
                                content.billingAddress.should.have.property("street", ProfileData.TEST_PROFILE.address.street);
                                Validator.validateAddress(content.billingAddress);
                                content.should.have.property("shippingAddress");
                                content.shippingAddress.should.have.property("street", ProfileData.TEST_PROFILE.address.street);
                                Validator.validateAddress(content.shippingAddress);
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
            
            it('[' + serie + '] ' + 'initiateOrder with no money no bonus fail', function (done) {
                var reqContent = {
                    articleId: DataIds.ARTICLE_ID_NO_MONEY_NO_BONUS,
                };
                global.wsHelper.initProfiles();
                global.wsHelper.apiSecureCall(serie, 'shop/articleSetSelect', ProfileData.CLIENT_INFO, reqContent, function (err, message) {
                    try {
                        global.wsHelper.apiSecureCall(serie, 'shop/initiateOrder', ProfileData.CLIENT_INFO, {}, function (err, message) {
                            try {
                                should(err).not.Error;
                                message.getError().should.have.property("message", Errors.MissingPrice);
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

        //WARNING: due to error in payment response (transactionId should be string)
        //all tests involving payment will fail when validation is enabled
        describe('[' + serie + '] ' + 'completeOrder', function () {
            
            it('[' + serie + '] ' + 'completeOrder success with credit reward', function (done) {
                var reqContent = {
                    articleId: DataIds.ARTICLE_ID,
                };
                global.wsHelper.initProfiles();
                global.wsHelper.apiSecureCall(serie, 'shop/articleSetSelect', ProfileData.CLIENT_INFO, reqContent, function (err, message) {
                    try {
                        global.wsHelper.apiSecureCall(serie, 'shop/initiateOrder', ProfileData.CLIENT_INFO, {}, function (err, message) {
                            try {
                                global.wsHelper.apiSecureCall(serie, 'shop/completeOrder', ProfileData.CLIENT_INFO, {}, function (err, message) {
                                    try {
                                        var content = message.getContent();
                                        should(err).not.Error;
                                        should.not.exist(message.getError());
                                        should.exist(content);
                                        content.should.be.an.Object;
                                        content.should.have.property("orderId");
                                        done();
                                    } catch (e) {
                                        done(e);
                                    }
                                });
                            } catch (e) {
                                done(e);
                            }
                        });
                    } catch (e) {
                        done(e);
                    }
                });
            });
            
            it('[' + serie + '] ' + 'completeOrder only money success', function (done) {
                var reqContent = {
                    articleId: DataIds.ARTICLE_ID_MONEY_NO_BONUS,
                };
                global.wsHelper.initProfiles();
                global.wsHelper.apiSecureCall(serie, 'shop/articleSetSelect', ProfileData.CLIENT_INFO, reqContent, function (err, message) {
                    try {
                        global.wsHelper.apiSecureCall(serie, 'shop/initiateOrder', ProfileData.CLIENT_INFO, {}, function (err, message) {
                            try {
                                global.wsHelper.apiSecureCall(serie, 'shop/completeOrder', ProfileData.CLIENT_INFO, {}, function (err, message) {
                                    try {
                                        var content = message.getContent();
                                        should(err).not.Error;
                                        should.not.exist(message.getError());
                                        should.exist(content);
                                        content.should.be.an.Object;
                                        content.should.have.property("orderId");
                                        done();
                                    } catch (e) {
                                        done(e);
                                    }
                                });
                            } catch (e) {
                                done(e);
                            }
                        });
                    } catch (e) {
                        done(e);
                    }
                });
            });
            
            it('[' + serie + '] ' + 'completeOrder only bonus success', function (done) {
                var reqContent = {
                    articleId: DataIds.ARTICLE_ID_BONUS_NO_MONEY,
                };
                global.wsHelper.initProfiles();
                global.wsHelper.apiSecureCall(serie, 'shop/articleSetSelect', ProfileData.CLIENT_INFO, reqContent, function (err, message) {
                    try {
                        global.wsHelper.apiSecureCall(serie, 'shop/initiateOrder', ProfileData.CLIENT_INFO, {}, function (err, message) {
                            try {
                                global.wsHelper.apiSecureCall(serie, 'shop/completeOrder', ProfileData.CLIENT_INFO, {}, function (err, message) {
                                    try {
                                        var content = message.getContent();
                                        should(err).not.Error;
                                        should.not.exist(message.getError());
                                        should.exist(content);
                                        content.should.be.an.Object;
                                        content.should.have.property("orderId");
                                        done();
                                    } catch (e) {
                                        done(e);
                                    }
                                });
                            } catch (e) {
                                done(e);
                            }
                        });
                    } catch (e) {
                        done(e);
                    }
                });
            });
            
            it('[' + serie + '] ' + 'completeOrder full money price success', function (done) {
                var articleSetSelectRequestContent = {
                    articleId: DataIds.ARTICLE_ID,
                };
                var completeOrderRequestContent = {
                    isFullPrice: true,
                };
                global.wsHelper.initProfiles();
                global.wsHelper.apiSecureCall(serie, 'shop/articleSetSelect', ProfileData.CLIENT_INFO,
                articleSetSelectRequestContent, function (err, message) {
                    try {
                        global.wsHelper.apiSecureCall(serie, 'shop/initiateOrder', ProfileData.CLIENT_INFO, 
                        null, function (err, message) {
                            try {
                                global.wsHelper.apiSecureCall(serie, 'shop/completeOrder', ProfileData.CLIENT_INFO,
                                completeOrderRequestContent, function (err, message) {
                                    try {
                                        var content = message.getContent();
                                        should(err).not.Error;
                                        should.not.exist(message.getError());
                                        should.exist(content);
                                        content.should.be.an.Object;
                                        content.should.have.property("orderId");
                                        done();
                                    } catch (e) {
                                        done(e);
                                    }
                                });
                            } catch (e) {
                                done(e);
                            }
                        });
                    } catch (e) {
                        done(e);
                    }
                });
            });
            
            it('[' + serie + '] ' + 'completeOrder no money no bonus fail', function (done) {
                var reqContent = {
                    articleId: DataIds.ARTICLE_ID_NO_MONEY_NO_BONUS,
                };
                global.wsHelper.initProfiles();
                global.wsHelper.apiSecureCall(serie, 'shop/articleSetSelect', ProfileData.CLIENT_INFO, reqContent, function (err, message) {
                    try {
                        global.wsHelper.apiSecureCall(serie, 'shop/initiateOrder', ProfileData.CLIENT_INFO, {}, function (err, message) {
                            try {
                                global.wsHelper.apiSecureCall(serie, 'shop/completeOrder', ProfileData.CLIENT_INFO, {}, function (err, message) {
                                    try {
                                        should(err).not.Error;
                                        message.getError().should.have.property("message", Errors.MissingPrice);
                                        done();
                                    } catch (e) {
                                        done(e);
                                    }
                                });
                            } catch (e) {
                                done(e);
                            }
                        });
                    } catch (e) {
                        done(e);
                    }
                });
            });
        
            it('[' + serie + '] ' + 'completeOrder no full money fail', function (done) {
                var articleSetSelectRequestContent = {
                    articleId: DataIds.ARTICLE_ID_NO_MONEY_NO_BONUS,
                };
                var completeOrderRequestContent = {
                    isFullPrice: true,
                };
                global.wsHelper.initProfiles();
                global.wsHelper.apiSecureCall(serie, 'shop/articleSetSelect', ProfileData.CLIENT_INFO,
                articleSetSelectRequestContent, function (err, message) {
                    try {
                        global.wsHelper.apiSecureCall(serie, 'shop/initiateOrder', ProfileData.CLIENT_INFO,
                        null, function (err, message) {
                            try {
                                global.wsHelper.apiSecureCall(serie, 'shop/completeOrder', ProfileData.CLIENT_INFO,
                                completeOrderRequestContent, function (err, message) {
                                    try {
                                        should(err).not.Error;
                                        message.getError().should.have.property("message", Errors.MissingPrice);
                                        done();
                                    } catch (e) {
                                        done(e);
                                    }
                                });
                            } catch (e) {
                                done(e);
                            }
                        });
                    } catch (e) {
                        done(e);
                    }
                });
            });
        
            it('[' + serie + '] ' + 'completeOrder fail money payment', function (done) {
                var reqContent = {
                    articleId: DataIds.ARTICLE_ID,
                };
                var fakePay = global.wsHelper.sinonSandbox.stub(require('./../../factories/paymentApiFactory.js'), 'pay');
                fakePay.yields(Errors.FatalError);
                global.wsHelper.initProfiles();
                global.wsHelper.apiSecureCall(serie, 'shop/articleSetSelect', ProfileData.CLIENT_INFO, reqContent, function (err, message) {
                    try {
                        global.wsHelper.apiSecureCall(serie, 'shop/initiateOrder', ProfileData.CLIENT_INFO, {}, function (err, message) {
                            try {
                                global.wsHelper.apiSecureCall(serie, 'shop/completeOrder', ProfileData.CLIENT_INFO, {}, function (err, message) {
                                    try {
                                        should(err).not.Error;
                                        message.getError().should.have.property("message")
                                            .oneOf(Errors.Payment, Errors.PaymentWithoutNotificationRecipient);
                                        done();
                                    } catch (e) {
                                        done(e);
                                    }
                                });
                            } catch (e) {
                                done(e);
                            }
                        });
                    } catch (e) {
                        done(e);
                    }
                });
            });
             
            it('[' + serie + '] ' + 'completeOrder fail bonus payment rollback money success', function (done) {
                var reqContent = {
                    articleId: DataIds.ARTICLE_ID,
                };
                var fakePay = global.wsHelper.sinonSandbox.stub(require('./../../factories/paymentApiFactory.js'), 'pay');
                fakePay.withArgs(sinon.match.any, sinon.match.any, Config.currencyType.money, sinon.match.any, sinon.match.any)
                        .yields(null, 1);
                fakePay.withArgs(sinon.match.any, sinon.match.any, Config.currencyType.bonus, sinon.match.any, sinon.match.any)
                        .yields(Errors.FatalError);
                global.wsHelper.initProfiles();
                global.wsHelper.apiSecureCall(serie, 'shop/articleSetSelect', ProfileData.CLIENT_INFO, reqContent, function (err, message) {
                    try {
                        global.wsHelper.apiSecureCall(serie, 'shop/initiateOrder', ProfileData.CLIENT_INFO, {}, function (err, message) {
                            try {
                                global.wsHelper.apiSecureCall(serie, 'shop/completeOrder', ProfileData.CLIENT_INFO, {}, function (err, message) {
                                    try {
                                        should(err).not.Error;
                                        message.getError().should.have.property("message")
                                            .oneOf(Errors.PaymentRollback, Errors.PaymentRollbackWithoutNotificationRecipient);
                                        done();
                                    } catch (e) {
                                        done(e);
                                    }
                                });
                            } catch (e) {
                                done(e);
                            }
                        });
                    } catch (e) {
                        done(e);
                    }
                });
            });
            
            it('[' + serie + '] ' + 'completeOrder fail bonus payment rollback money fail', function (done) {
                var reqContent = {
                    articleId: DataIds.ARTICLE_ID,
                };
                var fakePay = global.wsHelper.sinonSandbox.stub(require('./../../factories/paymentApiFactory.js'), 'pay');
                fakePay.withArgs(sinon.match.any, sinon.match.any, Config.currencyType.money, sinon.match.any, sinon.match.any)
                        .yields(null, 1);
                fakePay.withArgs(sinon.match.any, sinon.match.any, Config.currencyType.bonus, sinon.match.any, sinon.match.any)
                        .yields(Errors.FatalError);
                var fakeRollback = global.wsHelper.sinonSandbox.stub(require('./../../factories/paymentApiFactory.js'), 'rollback');
                fakeRollback.yields(Errors.FatalError);
                global.wsHelper.initProfiles();
                global.wsHelper.apiSecureCall(serie, 'shop/articleSetSelect', ProfileData.CLIENT_INFO, reqContent, function (err, message) {
                    try {
                        global.wsHelper.apiSecureCall(serie, 'shop/initiateOrder', ProfileData.CLIENT_INFO, {}, function (err, message) {
                            try {
                                global.wsHelper.apiSecureCall(serie, 'shop/completeOrder', ProfileData.CLIENT_INFO, {}, function (err, message) {
                                    try {
                                        should(err).not.Error;
                                        message.getError().should.have.property("message")
                                            .oneOf(Errors.Rollback, Errors.RollbackWithoutNotificationRecipient);
                                        done();
                                    } catch (e) {
                                        done(e);
                                    }
                                });
                            } catch (e) {
                                done(e);
                            }
                        });
                    } catch (e) {
                        done(e);
                    }
                });
            });
            
            it('[' + serie + '] ' + 'completeOrder fail full rollback success', function (done) {
                var reqContent = {
                    articleId: DataIds.ARTICLE_ID,
                };
                var fakeFinalize = global.wsHelper.sinonSandbox.stub(require('./../../factories/orderApiFactory.js'), 'completeOrder');
                fakeFinalize.yields(Errors.FatalError);
                global.wsHelper.initProfiles();
                global.wsHelper.apiSecureCall(serie, 'shop/articleSetSelect', ProfileData.CLIENT_INFO, reqContent, function (err, message) {
                    try {
                        global.wsHelper.apiSecureCall(serie, 'shop/initiateOrder', ProfileData.CLIENT_INFO, {}, function (err, message) {
                            try {
                                global.wsHelper.apiSecureCall(serie, 'shop/completeOrder', ProfileData.CLIENT_INFO, {}, function (err, message) {
                                    try {
                                        should(err).not.Error;
                                        message.getError().should.have.property("message")
                                            .oneOf(Errors.PaymentRollback, Errors.PaymentRollbackWithoutNotificationRecipient);
                                        done();
                                    } catch (e) {
                                        done(e);
                                    }
                                });
                            } catch (e) {
                                done(e);
                            }
                        });
                    } catch (e) {
                        done(e);
                    }
                });
            });
            
            it('[' + serie + '] ' + 'completeOrder fail full rollback fail', function (done) {
                var reqContent = {
                    articleId: DataIds.ARTICLE_ID,
                };
                var fakeFinalize = global.wsHelper.sinonSandbox.stub(require('./../../factories/orderApiFactory.js'), 'completeOrder');
                fakeFinalize.yields(Errors.FatalError);
                var fakeRollback = global.wsHelper.sinonSandbox.stub(require('./../../factories/paymentApiFactory.js'), 'rollback');
                fakeRollback.yields(Errors.FatalError);
                global.wsHelper.initProfiles();
                global.wsHelper.apiSecureCall(serie, 'shop/articleSetSelect', ProfileData.CLIENT_INFO, reqContent, function (err, message) {
                    try {
                        global.wsHelper.apiSecureCall(serie, 'shop/initiateOrder', ProfileData.CLIENT_INFO, {}, function (err, message) {
                            try {
                                global.wsHelper.apiSecureCall(serie, 'shop/completeOrder', ProfileData.CLIENT_INFO, {}, function (err, message) {
                                    try {
                                        should(err).not.Error;
                                        message.getError().should.have.property("message")
                                            .oneOf(Errors.Rollback, Errors.RollbackWithoutNotificationRecipient);
                                        done();
                                    } catch (e) {
                                        done(e);
                                    }
                                });
                            } catch (e) {
                                done(e);
                            }
                        });
                    } catch (e) {
                        done(e);
                    }
                });
            });
            
            it('[' + serie + '] ' + 'completeOrder fail credit reward', function (done) {
                var reqContent = {
                    articleId: DataIds.ARTICLE_ID,
                };
                var fakePay = global.wsHelper.sinonSandbox.stub(require('./../../factories/paymentApiFactory.js'), 'pay');
                fakePay.withArgs(sinon.match.any, sinon.match.any, Config.currencyType.money, sinon.match.any, sinon.match.any)
                        .yields(null, 1);
                fakePay.withArgs(sinon.match.any, sinon.match.any, Config.currencyType.bonus, sinon.match.any, sinon.match.any)
                        .yields(null, 1);
                fakePay.withArgs(sinon.match.any, sinon.match.any, Config.currencyType.credit, sinon.match.any, sinon.match.any)
                        .yields(Errors.FatalError);
                global.wsHelper.initProfiles();
                global.wsHelper.apiSecureCall(serie, 'shop/articleSetSelect', ProfileData.CLIENT_INFO, reqContent, function (err, message) {
                    try {
                        global.wsHelper.apiSecureCall(serie, 'shop/initiateOrder', ProfileData.CLIENT_INFO, {}, function (err, message) {
                            try {
                                global.wsHelper.apiSecureCall(serie, 'shop/completeOrder', ProfileData.CLIENT_INFO, {}, function (err, message) {
                                    try {
                                        should(err).not.Error;
                                        message.getError().should.have.property("message", Errors.NoReward);
                                        done();
                                    } catch (e) {
                                        done(e);
                                    }
                                });
                            } catch (e) {
                                done(e);
                            }
                        });
                    } catch (e) {
                        done(e);
                    }
                });
            });
             
            it('[' + serie + '] ' + 'completeOrder fail clearing shopping cart', function (done) {
                var reqContent = {
                    articleId: DataIds.ARTICLE_ID,
                };
                var fakeFinalize = global.wsHelper.sinonSandbox.stub(require('./../../factories/userProfileApiFactory.js'), 'clearSelectedArticleId');
                fakeFinalize.yields(Errors.ClearingShoppingCart);
                global.wsHelper.initProfiles();
                global.wsHelper.apiSecureCall(serie, 'shop/articleSetSelect', ProfileData.CLIENT_INFO, reqContent, function (err, message) {
                    try {
                        global.wsHelper.apiSecureCall(serie, 'shop/initiateOrder', ProfileData.CLIENT_INFO, {}, function (err, message) {
                            try {
                                global.wsHelper.apiSecureCall(serie, 'shop/completeOrder', ProfileData.CLIENT_INFO, {}, function (err, message) {
                                    try {
                                        should(err).not.Error;
                                        message.getError().should.have.property("message", Errors.ClearingShoppingCart);
                                        done();
                                    } catch (e) {
                                        done(e);
                                    }
                                });
                            } catch (e) {
                                done(e);
                            }
                        });
                    } catch (e) {
                        done(e);
                    }
                });
            });
             
            it('[' + serie + '] ' + 'completeOrder fail credit reward and clearing shopping cart', function (done) {
                var reqContent = {
                    articleId: DataIds.ARTICLE_ID,
                };
                var fakePay = global.wsHelper.sinonSandbox.stub(require('./../../factories/paymentApiFactory.js'), 'pay');
                fakePay.withArgs(sinon.match.any, sinon.match.any, Config.currencyType.money, sinon.match.any, sinon.match.any)
                        .yields(null, 1);
                fakePay.withArgs(sinon.match.any, sinon.match.any, Config.currencyType.bonus, sinon.match.any, sinon.match.any)
                        .yields(null, 1);
                fakePay.withArgs(sinon.match.any, sinon.match.any, Config.currencyType.credit, sinon.match.any, sinon.match.any)
                        .yields(Errors.FatalError);
                var fakeFinalize = global.wsHelper.sinonSandbox.stub(require('./../../factories/userProfileApiFactory.js'), 'clearSelectedArticleId');
                fakeFinalize.yields(Errors.FatalError);
                global.wsHelper.initProfiles();
                global.wsHelper.apiSecureCall(serie, 'shop/articleSetSelect', ProfileData.CLIENT_INFO, reqContent, function (err, message) {
                    try {
                        global.wsHelper.apiSecureCall(serie, 'shop/initiateOrder', ProfileData.CLIENT_INFO, {}, function (err, message) {
                            try {
                                global.wsHelper.apiSecureCall(serie, 'shop/completeOrder', ProfileData.CLIENT_INFO, {}, function (err, message) {
                                    try {
                                        should(err).not.Error;
                                        message.getError().should.have.property("message", Errors.NoRewardAndClearingShoppingCart);
                                        done();
                                    } catch (e) {
                                        done(e);
                                    }
                                });
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

        describe('[' + serie + '] ' + 'orderList', function () {
            
            it('[' + serie + '] ' + 'get first page orders', function (done) {
                var reqContent = {
                    limit: 10,
                    offset: 0,
                    orderBy: {
                        field: "finalizeDate",
                        direction: "asc"
                    },
                    searchBy: {
                        status: "1"
                    }
                };
                global.wsHelper.apiSecureCall(serie, 'shop/orderList', ProfileData.CLIENT_INFO, reqContent, function (err, message) {
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
            
            it('[' + serie + '] ' + 'get orders error mapping order field', function (done) {
                var reqContent = {
                    limit: 10,
                    offset: 0,
                    orderBy: {
                        field: "invalid",
                        direction: "asc"
                    },
                };
                global.wsHelper.apiSecureCall(serie, 'shop/orderList', ProfileData.CLIENT_INFO, reqContent, function (err, message) {
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
        
        describe('[' + serie + '] ' + 'finalizeOrder', function () {
            it('[' + serie + '] ' + 'finalizeOrder success', function (done) {
                var reqContent = {
                    id: DataIds.ORDER_ID,
                    reset: true //for testing purposes
                };
                global.wsHelper.initProfiles();
                //make sure order is pending
                global.wsHelper.apiSecureCall(serie, 'shop/finalizeOrder', ProfileData.CLIENT_INFO, reqContent, function (err, message) {
                    try {
                        //set order to finished
                        delete reqContent.reset;
                        global.wsHelper.apiSecureCall(serie, 'shop/finalizeOrder', ProfileData.CLIENT_INFO, reqContent, function (err, message) {
                            try {
                                var content = message.getContent();
                                should(err).not.Error;
                                should.not.exist(message.getError());
                                should.exist(content);
                                content.should.be.an.Object;
                                content.should.have.property("id");
                                content.id.should.exactly(reqContent.id);
                                content.should.have.property("finalizeDate");
                                Timestamp.isCloseFromNow(content.finalizeDate, 2/*minutes*/).should.be.true();
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
            
            it('[' + serie + '] ' + 'finalizeOrder fail already finalized', function (done) {
                var reqContent = {
                    id: DataIds.ORDER_ID,
                    reset: true //for testing purposes
                };
                global.wsHelper.initProfiles();
                //make sure order is pending
                global.wsHelper.apiSecureCall(serie, 'shop/finalizeOrder', ProfileData.CLIENT_INFO, reqContent, function (err, message) {
                    try {
                        //set order to finished
                        delete reqContent.reset;
                        global.wsHelper.apiSecureCall(serie, 'shop/finalizeOrder', ProfileData.CLIENT_INFO, reqContent, function (err, message) {
                            try {
                                //second set order to finished should throw error
                                global.wsHelper.apiSecureCall(serie, 'shop/finalizeOrder', ProfileData.CLIENT_INFO, reqContent, function (err, message) {
                                    try {
                                        should(err).not.Error;
                                        message.getError().should.have.property("message", Errors.AlreadyFinalized);
                                        done();
                                    } catch (e) {
                                        done(e);
                                    }
                                });
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