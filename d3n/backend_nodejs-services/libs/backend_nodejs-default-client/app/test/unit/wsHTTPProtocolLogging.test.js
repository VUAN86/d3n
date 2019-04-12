var ip = process.env.HOST || 'localhost',
    port = process.env.PORT || 4001,
    cp = require('child_process'),
    fs = require('fs'),
    async = require('async'),
    should = require('should'),
    assert = require('chai').assert,
    workerService = null,
    token = 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhIjoiYXNkIiwiYiI6MTIzLCJpYXQiOjE0NjU5MDQwMzB9.WGKKroIKmu4BZu_hWZc7AdUNj2ItUU1K-b7-OvC1AA4G6vE8nYkDpHoqdRazoRbPa6wXJY_WKEPpUrSNphBMDb3803vbKRloCr8WAt3ekrKeo1fiTiE1-AhGYAk3omu90sR4zrDiGxC2-8vlvnt5O7ZgjyEktIjNPmeW-hGrnrg_ST8903aLCGBq7pOxfiyDbtRalzoawiv_I1krQKYQ6FujZHM7y0486ZnJ8TnILTBuFn8YHEiVon63kUF6VzMclD_byS7d4vI0zybQnjjos_ueb8J_y8wvHn5Dm3y9MOrdNtda-OsTDG93jjDomWF-hr2HWS_3PHlM_gxKFlQxjA',
    ProtocolMessage = require('nodejs-protocol')
    ;

var logger = null;
var loggingPort = 9615;
var http = require('http');
var server = null;
var receivedMessages = [];
var ServiceClient = null;
var serviceClient = null;

describe('HTTP Protocol Logging', function() {
    before(function(done) {
        this.timeout(10000);
        workerService = cp.fork(__dirname + '/worker-service.js', [], {
            execArgv: [],
            env: {
                'WORKER_CONFIG': JSON.stringify({
                    'service': {
                        secure: true,
                        ip: ip,
                        port: port,
                        serviceName: 'test_service',
                        key: fs.readFileSync(__dirname + '/ssl-certificate/key.pem', 'utf8'),
                        cert: fs.readFileSync(__dirname + '/ssl-certificate/cert.pem', 'utf8'),
                        auth: {
                            algorithm: 'RS256',
                            publicKey: fs.readFileSync(__dirname + '/jwt-keys/pubkey.pem', 'utf8')
                        }
                    }
                })
            }
        });

        workerService.on('message', function (message) {
            if(message.success === true) {
                server = http.createServer(function (req, res) {
                    var data = "";
                    req.on('data', function (chunk) {
                        data += chunk;
                    });
                    req.on('end', function () {
                        receivedMessages.push(data);
                        res.writeHead(200, {'Content-Type': 'text/plain'});
                        res.end("");
                    });
                }).listen(loggingPort, function() {
                    done();
                });
                return;
            }

            return done(new Error('error starting testing service'));
        });
    });

    it('enabling http logger', function (done) {
        this.timeout(15000);

        delete require.cache[require.resolve('../../classes/Client')];
        process.env.LOG_LEVEL = undefined;
        process.env.LOG_FILE = undefined;
        process.env.ENV = 'development';
        process.env.LOG_SERVER = '127.0.0.1';
        process.env.LOG_SERVER_PORT = loggingPort;
        process.env.PROTOCOL_LOGGING = true;
        ServiceClient = require('../../classes/Client');
        serviceClient = new ServiceClient({
                secure: true,
                service: {
                    ip: ip,
                    port: port
                },
                jwt: token,
                validateFullMessage: false
        });
        receivedMessages = [];
        serviceClient.connect(function(err) {
            assert.ifError(err);
            assert.strictEqual(serviceClient.connected(), true);

            setTimeout(function() {
                assert.isAtLeast(receivedMessages.length, 1);
                var receivedMessage = JSON.parse(receivedMessages.pop());
                assert.isNotNull(receivedMessage.params);
                assert.isNotNull(receivedMessage.params.info);
                assert.isNotNull(receivedMessage.params.message);
                var message = JSON.parse(receivedMessage.params.message);
                assert.strictEqual(message.type, "connection");
                assert.strictEqual(message.action, "created");
                assert.strictEqual(message.to, "127.0.0.1:"+port);
                assert.strictEqual(message.identification, process.cwd()+":"+process.pid);

                done();
            },10);
        });
    });

    it('sent log entry', function (done) {
        var reqMessage = new ProtocolMessage();
        reqMessage.setMessage('ping');
        reqMessage.setContent(null);
        reqMessage.setSeq(null);
        reqMessage.setAck(null);
        reqMessage.setError(null);

        receivedMessages = [];
        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            assert.ifError(err);
            assert.typeOf(resMessage, 'undefined');

            setTimeout(function() {
                assert.strictEqual(receivedMessages.length, 2);
                receivedMessages.pop();
                var receivedMessage = JSON.parse(receivedMessages.pop());
                assert.isNotNull(receivedMessage.params);
                assert.isNotNull(receivedMessage.params.info);
                assert.isNotNull(receivedMessage.params.message);
                var message = JSON.parse(receivedMessage.params.message);
                assert.strictEqual(message.type, "data");
                assert.strictEqual(message.action, "sent");
                assert.strictEqual(message.to, "127.0.0.1:"+port);
                assert.strictEqual(message.identification, process.cwd()+":"+process.pid);

                done();
            },50);
        });
    });


    it('receive log message', function (done) {
        var reqMessage = new ProtocolMessage();
        reqMessage.setMessage('ping');
        reqMessage.setContent({data: 'for ping'});
        reqMessage.setSeq(serviceClient.getSeq());
        reqMessage.setAck(null);
        reqMessage.setError(null);

        receivedMessages = [];
        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            setTimeout(function() {
                assert.strictEqual(receivedMessages.length, 2);
                var receivedMessage = JSON.parse(receivedMessages.pop());
                assert.isNotNull(receivedMessage.params);
                assert.isNotNull(receivedMessage.params.info);
                assert.isNotNull(receivedMessage.params.message);
                var message = JSON.parse(receivedMessage.params.message);
                assert.strictEqual(message.type, "data");
                assert.strictEqual(message.action, "received");
                assert.strictEqual(message.from, "127.0.0.1:"+port);
                assert.strictEqual(message.identification, process.cwd()+":"+process.pid);

                done();
            },10);
        });
    });

    it('disconnect', function (done) {
        //return done();
        var onDisconnectHandler = function(reasonCode, description) {
            serviceClient.removeListener('connectionClose', onDisconnectHandler);

            assert.strictEqual(reasonCode, 1000);
            assert.strictEqual(description, "testing disconnect");
            assert.strictEqual(serviceClient.connected(), false);
            
            setTimeout(function() {
                var receivedMessage = JSON.parse(receivedMessages.pop());
                assert.isNotNull(receivedMessage.params);
                assert.isNotNull(receivedMessage.params.info);
                assert.isNotNull(receivedMessage.params.message);
                var message = JSON.parse(receivedMessage.params.message);
                assert.strictEqual(message.type, "connection");
                assert.strictEqual(message.action, "removed");
                assert.strictEqual(message.to, "127.0.0.1:"+port);
                assert.strictEqual(message.identification, process.cwd()+":"+process.pid);

                done();
            },100);

        };
        serviceClient.on('connectionClose', onDisconnectHandler);
        receivedMessages = [];
        serviceClient.disconnect(1000, "testing disconnect", true);

    });

    after(function(done) {
        workerService.kill();
        setTimeout(done, 1000);
    });

});