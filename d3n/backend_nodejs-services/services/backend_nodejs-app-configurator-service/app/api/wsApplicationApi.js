var _ = require('lodash');
var Errors = require('./../config/errors.js');
var Config = require('./../config/config.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var CrudHelper = AutoMapperInstance.CrudHelper;
var ApplicationApiFactory = require('./../factories/applicationApiFactory.js');
var Database = require('nodejs-database').getInstance(Config);
var Application = Database.RdbmsService.Models.Application.Application;
var ApplicationHasGame = Database.RdbmsService.Models.Application.ApplicationHasGame;
var logger = require('nodejs-logger')();

module.exports = {

    /**
     * WS API application/applicationList
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    applicationList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            ApplicationApiFactory.applicationList(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            logger.error('applicationList error: ', ex);
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },
    
    /**
     * WS application/applicationGet
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    applicationGet: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            ApplicationApiFactory.applicationGet(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            logger.error('applicationGet error: ', ex);
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS application/applicationCreate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    applicationCreate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            ApplicationApiFactory.applicationUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            logger.error('applicationCreate error: ', ex, ' message:"',message,'"');
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },
    
    /**
     * WS application/applicationUpdate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    applicationUpdate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            ApplicationApiFactory.applicationUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            logger.error('applicationUpdate error: ', ex);
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },
    
    /**
     * WS application/applicationDelete
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    applicationDelete: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            ApplicationApiFactory.applicationDelete(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            logger.error('applicationDelete error: ', ex);
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS application/applicationActivate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    applicationActivate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            ApplicationApiFactory.applicationPublish(params, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            logger.error('applicationActivate error: ', ex);
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS application/applicationDeactivate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    applicationDeactivate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            ApplicationApiFactory.applicationUnpublish(params, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            logger.error('applicationDeactivate error: ', ex);
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS application/applicationArchive
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    applicationArchive: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            params.status = Application.constants().STATUS_ARCHIVED;
            ApplicationApiFactory.applicationSetStatus(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            logger.error('applicationArchive error: ', ex);
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API application/applicationAnimationList
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    applicationAnimationList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            ApplicationApiFactory.applicationAnimationList(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            logger.error('applicationAnimationList error: ', ex);
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API application/applicationCharacterList
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    applicationCharacterList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            ApplicationApiFactory.applicationCharacterList(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS application/applicationAnimationActivate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    applicationAnimationActivate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            params.status = 'active';
            ApplicationApiFactory.applicationAnimationSetStatus(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS application/applicationAnimationDeactivate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    applicationAnimationDeactivate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            params.status = 'inactive';
            ApplicationApiFactory.applicationAnimationSetStatus(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS application/applicationCharacterActivate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    applicationCharacterActivate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            params.status = 'active';
            ApplicationApiFactory.applicationCharacterSetStatus(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS application/applicationCharacterDeactivate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    applicationCharacterDeactivate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            params.status = 'inactive';
            ApplicationApiFactory.applicationCharacterSetStatus(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS application/gameModuleActivate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    gameModuleActivate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            params.status = Application.constants().STATUS_ACTIVE;
            ApplicationApiFactory.gameModuleSetStatus(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS application/gameModuleDeactivate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    gameModuleDeactivate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            params.status = Application.constants().STATUS_INACTIVE;
            ApplicationApiFactory.gameModuleSetStatus(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS application/applicationGameAdd
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    applicationGameAdd: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            ApplicationApiFactory.applicationGameAdd(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS application/applicationGameRemove
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    applicationGameRemove: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            ApplicationApiFactory.applicationGameRemove(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

};