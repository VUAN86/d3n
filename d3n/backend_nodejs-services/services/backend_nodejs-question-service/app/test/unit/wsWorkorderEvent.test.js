var _ = require('lodash');
var fs = require('fs');
var should = require('should');
var assert = require('assert');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var Database = require('nodejs-database').getInstance(Config);
var DataIds = require('./../config/_id.data.js');
var ProfileData = require('./../config/profile.data.js');
var WorkorderData = require('./../config/workorder.data.js');
var TestConfig = require('../config/test.config.js');
var ProtocolMessage = require('nodejs-protocol');

describe('Test workorder activate send message', function () {
    this.timeout(10000);
    global.wsHelper.series().forEach(function (serie) {
        it('[' + serie + '] ' + ' activate workorder, send emails', function (done) {
            var reqContent = { id: DataIds.WWMSG.WORKORDER.ID1 };
            var sendEmailFile = TestConfig.sendEmailFile;
            fs.writeFileSync(sendEmailFile, '', 'utf8');
            
            global.wsHelper.apiSecureCall(serie, 'workorder/workorderActivate', ProfileData.CLIENT_INFO_1, reqContent, function(err, message) {
                try {
                    assert.ifError(err);
                    assert.strictEqual(message.getError(), null);
                    setTimeout(function () {
                        var data = JSON.parse(fs.readFileSync(sendEmailFile, 'utf8'));
                        
                        var msgContainer = data[0];
                        
                        var pm = new ProtocolMessage(msgContainer);
                        
                        var actual = {
                            message: pm.getMessage(),
                            error: pm.getError()
                        };
                        
                        var expected = {
                            message: 'userMessage/sendEmailResponse',
                            error: null
                        };
                        
                        assert.deepStrictEqual(actual, expected);
                        done();
                    }, 2500);
                } catch (e) {
                    done(e);
                }
            });
        });
        
        it('[' + serie + '] ' + ' activate workorder, not send emails (no resource)', function (done) {
            var reqContent = { id: DataIds.WWMSG.WORKORDER.ID_NO_RESOURCE };
            var sendEmailFile = TestConfig.sendEmailFile;
            fs.writeFileSync(sendEmailFile, '', 'utf8');
            
            global.wsHelper.apiSecureCall(serie, 'workorder/workorderActivate', ProfileData.CLIENT_INFO_1, reqContent, function(err, message) {
                try {
                    //console.log('message:', message);
                    assert.ifError(err);
                    assert.strictEqual(message.getError(), null);
                    setTimeout(function () {
                        var data = fs.readFileSync(sendEmailFile, 'utf8');
                        
                        assert.strictEqual(data, '');
                        
                        done();
                    }, 2500);
                } catch (e) {
                    done(e);
                }
            });
        });
        
        
        it('[' + serie + '] ' + ' activate workorder,send 2 emails', function (done) {
            var reqContent = { id: DataIds.WWMSG.WORKORDER.ID_2_RESOURCES };
            var sendEmailFile = TestConfig.sendEmailFile;
            fs.writeFileSync(sendEmailFile, '', 'utf8');
            
            global.wsHelper.apiSecureCall(serie, 'workorder/workorderActivate', ProfileData.CLIENT_INFO_1, reqContent, function(err, message) {
                try {
                    
                    
                    assert.ifError(err);
                    assert.strictEqual(message.getError(), null);
                    setTimeout(function () {
                        var data = JSON.parse(fs.readFileSync(sendEmailFile, 'utf8'));
                        var len = 2;
                        assert.strictEqual(data.length, len);
                        for(var i=0; i<len; i++) {
                            var msgContainer = data[i];
                            var pm = new ProtocolMessage(msgContainer);

                            var actual = {
                                message: pm.getMessage(),
                                error: pm.getError()
                            };

                            var expected = {
                                message: 'userMessage/sendEmailResponse',
                                error: null
                            };

                            assert.deepStrictEqual(actual, expected);
                        }
                        done();
                    }, 2500);
                } catch (e) {
                    done(e);
                }
            });
        });
    });
});