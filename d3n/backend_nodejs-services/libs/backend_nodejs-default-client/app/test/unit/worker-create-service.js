var DefaultService = require('nodejs-default-service');
var workerConfig = JSON.parse(process.argv[2]);
workerConfig.service.validateFullMessage = false;
var testService = new DefaultService(workerConfig.service);
var ProtocolMessageClass = require('nodejs-protocol');


testService.messageHandlers['pingSleep'] = function (message, clientSession) {
    var resMessage = new ProtocolMessageClass(message);
    
    var sleep = 2000;
    
    if (message.getContent() && message.getContent().sleep) {
        sleep = message.getContent().sleep;
    }
    
    setTimeout(function () {
        resMessage.setContent('pingSleepContent');
        resMessage.setError(null);

        clientSession.sendMessage(resMessage);
    }, sleep);
};

testService.build(function (err) {
    process.send({
        success: err ? false : true
    });
});