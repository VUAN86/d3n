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
    ProtocolMessage = require('nodejs-protocol'),
    token = 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhIjoiYXNkIiwiYiI6MTIzLCJpYXQiOjE0NjU5MDQwMzB9.WGKKroIKmu4BZu_hWZc7AdUNj2ItUU1K-b7-OvC1AA4G6vE8nYkDpHoqdRazoRbPa6wXJY_WKEPpUrSNphBMDb3803vbKRloCr8WAt3ekrKeo1fiTiE1-AhGYAk3omu90sR4zrDiGxC2-8vlvnt5O7ZgjyEktIjNPmeW-hGrnrg_ST8903aLCGBq7pOxfiyDbtRalzoawiv_I1krQKYQ6FujZHM7y0486ZnJ8TnILTBuFn8YHEiVon63kUF6VzMclD_byS7d4vI0zybQnjjos_ueb8J_y8wvHn5Dm3y9MOrdNtda-OsTDG93jjDomWF-hr2HWS_3PHlM_gxKFlQxjA',
    logger = require('nodejs-logger')()
;

var services = [];
var clients = [];
describe('Testing hearthbeat', function() {
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

    it('should receive heartbeat response success', function (done) {
        var reqMessage = new ProtocolMessage();
        var sequenceNumber = serviceClient.getSeq();
        reqMessage.setMessage('test_service/heartbeat');
        reqMessage.setContent(null);
        reqMessage.setSeq(sequenceNumber);
        reqMessage.setAck(null);
        reqMessage.setError(null);

        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            try {
                assert.ifError(err);
                assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                assert.strictEqual(resMessage.getError(), null);
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
                services[i].shutdown();
            } catch (e) {}
        }

        setTimeout(done, 500);
    });

});
