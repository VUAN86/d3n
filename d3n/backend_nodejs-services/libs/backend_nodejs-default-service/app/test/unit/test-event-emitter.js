var ip = process.env.HOST || 'localhost',
    port = process.env.PORT ? parseInt(process.env.PORT) : 4001,
    cp = require('child_process'),
    fs = require('fs'),
    async = require('async'),
    should = require('should'),
    assert = require('assert'),
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
    logger = require('nodejs-logger')(),
    TestService = require('./classes/TestService'),
    testService = null
;
var services = [];
var clients = [];

describe('Testing internal event emitter', function() {
    this.timeout(10000);

    it('create test service instance', function () {
        testService = new TestService({
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
        });
        services.push(workerService);
    });

    it('emit workorderUpdated event', function () {
        var obj1 = {
            id: 1,
            name: 'obj1'
        };
        var obj2 = {
            id: 2,
            name: 'obj2'
        };

        testService.eventHandlers['workorderUpdatedSuccess'] = function (res_obj1, res_obj2) {
            assert.deepStrictEqual(res_obj1, obj1);
            assert.deepStrictEqual(res_obj2, obj2);
        };

        // emit workorderUpdated . workorderUpdated handler will emit workorderUpdatedSuccess event
        var res = testService.emitEvent('workorderUpdated', 'workorderUpdatedSuccess', obj1, obj2);
        assert(res, true);

    });

    it('emit success without arguments', function () {
        var res = testService.emitEvent('noArgument');
        assert.strictEqual(res, true);
    });



    it('handler not found', function () {
        var res = testService.emitEvent('workorderUpdated_asdasas');

        assert(res instanceof Error);
        assert.strictEqual(res.message, 'ERR_EVENT_HANDLER_NOT_FOUND');

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
