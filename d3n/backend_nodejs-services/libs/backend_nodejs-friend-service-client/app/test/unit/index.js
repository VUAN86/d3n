process.env.VALIDATE_FULL_MESSAGE='true';

var ip =  process.env.HOST || 'localhost';
var port = process.env.PORT ? parseInt(process.env.PORT) : 4200;
var secure = true;
var registryServiceURIs = process.env.REGISTRY_SERVICE_URIS || 'wss://localhost:4000';

var async = require('async');
var _ = require('lodash');
var nodeUrl = require('url');
var FakeRegistryService = require('../classes/FakeRegistryService.js');
var FakeFriendService = require('../classes/FakeFriendService.js');
var DefaultService = require('nodejs-default-service');
var Data = require('./_id.data.js');
var logger = require('nodejs-logger')();
var FriendServiceClient = require('../../../index.js');
var assert = require('assert');
var fs = require('fs');

var instancesConfig = [];

var friendClient;

// service registry instances
_.each(registryServiceURIs.split(','), function (uri) {
    var hostConfig = nodeUrl.parse(uri);
    instancesConfig.push({
        ip: hostConfig.hostname,
        port: hostConfig.port,
        secure: hostConfig.protocol === 'wss:',
        serviceName: 'serviceRegistry',
        registryServiceURIs: '',

        class: FakeRegistryService
    });
});

// other instances
instancesConfig.push({
    ip: ip,
    port: port,
    secure: true,
    serviceName: 'friend',
    registryServiceURIs: registryServiceURIs,

    class: FakeFriendService
});

for(var i=0; i<instancesConfig.length; i++) {
    instancesConfig[i].key = fs.readFileSync(__dirname + '/../ssl-certificate/key.pem', 'utf8');
    instancesConfig[i].cert = fs.readFileSync(__dirname + '/../ssl-certificate/cert.pem', 'utf8');
    instancesConfig[i].auth = {
        algorithm: 'RS256',
        publicKey: fs.readFileSync(__dirname + '/../jwt-keys/pubkey.pem', 'utf8')
    };
    instancesConfig[i].validateFullMessage=false;
}

var serviceInstances = [];

describe('Friend', function() {
    it('create service instances', function (done) {
        function createInstances(configs, cb) {
            async.mapSeries(configs, function (config, cbItem) {
                var cls = config.class;
                delete config.class;
                var inst = new cls(config);
                inst.build(function (err) {
                    logger.debug('build err:', err);
                    cbItem(err);
                });
            }, cb);
        };
        createInstances(instancesConfig, done);
    });

    it('create friend client instance', function () {
        friendClient = new FriendServiceClient({
            registryServiceURIs: registryServiceURIs
        });
    });


    it('groupGet', function (done) {
        friendClient.groupGet(Data.GROUP_1_ID, Data.TENANT_1_ID, Data.USER_1_ID, function (err, response) {
            try {
                var content = response.getContent();
                assert.ifError(err);
                assert.deepEqual(content.group, Data.GROUP_1);
                done();
            } catch (e) {
                done(e);
            }
        });
    });

    it('groupUpdate', function (done) {
        friendClient.groupUpdate(Data.GROUP_1_ID, Data.TENANT_1_ID, Data.USER_1_ID, 'New Group Name', 'image/path', function (err, response) {
            try {
                var content = response.getContent();
                assert.ifError(err);
                assert.deepEqual(content.group, Data.GROUP_1);
                done();
            } catch (e) {
                done(e);
            }
        });
    });

    it('playerIsGroupMember', function (done) {
        friendClient.playerIsGroupMember(Data.GROUP_1_ID, Data.TENANT_1_ID, Data.USER_1_ID, function (err, response) {
            try {
                var content = response.getContent();
                assert.ifError(err);
                assert.equal(content.userIsMember, true);
                done();
            } catch (e) {
                done(e);
            }
        });
    });
    
    it('buddyAddForUser', function (done) {
        friendClient.buddyAddForUser('1111-222', ['333-444'], undefined, function (err, response) {
            try {
                var content = response.getContent();
                assert.ifError(err);
                assert.equal(content, null);
                done();
            } catch (e) {
                done(e);
            }
        });
    });
});
