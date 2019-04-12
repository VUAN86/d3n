var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikeAnalyticEvent = KeyvalueService.Models.AerospikeAnalyticEvent;

module.exports = {

    _issueEvent: function (clientInfo, eventData, callback) {
        try {
            var key = 'auth' + clientInfo.sessionId + parseInt(Date.now() / 1000);
            var content = {
                eventType: 'de.ascendro.f4m.server.analytics.model.UserRegistrationEvent',
                userId: clientInfo.userId || null,
                appId: clientInfo.appId || null,
                tenantId: clientInfo.tenantId || null,
                sessionIp: clientInfo.sessionIp || null,
                eventData: eventData
            };
            return AerospikeAnalyticEvent.add({
                key: key,
                timestamp: Date.now(),
                content: content
            }, callback);
        } catch (ex) {
            return setImmediate(callback, ex);
        }
    },

    issueRegisteredEvent: function (clientInfo, callback) {
        var self = this;
        return self._issueEvent(clientInfo, { registered: true }, callback);
    },

    issueFullyRegisteredEvent: function (clientInfo, callback) {
        var self = this;
        return self._issueEvent(clientInfo, { fullyRegistered: true }, callback);
    },

};