var _ = require('lodash');
var Errors = require('./../config/errors.js');
var Config = require('./../config/config.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var CrudHelper = AutoMapperInstance.CrudHelper;
var WorkorderApiFactory = require('./../factories/workorderApiFactory.js');

module.exports = {

    /**
     * WS API workorder/workorderCreate
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    workorderCreate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            WorkorderApiFactory.workorderUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },
    
    /**
     * WS API workorder/workorderUpdate
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    workorderUpdate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            WorkorderApiFactory.workorderUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API workorder/workorderDelete
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    workorderDelete: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            WorkorderApiFactory.workorderDelete(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API workorder/workorderGet
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    workorderGet: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            WorkorderApiFactory.workorderGet(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API workorder/workorderList
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    workorderList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            WorkorderApiFactory.workorderList(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API workorder/workorderByResourceList
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    workorderByResourceList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            WorkorderApiFactory.workorderByResourceList(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API workorder/workorderActivate
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    workorderActivate: function (message, clientSession) {
        var params = {};
        var self = this;
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            params.status = 'inprogress';
            WorkorderApiFactory.workorderSetStatus(params, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
                if (!err) {
                    self.emitEvent('workorderActivate', clientSession, params.id);
                }
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API workorder/workorderDeactivate
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    workorderDeactivate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            params.status = 'inactive';
            WorkorderApiFactory.workorderSetStatus(params, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API workorder/workorderDeactivate
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    workorderClose: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            WorkorderApiFactory.workorderClose(params, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API workorder/workorderAssignRemoveResources
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    workorderAssignRemoveResources: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            WorkorderApiFactory.workorderAssignRemoveResources(params, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API workorder/workorderGetResources
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    workorderGetResources: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            WorkorderApiFactory.workorderGetResources(params, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API workorder/workorderQuestionStatistic
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    workorderQuestionStatistic: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            params.includeStatistic = true;
            WorkorderApiFactory.workorderGet(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API workorder/workorderQuestionStatisticList
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    workorderQuestionStatisticList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            params.includeStatistic = true;
            WorkorderApiFactory.workorderList(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },
}