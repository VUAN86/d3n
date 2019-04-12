var DefaultService = require('nodejs-default-service');
var workerConfig = JSON.parse(process.env.WORKER_CONFIG);
workerConfig.service.validateFullMessage = false;
var testService = new DefaultService(workerConfig.service);
var ProtocolMessage = require('nodejs-protocol');



//console.log("sdasadasdada:", process.env);process.exit();

testService.messageHandlers['pingCustom'] = function (message, clientSession) {
    var resMessage = new ProtocolMessage(message);

    resMessage.setContent({
        'evenMoreData': 'pong data'
    });
    
    clientSession.sendMessage(resMessage);
};

testService.messageHandlers['sr_register'] = function (message, clientSession) { // simulate service registry register API
    var resMessage = new ProtocolMessage(message);
    
    if (message.getContent().serviceNamespace === 'return-error') {
        setTimeout(function () {
            clientSession.sendMessage(resMessage);
        }, 1000);
    }
};


testService.build(function (err) {
    process.send({
        success: err ? false : true
    });
    
    
});