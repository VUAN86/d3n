var Config = require('../../config/config.js'),
    ProtocolDebugger = require('../../classes/ProtocolDebugger.js');

var service = new ProtocolDebugger(Config);
describe('Testing protocol debugger', function() {

    it('should start up', function (done) {
        service.build(done);
    });
    it('do not crash on empty broadcast', function (done) {
        service._broadcastWebsocket("stuff");
        done();
    });
    it('do not crash on winston log empty message', function (done) {
        service._winstonLog({connection:{remoteAddress:""},body:{params:{message:{}}}});
        done();
    });
    it('do not crash on winston log wrong message', function (done) {
        service._winstonLog({connection:{remoteAddress:""},body:{params:{message:""}}});
        done();
    });
    it('do not crash on winston log no message', function (done) {
        service._winstonLog({connection:{remoteAddress:""},body:{params:{}}});
        done();
    });
    it('should shut down', function (done) {
        service.shutdown();
        done();
    });
});