var _ = require('lodash');
var async = require('async');
var KeyvalueService = require('nodejs-aerospike').getInstance().KeyvalueService;
var AerospikeAnalyticEvent = KeyvalueService.Models.AerospikeAnalyticEvent;

module.exports = {
    
    addQuestionEvent: function (clientInfo, eventData, cb) {
        try {
            var ed = _.assign({
                questionsCreated: 0,
                questionsTranslated: 0,
                questionsReviewed: 0,
                questionsApprovedReviewed: 0,
                questionsRated: 0
            }, eventData);
            
            
            var content = {
                eventType: 'de.ascendro.f4m.server.analytics.model.QuestionFactoryEvent',
                userId: clientInfo.userId || null,
                appId: clientInfo.appId || null,
                sessionIp: clientInfo.sessionIp || null,
                eventData: ed
            };
            
            var key = 'question' + clientInfo.sessionId + parseInt(Date.now()/1000);
            
            AerospikeAnalyticEvent.add({
                key: key,
                timestamp: Date.now(),
                content: content
            }, cb);
        } catch (e) {
            return setImmediate(cb, e);
        }
    }
    
    
};