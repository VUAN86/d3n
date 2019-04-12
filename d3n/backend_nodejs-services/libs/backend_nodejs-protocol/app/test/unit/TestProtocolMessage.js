var should = require('should');
var assert = require('chai').assert;
var ProtocolMessage = require('../../classes/ProtocolMessage.js');
var fs = require('fs');
var Errors = require('nodejs-errors');

describe('Testing protocol message container', function() {
    
    
    it('should setMessage() and getMessage() success', function (){
        var message = new ProtocolMessage();
        
        should.strictEqual(message.getMessage(), null);
        message.setMessage('test/ping');
        should.strictEqual(message.getMessage(), 'test/ping');
    });
    
    it('should setContent() and getContent() success', function (){
        var message = new ProtocolMessage();
        
        should.strictEqual(message.getContent(), null);
        message.setContent('content');
        should.strictEqual(message.getContent(), 'content');
    });
    
    it('should setToken() and getToken() success', function (){
        var message = new ProtocolMessage();
        
        should.strictEqual(message.getToken(), undefined);
        message.setToken('token');
        should.strictEqual(message.getToken(), 'token');
    });
    
    it('should setTimestamp() and getTimestamp() success', function (){
        var message = new ProtocolMessage();
        
        should.strictEqual(message.getTimestamp(), undefined);
        message.setTimestamp(1469187124000);
        should.strictEqual(message.getTimestamp(), 1469187124000);
    });
    
    it('should setSeq() and getSeq() success', function (){
        var message = new ProtocolMessage();
        
        should.strictEqual(message.getSeq(), null);
        message.setSeq(11);
        should.strictEqual(message.getSeq(), 11);
    });
    
    it('should setAck() and getAck() success', function (){
        var message = new ProtocolMessage();
        
        should.strictEqual(message.getAck(), null);
        
        var ack = [11];
        message.setAck(ack);
        should.strictEqual(message.getAck(), ack);
        should.strictEqual(message.getAck()[0], ack[0]);
    });

    it('should setError() and getError() success', function (){
        var message = new ProtocolMessage();

        should.strictEqual(message.getError(), null);

        var error = {
            type: ProtocolMessage.ErrorType.CLIENT,
            message: 'ERR_TEST_PROTOCOL'
        };
        message.setError(error);
        should.strictEqual(message.getError(), error);
        should.strictEqual(message.getError().code, undefined);
        should.strictEqual(message.getError().type, error.type);
        should.strictEqual(message.getError().message, error.message);
    });

    it('should setError() and getError() protocol <0.4 success', function (){
        var message = new ProtocolMessage();

        should.strictEqual(message.getError(), null);

        var error = {
            code: 1,
            message: 'ERR_TEST_PROTOCOL'
        };
        message.setError(error);
        should.strictEqual(message.getError().code, undefined);
        should.strictEqual(message.getError().type, ProtocolMessage.ErrorType.SERVER);
        should.strictEqual(message.getError().message, error.message);
    });
    
    
    it('should create instance with null values', function (){
        var message = new ProtocolMessage();
        should.strictEqual(message.getMessage(), null);
        should.strictEqual(message.getContent(), null);
        should.strictEqual(message.getSeq(), null);
        should.strictEqual(message.getAck(), null);
        should.strictEqual(message.getError(), null);
    });
    
    it('should create instance out of a request message', function (){
        var reqMessage = new ProtocolMessage();
        reqMessage.setMessage('test/ping');
        reqMessage.setContent('req message content');
        reqMessage.setSeq(1);
        reqMessage.setAck(null);
        reqMessage.setError(null);
        
        var resMessage = new ProtocolMessage(reqMessage);
        
        should.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
        should.strictEqual(resMessage.getContent(), reqMessage.getContent());
        should.strictEqual(resMessage.getSeq(), null);
        should.strictEqual(resMessage.getAck()[0], reqMessage.getSeq());
        should.strictEqual(resMessage.getError(), reqMessage.getError());
    });
    
    it('should create instance out of a plain object', function (){
        var plainObject = {
            message: 'test/ping',
            content: 'content',
            seq: 1,
            ack: [1],
            error: {
                code: 1,
                message: 'ERR_TEST'
            }
        };
        var message = new ProtocolMessage(plainObject);
        
        should.strictEqual(message.getMessage(), plainObject.message);
        should.strictEqual(message.getContent(), plainObject.content);
        should.strictEqual(message.getSeq(), plainObject.seq);
        should.strictEqual(message.getAck()[0], plainObject.ack[0]);
        should.strictEqual(message.getError().code, plainObject.error.code);
        should.strictEqual(message.getError().message, plainObject.error.message);
    });
    
    it('is instance of ProtocolMessage', function (){
        var message = new ProtocolMessage();
        should.strictEqual(ProtocolMessage.isInstance(message), true);
    });
    
    it('is not instance of ProtocolMessage', function (){
        var message = {};
        should.strictEqual(ProtocolMessage.isInstance(message), false);
    });
    
    it('setError replace code with type correct', function (){
        var m1 = new ProtocolMessage();
        var err1 = {
            code: 0,
            message: 'message1'
        };
        
        m1.setError(err1);
        
        var m2 = new ProtocolMessage();
        var err2 = {
            code: 5,
            message: 'message2'
        };
        m2.setError(err2);

        var m3 = new ProtocolMessage();
        var err3 = {
            code: false,
            message: 'message3'
        };
        m3.setError(err3);
        
        
        assert.strictEqual(m1.getError().type, ProtocolMessage.ErrorType.SERVER);
        assert.strictEqual(m1.getError().message, err1.message);
        
        assert.strictEqual(m2.getError().type, ProtocolMessage.ErrorType.SERVER);
        assert.strictEqual(m2.getError().message, err2.message);
        
        assert.strictEqual(m3.getError().type, ProtocolMessage.ErrorType.SERVER);
        assert.strictEqual(m3.getError().message, err3.message);
    });
    
    it('setError , if error is string look up in Errors object', function (){
        var m1 = new ProtocolMessage();
        var err1 = 'not_a_key';
        m1.setError(err1);
        
        assert.strictEqual(m1.getError().type, ProtocolMessage.ErrorType.SERVER);
        assert.strictEqual(m1.getError().message, err1);
        
        var m2 = new ProtocolMessage();
        var err2 = 'ERR_TOKEN_NOT_VALID';
        m2.setError(err2);
        
        assert.strictEqual(m2.getError().type, Errors[err2].type);
        assert.strictEqual(m2.getError().message, Errors[err2].message);
    });
});