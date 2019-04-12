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
        secure: true,
        service: {
            ip: ip,
            port: port
        },
        validateFullMessage: false
    }),
    serviceClient1 = null,
    serviceClient2 = null,
    ProtocolMessage = require('nodejs-protocol'),
    token = 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhIjoiYXNkIiwiYiI6MTIzLCJpYXQiOjE0NjU5MDQwMzB9.WGKKroIKmu4BZu_hWZc7AdUNj2ItUU1K-b7-OvC1AA4G6vE8nYkDpHoqdRazoRbPa6wXJY_WKEPpUrSNphBMDb3803vbKRloCr8WAt3ekrKeo1fiTiE1-AhGYAk3omu90sR4zrDiGxC2-8vlvnt5O7ZgjyEktIjNPmeW-hGrnrg_ST8903aLCGBq7pOxfiyDbtRalzoawiv_I1krQKYQ6FujZHM7y0486ZnJ8TnILTBuFn8YHEiVon63kUF6VzMclD_byS7d4vI0zybQnjjos_ueb8J_y8wvHn5Dm3y9MOrdNtda-OsTDG93jjDomWF-hr2HWS_3PHlM_gxKFlQxjA',
    logger = require('nodejs-logger')()
;

var services = [];
var clients = [];

describe('Testing broadcast subsystem', function() {
    before(function(done) {
        this.timeout(10000);
        var args = {
                 secure: true,
                 ip: ip,
                 port: port,
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

    it('adding user to a room', function (done) {
        var reqMessage = new ProtocolMessage();
        var sequenceNumber = serviceClient.getSeq();
        reqMessage.setMessage('test_service/enterRoom');
        reqMessage.setContent({room: 'myLittleTest'});
        reqMessage.setSeq(sequenceNumber);
        reqMessage.setAck(null);
        reqMessage.setError(null);

        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            try {
                assert.ifError(err);
                assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                assert.strictEqual(resMessage.getError(), null);
                assert.deepEqual(resMessage.getMessageContainer(),{ message: 'test_service/enterRoomResponse', content: { "response": true }, seq: null, ack: [ sequenceNumber ], error: null } );
                done();
            } catch (e) {
                done(e);
            }
        });
    });

    it('user can not be added twice', function (done) {
        var reqMessage = new ProtocolMessage();
        var sequenceNumber = serviceClient.getSeq();
        reqMessage.setMessage('test_service/enterRoom');
        reqMessage.setContent({room: 'myLittleTest'});
        reqMessage.setSeq(sequenceNumber);
        reqMessage.setAck(null);
        reqMessage.setError(null);

        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            try {
                assert.ifError(err);
                assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                assert.strictEqual(resMessage.getError(), null);
                assert.deepEqual(resMessage.getMessageContainer(),{ message: 'test_service/enterRoomResponse', content: { "response": false }, seq: null, ack: [ sequenceNumber ], error: null } );
                done();
            } catch (e) {
                done(e);
            }
        });
    });

    it('validate user is in room', function (done) {
        var reqMessage = new ProtocolMessage();
        var sequenceNumber = serviceClient.getSeq();
        reqMessage.setMessage('test_service/isInRoom');
        reqMessage.setContent({room: 'myLittleTest'});
        reqMessage.setSeq(sequenceNumber);
        reqMessage.setAck(null);
        reqMessage.setError(null);

        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            try {
                assert.ifError(err);
                assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                assert.strictEqual(resMessage.getError(), null);
                assert.deepEqual(resMessage.getMessageContainer(),{ message: 'test_service/isInRoomResponse', content: { "response": true }, seq: null, ack: [ sequenceNumber ], error: null } );
                done();
            } catch (e) {
                done(e);
            }
        });
    });

    it('leave room', function (done) {
        var reqMessage = new ProtocolMessage();
        var sequenceNumber = serviceClient.getSeq();
        reqMessage.setMessage('test_service/leaveRoom');
        reqMessage.setContent({room: 'myLittleTest'});
        reqMessage.setSeq(sequenceNumber);
        reqMessage.setAck(null);
        reqMessage.setError(null);

        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            try {
                assert.ifError(err);
                assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                assert.strictEqual(resMessage.getError(), null);
                assert.deepEqual(resMessage.getMessageContainer(),{ message: 'test_service/leaveRoomResponse', content: {}, seq: null, ack: [ sequenceNumber ], error: null } );
                done();
            } catch (e) {
                done(e);
            }
        });
    });

    it('validate user is not in room', function (done) {
        var reqMessage = new ProtocolMessage();
        var sequenceNumber = serviceClient.getSeq();
        reqMessage.setMessage('test_service/isInRoom');
        reqMessage.setContent({room: 'myLittleTest'});
        reqMessage.setSeq(sequenceNumber);
        reqMessage.setAck(null);
        reqMessage.setError(null);

        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            try {
                assert.ifError(err);
                assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                assert.strictEqual(resMessage.getError(), null);
                assert.deepEqual(resMessage.getMessageContainer(),{ message: 'test_service/isInRoomResponse', content: { "response": false }, seq: null, ack: [ sequenceNumber ], error: null } );
                done();
            } catch (e) {
                done(e);
            }
        });
    });

    it('leave unknown room', function (done) {
        var reqMessage = new ProtocolMessage();
        var sequenceNumber = serviceClient.getSeq();
        reqMessage.setMessage('test_service/leaveRoom');
        reqMessage.setContent({room: 'unknownRoom'});
        reqMessage.setSeq(sequenceNumber);
        reqMessage.setAck(null);
        reqMessage.setError(null);

        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            try {
                assert.ifError(err);
                assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                assert.strictEqual(resMessage.getError(), null);
                assert.deepEqual(resMessage.getMessageContainer(),{ message: 'test_service/leaveRoomResponse', content: {}, seq: null, ack: [ sequenceNumber ], error: null } );
                done();
            } catch (e) {
                done(e);
            }
        });
    });

    it('leave empty room', function (done) {
        var reqMessage = new ProtocolMessage();
        var sequenceNumber = serviceClient.getSeq();
        reqMessage.setMessage('test_service/leaveRoom');
        reqMessage.setContent({room: 'myLittleTest'});
        reqMessage.setSeq(sequenceNumber);
        reqMessage.setAck(null);
        reqMessage.setError(null);

        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            try {
                assert.ifError(err);
                assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                assert.strictEqual(resMessage.getError(), null);
                assert.deepEqual(resMessage.getMessageContainer(),{ message: 'test_service/leaveRoomResponse', content: {}, seq: null, ack: [ sequenceNumber ], error: null } );
                done();
            } catch (e) {
                done(e);
            }
        });
    });

    it('leave multiple rooms', function (done) {
        var reqMessage = new ProtocolMessage();
        async.series([
            function(callback) {
                var sequenceNumber = serviceClient.getSeq();
                reqMessage.setMessage('test_service/enterRoom');
                reqMessage.setContent({room: 'myLittleTest'});
                reqMessage.setSeq(sequenceNumber);

                serviceClient.sendMessage(reqMessage, function (err, resMessage) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                        assert.strictEqual(resMessage.getError(), null);
                        assert.deepEqual(resMessage.getMessageContainer(),{ message: 'test_service/enterRoomResponse', content: { "response": true }, seq: null, ack: [ sequenceNumber ], error: null } );
                        callback();
                    } catch (e) {
                        callback(e);
                    }
                });
            },
            function(callback) {
                var sequenceNumber = serviceClient.getSeq();
                reqMessage.setMessage('test_service/enterRoom');
                reqMessage.setContent({room: 'myLittleTest2'});
                reqMessage.setSeq(sequenceNumber);

                serviceClient.sendMessage(reqMessage, function (err, resMessage) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                        assert.strictEqual(resMessage.getError(), null);
                        assert.deepEqual(resMessage.getMessageContainer(),{ message: 'test_service/enterRoomResponse', content: { "response": true }, seq: null, ack: [ sequenceNumber ], error: null } );
                        callback();
                    } catch (e) {
                        callback(e);
                    }
                });
            },
            function(callback) {
                var sequenceNumber = serviceClient.getSeq();
                reqMessage.setMessage('test_service/isInRoom');
                reqMessage.setContent({room: 'myLittleTest'});
                reqMessage.setSeq(sequenceNumber);

                serviceClient.sendMessage(reqMessage, function (err, resMessage) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                        assert.strictEqual(resMessage.getError(), null);
                        assert.deepEqual(resMessage.getMessageContainer(),{ message: 'test_service/isInRoomResponse', content: { "response": true }, seq: null, ack: [ sequenceNumber ], error: null } );
                        callback();
                    } catch (e) {
                        callback(e);
                    }
                });
            },
            function(callback) {
                var sequenceNumber = serviceClient.getSeq();
                reqMessage.setMessage('test_service/isInRoom');
                reqMessage.setContent({room: 'myLittleTest2'});
                reqMessage.setSeq(sequenceNumber);

                serviceClient.sendMessage(reqMessage, function (err, resMessage) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                        assert.strictEqual(resMessage.getError(), null);
                        assert.deepEqual(resMessage.getMessageContainer(),{ message: 'test_service/isInRoomResponse', content: { "response": true }, seq: null, ack: [ sequenceNumber ], error: null } );
                        callback();
                    } catch (e) {
                        callback(e);
                    }
                });
            },
            function(callback) {
                var sequenceNumber = serviceClient.getSeq();
                reqMessage.setMessage('test_service/leaveAllRoom');
                reqMessage.setContent(null);
                reqMessage.setSeq(sequenceNumber);

                serviceClient.sendMessage(reqMessage, function (err, resMessage) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                        assert.strictEqual(resMessage.getError(), null);
                        assert.deepEqual(resMessage.getMessageContainer(),{ message: 'test_service/leaveAllRoomResponse', content: { }, seq: null, ack: [ sequenceNumber ], error: null } );
                        callback();
                    } catch (e) {
                        callback(e);
                    }
                });
            },
            function(callback) {
                var sequenceNumber = serviceClient.getSeq();
                reqMessage.setMessage('test_service/isInRoom');
                reqMessage.setContent({room: 'myLittleTest'});
                reqMessage.setSeq(sequenceNumber);

                serviceClient.sendMessage(reqMessage, function (err, resMessage) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                        assert.strictEqual(resMessage.getError(), null);
                        assert.deepEqual(resMessage.getMessageContainer(),{ message: 'test_service/isInRoomResponse', content: { "response": false }, seq: null, ack: [ sequenceNumber ], error: null } );
                        callback();
                    } catch (e) {
                        callback(e);
                    }
                });
            },
            function(callback) {
                var sequenceNumber = serviceClient.getSeq();
                reqMessage.setMessage('test_service/isInRoom');
                reqMessage.setContent({room: 'myLittleTest2'});
                reqMessage.setSeq(sequenceNumber);

                serviceClient.sendMessage(reqMessage, function (err, resMessage) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                        assert.strictEqual(resMessage.getError(), null);
                        assert.deepEqual(resMessage.getMessageContainer(),{ message: 'test_service/isInRoomResponse', content: { "response": false }, seq: null, ack: [ sequenceNumber ], error: null } );
                        callback();
                    } catch (e) {
                        callback(e);
                    }
                });
            }
        ], function(err) {
           done(err);
        });
    });

    it('validate user is not in unknown room', function (done) {
        var reqMessage = new ProtocolMessage();
        var sequenceNumber = serviceClient.getSeq();
        reqMessage.setMessage('test_service/isInRoom');
        reqMessage.setContent({room: 'unknownRoom2'});
        reqMessage.setSeq(sequenceNumber);
        reqMessage.setAck(null);
        reqMessage.setError(null);

        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            try {
                assert.ifError(err);
                assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                assert.strictEqual(resMessage.getError(), null);
                assert.deepEqual(resMessage.getMessageContainer(),{ message: 'test_service/isInRoomResponse', content: { "response": false }, seq: null, ack: [ sequenceNumber ], error: null } );
                done();
            } catch (e) {
                done(e);
            }
        });
    });

    it('get userlist from empty room', function (done) {
        var reqMessage = new ProtocolMessage();
        var sequenceNumber = serviceClient.getSeq();
        reqMessage.setMessage('test_service/getUsersByRoom');
        reqMessage.setContent({room: 'unknownRoom2'});
        reqMessage.setSeq(sequenceNumber);
        reqMessage.setAck(null);
        reqMessage.setError(null);

        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            try {
                assert.ifError(err);
                assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                assert.strictEqual(resMessage.getError(), null);
                assert.deepEqual(resMessage.getMessageContainer(),{ message: 'test_service/getUsersByRoomResponse', content: { "response": [] }, seq: null, ack: [ sequenceNumber ], error: null } );
                done();
            } catch (e) {
                done(e);
            }
        });
    });

    it('connect more users to the system', function (done) {
        serviceClient1 = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: port
            },
            validateFullMessage: false
        });
        serviceClient2 = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: port
            },
            validateFullMessage: false
        });
        async.series([
            function(callback) {
                serviceClient1.connect(callback);
            },
            function(callback) {
                serviceClient2.connect(callback);
            }
        ], function(err) {
            done(err);
        });
    });


    it('adding multiple people in rooms', function (done) {
        async.parallel([
            function(callback) {
                var reqMessage = new ProtocolMessage();
                var sequenceNumber = serviceClient.getSeq();
                reqMessage.setMessage('test_service/enterRoom');
                reqMessage.setContent({room: 'myLittleTest'});
                reqMessage.setToken(token);
                reqMessage.setSeq(sequenceNumber);
                serviceClient.sendMessage(reqMessage, function (err, resMessage) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                        assert.strictEqual(resMessage.getError(), null);
                        assert.deepEqual(resMessage.getMessageContainer(),{ message: 'test_service/enterRoomResponse', content: { "response": true }, seq: null, ack: [ sequenceNumber ], error: null } );
                        callback();
                    } catch (e) {
                        callback(e);
                    }
                });
            },
            function(callback) {
                var reqMessage = new ProtocolMessage();
                var sequenceNumber = serviceClient1.getSeq();
                reqMessage.setMessage('test_service/enterRoom');
                reqMessage.setContent({room: 'myLittleTest'});
                reqMessage.setToken(token);
                reqMessage.setSeq(sequenceNumber);

                serviceClient1.sendMessage(reqMessage, function (err, resMessage) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                        assert.strictEqual(resMessage.getError(), null);
                        assert.deepEqual(resMessage.getMessageContainer(),{ message: 'test_service/enterRoomResponse', content: { "response": true }, seq: null, ack: [ sequenceNumber ], error: null } );
                        callback();
                    } catch (e) {
                        callback(e);
                    }
                });
            },
            function(callback) {
                var reqMessage = new ProtocolMessage();
                var sequenceNumber = serviceClient2.getSeq();
                reqMessage.setMessage('test_service/enterRoom');
                reqMessage.setContent({room: 'myLittleTest'});
                reqMessage.deleteToken();
                reqMessage.setSeq(sequenceNumber);

                serviceClient2.sendMessage(reqMessage, function (err, resMessage) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                        assert.strictEqual(resMessage.getError(), null);
                        assert.deepEqual(resMessage.getMessageContainer(),{ message: 'test_service/enterRoomResponse', content: { "response": true }, seq: null, ack: [ sequenceNumber ], error: null } );
                        callback();
                    } catch (e) {
                        callback(e);
                    }
                });
            }
        ], function(err) {
            done(err);
        });
    });

    it('get userlist from full  room', function (done) {
        var reqMessage = new ProtocolMessage();
        var sequenceNumber = serviceClient.getSeq();
        reqMessage.setMessage('test_service/getUsersByRoom');
        reqMessage.setContent({room: 'myLittleTest'});
        reqMessage.setSeq(sequenceNumber);
        reqMessage.setAck(null);
        reqMessage.setError(null);
        
        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            try {
                assert.ifError(err);
                assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                assert.strictEqual(resMessage.getError(), null);
                assert.strictEqual(resMessage.getContent().response.length, 3);
                done();
            } catch (e) {
                done(e);
            }
        });
    });

    it('send message to unknown room', function (done) {
        var reqMessage = new ProtocolMessage();
        var sequenceNumber = serviceClient.getSeq();
        reqMessage.setMessage('test_service/sendMessageToRooms');
        reqMessage.setContent({room: 'unknownRoom', message: 'test'});
        reqMessage.setSeq(sequenceNumber);
        reqMessage.setAck(null);
        reqMessage.setError(null);

        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            try {
                assert.ifError(err);
                assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                assert.strictEqual(resMessage.getError(), null);
                assert.deepEqual(resMessage.getMessageContainer(),{ message: 'test_service/sendMessageToRoomsResponse', content: {}, seq: null, ack: [ sequenceNumber ], error: null } );
                done();
            } catch (e) {
                done(e);
            }
        });
    });


    it('multiple people in rooms - test broadcast', function (done) {
        var messageClient = null;
        var messageClient1 = null;
        var messageClient2 = null;

        serviceClient.on("message:testBroadcast", function(message) {
            try {
                message.deleteClientId(); //We dont cate about the client id
                assert.deepEqual(message.getMessageContainer(),{ message: 'test_service/testBroadcast', content: null, seq: null, ack: null, error: null } );
                messageClient();
            } catch (e) {
                messageClient(e);
            }
        });

        serviceClient1.on("message:testBroadcast", function(message) {
            try {
                message.deleteClientId(); //We dont cate about the client id
                assert.deepEqual(message.getMessageContainer(),{ message: 'test_service/testBroadcast', content: null, seq: null, ack: null, error: null } );
                messageClient1();
            } catch (e) {
                messageClient1(e);
            }
        });

        serviceClient2.on("message:testBroadcast", function(message) {
            try {
                message.deleteClientId(); //We dont cate about the client id
                assert.deepEqual(message.getMessageContainer(),{ message: 'test_service/testBroadcast', content: null, seq: null, ack: null, error: null } );
                messageClient2();
            } catch (e) {
                messageClient2(e);
            }
        });

        async.parallel([
            function(callback) {
                messageClient = callback;
            },
            function(callback) {
                messageClient1 = callback;
            },
            function(callback) {
                messageClient2 = callback;
            },
            function(callback) {
                var reqMessage = new ProtocolMessage();
                var sequenceNumber = serviceClient.getSeq();
                reqMessage.setMessage('test_service/sendMessageToRooms');
                reqMessage.setContent({room: 'myLittleTest', messageName: 'testBroadcast'});
                reqMessage.setSeq(sequenceNumber);
                reqMessage.setAck(null);
                reqMessage.setError(null);

                serviceClient.sendMessage(reqMessage, function (err, resMessage) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                        assert.strictEqual(resMessage.getError(), null);
                        assert.deepEqual(resMessage.getMessageContainer(),{ message: 'test_service/sendMessageToRoomsResponse', content: {}, seq: null, ack: [ sequenceNumber ], error: null } );
                        callback();
                    } catch (e) {
                        callback(e);
                    }
                });
            }
        ], function(err) {
            done(err);
        });
    });


    it('user is not in room anymore after disconnect', function (done) {
        var reqMessage = new ProtocolMessage();
        async.series([
            function(callback) {
                serviceClient.on("connectionClose",function(reasonCode, description) {
                    try {
                        assert.strictEqual(reasonCode, 1000);
                        assert.strictEqual(description, "because i can");
                    } catch (e) {
                        callback(e);
                    }
                    callback();
                });
                serviceClient.disconnect(1000, "because i can", true);
            },
            function(callback) {
                var sequenceNumber = serviceClient1.getSeq();
                reqMessage.setMessage('test_service/getUsersByRoom');
                reqMessage.setContent({room: 'myLittleTest'});
                reqMessage.setToken(token);
                reqMessage.setSeq(sequenceNumber);

                serviceClient1.sendMessage(reqMessage, function (err, resMessage) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                        assert.strictEqual(resMessage.getError(), null);
                        assert.strictEqual(resMessage.getContent().response.length, 2);
                        callback();
                    } catch (e) {
                        callback(e);
                    }
                });
            }
        ], function(err) {
            done(err);
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
