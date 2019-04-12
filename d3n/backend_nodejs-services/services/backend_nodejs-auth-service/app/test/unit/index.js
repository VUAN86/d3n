process.env.CHECK_CERT="false";
process.env.TEST = 'true';
process.env.FAKE_SERVICES = 'true';
process.env.REGISTRY_SERVICE_URIS = process.env.REGISTRY_SERVICE_URIS || 'ws://localhost:9093'; //'wss://127.0.0.1:8443'
process.env.SKIP_PROVIDER_TOKEN_VALIDATION = 'true';
process.env.TEST_RECOVER_PASSWORD = process.env.TEST_RECOVER_PASSWORD || '_int3rnal$';
process.env.TEST_EMAIL_VERIFICATION_CODE = process.env.TEST_EMAIL_VERIFICATION_CODE || '123456';
process.env.TEST_PHONE_VERIFICATION_CODE = process.env.TEST_PHONE_VERIFICATION_CODE || '654321';
process.env.TEST_EMAIL_VERIFICATION_CODE_NEW = process.env.TEST_EMAIL_VERIFICATION_CODE;
process.env.TEST_PHONE_VERIFICATION_CODE_NEW = process.env.TEST_PHONE_VERIFICATION_CODE;
process.env.USER_MESSAGE_WAIT_FOR_RESPONSE = 'false';
process.env.VALIDATE_FULL_MESSAGE = 'false';
 
var WsHelper = require('./../helpers/wsHelper.js');

//global.wsHelper = new WsHelper(); // direct, gateway - for Jenkins
global.wsHelper = new WsHelper(['direct']); // direct - for development

before(function (done) {
    this.timeout(10000);
    global.wsHelper.build(done);
});
beforeEach(function () {
    global.wsHelper._profiles = undefined;
    global.wsHelper._profileBlobs = undefined;
});
after(function (done) {
    this.timeout(10000);
    global.wsHelper.shutdown(done);
});

require('./wsAuthApi.test.js');