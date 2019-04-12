var _ = require('lodash');
var Errors = require('./../config/errors.js');
var Config = require('./../config/config.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var CrudHelper = AutoMapperInstance.CrudHelper;
var PoolApiFactory = require('./../factories/poolApiFactory.js');

module.exports = {

    /**
     * WS question/poolCreate
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    poolCreate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            PoolApiFactory.poolUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS question/poolUpdate
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    poolUpdate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            PoolApiFactory.poolUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS question/poolDelete
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    poolDelete: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            PoolApiFactory.poolDelete(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS question/poolGet
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    poolGet: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            PoolApiFactory.poolGet(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS question/poolList
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    poolList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            PoolApiFactory.poolList(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API question/poolActivate
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    poolActivate: function (message, clientSession) {
       var params = {};
       try {
           _.each(_.keys(message.getContent()), function (name) {
               CrudHelper.pushParam(message, clientSession, params, name);
           });
           params.status = 'active';
           PoolApiFactory.poolSetStatus(params, clientSession, function (err, data) {
               CrudHelper.handleProcessed(err, data, message, clientSession);
           });
       } catch (ex) {
           CrudHelper.handleFailure(ex, message, clientSession);
       }
    },

    /**
     * WS API question/poolDeactivate
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    poolDeactivate: function (message, clientSession) {
       var params = {};
       try {
           _.each(_.keys(message.getContent()), function (name) {
               CrudHelper.pushParam(message, clientSession, params, name);
           });
           params.status = 'inactive';
           PoolApiFactory.poolSetStatus(params, clientSession, function (err, data) {
               CrudHelper.handleProcessed(err, data, message, clientSession);
           });
       } catch (ex) {
           CrudHelper.handleFailure(ex, message, clientSession);
       }
    },

    /**
     * WS API pool/poolQuestionStatistic
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    poolQuestionStatistic: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            params.includeStatistic = true;
            PoolApiFactory.poolGet(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API pool/poolQuestionStatisticList
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    poolQuestionStatisticList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            params.includeStatistic = true;
            PoolApiFactory.poolList(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API question/poolAddDeleteQuestion
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    poolAddDeleteQuestion: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            PoolApiFactory.poolAddDeleteQuestion(params, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API question/poolAddDeleteMedia
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    poolAddDeleteMedia: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            PoolApiFactory.poolAddDeleteMedia(params, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },
}