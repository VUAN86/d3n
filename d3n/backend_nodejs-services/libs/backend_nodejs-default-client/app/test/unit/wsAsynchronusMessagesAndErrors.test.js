var ip = process.env.HOST || 'localhost',
    port = process.env.PORT || 4001,
    cp = require('child_process'),
    fs = require('fs'),
    async = require('async'),
    should = require('should'),
    assert = require('chai').assert,
    workerService = null,
    ServiceClient = require('../../classes/Client'),
    Service = require('nodejs-default-service'),
    token = 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhIjoiYXNkIiwiYiI6MTIzLCJpYXQiOjE0NjU5MDQwMzB9.WGKKroIKmu4BZu_hWZc7AdUNj2ItUU1K-b7-OvC1AA4G6vE8nYkDpHoqdRazoRbPa6wXJY_WKEPpUrSNphBMDb3803vbKRloCr8WAt3ekrKeo1fiTiE1-AhGYAk3omu90sR4zrDiGxC2-8vlvnt5O7ZgjyEktIjNPmeW-hGrnrg_ST8903aLCGBq7pOxfiyDbtRalzoawiv_I1krQKYQ6FujZHM7y0486ZnJ8TnILTBuFn8YHEiVon63kUF6VzMclD_byS7d4vI0zybQnjjos_ueb8J_y8wvHn5Dm3y9MOrdNtda-OsTDG93jjDomWF-hr2HWS_3PHlM_gxKFlQxjA',
    ProtocolMessage = require('nodejs-protocol')
    ;

var serviceClient = null;
var serviceClient2 = null;
var serviceConnections = {};

describe('Asynchronous Messages and Errors', function() {
    before(function(done) {
        this.timeout(10000);
        workerService = new Service({
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
            validateFullMessage: false
        });
        workerService.messageHandlers['enter'] = function (message, clientSession) {
            var resMessage = new ProtocolMessage(message);

            serviceConnections["id"+Math.random()] = clientSession;

            clientSession.sendMessage(resMessage);
        };

        workerService.messageHandlers['sendMessage'] = function (message, clientSession) {
            var resMessage = new ProtocolMessage(message);
            resMessage.setContent(null);

            var content = message.getContent();
            var outgoingMessage = new ProtocolMessage();
            outgoingMessage.setMessage("messageReceived");
            outgoingMessage.setContent(content);

            for (var prop in serviceConnections) {
                if( serviceConnections.hasOwnProperty( prop ) ) {
                    serviceConnections[prop].sendMessage(outgoingMessage);
                }
            }
            clientSession.sendMessage(resMessage);
        };


        serviceClient = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: port
            },
            jwt: token,
            validateFullMessage: false
        });

        serviceClient2 = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: port
            },
            jwt: token,
            validateFullMessage: false
        });
        workerService.build(function (err) {
            if (err) {
                return done(err);
            }

            serviceClient.connect(function(err) {
                if (err) {
                    return done(err);
                }
                serviceClient2.connect(function(err) {
                    if (err) {
                        return done(err);
                    }
                    return done();
                });
            });
        });
    });

    it('registering service 1 on service', function (done) {
        var message = new ProtocolMessage();
        message.setMessage('enter');
        var sequenceNumber = serviceClient.getSeq();
        message.setSeq(sequenceNumber);
        serviceClient.sendMessage(message, function (err, resMessage) {
            assert.equal(err, false);
            resMessage.deleteClientId();
            assert.deepEqual(resMessage.getMessageContainer(), {message:"enterResponse",content:null,seq:null,ack:[sequenceNumber],error:null});
            done();
        });
    });

    it('registering service 2 on service', function (done) {
        var message = new ProtocolMessage();
        message.setMessage('enter');
        var sequenceNumber = serviceClient2.getSeq();
        message.setSeq(sequenceNumber);
        serviceClient2.sendMessage(message, function (err, resMessage) {
            assert.equal(err, false);
            resMessage.deleteClientId();
            assert.deepEqual(resMessage.getMessageContainer(), {message:"enterResponse",content:null,seq:null,ack:[sequenceNumber],error:null});
            done();
        });
    });

    it('receive message without sending a message', function (done) {
        var callbackMessage2Received = null;
        var callbackMessage1Received = null;
        var callbackMessageResponseReceived = null;


        serviceClient2.on('message:messageReceived', function(resMessage) {
            resMessage.deleteClientId();
            assert.deepEqual(resMessage.getMessageContainer(), {message:"messageReceived",content:{test:'test'},seq:null,ack:null,error:null});
            callbackMessage2Received();
        });
        serviceClient.on('message:messageReceived', function(resMessage) {
            resMessage.deleteClientId();
            assert.deepEqual(resMessage.getMessageContainer(), {message:"messageReceived",content:{test:'test'},seq:null,ack:null,error:null});
            callbackMessage1Received();
        });



        async.parallel([
                function(callback) {
                    callbackMessage2Received = callback;
                },
                function(callback) {
                    callbackMessage1Received = callback;
                },
                function(callback) {
                    callbackMessageResponseReceived = callback;
                },
                function(callback) {
                    setTimeout(function() {
                        var message = new ProtocolMessage();
                        message.setMessage('sendMessage');
                        var sequenceNumber = serviceClient.getSeq();
                        message.setSeq(sequenceNumber);
                        message.setContent({test:"test"});
                        serviceClient.sendMessage(message, function (err, resMessage) {
                            assert.equal(err, false);
                            resMessage.deleteClientId();
                            assert.deepEqual(resMessage.getMessageContainer(), {message:"sendMessageResponse",content:null,seq:null,ack:[sequenceNumber],error:null});
                            callbackMessageResponseReceived();
                        });
                    },1);
                    callback();
                }
        ],
        function(err) {
            done(err);
        });
    });

    it('disconnecting without reconnect', function (done) {
        var didReconnect = false;

        var onDisconnectHandler = function(reasonCode, description) {
            serviceClient.removeListener('connectionClose', onDisconnectHandler);

            assert.strictEqual(reasonCode, 1000);
            assert.strictEqual(description, "testing disconnect");

            serviceClient.on('reconnected', function(callback) {
                didReconnect = true;
                callback();
            });

            //Give him a second, as its in the same process thats more then enough time
            setTimeout(function() {
                assert.strictEqual(didReconnect, false);
                done();
            },1000);
        };

        serviceClient.on('connectionClose', onDisconnectHandler);
        serviceClient.disconnect(1000, "testing disconnect", true);
    });

    it('send message on disconnected service', function (done) {
        var message = new ProtocolMessage();
        message.setMessage('sendMessage');
        var sequenceNumber = serviceClient.getSeq();
        message.setSeq(sequenceNumber);
        message.setContent({test:"test"});
        serviceClient.sendMessage(message, function (err, resMessage) {
            assert.isOk(err);
            assert.strictEqual(resMessage, undefined);
            done();
        });
    });

    it('send authenticate on disconnected service', function (done) {
        serviceClient.authenticate(function (err, resMessage) {
            assert.isOk(err);
            assert.strictEqual(resMessage, undefined);
            done();
        });
    });

    after(function(done) {
        workerService.shutdown();
        setTimeout(done, 1000);
    });

});