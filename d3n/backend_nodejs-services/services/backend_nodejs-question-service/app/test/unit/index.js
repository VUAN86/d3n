process.env.CHECK_CERT="false";
process.env.TEST = 'true';
process.env.FAKE_SERVICES = process.env.FAKE_SERVICES || 'true';
process.env.REGISTRY_SERVICE_URIS = process.env.REGISTRY_SERVICE_URIS || 'ws://localhost:9093'; //'wss://127.0.0.1:8443'
process.env.SKIP_PROVIDER_TOKEN_VALIDATION = process.env.SKIP_PROVIDER_TOKEN_VALIDATION || 'true';
process.env.BLOB_BUCKET = 'f4m-test';
process.env.VALIDATE_FULL_MESSAGE = false;
process.env.WORKFLOW_ENABLED='false';

var WsHelper = require('./../helpers/wsHelper.js');
//global.wsHelper = new WsHelper(); // direct, gateway - for Jenkins
global.wsHelper = new WsHelper(['direct']); // direct - for development

// Disable app-configurator service jobs
var Config = require('./../../config/config.js');
Config.backgroundProcesses.enabled = false;
var BackgroundProcessingService = require('nodejs-background-processing').getInstance(Config);

before(function (done) {
    this.timeout(10000);
    global.wsHelper.build(done);
});
after(function (done) {
    this.timeout(10000);
    global.wsHelper.shutdown(done);
});

beforeEach(function (done) {
    process.env.WORKFLOW_ENABLED = 'false';
    this.timeout(10000);
    global.wsHelper.sinonSandbox = global.wsHelper.sinon.sandbox.create();
    global.wsHelper.loadData(done);
});
afterEach(function (done) {
    this.timeout(10000);
    global.wsHelper.sinonSandbox.restore();
    done();
});

require('./wsQuestionReportIssue.test');
require('./wsQuestionApi.test.js');
require('./wsQuestionVotingApi.test.js');
require('./wsQuestionTranslationApi.test.js');
require('./wsQuestionTemplateApi.test.js');
require('./wsWorkorderApi.test.js'); 
require('./wsWorkorderEvent.test.js');
require('./wsWorkorderBillingApi.test.js');
require('./wsRegionalSettingApi.test');
require('./wsPoolApi.test.js');
require('./wsBillingApi.test.js');
require('./workflow-question.test.js');
require('./workflow-translation.test.js');
require('./workflow-question-unpublish.test.js');
require('./workflow-translation-unpublish.test.js');
require('./workflow-list-events.test.js');
require('./workflow-question-archive-test.js');
