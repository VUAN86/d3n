var ProtocolMessage = require('nodejs-protocol');
var logger = require('nodejs-logger')();
var Errors = require('nodejs-errors');
/**
 * Removes client from the client session
 * @param {ProtocolMessage} message
 * @param {ClientSession} clientSession
 */
var defaultClientDisconnectHandler = function (message, clientSession) {
    clientSession.getConnectionSession().deleteClientSession(clientSession.getId());
};


module.exports = {
    'ping': function (message, clientSession) {
        var resMessage = new ProtocolMessage(message);
        resMessage.setContent({
            'evenMoreData': 'pong data'
        });
        resMessage.setError(null);

        clientSession.sendMessage(resMessage);
    },

    'clientDisconnect': function (message, clientSession) { // sent by gateway
        // call default handler
        defaultClientDisconnectHandler(message, clientSession);
    },
    // heartbeat message sent by registry service. just replay
    'heartbeat': function (message, clientSession) {
        var resMessage = new ProtocolMessage(message);
        resMessage.setContent(null);
        resMessage.setError(null);
        
        clientSession.sendMessage(resMessage);
    },
    
    'auth': function (message, clientSession) {
        //Nothing to do anymore, as this should have been done already by the token check
        var resMessage = new ProtocolMessage(message);
        resMessage.setContent(null);
        resMessage.setError(null);

        clientSession.sendMessage(resMessage);
    }
};
