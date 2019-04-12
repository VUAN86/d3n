process.env.CHECK_CERT="false";
process.env.TEST = 'true';
process.env.FAKE_SERVICES = 'true';
process.env.REGISTRY_SERVICE_URIS = process.env.REGISTRY_SERVICE_URIS || 'ws://localhost:9093'; //'wss://127.0.0.1:8443'
process.env.SKIP_PROVIDER_TOKEN_VALIDATION = process.env.SKIP_PROVIDER_TOKEN_VALIDATION || 'true';
process.env.BLOB_BUCKET = 'f4m-test';
process.env.VALIDATE_FULL_MESSAGE = false;

var WsHelper = require('./../helpers/wsHelper.js');
//global.wsHelper = new WsHelper(); // direct, gateway - for Jenkins
global.wsHelper = new WsHelper(['direct']); // direct - for development

before(function (done) {
    this.timeout(10000);
    global.wsHelper.build(done);
});
after(function (done) {
    this.timeout(10000);
    global.wsHelper.shutdown(done);
});

beforeEach(function (done) {
    this.timeout(10000);
    global.wsHelper.sinonSandbox = global.wsHelper.sinon.sandbox.create();
    global.wsHelper.loadData(done);
});
afterEach(function (done) {
    this.timeout(10000);
    global.wsHelper.sinonSandbox.restore();
    done();
});

require('./wsLiveMessageApi.test.js');