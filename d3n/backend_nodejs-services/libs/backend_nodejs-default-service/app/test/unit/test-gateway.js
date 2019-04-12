var ip = process.env.HOST || 'localhost',
    port = process.env.PORT ? parseInt(process.env.PORT) : 4000,
    cp = require('child_process'),
    fs = require('fs'),
    async = require('async'),
    should = require('should'),
    assert = require('chai').assert,
    Errors = require('nodejs-errors'),
    workerService = null,
    ServiceClient = require('nodejs-default-client'),
    Gateway = require('nodejs-gateway'),
    serviceClient = new ServiceClient({
        secure: true,
        service: {
            ip: ip,
            port: port
        },
        validateFullMessage: false
    }),
    ProtocolMessage = require('nodejs-protocol'),
    token = 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhIjoiYXNkIiwiYiI6MTIzLCJpYXQiOjE0NjU5MDQwMzB9.WGKKroIKmu4BZu_hWZc7AdUNj2ItUU1K-b7-OvC1AA4G6vE8nYkDpHoqdRazoRbPa6wXJY_WKEPpUrSNphBMDb3803vbKRloCr8WAt3ekrKeo1fiTiE1-AhGYAk3omu90sR4zrDiGxC2-8vlvnt5O7ZgjyEktIjNPmeW-hGrnrg_ST8903aLCGBq7pOxfiyDbtRalzoawiv_I1krQKYQ6FujZHM7y0486ZnJ8TnILTBuFn8YHEiVon63kUF6VzMclD_byS7d4vI0zybQnjjos_ueb8J_y8wvHn5Dm3y9MOrdNtda-OsTDG93jjDomWF-hr2HWS_3PHlM_gxKFlQxjA',
    instances = {
        'question': [
            {
                ip: ip,
                port: port+2,
                secure: true
            },
            {
                ip: ip,
                port: port+3,
                secure: true
            }
        ],
        'workorder': [
            {
                ip: ip,
                port: port+4,
                secure: true
            }
        ]
    },

    workers = []
;


//for(var i)

describe('Testing gateway communication', function() {

    it('should create service instances', function (done) {
        this.timeout(10000);

        var items = [];
        for(var k in instances) {
            for(var i=0; i<instances[k].length; i++) {
                var config = instances[k][i];
                config.serviceName = k;
                items.push(config);
            }
        }

        async.mapSeries(items, function (item, cb) {
            var args = {
                        secure: item.secure,
                        ip: item.ip,
                        port: item.port,
                        serviceName: 'test_service',
                        key: fs.readFileSync(__dirname + '/ssl-certificate/key.pem', 'utf8'),
                        cert: fs.readFileSync(__dirname + '/ssl-certificate/cert.pem', 'utf8'),
                        auth: {
                            algorithm: 'RS256',
                            publicKey: fs.readFileSync(__dirname + '/jwt-keys/pubkey.pem', 'utf8')
                        },
                        validateFullMessage: false,
                        validatePermissions: false
                    };

            var workerService = require(__dirname + '/worker-create-service.js')(args);

            workers.push(workerService);

            workerService.build(function (err) {
                if(err) {
                    return cb(new Error('error starting testing service'));
                }
                return cb(false);
            });

        }, function (err) {
            assert.ifError(err);
            done();
        });
    });

    it('should create gateway', function (done) {
        var args = {
                'gateway': {
                    secure: true,
                    ip: ip,
                    port: port,
                    key: __dirname + '/ssl-certificate/key.pem',
                    cert: __dirname + '/ssl-certificate/cert.pem',
                    auth: {
                        algorithm: 'RS256',
                        publicKey: fs.readFileSync(__dirname + '/jwt-keys/pubkey.pem', 'utf8'),
                        privateKey: fs.readFileSync(__dirname + '/jwt-keys/privkey.pem', 'utf8')
                    }
                },
                serviceInstances: instances
            };
        var worker = require(__dirname + '/worker-create-gateway.js')(args);

        workers.push(worker);

        worker.build(function (err) {
            if(err) {
                return done(new Error('error starting testing service'));
            }
            return done(false);
        });

    });

    it('should connect to gateway', function (done) {
        this.timeout(10000);
        serviceClient.connect(done);
    });


    it('should receive ping response success', function (done) {
        var reqMessage = new ProtocolMessage();
        var sequenceNumber = serviceClient.getSeq();
        reqMessage.setMessage('question/ping');
        reqMessage.setContent({data: 'for ping'});
        reqMessage.setSeq(sequenceNumber);
        reqMessage.setAck(null);
        reqMessage.setError(null);

        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            try {
                assert.ifError(err);
                assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');

                var now = new Date().getTime();
                assert.isTrue(Math.abs(resMessage.getTimestamp() - now) < 1000);
                resMessage.deleteTimestamp();
                assert.deepEqual(resMessage.getMessageContainer(),{ message: 'question/pingResponse', content: { evenMoreData: 'pong data' }, seq: null, ack: [ sequenceNumber ], error: null } );
                done();
            } catch (e) {
                done(e);
            }
        });
    });

    it('should receive ping response success with token (token should not be included in response)', function (done) {
        var reqMessage = new ProtocolMessage();
        var sequenceNumber = serviceClient.getSeq();
        reqMessage.setMessage('workorder/ping');
        reqMessage.setToken(token);
        reqMessage.setContent({data: 'for ping'});
        reqMessage.setSeq(sequenceNumber);
        reqMessage.setAck(null);
        reqMessage.setError(null);

        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            try {
                assert.ifError(err);
                assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');

                var now = new Date().getTime();
                assert.isTrue(Math.abs(resMessage.getTimestamp() - now) < 1000);
                resMessage.deleteTimestamp();
                assert.deepEqual(resMessage.getMessageContainer(),{ message: 'workorder/pingResponse', content: { evenMoreData: 'pong data' }, seq: null, ack: [ sequenceNumber ], error: null } );
                done();
            } catch (e) {
                done(e);
            }
        });
    });

    it('should receive invalid token', function (done) {
        var reqMessage = new ProtocolMessage();
        var sequenceNumber = serviceClient.getSeq();
        reqMessage.setMessage('question/ping');
        reqMessage.setToken(token + 'sadas');
        reqMessage.setContent({});
        reqMessage.setSeq(sequenceNumber);
        reqMessage.setAck(null);
        reqMessage.setError(null);

        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            try {
                assert.ifError(err);
                assert.strictEqual(resMessage.getMessage(), 'global/authResponse');
                assert.isNotNull(resMessage.getError());
                assert.strictEqual(resMessage.getError().message, Errors.ERR_TOKEN_NOT_VALID.message);

                var now = new Date().getTime();
                assert.isTrue(Math.abs(resMessage.getTimestamp() - now) < 1000);
                resMessage.deleteTimestamp();
                assert.deepEqual(resMessage.getMessageContainer(),{ message: 'global/authResponse', content: null, seq: null, ack: [ sequenceNumber ], error: { type: Errors.ERR_TOKEN_NOT_VALID.type, message: Errors.ERR_TOKEN_NOT_VALID.message } } );
                done();
            } catch (e) {
                done(e);
            }
        });
    });

    it('should receive invalid message', function (done) {
        var reqMessage = new ProtocolMessage();
        reqMessage.setMessage('question/ping');
        reqMessage.setToken(token);
        reqMessage.setContent({});
        reqMessage.setSeq('1');
        reqMessage.setAck(null);
        reqMessage.setError(null);

        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            //console.log('>>>>>', resMessage.getMessageContainer());
            try {
                assert.ifError(err);
                assert.isNotNull(resMessage.getError());
                assert.strictEqual(resMessage.getError().message, Errors.ERR_VALIDATION_FAILED.message);

                var now = new Date().getTime();
                assert.isTrue(Math.abs(resMessage.getTimestamp() - now) < 1000);
                resMessage.deleteTimestamp();
                assert.deepEqual(resMessage.getMessageContainer(),{ message: 'question/pingResponse', content: null, seq: null, ack: [ '1' ], error: { type: Errors.ERR_VALIDATION_FAILED.type, message: Errors.ERR_VALIDATION_FAILED.message } } );
                done();
            } catch (e) {
                done(e);
            }
        });
    });

    it('should receive message handler not found', function (done) {
        var reqMessage = new ProtocolMessage();
        var sequenceNumber = serviceClient.getSeq();
        reqMessage.setMessage('question/qqwwee');
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

                var now = new Date().getTime();
                assert.isTrue(Math.abs(resMessage.getTimestamp() - now) < 1000);
                resMessage.deleteTimestamp();
                assert.deepEqual(resMessage.getMessageContainer(),{ message: 'question/qqwweeResponse', content: null, seq: null, ack: [ sequenceNumber ], error: { type: Errors.ERR_FATAL_ERROR.type, message: Errors.ERR_FATAL_ERROR.message } } );
                done();
            } catch (e) {
                done(e);
            }
        });
    });


    it('should receive gateway service not found', function (done) {
        var reqMessage = new ProtocolMessage();
        var sequenceNumber = serviceClient.getSeq();
        reqMessage.setMessage('question_sdsdsfsd/ping');
        reqMessage.setToken(token);
        reqMessage.setContent({});
        reqMessage.setSeq(sequenceNumber);
        reqMessage.setAck(null);
        reqMessage.setError(null);

        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            try {
                assert.ifError(err);
                assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                assert.isNotNull(resMessage.getError());
                assert.strictEqual(resMessage.getError().message, Errors.ERR_SERVICE_NOT_FOUND.message);

                var now = new Date().getTime();
                assert.isTrue(Math.abs(resMessage.getTimestamp() - now) < 1000);
                resMessage.deleteTimestamp();
                resMessage.deleteClientId(); //We dont cate about the client ID
                assert.deepEqual(resMessage.getMessageContainer(),{ message: 'question_sdsdsfsd/pingResponse', content: {}, seq: null, ack: [ sequenceNumber ], error: { type: Errors.ERR_SERVICE_NOT_FOUND.type, message: Errors.ERR_SERVICE_NOT_FOUND.message } } );
                done();
            } catch (e) {
                done(e);
            }
        });
    });


    it('should send two concurrent messages', function (done) {

        var client = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: port
            },
            validateFullMessage: false
        });

        client.connect(function (err) {
            assert.ifError(err);
            var reqMessage1 = new ProtocolMessage();
            var reqMessage2 = new ProtocolMessage();
            var sequenceNumber = client.getSeq();
            var sequenceNumber2 = client.getSeq();
            reqMessage1.setMessage('question/ping');
            reqMessage1.setContent({data: 'for ping'});
            reqMessage1.setSeq(sequenceNumber);
            reqMessage1.setAck(null);
            reqMessage1.setError(null);

            reqMessage2.setMessage('workorder/ping');
            reqMessage2.setContent({data: 'for ping'});
            reqMessage2.setSeq(sequenceNumber2);
            reqMessage2.setAck(null);
            reqMessage2.setError(null);

            async.parallel([
                function (next) {
                    client.sendMessage(reqMessage1, next);
                },

                function (next) {
                    client.sendMessage(reqMessage2, next);
                }

            ], function (err, responses) {
                try {
                    var resMessage1 = responses[0];
                    var resMessage2 = responses[1];

                    assert.strictEqual(resMessage1.getMessage(), reqMessage1.getMessage() + 'Response');
                    assert.strictEqual(resMessage2.getMessage(), reqMessage2.getMessage() + 'Response');

                    assert.strictEqual(resMessage1.getError(), null);
                    assert.strictEqual(resMessage2.getError(), null);
                    assert.ifError(err);

                    var now = new Date().getTime();
                    assert.isTrue(Math.abs(resMessage1.getTimestamp() - now) < 1000);
                    resMessage1.deleteTimestamp();
                    assert.isTrue(Math.abs(resMessage2.getTimestamp() - now) < 1000);
                    resMessage2.deleteTimestamp();

                    assert.deepEqual(resMessage1.getMessageContainer(),{ message: 'question/pingResponse', content: { evenMoreData: 'pong data' }, seq: null, ack: [ sequenceNumber ], error: null } );
                    assert.deepEqual(resMessage2.getMessageContainer(),{ message: 'workorder/pingResponse', content: { evenMoreData: 'pong data' }, seq: null, ack: [ sequenceNumber2 ], error: null } );
                    done();
                } catch (e) {
                    done(e);
                }
            });

        });
    });

    it('disconnect and reconnect client - should reset client session', function (done) {
        async.series([
            function (next) {
                var reqMessage = new ProtocolMessage();
                var sequenceNumber = serviceClient.getSeq();
                reqMessage.setMessage('question/saveToStorage');
                reqMessage.setContent({key: 'test', value:[1,2,3]});
                reqMessage.setSeq(sequenceNumber);
                reqMessage.setAck(null);
                reqMessage.setError(null);

                serviceClient.sendMessage(reqMessage, function (err, resMessage) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                        assert.strictEqual(resMessage.getError(), null);

                        var now = new Date().getTime();
                        assert.isTrue(Math.abs(resMessage.getTimestamp() - now) < 1000);
                        resMessage.deleteTimestamp();
                        assert.deepEqual(resMessage.getMessageContainer(),{ message: 'question/saveToStorageResponse', content: {}, seq: null, ack: [ sequenceNumber ], error: null } );
                        next();
                    } catch (e) {
                        next(e);
                    }
                });
            },
            function (next) {
                var onDisconnectHandler = function(reasonCode, description) {
                    serviceClient.removeListener('connectionClose', onDisconnectHandler);
                    try {
                        assert.strictEqual(reasonCode, 1000);
                        assert.strictEqual(description, "testing disconnect");
                        next();

                    } catch (e) {
                        next(e);
                    }
                };
                serviceClient.on('connectionClose', onDisconnectHandler);
                serviceClient.disconnect(1000, "testing disconnect", true);
            },
            function (next) {
                serviceClient.connect(function (err) {
                    try {
                        assert.ifError(err);
                        next();
                    } catch (e) {
                        next(e);
                    }
                });
            },
            function (next) {
                var reqMessage = new ProtocolMessage();
                var sequenceNumber = serviceClient.getSeq();
                reqMessage.setMessage('question/loadFromStorage');
                reqMessage.setContent({key: 'test'});
                reqMessage.setSeq(sequenceNumber);
                reqMessage.setAck(null);
                reqMessage.setError(null);

                serviceClient.sendMessage(reqMessage, function (err, resMessage) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                        assert.strictEqual(resMessage.getError(), null);

                        var now = new Date().getTime();
                        assert.isTrue(Math.abs(resMessage.getTimestamp() - now) < 1000);
                        resMessage.deleteTimestamp();
                        assert.deepEqual(resMessage.getMessageContainer(),{ message: 'question/loadFromStorageResponse', content: {response: null}, seq: null, ack: [ sequenceNumber ], error: null } );
                        next();
                    } catch (e) {
                        next(e);
                    }
                });
            }
        ], function (err) {
             done(err);
        });
    });

    after(function() {
        try {
            for(var i=0; i<workers.length; i++) {
                workers[i].shutdown();
            }
        } catch (e) {
        }
    });

});
