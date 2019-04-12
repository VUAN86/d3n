var DefaultService = require('nodejs-default-service');
var workerConfig = JSON.parse(process.argv[2]);
workerConfig.service.validateFullMessage = false;
var testService = new DefaultService(workerConfig.service);
var ProtocolMessageClass = require('nodejs-protocol');

if(workerConfig.ping2) {
    testService.messageHandlers['ping2'] = function (message, clientSession) {
        var resMessage = new ProtocolMessageClass(message);
        resMessage.setContent({
            'evenMoreData': 'pong data'
        });
        resMessage.setError(null);

        clientSession.sendMessage(resMessage);
    };
}

testService.messageHandlers['echo'] = function (message, clientSession) {
    try {
        var resMessage = new ProtocolMessageClass(message);
        resMessage.setContent(message.getContent());
        resMessage.setError(null);

        return clientSession.sendMessage(resMessage);
        
    } catch (e) {
        var errMessage = new ProtocolMessageClass(message);
        errMessage.setContent(null);
        errMessage.setError({
            code: 0,
            message: 'ERR_ECHO_HANDLER'
        });

        return clientSession.sendMessage(errMessage);
    }
};

testService.messageHandlers['echo2'] = function (message, clientSession) {
    try {
        var resMessage = new ProtocolMessageClass(message);
        resMessage.setContent(message.getContent());
        resMessage.setError(null);

        return clientSession.sendMessage(resMessage);
        
    } catch (e) {
        var errMessage = new ProtocolMessageClass(message);
        errMessage.setContent(null);
        errMessage.setError({
            code: 0,
            message: 'ERR_ECHO_HANDLER'
        });

        return clientSession.sendMessage(errMessage);
    }
};


testService.messageHandlers['echoSecured'] = function (message, clientSession) {
    try {
        var resMessage = new ProtocolMessageClass(message);
        
        if ( !clientSession.isAuthenticated() ) {
            resMessage.setContent(null);
            resMessage.setError({
                code: 0,
                message: 'ERR_NOT_ALLOWED'
            });

            return clientSession.sendMessage(resMessage);
        } else {
            resMessage.setContent(message.getContent());
            resMessage.setError(null);

            return clientSession.sendMessage(resMessage);
        }
        
    } catch (e) {
        var errMessage = new ProtocolMessageClass(message);
        errMessage.setContent(null);
        errMessage.setError({
            code: 0,
            message: 'ERR_ECHO_HANDLER'
        });

        return clientSession.sendMessage(errMessage);
    }
};


testService.messageHandlers['echo2Secured'] = function (message, clientSession) {
    try {
        var resMessage = new ProtocolMessageClass(message);
        
        if ( !clientSession.isAuthenticated() ) {
            resMessage.setContent(null);
            resMessage.setError({
                code: 0,
                message: 'ERR_NOT_ALLOWED'
            });

            return clientSession.sendMessage(resMessage);
        } else {
            resMessage.setContent(message.getContent());
            resMessage.setError(null);

            return clientSession.sendMessage(resMessage);
        }
        
    } catch (e) {
        var errMessage = new ProtocolMessageClass(message);
        errMessage.setContent(null);
        errMessage.setError({
            code: 0,
            message: 'ERR_ECHO_HANDLER'
        });

        return clientSession.sendMessage(errMessage);
    }
};


testService.messageHandlers['pingSecured'] = function (message, clientSession) {
    var resMessage = new ProtocolMessageClass(message);
    
    if ( !clientSession.isAuthenticated() ) {
        resMessage.setContent(null);
        resMessage.setError({
            code: 0,
            message: 'ERR_NOT_ALLOWED'
        });
        
        return clientSession.sendMessage(resMessage);
    }

    resMessage.setContent(message.getContent());
    resMessage.setError(null);

    return clientSession.sendMessage(resMessage);
};

testService.messageHandlers['pingSecuredIncludeToken'] = function (message, clientSession) {
    var resMessage = new ProtocolMessageClass(message);
    
    if ( !clientSession.isAuthenticated() ) {
        resMessage.setContent(null);
        resMessage.setError({
            code: 0,
            message: 'ERR_NOT_ALLOWED'
        });
        
        return clientSession.sendMessage(resMessage);
    }

    resMessage.setContent({
        token: clientSession.getAuthentication().getTokenPayload()
    });
    
    resMessage.setError(null);

    return clientSession.sendMessage(resMessage);
};

testService.messageHandlers['needAdminRole'] = function (message, clientSession) {
    var resMessage = new ProtocolMessageClass(message);
    
    var tokenPayload = clientSession.getAuthentication().getTokenPayload() || {};
    //console.log('tokenPayload', tokenPayload);
    if ( tokenPayload.roles.indexOf('admin') === -1 ) {
        resMessage.setContent(null);
        resMessage.setError({
            code: 0,
            message: 'ERR_ADMIN_ROLE_NEEDED'
        });
        
        return clientSession.sendMessage(resMessage);
    }

    resMessage.setContent(message.getContent());
    resMessage.setError(null);

    return clientSession.sendMessage(resMessage);
};

testService.messageHandlers['needManagerRole'] = function (message, clientSession) {
    var resMessage = new ProtocolMessageClass(message);
    
    var tokenPayload = clientSession.getAuthentication().getTokenPayload() || {};
    if ( tokenPayload.roles.indexOf('manager') === -1 ) {
        resMessage.setContent(null);
        resMessage.setError({
            code: 0,
            message: 'ERR_SUPER_ADMIN_ROLE_NEEDED'
        });
        
        return clientSession.sendMessage(resMessage);
    }

    resMessage.setContent(message.getContent());
    resMessage.setError(null);

    return clientSession.sendMessage(resMessage);
};


testService.build(function (err) {
    //console.log(testService.config);
    process.send({
        success: err ? false : true
    });
    
    
});