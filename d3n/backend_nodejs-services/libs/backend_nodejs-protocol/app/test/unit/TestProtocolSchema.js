var assert = require('chai').assert,
    should = require('should'),
    _ = require('lodash'),
    ProtocolMessage = require('../../classes/ProtocolMessage.js'),
    fs = require('fs'),
    
    test_validMessages = [
        {
            message: 'question/getQuestions',
            content: {
                metas: 'metas value',
                limit: 25
            },
            token: 'token value',
            timestamp: 1234,
            seq: 1,
            ack: [1],
            error: {
                code: 2,
                message: 'asdas'
            }
        },
        
        // empty content
        {
            message: 'question/getQuestions',
            content: {},
            token: 'token value',
            timestamp: 1234,
            seq: 1,
            ack: [1],
            error: {
                code: 2,
                message: 'asdas'
            }
        },
        // null content
        {
            message: 'question/getQuestions',
            content: null,
            token: 'token value',
            timestamp: 1234,
            seq: 1,
            ack: [1],
            error: {
                code: 2,
                message: 'asdas'
            }
        },
        // no token
        {
            message: 'question/getQuestions',
            content: {},
            timestamp: 1234,
            seq: 1,
            ack: [1],
            error: {
                code: 2,
                message: 'asdas'
            }
        },
        
        // no timestamp
        {
            message: 'question/getQuestions',
            content: {},
            seq: 1,
            ack: [1],
            error: {
                code: 2,
                message: 'asdas'
            }
        },
        // null seq
        {
            message: 'question/getQuestions',
            content: {
                metas: 'metas value',
                limit: 25
            },
            token: 'token value',
            timestamp: 1234,
            seq: null,
            ack: [1],
            error: null
        },
        
        // null ack
        {
            message: 'question/getQuestions',
            content: {},
            token: 'token value',
            timestamp: 1234,
            seq: 1,
            ack: null,
            error: {
                code: 2,
                message: 'asdas'
            }
        },
        
        // null error
        {
            message: 'question/getQuestions',
            content: {},
            token: 'token value',
            timestamp: 1234,
            seq: 1,
            ack: null,
            error: null
        }
    ],

    test_invalidMessagesResponses = [
        { validation: { message: { type: 'string' } } },
        { validation: { message: { type: 'string' } } },
        { validation: { timestamp: { type: 'integer' } } },
        { validation: { seq: { type: [ 'integer', 'null' ] }, timestamp: { type: 'integer' } } },
        { validation: { ack: { type: [ 'null', 'array' ] }, seq: { type: [ 'integer', 'null' ] }, timestamp: { type: 'integer' } } }
    ];
    test_invalidMessages = [
        // null message
        {
            message: null,
            content: {
                metas: 'metas value',
                limit: 25
            },
            token: 'token value',
            timestamp: 1234,
            seq: 1,
            ack: [1],
            error: {
                code: 2,
                message: 'asdas'
            }
        },
        
        // missing message
        {
            content: {
                metas: 'metas value',
                limit: 25
            },
            token: 'token value',
            timestamp: 1234,
            seq: 1,
            ack: [1],
            error: {
                code: 2,
                message: 'asdas'
            }
        },
        /*
        // missing content
        {
            message: 'sdasda/asda',
            token: 'token value',
            timestamp: 1234,
            seq: 1,
            ack: [1],
            error: {
                code: 2,
                message: 'asdas'
            }
        },
        // missing seq
        {
            message: 'asdas/erw',
            content: {
                metas: 'metas value',
                limit: 25
            },
            token: 'token value',
            timestamp: 1234,
            ack: [1],
            error: {
                code: 2,
                message: 'asdas'
            }
        },
        
        // missing ack
        {
            message: 'asdas/erw',
            content: {
                metas: 'metas value',
                limit: 25
            },
            token: 'token value',
            timestamp: 1234,
            seq: 1,
            error: {
                code: 2,
                message: 'asdas'
            }
        },*/
        // timestamp not integer
        {
            message: 'asdas/erw',
            content: {
                metas: 'metas value',
                limit: 25
            },
            token: 'token value',
            timestamp: '1234',
            seq: 1,
            ack: [1],
            error: {
                code: 2,
                message: 'asdas'
            }
        },
        // seq not integer
        {
            message: 'asdas/erw',
            content: {
                metas: 'metas value',
                limit: 25
            },
            token: 'token value',
            timestamp: '1234',
            seq: '1',
            ack: [1],
            error: {
                code: 2,
                message: 'asdas'
            }
        },
        
        // ack not array
        {
            message: 'asdas/erw',
            content: {
                metas: 'metas value',
                limit: 25
            },
            token: 'token value',
            timestamp: '1234',
            seq: '1',
            ack: {},
            error: {
                code: 2,
                message: 'asdas'
            }
        }
        
        
    ]
            
;

describe('Testing protocol schema', function() {
    it('should be valid messages', function () {
        
        for(var i=0; i<test_validMessages.length; i++) {
            var message = new ProtocolMessage();
            var data = test_validMessages[i];
            
            if( !_.isUndefined(data.message) ) {
                message.setMessage(data.message);
            }
            
            if( !_.isUndefined(data.content) ) {
                message.setContent(data.content);
            }
            
            if( !_.isUndefined(data.token) ) {
                message.setToken(data.token);
            }
            
            if( !_.isUndefined(data.timestamp) ) {
                message.setTimestamp(data.timestamp);
            }
            
            if( !_.isUndefined(data.seq) ) {
                message.setSeq(data.seq);
            }
            
            if( !_.isUndefined(data.ack) ) {
                message.setAck(data.ack);
            }
            
            if( !_.isUndefined(data.error) ) {
                message.setError(data.error);
            }
            
            var res = message.isValid('websocketMessage');
            should.strictEqual(res, true);
        }
        
    }); 
    it('should be invalid schema', function() {
        var message = new ProtocolMessage();
        var data = test_validMessages[0];

        if( !_.isUndefined(data.message) ) {
            message.setMessage(data.message);
        }

        if( !_.isUndefined(data.content) ) {
            message.setContent(data.content);
        }

        if( !_.isUndefined(data.token) ) {
            message.setToken(data.token);
        }

        if( !_.isUndefined(data.timestamp) ) {
            message.setTimestamp(data.timestamp);
        }

        if( !_.isUndefined(data.seq) ) {
            message.setSeq(data.seq);
        }

        if( !_.isUndefined(data.ack) ) {
            message.setAck(data.ack);
        }

        if( !_.isUndefined(data.error) ) {
            message.setError(data.error);
        }

        var res = message.isValid('websocketMessageNONEXISTING');
        should.notEqual(res, true);

        var lastKnownError = message.getLastError();
        should.deepEqual(lastKnownError, { schema: "unknown" });
    });
    it('should be invalid messages', function () {
        for(var i=0; i<test_invalidMessages.length; i++) {
            var message = new ProtocolMessage();
            var data = test_invalidMessages[i];
            
            if( !_.isUndefined(data.message) ) {
                message.setMessage(data.message);
            }
            
            if( !_.isUndefined(data.content) ) {
                message.setContent(data.content);
            }
            
            if( !_.isUndefined(data.token) ) {
                message.setToken(data.token);
            }
            
            if( !_.isUndefined(data.timestamp) ) {
                message.setTimestamp(data.timestamp);
            }
            
            if( !_.isUndefined(data.seq) ) {
                message.setSeq(data.seq);
            }
            
            if( !_.isUndefined(data.ack) ) {
                message.setAck(data.ack);
            }
            
            if( !_.isUndefined(data.error) ) {
                message.setError(data.error);
            }

            var res = message.isValid('websocketMessage');
            should.notEqual(res, true);

            var lastKnownError = message.getLastError();
            should.deepEqual(lastKnownError, test_invalidMessagesResponses[i]);
        }
    }); 
    
    
});