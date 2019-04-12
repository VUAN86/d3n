var async = require('async');
var _ = require('lodash');
var nodeUrl = require('url');
var DefaultClient = require('../../classes/Client');
var logger = require('nodejs-logger')();
var fs = require('fs');
var assert = require('chai').assert;
var ProtocolMessage = require('nodejs-protocol');
var Errors = require('nodejs-errors');


describe('Tests message validation', function() {
    this.timeout(10000);
    
    before(function (done) {
        setTimeout(done, 300);
    });
    
    it('valid message', function (done) {
        var client = new DefaultClient({
            protocolLogging: false
        });
        var m = new ProtocolMessage();
        m.setMessage('question/languageGetResponse');
        m.setSeq(12);
        m.setAck([12]);
        m.setContent({
            id: 1
        });
        
        client.on('message', function (message) {
            try {
                assert.strictEqual(message.getMessage(), m.getMessage());
                assert.isNull(message.getError());
                return done();
            } catch (e) {
                return done(e);
            }
        });
        
        var str = {
            utf8Data: JSON.stringify(m.getMessageContainer())
        };
        client._onmessage(str);
    });
    
    it('invalid message', function (done) {
        var client = new DefaultClient({
            protocolLogging: false
        });
        var m = new ProtocolMessage();
        m.setMessage('question/languageGetResponse');
        m.setSeq(12);
        m.setAck([12]);
        m.setContent(false);
        
        client.on('message', function (message) {
            try {
                assert.strictEqual(message.getMessage(), m.getMessage());
                assert.isNotNull(message.getError());
                assert.strictEqual(message.getError().type, Errors.ERR_VALIDATION_FAILED.type);
                assert.strictEqual(message.getError().message, Errors.ERR_VALIDATION_FAILED.message);
                return done();
            } catch (e) {
                return done(e);
            }
        });
        
        var str = {
            utf8Data: JSON.stringify(m.getMessageContainer())
        };
        client._onmessage(str);
    });
});

function _sendMessageResponseSuccess (args, cb) {
    try {
        var message = args.message;
        var client = args.client;
        
        client.sendMessage(message, function (err, response) {
            try {
                if (message.getSeq() === null) {
                    assert.ifError(err);
                    return cb(false, response);
                } else {
                    assert.ifError(err);
                    assert.isNull(response.getError());
                    assert.isNotNull(response.getAck());
                    assert.strictEqual(response.getAck()[0], message.getSeq());
                    assert.strictEqual(response.getMessage(), message.getMessage() + 'Response');
                    return cb(false, response);
                }
            } catch (e) {
                return cb(e);
            }
        });
    } catch (e) {
        return setImmediate(cb, e);
    }
};

function _sendMessageResponseError (args, cb) {
    try {
        var message = args.message;
        var client = args.client;
        var errorMessage = args.errorMessage;
        
        client.sendMessage(message, function (err, response) {
            try {
                assert.ifError(err);
                assert.isNotNull(response.getError());
                assert.isNotNull(response.getAck());
                assert.strictEqual(response.getAck()[0], message.getSeq());
                assert.strictEqual(response.getError().message, errorMessage);
                return cb(false, response);
            } catch (e) {
                return cb(e);
            }
        });
    } catch (e) {
        return setImmediate(cb, e);
    }
};

function _createInstance(config, cb) {
    var cls = config.class;
    var inst = new cls(config);
    inst.build(function (err) {
        cb(err, inst);
    });
};
