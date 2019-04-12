process.env.CHECK_CERT="false";
process.env.TEST = 'true';
process.env.FAKE_SERVICES = 'true';
process.env.REGISTRY_SERVICE_URIS = 'ws://localhost:9203';
process.env.SKIP_PROVIDER_TOKEN_VALIDATION = 'true';
process.env.VALIDATE_FULL_MESSAGE = true;
process.env.VALIDATE_PERMISSIONS = true;

var WsHelper = require('./../helpers/wsHelper.js');
var DataIds = require('./../config/_id.data.js');
var Config = require('./../../config/config.js');
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikeGlobalClientSession = KeyvalueService.Models.AerospikeGlobalClientSession;
var logger = require('nodejs-logger')();

//global.wsHelper = new WsHelper(); // direct, gateway - for Jenkins
global.wsHelper = new WsHelper(['direct']); // direct - for development

before(function (done) {
    this.timeout(15000)
    global.wsHelper.build(done);
});

before(function (done) {
    var clientSession = {
        clientId: DataIds.CLIENT_SESSION_ID,
        gatewayURL: 'wss://localhost:80',
        appConfig:   {
            appId: 'appid',
            deviceUUID: 'deviceUUID',
            tenantId: DataIds.TENANT_1_ID,
            device: {
                a: 'aa',
                b: 'bb'
            }
        }
    };
    AerospikeGlobalClientSession.addOrUpdateClientSession(DataIds.LOCAL_USER_ID, clientSession, function (err) {
        if (err) {
            logger.error("Could not update client session ", err);
            return done(err);
        }
        logger.debug("Session init ->", JSON.stringify({userId: DataIds.LOCAL_USER_ID, clientSession: clientSession }));
        return done();
    });
});

beforeEach(function (done) {
    this.timeout(15000)
    global.wsHelper.loadData(done);
});
after(function (done) {
    this.timeout(15000)
    global.wsHelper.shutdown(done);
});

require('./wsMediaApi.test.js');
require('./httpMediaApi.test.js');
require('./httpUpdateProfileApi.test.js');
require('./httpDocumentApi.test.js');
require('./httpUpdateGroupApi.test.js');
require('./uploadApiFactory.test.js');
