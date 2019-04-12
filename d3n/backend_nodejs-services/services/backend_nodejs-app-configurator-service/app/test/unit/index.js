process.env.CHECK_CERT="false";
process.env.TEST = 'true';
process.env.FAKE_SERVICES = 'true';
process.env.REGISTRY_SERVICE_URIS = 'ws://localhost:9093';
process.env.SKIP_PROVIDER_TOKEN_VALIDATION = 'true';
process.env.USER_MESSAGE_WAIT_FOR_RESPONSE = 'false';
process.env.VALIDATE_FULL_MESSAGE = false;
process.env.GAME_MIN_QUESTION = 1;
process.env.S3_API_BUCKET_PREFIX = 'f4m-dev-';

var WsHelper = require('./../helpers/wsHelper.js');
var HttpHelper = require('./../helpers/httpHelper.js');

// Disable app-configurator service jobs
var Config = require('./../../config/config.js');
Config.backgroundProcesses.enabled = false;
var BackgroundProcessingService = require('nodejs-background-processing').getInstance(Config);

//global.wsHelper = new WsHelper(); // direct, gateway - for Jenkins
global.wsHelper = new WsHelper(['direct']); // direct - for development
global.httpHelper = new HttpHelper(['direct']);

describe('Database Dependend', function () {
    before(function (done) {
        return setImmediate(done);
    });
    after(function (done) {
        return setImmediate(done);
    });

    before(function (done) {
        this.timeout(20000);
        global.wsHelper.build(done);
    });
    after(function (done) {
        this.timeout(20000);
        global.wsHelper.shutdown(done);
    });

    before(function (done) {
        this.timeout(20000);
        global.httpHelper.build(done);
    });
    after(function (done) {
        this.timeout(20000);
        global.httpHelper.shutdown(done);
    });
    
    beforeEach(function (done) {
        this.timeout(20000);
        global.wsHelper.sinonSandbox = global.wsHelper.sinon.sandbox.create();
        global.wsHelper.loadData(done);
    });
    afterEach(function (done) {
        this.timeout(20000);
        global.wsHelper.sinonSandbox.restore();
        done();
    });

    require('./wsTenantApi.test.js');
    require('./wsApplicationApi.test.js');
    require('./wsGameApi.test.js');
    require('./wsProfileApi.test.js');
    require('./wsVoucherApi.test.js');
    require('./wsWinningApi.test.js');
    require('./wsAchievementApi.test.js');
    require('./wsAdvertisementApi.test.js');
    require('./wsTombolaApi.test.js');
    require('./wsPromocodeApi.test.js');
    require('./scheduler.test.js');
    require('./scheduler.emitevents.test.js');
    require('./test-check-tenants-money.js');
    require('./test-scheduler-cleanup.js');
    require('./wsDummyApi.test.js');

    // HTTP api testing
    require('./httpTenantApi.test.js');

    // Service testing
    require('./profileSyncService.test.js');
});

require('./internElasticSearch.test.js');

