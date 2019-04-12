process.env.TEST = 'true';
process.env.FAKE_SERVICES = 'true';
process.env.VALIDATE_FULL_MESSAGE = 'false';

var WsHelper = require('./../helpers/wsHelper.js');

global.wsHelper = new WsHelper();

before(function (done) {
    global.wsHelper.build(done);
});
after(function (done) {
    global.wsHelper.shutdown(done);
});

require('./clientService.test.js');