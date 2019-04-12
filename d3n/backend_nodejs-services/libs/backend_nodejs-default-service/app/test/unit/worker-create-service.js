var DefaultService = require('../../classes/Service.js'),
    ProtocolMessage = require('nodejs-protocol')
;

module.exports = function(config) {
    var testService = new DefaultService(config);
    testService.messageHandlers['pingCustom'] = function (message, clientSession) {
        var resMessage = new ProtocolMessage(message);

        resMessage.setContent({
            'evenMoreData': 'pong data'
        });
        resMessage.setError(null);

        clientSession.sendMessage(resMessage);
    };

    testService.messageHandlers['pingSecured'] = function (message, clientSession) {
        var resMessage = new ProtocolMessage(message);

        if ( !clientSession.isAuthenticated() ) {
            resMessage.setError({
                code: 0,
                message: 'ERR_NOT_ALLOWED'
            });
            return clientSession.sendMessage(resMessage);
        }

        resMessage.setContent({
            'evenMoreData': 'pong data'
        });
        resMessage.setError(null);

        clientSession.sendMessage(resMessage);
    };


    //=========================================================================
    // Test messages for broadcast system

    testService.messageHandlers['enterRoom'] = function (message, clientSession) {
        var content = message.getContent();
        var added = this._broadcastSystem.add(content.room, clientSession);

        var resMessage = new ProtocolMessage(message);
        resMessage.setContent({
            response: added
        });
        clientSession.sendMessage(resMessage);
    };

    testService.messageHandlers['leaveRoom'] = function (message, clientSession) {
        var content = message.getContent();
        this._broadcastSystem.remove(content.room, clientSession);

        var resMessage = new ProtocolMessage(message);
        resMessage.setContent({});
        clientSession.sendMessage(resMessage);
    };

    testService.messageHandlers['leaveAllRoom'] = function (message, clientSession) {
        var content = message.getContent();
        this._broadcastSystem.removeFromAllRooms(clientSession);

        var resMessage = new ProtocolMessage(message);
        resMessage.setContent({});
        clientSession.sendMessage(resMessage);
    };

    testService.messageHandlers['isInRoom'] = function (message, clientSession) {
        var content = message.getContent();
        var isIn = this._broadcastSystem.isInRoom(content.room, clientSession);

        var resMessage = new ProtocolMessage(message);
        resMessage.setContent({
            response: isIn
        });
        clientSession.sendMessage(resMessage);
    };

    testService.messageHandlers['sendMessageToRooms'] = function (message, clientSession) {
        var content = message.getContent();

        var fakeMessage = new ProtocolMessage();
        fakeMessage.setMessage("test_service/"+content.messageName);
        this._broadcastSystem.sendMessage(content.room, fakeMessage);

        var resMessage = new ProtocolMessage(message);
        resMessage.setContent({});
        clientSession.sendMessage(resMessage);
    };

    testService.messageHandlers['getUsersByRoom'] = function (message, clientSession) {
        var content = message.getContent();
        var sessions = this._broadcastSystem.getUsersByRoom(content.room);

        var result = [];
        for (var i = 0;i < sessions.length; i++) {
            result.push(sessions[i].getId());
        }
        var resMessage = new ProtocolMessage(message);
        resMessage.setContent( { response: result });
        clientSession.sendMessage(resMessage);
    };

    //=========================================================================
    // Test messages for data storage system

    testService.messageHandlers['saveToStorage'] = function (message, clientSession) {
        var content = message.getContent();
        clientSession.getDataStorage().set(content.key, content.value);

        var resMessage = new ProtocolMessage(message);
        resMessage.setContent({});
        clientSession.sendMessage(resMessage);
    };

    testService.messageHandlers['loadFromStorage'] = function (message, clientSession) {
        var content = message.getContent();
        var result = clientSession.getDataStorage().get(content.key);
        var resMessage = new ProtocolMessage(message);
        resMessage.setContent( { response: result });
        clientSession.sendMessage(resMessage);
    };

    return testService;
};
