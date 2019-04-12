var ip = process.env.HOST || 'localhost',
    port = process.env.PORT ? parseInt(process.env.PORT) : 4001,
    cp = require('child_process'),
    fs = require('fs'),
    async = require('async'),
    should = require('should'),
    assert = require('chai').assert,
    workerService = null,
    ServiceClient = require('nodejs-default-client'),
    serviceClient = new ServiceClient({
        secure: false,
        service: {
            ip: ip,
            port: port
        },
        validateFullMessage: false
    }),
    ProtocolMessage = require('nodejs-protocol'),
    token = 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhIjoiYXNkIiwiYiI6MTIzLCJpYXQiOjE0NjU5MDQwMzB9.WGKKroIKmu4BZu_hWZc7AdUNj2ItUU1K-b7-OvC1AA4G6vE8nYkDpHoqdRazoRbPa6wXJY_WKEPpUrSNphBMDb3803vbKRloCr8WAt3ekrKeo1fiTiE1-AhGYAk3omu90sR4zrDiGxC2-8vlvnt5O7ZgjyEktIjNPmeW-hGrnrg_ST8903aLCGBq7pOxfiyDbtRalzoawiv_I1krQKYQ6FujZHM7y0486ZnJ8TnILTBuFn8YHEiVon63kUF6VzMclD_byS7d4vI0zybQnjjos_ueb8J_y8wvHn5Dm3y9MOrdNtda-OsTDG93jjDomWF-hr2HWS_3PHlM_gxKFlQxjA',
    logger = require('nodejs-logger')(),
    Errors = require('nodejs-errors')
;

var clients = [];
var services = [];

describe('Testing direct communication unsecured channel', function() {
    before(function(done) {
        this.timeout(10000);
        var args = {
                secure: false,
                ip: ip,
                port: port,
                key: fs.readFileSync(__dirname + '/ssl-certificate/key.pem', 'utf8'),
                cert: fs.readFileSync(__dirname + '/ssl-certificate/cert.pem', 'utf8'),
                auth: {
                    algorithm: 'RS256',
                    publicKey: fs.readFileSync(__dirname + '/jwt-keys/pubkey.pem', 'utf8')
                },
                validateFullMessage: false,
                validatePermissions: false
            };

        workerService = require(__dirname + '/worker-create-service.js')(args);
        workerService.build(function (err) {
            if(err) {
                return done(err);
            }
            services.push(workerService);
            return done();
        });
    });

    it('should connect to service', function (done) {
        this.timeout(10000);
        clients.push(serviceClient);
        serviceClient.connect(done);
    });

    it('should receive ping response success', function (done) {
        var reqMessage = new ProtocolMessage();
        var sequenceNumber = serviceClient.getSeq();
        reqMessage.setMessage('test_service/pingCustom');
        reqMessage.setContent({data: 'for ping'});
        reqMessage.setSeq(sequenceNumber);
        reqMessage.setAck(null);
        reqMessage.setError(null);

        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            try {
                assert.ifError(err);
                assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                assert.strictEqual(resMessage.getError(), null);
                console.log(resMessage.getClientId());
                assert.deepEqual(resMessage.getMessageContainer(),{ message: 'test_service/pingCustomResponse', content: { evenMoreData: 'pong data' }, seq: null, ack: [ sequenceNumber ], error: null } );
                done();
            } catch (e) {
                done(e);
            }
        });
    });

    it('should receive ping response success with token (token should not be included in response)', function (done) {
        var reqMessage = new ProtocolMessage();
        var sequenceNumber = serviceClient.getSeq();
        reqMessage.setMessage('test_service/pingCustom');
        reqMessage.setToken(token);
        reqMessage.setContent({data: 'for ping'});
        reqMessage.setSeq(sequenceNumber);
        reqMessage.setAck(null);
        reqMessage.setError(null);

        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            try {
                assert.ifError(err);
                assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                assert.strictEqual(resMessage.getError(), null);
                assert.deepEqual(resMessage.getMessageContainer(),{ message: 'test_service/pingCustomResponse', content: { evenMoreData: 'pong data' }, seq: null, ack: [ sequenceNumber ], error: null } );
                done();
            } catch (e) {
                done(e);
            }
        });
    });



    it('should receive not allowed', function (done) {
        var reqMessage = new ProtocolMessage();
        reqMessage.setMessage('test_service/pingSecured');
        var sequenceNumber = serviceClient.getSeq();
        reqMessage.setSeq(sequenceNumber);

        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            try {
                assert.ifError(err);
                assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                assert.isNotNull(resMessage.getError());
                assert.strictEqual(resMessage.getError().message, 'ERR_NOT_ALLOWED');
                assert.deepEqual(resMessage.getMessageContainer(),{ message: 'test_service/pingSecuredResponse', content: null, seq: null, ack: [ sequenceNumber ], error: { type: ProtocolMessage.ErrorType.SERVER, message: 'ERR_NOT_ALLOWED' } } );
                done();
            } catch (e) {
                done(e);
            }
        });
    });


    it('should receive invalid message', function (done) {
        var reqMessage = new ProtocolMessage();
        reqMessage.setMessage('test_service/pingCustom');
        reqMessage.setToken(token);
        reqMessage.setContent({});
        reqMessage.setSeq({test: "test"});
        reqMessage.setAck(null);
        reqMessage.setError(null);

        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            //console.log('>>>>>', resMessage.getMessageContainer());
            try {
                assert.ifError(err);
                assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                assert.isNotNull(resMessage.getError());
                assert.strictEqual(resMessage.getError().message, Errors.ERR_VALIDATION_FAILED.message);
                assert.deepEqual(resMessage.getMessageContainer(),{ message: 'test_service/pingCustomResponse', content: null, seq: null, ack: [ {test: "test"} ], error: { type: Errors.ERR_VALIDATION_FAILED.type, message: Errors.ERR_VALIDATION_FAILED.message } } );
                done();
            } catch (e) {
                done(e);
            }
        });
    });


    it('should receive message handler not found', function (done) {
        var reqMessage = new ProtocolMessage();
        var sequenceNumber = serviceClient.getSeq();
        reqMessage.setMessage('test_service/qqwwee');
        reqMessage.setContent({});
        reqMessage.setSeq(sequenceNumber);
        reqMessage.setAck(null);
        reqMessage.setError(null);

        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            try {
                assert.ifError(err);
                assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                assert.isNotNull(resMessage.getError());
                assert.strictEqual(resMessage.getError().message, Errors.ERR_FATAL_ERROR.message);
                assert.deepEqual(resMessage.getMessageContainer(),{ message: 'test_service/qqwweeResponse', content: null, seq: null, ack: [ sequenceNumber ], error: { type: Errors.ERR_FATAL_ERROR.type, message: Errors.ERR_FATAL_ERROR.message } } );
                done();
            } catch (e) {
                done(e);
            }
        });
    });

    it('should send two concurrent messages', function (done) {
        var reqMessage1 = new ProtocolMessage();
        var reqMessage2 = new ProtocolMessage();
        var sequenceNumber = serviceClient.getSeq();
        reqMessage1.setMessage('test_service/pingCustom');
        reqMessage1.setContent({data: 'for ping'});
        reqMessage1.setSeq(sequenceNumber);
        reqMessage1.setAck(null);
        reqMessage1.setError(null);

        var sequenceNumber2 = serviceClient.getSeq();
        reqMessage2.setMessage('test_service/ping');
        reqMessage2.setContent({data: 'for ping'});
        reqMessage2.setSeq(sequenceNumber2);
        reqMessage2.setAck(null);
        reqMessage2.setError(null);

        async.parallel([
            function (next) {
                serviceClient.sendMessage(reqMessage1, next);
            },

            function (next) {
                serviceClient.sendMessage(reqMessage2, next);
            }

        ], function (err, responses) {
            try {
                var resMessage1 = responses[0];
                var resMessage2 = responses[1];

                assert.strictEqual(resMessage1.getMessage(), reqMessage1.getMessage() + 'Response');
                assert.strictEqual(resMessage2.getMessage(), reqMessage2.getMessage() + 'Response');

                assert.strictEqual(resMessage1.getError(), null);
                assert.strictEqual(resMessage2.getError(), null);
                //console.log('responses:', responses);
                assert.ifError(err);

                assert.deepEqual(resMessage1.getMessageContainer(),{ message: 'test_service/pingCustomResponse', content: { evenMoreData: 'pong data' }, seq: null, ack: [ sequenceNumber ], error: null } );
                assert.deepEqual(resMessage2.getMessageContainer(),{ message: 'test_service/pingResponse', content: { evenMoreData: 'pong data' }, seq: null, ack: [ sequenceNumber2 ], error: null } );
                done();
            } catch (e) {
                done(e);
            }
        });
    });


    after(function(done) {
        for(var i=0; i<clients.length; i++) {
            try {
                clients[i].disconnect(1000, '', true);
            } catch (e) {}
        }
        for(var i=0; i<services.length; i++) {
            try {
                services[i].shutdown(false);
            } catch (e) {}
        }

        setTimeout(done, 500);
    });

});
