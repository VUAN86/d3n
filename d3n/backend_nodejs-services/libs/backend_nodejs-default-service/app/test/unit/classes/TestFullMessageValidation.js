var DefaultService = require('../../../classes/Service.js');
var util = require('util');
var _ = require('lodash');
var logger = require('nodejs-logger')();
var ProtocolMessage = require('nodejs-protocol');
var fs = require('fs');


function TestFullMessageValidation (config) {
    DefaultService.call(this, config);
};

util.inherits(TestFullMessageValidation, DefaultService);

var o = TestFullMessageValidation.prototype;


o.messageHandlers = _.assign(_.clone(o.messageHandlers), {
    
    'languageGet': function (message, clientSession) {
        var resMessage = new ProtocolMessage(message);
        resMessage.setError(null);
        clientSession.sendMessage(resMessage);
    },
    
    'clientDisconnect': function (message, clientSession) { // sent by gateway
        var file = this.config.outFile;
        fs.writeFileSync(file, 'clientDisconnectCalled', 'utf8');
    }    
});

function _errorMessage (reqMessage, err) {
    var errMesage = new ProtocolMessage(reqMessage);
    errMesage.setError(err);
};

module.exports = TestFullMessageValidation;