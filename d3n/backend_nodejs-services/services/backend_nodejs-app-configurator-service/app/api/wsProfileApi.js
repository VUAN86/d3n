var _ = require('lodash');
var Config = require('./../config/config.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var CrudHelper = AutoMapperInstance.CrudHelper;
var ProfileApiFactory = require('./../factories/profileApiFactory.js');
var ProfileSyncService = require('./../services/profileSyncService.js');

module.exports = {

    /**
     * WS profileManager/profileSync
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    profileSync: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            var clientInstances = CrudHelper.getClientInstances(clientSession);
            ProfileSyncService.profileSync(params, clientInstances, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS profileManager/profileCreate
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    profileCreate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            ProfileApiFactory.profileUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS profileManager/profileUpdate
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    profileUpdate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            ProfileApiFactory.profileUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS profileManager/profileGet
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    profileGet: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            ProfileApiFactory.profileGet(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS profileManager/profileList
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    profileList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            ProfileApiFactory.profileList(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS profileManager/profileAddRemoveEmails
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    profileAddRemoveEmails: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            ProfileApiFactory.profileAddRemoveEmails(params, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS profileManager/profileAddRemovePhones
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    profileAddRemovePhones: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            ProfileApiFactory.profileAddRemovePhones(params, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS profileManager/profileValidateEmail
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    profileValidateEmail: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            ProfileApiFactory.profileValidateEmail(params, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS profileManager/profileValidatePhone
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    profileValidatePhone: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            ProfileApiFactory.profileValidatePhone(params, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API profileManager/addRemoveRoles
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    addRemoveRoles: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            ProfileApiFactory.addRemoveRoles(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API profileManager/addToCommunity
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    addToCommunity: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            ProfileApiFactory.addToCommunity(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },
    
    /**
     * WS API profileManager/requestCommunityAccess
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    requestCommunityAccess: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            ProfileApiFactory.requestCommunityAccess(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API profileManager/userRoleListByTenantId
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    userRoleListByTenantId: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            ProfileApiFactory.userRoleListByTenantId(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },
    
    /**
     * WS API profileManager/adminList
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    adminList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            ProfileApiFactory.adminList(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API profileManager/adminList
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    adminListByTenantId: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            ProfileApiFactory.adminListByTenantId(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },
    
    /**
     * WS API profileManager/adminAdd
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    adminAdd: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            ProfileApiFactory.adminAdd(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API profileManager/adminDelete
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    adminDelete: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            ProfileApiFactory.adminDelete(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API profileManager/inviteUser
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    inviteUser: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            ProfileApiFactory.inviteUser(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    }
}
