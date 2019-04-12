var _ = require('lodash');
var Config = require('./../config/config.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var CrudHelper = AutoMapperInstance.CrudHelper;
var QuestionVotingApiFactory = require('./../factories/questionVotingApiFactory.js');

module.exports = {

    /**
     * WS API questionVoting/questionVotingList
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    questionVotingList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            QuestionVotingApiFactory.questionVotingList(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },
    
    /**
     * WS questionVoting/questionVotingGet
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    questionVotingGet: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            QuestionVotingApiFactory.questionVotingGet(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },
    
    /**
     * WS questionVoting/questionVotingCreate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    questionVotingCreate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            QuestionVotingApiFactory.questionVotingUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS questionVoting/questionVotingUpdate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    questionVotingUpdate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            QuestionVotingApiFactory.questionVotingUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },
    
    /**
     * WS questionVoting/questionVotingDelete
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    questionVotingDelete: function (message, clientSession) {        
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            QuestionVotingApiFactory.questionVotingDelete(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    }
    
};