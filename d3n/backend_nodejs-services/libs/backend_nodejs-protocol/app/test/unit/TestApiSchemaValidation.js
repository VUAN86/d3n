var assert = require('chai').assert;
var _ = require('lodash');
var ProtocolMessage = require('../../classes/ProtocolMessage.js');
var fs = require('fs');


var validReqMessages = [
    {
        message: 'question/languageGet',
        seq: parseInt(_.uniqueId()),
        ack:null,
        content: {
            id: 123
        }
    },
    
    {
        message: 'question/languageGet',
        seq: parseInt(_.uniqueId()),
        ack: null,
        content: {
            id: 123
        }
    }
];
var validResMessages = [
    {
        message: 'question/languageGetResponse',
        ack: [123],
        content: {
            language: {
                id: 11,
                name: 'lang name'
            }
        }
    },
    
    {
        message: 'question/languageGetResponse',
        ack: [22],
        content: {
            language: {
                id: 11,
                name: 'lang name'
            }
        }
    }
];

var invalidReqMessages = [
    {
        message: 'question/languageGet',
        seq: parseInt(_.uniqueId()),
        content: {
            id: 0
        }
    },
    
    {
        message: 'question/languageGet',
        seq: null,
        content: null
    }
];

var invalidResMessages = [
    {
        message: 'question/languageGetResponse',
        seq: null,
        ack: [123],
        content: true
    },
    
    {
        message: 'question/languageGetResponse',
        seq: null,
        ack: [123],
        content: false
    }
];

describe('Testing API schema validation', function() {
    it('valid request messages', function () {
        for(var i=0; i<validReqMessages.length; i++) {
            var m = new ProtocolMessage(validReqMessages[i]);
            var valid = m.isValid(m.getMessage());
            if (valid !== true) {
                console.log('lastValidationError=', m.lastValidationError);
            }
            assert.strictEqual(valid, true);
        }
    }); 
    
    it('valid response messages', function () {
        for(var i=0; i<validResMessages.length; i++) {
            var m = new ProtocolMessage(validReqMessages[i]);
            var valid = m.isValid(m.getMessage());
            if (valid !== true) {
                console.log('lastValidationError=', m.lastValidationError);
            }
            assert.strictEqual(valid, true);
        }
    }); 
    
    it('invalid request messages', function () {
        for(var i=0; i<invalidReqMessages.length; i++) {
            var m = new ProtocolMessage(invalidReqMessages[i]);
            assert.strictEqual(m.isValid(m.getMessage()), false);
        }
    }); 
    it('invalid response messages', function () {
        for(var i=0; i<invalidResMessages.length; i++) {
            var m = new ProtocolMessage(invalidResMessages[i]);
            assert.strictEqual(m.isValid(m.getMessage()), false);
        }
    }); 
});