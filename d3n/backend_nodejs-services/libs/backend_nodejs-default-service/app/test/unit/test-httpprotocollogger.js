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
        }
    }),
    ProtocolMessage = require('nodejs-protocol'),
    token = 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhIjoiYXNkIiwiYiI6MTIzLCJpYXQiOjE0NjU5MDQwMzB9.WGKKroIKmu4BZu_hWZc7AdUNj2ItUU1K-b7-OvC1AA4G6vE8nYkDpHoqdRazoRbPa6wXJY_WKEPpUrSNphBMDb3803vbKRloCr8WAt3ekrKeo1fiTiE1-AhGYAk3omu90sR4zrDiGxC2-8vlvnt5O7ZgjyEktIjNPmeW-hGrnrg_ST8903aLCGBq7pOxfiyDbtRalzoawiv_I1krQKYQ6FujZHM7y0486ZnJ8TnILTBuFn8YHEiVon63kUF6VzMclD_byS7d4vI0zybQnjjos_ueb8J_y8wvHn5Dm3y9MOrdNtda-OsTDG93jjDomWF-hr2HWS_3PHlM_gxKFlQxjA'
;

var logger = null;
var loggingPort = port + 5;
var http = require('http');
var server = null;
var receivedMessages = [];


describe('HTTP Protocol Logger', function() {
    before(function(done) {
        done();
    });


    it('enabling http logger, receive service created', function (done) {
        this.timeout(15000);

        delete require.cache[require.resolve('../../classes/Service')];
        delete require.cache[require.resolve(__dirname + '/worker-create-service.js')];
        process.env.LOG_LEVEL = undefined;
        process.env.LOG_FILE = undefined;
        process.env.ENV = 'development';
        process.env.LOG_SERVER = '127.0.0.1';
        process.env.LOG_SERVER_PORT = loggingPort;
        process.env.PROTOCOL_LOGGING = true;


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
            }
        };

        workerService = require(__dirname + '/worker-create-service.js')(args);
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
            workerService.build(function (err) {
                if(err) {
                    return done(err);
                }

                setTimeout(function() {
                    assert.strictEqual(receivedMessages.length, 1);
                    var receivedMessage = JSON.parse(receivedMessages.pop());
                    assert.isNotNull(receivedMessage.params);
                    assert.isNotNull(receivedMessage.params.info);
                    assert.isNotNull(receivedMessage.params.message);
                    var message = JSON.parse(receivedMessage.params.message);
                    assert.strictEqual(message.type, "service");
                    assert.strictEqual(message.action, "created");
                    assert.strictEqual(message.on, "localhost:"+port);
                    assert.strictEqual(message.identification, process.cwd()+":"+process.pid);
                    assert.strictEqual(message.visibleName, workerService._serviceName);

                    done();
                },10);
            });
        });
    });
    
    it('connect to service', function (done) {
        this.timeout(10000);
        serviceClient.connect(done);
    });

    it('sent and received log message', function (done) {
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
                //Last one is outgoing
                var receivedMessage = JSON.parse(receivedMessages.pop());
                assert.isNotNull(receivedMessage.params);
                assert.isNotNull(receivedMessage.params.info);
                assert.isNotNull(receivedMessage.params.message);
                var message = JSON.parse(receivedMessage.params.message);
                assert.strictEqual(message.type, "data");
                assert.strictEqual(message.action, "sent");
                assert.strictEqual(message.from, "127.0.0.1:"+port);
                assert.strictEqual(message.identification, process.cwd()+":"+process.pid);

                //Second last one is ingoing
                var receivedMessage = JSON.parse(receivedMessages.pop());
                assert.isNotNull(receivedMessage.params);
                assert.isNotNull(receivedMessage.params.info);
                assert.isNotNull(receivedMessage.params.message);
                var message = JSON.parse(receivedMessage.params.message);
                assert.strictEqual(message.type, "data");
                assert.strictEqual(message.action, "received");
                assert.strictEqual(message.to, "127.0.0.1:"+port);
                assert.strictEqual(message.identification, process.cwd()+":"+process.pid);

                done();
            },10);
        });
    });


    it('shutdown', function (done) {
        workerService.shutdown();

        setTimeout(function() {
            while (receivedMessages.length > 1) {
                receivedMessages.pop();
            }
            assert.strictEqual(receivedMessages.length, 1);
            var receivedMessage = JSON.parse(receivedMessages.pop());
            assert.isNotNull(receivedMessage.params);
            assert.isNotNull(receivedMessage.params.info);
            assert.isNotNull(receivedMessage.params.message);
            var message = JSON.parse(receivedMessage.params.message);
            assert.strictEqual(message.type, "service");
            assert.strictEqual(message.action, "removed");
            assert.strictEqual(message.on, "localhost:"+port);
            assert.strictEqual(message.identification, process.cwd()+":"+process.pid);
            assert.strictEqual(message.visibleName, workerService._serviceName);

            done();
        },10);
    });
    after(function() {
    });
    
});