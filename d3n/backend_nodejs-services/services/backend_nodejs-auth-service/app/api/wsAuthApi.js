var _ = require('lodash');
var ApiFactory = require('./../factories/apiFactory.js');
var RegisterApiFactory = require('./../factories/registerApiFactory.js');
var RecoverApiFactory = require('./../factories/recoverApiFactory.js');
var ConfirmApiFactory = require('./../factories/confirmApiFactory.js');
var AuthApiFactory = require('./../factories/authApiFactory.js');
var RoleApiFactory = require('./../factories/roleApiFactory.js');

module.exports = {

    registerAnonymous: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                ApiFactory.pushParam(message, params, name);
            });
            RegisterApiFactory.registerAnonymous(params, clientSession, function (err, data) {
                ApiFactory.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            ApiFactory.handleFailure(ex, message, clientSession);
        }
    },

    registerEmail: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                ApiFactory.pushParam(message, params, name);
            });
            RegisterApiFactory.registerEmail(params, clientSession, function (err, data) {
                ApiFactory.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            ApiFactory.handleFailure(ex, message, clientSession);
        }
    },

    registerPhone: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                ApiFactory.pushParam(message, params, name);
            });
            RegisterApiFactory.registerPhone(params, clientSession, function (err, data) {
                ApiFactory.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            ApiFactory.handleFailure(ex, message, clientSession);
        }
    },

    registerFacebook: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                ApiFactory.pushParam(message, params, name);
            });
            RegisterApiFactory.registerFacebook(params, clientSession, function (err, data) {
                ApiFactory.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            ApiFactory.handleFailure(ex, message, clientSession);
        }
    },

    registerGoogle: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                ApiFactory.pushParam(message, params, name);
            });
            RegisterApiFactory.registerGoogle(params, clientSession, function (err, data) {
                ApiFactory.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            ApiFactory.handleFailure(ex, message, clientSession);
        }
    },

    registerEmailNewCode: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                ApiFactory.pushParam(message, params, name);
            });
            RegisterApiFactory.registerEmailNewCode(params, clientSession, function (err, data) {
                ApiFactory.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            ApiFactory.handleFailure(ex, message, clientSession);
        }
    },

    registerPhoneNewCode: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                ApiFactory.pushParam(message, params, name);
            });
            RegisterApiFactory.registerPhoneNewCode(params, clientSession, function (err, data) {
                ApiFactory.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            ApiFactory.handleFailure(ex, message, clientSession);
        }
    },

    inviteUserByEmail: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                ApiFactory.pushParam(message, params, name);
            });
            RegisterApiFactory.inviteUserByEmail(params, clientSession, function (err, data) {
                ApiFactory.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            ApiFactory.handleFailure(ex, message, clientSession);
        }
    },

    inviteUserByEmailAndRole: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                ApiFactory.pushParam(message, params, name);
            });
            RegisterApiFactory.inviteUserByEmailAndRole(params, clientSession, function (err, data) {
                ApiFactory.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            ApiFactory.handleFailure(ex, message, clientSession);
        }
    },

    addConfirmedUser: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                ApiFactory.pushParam(message, params, name);
            });
            RegisterApiFactory.addConfirmedUser(params, message, clientSession, function (err, data) {
                ApiFactory.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            ApiFactory.handleFailure(ex, message, clientSession);
        }
    },

    recoverPasswordEmail: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                ApiFactory.pushParam(message, params, name);
            });
            RecoverApiFactory.recoverPasswordEmail(params, clientSession, function (err, data) {
                ApiFactory.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            ApiFactory.handleFailure(ex, message, clientSession);
        }
    },

    recoverPasswordPhone: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                ApiFactory.pushParam(message, params, name);
            });
            RecoverApiFactory.recoverPasswordPhone(params, clientSession, function (err, data) {
                ApiFactory.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            ApiFactory.handleFailure(ex, message, clientSession);
        }
    },

    recoverPasswordConfirmEmail: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                ApiFactory.pushParam(message, params, name);
            });
            RecoverApiFactory.recoverPasswordConfirmEmail(params, clientSession, function (err, data) {
                ApiFactory.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            ApiFactory.handleFailure(ex, message, clientSession);
        }
    },

    recoverPasswordConfirmPhone: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                ApiFactory.pushParam(message, params, name);
            });
            RecoverApiFactory.recoverPasswordConfirmPhone(params, clientSession, function (err, data) {
                ApiFactory.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            ApiFactory.handleFailure(ex, message, clientSession);
        }
    },

    confirmEmail: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                ApiFactory.pushParam(message, params, name);
            });
            ConfirmApiFactory.confirmEmail(params, clientSession, function (err, data) {
                ApiFactory.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            ApiFactory.handleFailure(ex, message, clientSession);
        }
    },

    confirmPhone: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                ApiFactory.pushParam(message, params, name);
            });
            ConfirmApiFactory.confirmPhone(params, clientSession, function (err, data) {
                ApiFactory.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            ApiFactory.handleFailure(ex, message, clientSession);
        }
    },

    confirmRecoverPasswordEmail: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                ApiFactory.pushParam(message, params, name);
            });
            RecoverApiFactory.confirmRecoverPasswordEmail(params, clientSession, function (err, data) {
                ApiFactory.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            ApiFactory.handleFailure(ex, message, clientSession);
        }
    },

    confirmRecoverPasswordPhone: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                ApiFactory.pushParam(message, params, name);
            });
            RecoverApiFactory.confirmRecoverPasswordPhone(params, clientSession, function (err, data) {
                ApiFactory.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            ApiFactory.handleFailure(ex, message, clientSession);
        }
    },

    authEmail: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                ApiFactory.pushParam(message, params, name);
            });
            AuthApiFactory.authEmail(params, clientSession, function (err, data) {
                ApiFactory.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            ApiFactory.handleFailure(ex, message, clientSession);
        }
    },

    authPhone: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                ApiFactory.pushParam(message, params, name);
            });
            AuthApiFactory.authPhone(params, clientSession, function (err, data) {
                ApiFactory.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            ApiFactory.handleFailure(ex, message, clientSession);
        }
    },

    authFacebook: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                ApiFactory.pushParam(message, params, name);
            });
            AuthApiFactory.authFacebook(params, clientSession, function (err, data) {
                ApiFactory.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            ApiFactory.handleFailure(ex, message, clientSession);
        }
    },

    authGoogle: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                ApiFactory.pushParam(message, params, name);
            });
            AuthApiFactory.authGoogle(params, clientSession, function (err, data) {
                ApiFactory.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            ApiFactory.handleFailure(ex, message, clientSession);
        }
    },

    refresh: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                ApiFactory.pushParam(message, params, name);
            });
            AuthApiFactory.refresh(params, clientSession, function (err, data) {
                ApiFactory.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            ApiFactory.handleFailure(ex, message, clientSession);
        }
    },

    changePassword: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                ApiFactory.pushParam(message, params, name);
            });
            AuthApiFactory.changePassword(params, clientSession, function (err, data) {
                ApiFactory.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            ApiFactory.handleFailure(ex, message, clientSession);
        }
    },

    getPublicKey: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                ApiFactory.pushParam(message, params, name);
            });
            AuthApiFactory.getPublicKey(params, clientSession, function (err, data) {
                ApiFactory.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            ApiFactory.handleFailure(ex, message, clientSession);
        }
    },

    setUserRole: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                ApiFactory.pushParam(message, params, name);
            });
            RoleApiFactory.setUserRole(params, clientSession, function (err, data) {
                ApiFactory.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            ApiFactory.handleFailure(ex, message, clientSession);
        }
    },

    generateImpersonateToken: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                ApiFactory.pushParam(message, params, name);
            });
            AuthApiFactory.generateImpersonateToken(params, clientSession, function (err, data) {
                ApiFactory.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            ApiFactory.handleFailure(ex, message, clientSession);
        }
    },

};