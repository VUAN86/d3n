var _ = require('lodash');
var ProtocolMessage = require('nodejs-protocol');
var logger = require('nodejs-logger')();
var passwordGenerator = require('generate-password');

module.exports = {

    /**
     * Generate verification code
     * @param api email/phone - used for test purposes
     * @returns Verification code, containing digits, 6 symbols totally
     */
    generateVerificationCode: function (api) {
        var verificationCode = passwordGenerator.generate({
            length: 6,
            numbers: false,
            uppercase: true,
            symbols: false
        });
        /*if (api === 'email' && process.env.TEST_EMAIL_VERIFICATION_CODE) {
            verificationCode = process.env.TEST_EMAIL_VERIFICATION_CODE;
        }*/
        if (api === 'phone' && process.env.TEST_PHONE_VERIFICATION_CODE) {
            verificationCode = process.env.TEST_PHONE_VERIFICATION_CODE;
        }
        console.log('api>>', api);
        console.log('process.env.TEST_EMAIL_VERIFICATION_CODE>>', process.env.TEST_EMAIL_VERIFICATION_CODE);
        console.log('verificationCode>>', verificationCode);
        return verificationCode.toUpperCase();
    },

    /**
     * Generate password
     * @param api email/phone - used for test purposes
     * @returns Password, containing digits and uppercase alphabet symbols, 10 symbols totally
     */
    generatePassword: function (api) {
        var password = passwordGenerator.generate({
            length: 10,
            numbers: false,
            strict: true
        });
        if ((api === 'email' || api === 'phone') && process.env.TEST_RECOVER_PASSWORD) {
            password = process.env.TEST_RECOVER_PASSWORD;
        }
        return password;
    },

    /**
     * Scans for API parameters and puts it to an array if found (standard object)
     * @param data WS message or HTTP body
     * @param params Parameter array for passing to API factory
     * @param names Source API parameter name(s)
     * @param dest Destination API parameter name (optional)
     */
    pushParam: function (data, params, names, dest) {
        var content = data;
        if (ProtocolMessage.isInstance(data)) {
            content = data.getContent();
        }
        if (_.isArray(names)) {
            _.each(names, function (name) {
                if (_.has(content, name)) {
                    var param = content[name];
                    if (param) {
                        params[dest || name] = param;
                    }
                }
            });
        } else {
            var name = names;
            if (_.has(content, name)) {
                var param = content[name];
                if (param) {
                    params[dest || name] = param;
                }
            }
        }
    },

    /**
     * Scans for API parameters and puts it to an array if found (collection object)
     * @param data WS message or HTTP body
     * @param params Parameter array for passing to API factory
     * @param name Source API parameter name
     * @param dest Destination API parameter name (optional)
     */
    pushParamColl: function (data, params, name, dest) {
        var content = data;
        if (ProtocolMessage.isInstance(data)) {
            content = data.getContent();
        }
        if (_.has(content, name)) {
            var param = [];
            if (_.isArray(content[name])) {
                param = content[name];
            } else if (content[name]) {
                param = content[name].split(',');
            }
            if (param) {
                params[dest || name] = param;
            }
        } else {
            params[dest || name] = [];
        }
    },

    /**
     * Processes error returned by API factory to Default Service handler
     * Setups some parameters from incoming message: Ack, Token, Client ID
     * @param err Outgoing error ProtocomMessage
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    handleFailure: function (err, message, clientSession) {
        var self = this;
        return self._handleMessage(err, null, message, clientSession);
    },

    /**
     * Processes results returned by API factory to Default Service handler
     * Setups some parameters from incoming message: Ack, Token, Client ID
     * @param data Outgoing success ProtocomMessage
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    handleProcessed: function (err, data, message, clientSession) {
        var self = this;
        return self._handleMessage(err, data, message, clientSession);
    },

    /**
     * Passes error through API callback
     * @param err Error object / exception coming from API factory/processor
     * @param callback API callback function
     */
    callbackError: function (err, callback) {
        var self = this;
        if (ProtocolMessage.isInstance(err)) {
            logger.error('apiFactory.callbackError', err.getMessage());
        } else if (_.isObject(err)) {
            logger.error('apiFactory.callbackError', err);
        }
        return self._callback(err, null, callback);
    },

    /**
     * Passes data through API callback
     * @param data Data coming from API factory/processor
     * @param callback API callback function
     */
    callbackSuccess: function (data, callback) {
        var self = this;
        return self._callback(null, data, callback);
    },

    /**
     * Passes data / error through API callback
     * @param err Error object / exception coming from API factory
     * @param data Data object coming from API factory
     * @param callback API callback function
     */
    _callback: function (err, data, callback) {
        return setImmediate(function (err, data, callback) {
            return callback(err, data);
        }, err, data, callback);
    },

    /**
     * Processes ProtocolMessage returned by API factory to Default Service handler
     * Setups some parameters from incoming message: Ack, Token, Client ID
     * @param err Outgoing error ProtocomMessage (optional)
     * @param data Outgoing success ProtocomMessage (optional)
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    _handleMessage: function (err, data, message, clientSession) {
        var self = this;
        var pm = self._outMessage(message);
        if (err) {
            logger.error('AUTH API error(err, received message)', err, message);
            if (err && _.isObject(err) && _.has(err, 'message') && err.message) {
                pm.setError(err.message);
            } else {
                pm.setError(err);
            }
        } else if (data) {
            pm.setContent(data);
        }
        clientSession.sendMessage(pm);
    },

    /**
     * Generates initial outgoing ProtocolMessage from incoming message
     * @param message Incoming message
     * @returns Outgoing ProtocolMessage
     */
    _outMessage: function (message) {
        var pm = new ProtocolMessage(message);
        pm.setMessage(message.getMessage() + 'Response');
        pm.setContent(null);
        return pm;
    }
};