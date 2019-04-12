var ip = process.env.HOST || 'localhost';
var port = process.env.PORT ? parseInt(process.env.PORT) : 4001;
port = port+10;
var cp = require('child_process');
var fs = require('fs');
var async = require('async');
var _ = require('lodash');
var should = require('should');
var assert = require('chai').assert;
var TestFullMessageValidation = require('./classes/TestFullMessageValidation.js');
var testService = null;
var ServiceClient = require('nodejs-default-client');
var Errors = require('nodejs-errors');
var jwt = require('jsonwebtoken');
var KeyvalueService = require('nodejs-aerospike').getInstance().KeyvalueService;
var AerospikeGlobalClientSession = KeyvalueService.Models.AerospikeGlobalClientSession;

function _encode(payload) {
    var jwtOptions = {
        algorithm: 'RS256',
        issuer: 'F4M',
        expiresIn: 3600
    };
    var privateKey = fs.readFileSync(__dirname + '/jwt-keys/privkey.pem', 'utf8');
    return jwt.sign(payload, privateKey, jwtOptions);
}

var serviceClient = new ServiceClient({
    secure: true,
    service: {
        ip: ip,
        port: port
    },
    validateFullMessage: false
});
var ProtocolMessage = require('nodejs-protocol');

var clientInfo = {
    profile: {
        userId: '1111111-1111-1111-1111-111111111111',
        roles: ['TENANT_thetenantid1232_ADMIN']
    },
    
    appConfig: {
        tenantId: 'thetenantid1232'
    }
};


var logger = require('nodejs-logger')();
var validReqMessages = [
    {
        message: 'question/languageGet',
        seq: parseInt(_.uniqueId()),
        token: null,
        clientInfo: clientInfo,
        content: {
            id: 123
        },
        ack: null
    },

    {
        message: 'question/languageGet',
        seq: null,
        token: null,
        clientInfo: clientInfo,
        content: {
            id: 123
        },
        ack: null
    }
];

var invalidReqMessages = [
    {
        message: 'question/languageGet',
        token: null,
        clientInfo: clientInfo,
        seq: parseInt(_.uniqueId()),
        content: {
            id: 0
        }
    },

    {
        message: 'question/languageGet',
        token: null,
        clientInfo: clientInfo,
        seq: parseInt(_.uniqueId()),
        content: null
    }
];


var serviceOutFile = __dirname + '/temp/out.txt';

var clients = [];
var services = [];

describe('Testing full message validation', function() {
    this.timeout(10000);

    before(function(done) {
        var config = {
            secure: true,
            ip: ip,
            port: port,
            serviceName: 'question',
            key: fs.readFileSync(__dirname + '/ssl-certificate/key.pem', 'utf8'),
            cert: fs.readFileSync(__dirname + '/ssl-certificate/cert.pem', 'utf8'),
            validatePermissions: true,
            auth: {
                algorithm: 'RS256',
                publicKey: fs.readFileSync(__dirname + '/jwt-keys/pubkey.pem', 'utf8')
            },
            outFile: serviceOutFile
        };

        testService = new TestFullMessageValidation(config);
        services.push(testService);
        testService.build(done);
    });
    it('should connect to service', function (done) {
        clients.push(serviceClient);
        serviceClient.connect(done);
    });

    it('valid request messages', function (done) {
        var items = [];
        for(var i=0; i<validReqMessages.length; i++) {
            var m = new ProtocolMessage(validReqMessages[i]);
            m.setClientId('asdas234asd');
            items.push(m);
        }

        async.map(items, function(item, cbItem) {
            serviceClient.sendMessage(item, function (err, response) {
                try {
                    if (item.getSeq() === null) {
                        assert.ifError(err);
                        return cbItem(false, response);
                    } else {
                        assert.ifError(err);
                        assert.isNull(response.getError());

                        assert.isNotNull(response.getAck());
                        assert.strictEqual(response.getAck()[0], item.getSeq());
                        return cbItem(false, response);
                    }
                } catch (e) {
                    return cbItem(e);
                }
            });
        }, function (err) {
            done(err);
        });
    });

    it('user doesn\'t have the required roles', function (done) {
        var message = _.cloneDeep(validReqMessages[0]);
        var cf = _.cloneDeep(clientInfo);
        cf.profile.roles = ['TENANT_1_COMMUNITY'];
        message.clientInfo = cf;

        var reqMessage = new ProtocolMessage(message);
        reqMessage.setClientId('dasd');

        serviceClient.sendMessage(reqMessage, function (err, response) {
            try {
                if (err) done(err);

                assert.ifError(err);
                assert.deepEqual(response.getError(), _.clone(Errors['ERR_INSUFFICIENT_RIGHTS']));
                return done(false);
            } catch (e) {
                return done(e);
            }
        });
    });

    it('invalid request messages', function (done) {
        var items = [];
        for(var i=0; i<invalidReqMessages.length; i++) {
            var m = new ProtocolMessage(invalidReqMessages[i]);
            items.push(m);
        }

        async.mapSeries(items, function(item, cbItem) {
            serviceClient.sendMessage(item, function (err, response) {
                try {
                    assert.ifError(err);
                    assert.isNotNull(response.getError());
                    assert.strictEqual(response.getError().message, Errors.ERR_VALIDATION_FAILED.message);
                    return cbItem(false);
                } catch (e) {
                    return cbItem(e);
                }
            });
        }, function (err) {
            done(err);
        });
    });

    it('valid gateway/clientDisconnect message', function (done) {
        var m = new ProtocolMessage();
        m.setMessage('gateway/clientDisconnect');
        m.setClientId('asdaasdsa');
        m.setClientInfo(clientInfo);

        fs.writeFileSync(serviceOutFile, '', 'utf8');

        serviceClient.sendMessage(m, function (err, response) {
            setTimeout(function () {
                try {
                    assert.ifError(err);
                    assert.isUndefined(response);
                    var fileContent = fs.readFileSync(serviceOutFile, 'utf8');
                    assert.strictEqual(fileContent, 'clientDisconnectCalled');
                    return done();
                } catch (e) {
                    return done(e);
                }
            }, 500);
        });
    });


    it('invalid gateway/clientDisconnect message, no client id', function (done) {
        var m = new ProtocolMessage();
        m.setMessage('gateway/clientDisconnect');

        serviceClient.on('message', function (response) {
            try {
                assert.isNotNull(response.getError());
                assert.strictEqual(response.getError().message, Errors.ERR_VALIDATION_FAILED.message);
                return done();
            } catch (e) {
                return done(e);
            } finally {
                serviceClient.removeAllListeners('message');
            }
        });

        serviceClient.sendMessage(m, function (err, response) {
            try {
                assert.ifError(err);
                assert.isUndefined(response);
            } catch (e) {
                return done(e);
            }
       });
    });

    it('invalid gateway/clientDisconnect message, seq provided', function (done) {
        var m = new ProtocolMessage();
        m.setMessage('gateway/clientDisconnect');
        m.setSeq(123);
        m.setClientId('11-22-33-44');

        serviceClient.sendMessage(m, function (err, response) {
            try {
                assert.ifError(err);
                assert.isNotNull(response.getError());
                assert.strictEqual(response.getError().message, Errors.ERR_VALIDATION_FAILED.message);
                return done();
            } catch (e) {
                return done(e);
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
