var _ = require('lodash');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var ProfileData = require('./../config/profile.data.js');

describe('WS ShopCategory API', function () {
    this.timeout(10000);
    global.wsHelper.series().forEach(function (serie) {
        
        describe('[' + serie + '] ' + 'categoryList', function () {
            
            it('[' + serie + '] ' + 'get first page categories', function (done) {
                global.wsHelper.apiSecureCall(serie, 'shop/categoryList', ProfileData.CLIENT_INFO, null, function (err, message) {
                    try {
                        var content = message.getContent();
                        should(err).not.Error;
                        should.not.exist(message.getError());
                        should.exist(content);
                        content.should.have.property("total");
                        content.should.have.property("items");
                        done();
                    } catch (e) {
                        done(e);
                    }
                });
            });
        });
    });
});