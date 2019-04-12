var ProtocolMessage = require('nodejs-protocol');

module.exports = {
    'start': function (message, clientSession) {
        var response = new ProtocolMessage(message);
        
        response.setContent({
            availableActionTypes: ['a', 'b'],
            processFinished: false
        });
        clientSession.sendMessage(response);
    },
    
    'perform': function (message, clientSession) {
        var response = new ProtocolMessage(message);
        response.setContent({
            triggeredActionType: 'www',
            availableActionTypes: ['a', 'b'],
            processFinished: false
        });
        clientSession.sendMessage(response);
    },
    
    'state': function (message, clientSession) {
        var response = new ProtocolMessage(message);
        response.setContent({
            availableActionTypes: ['a', 'b'],
            processFinished: false
        });
        clientSession.sendMessage(response);
    },
    
    'abort': function (message, clientSession) {
        var response = new ProtocolMessage(message);
        response.setContent({});
        clientSession.sendMessage(response);
    },
    
    'fakemethod': function () {
        console.log('\n\n>>>>>>>>>>>FAKE METHOD\n\n')
    }
};
