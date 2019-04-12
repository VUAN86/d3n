var _ = require('lodash');
var Errors = require('./../config/errors.js');
var Config = require('./../config/config.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var CrudHelper = AutoMapperInstance.CrudHelper;
var BillingService = require('./../services/billingService.js');
var BillingApiFactory = require('./../factories/billingApiFactory.js');
var Database = require('nodejs-database').getInstance(Config);
var PaymentAction = Database.RdbmsService.Models.Billing.PaymentAction;
var PaymentTenantBillOfMaterial = Database.RdbmsService.Models.Billing.PaymentTenantBillOfMaterial;
var PaymentResourceBillOfMaterial = Database.RdbmsService.Models.Billing.PaymentResourceBillOfMaterial;

module.exports = {

    /**
     * WS API billing/paymentStructureCreate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    paymentStructureCreate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            BillingApiFactory.paymentStructureUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API billing/paymentStructureUpdate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    paymentStructureUpdate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            BillingApiFactory.paymentStructureUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API billing/paymentStructureDelete
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    paymentStructureDelete: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            BillingApiFactory.paymentStructureDelete(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API billing/paymentStructureGet
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    paymentStructureGet: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            BillingApiFactory.paymentStructureGet(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API billing/paymentStructureList
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    paymentStructureList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            BillingApiFactory.paymentStructureList(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API billing/paymentStructureActivate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    paymentStructureActivate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            params.isActive = true;
            BillingApiFactory.paymentStructureSetActive(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API billing/paymentStructureDeactivate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    paymentStructureDeactivate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            params.isActive = false;
            BillingApiFactory.paymentStructureSetActive(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API billing/paymentStructureTierCreate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    paymentStructureTierCreate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            BillingApiFactory.paymentStructureTierUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API billing/paymentStructureTierUpdate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    paymentStructureTierUpdate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            BillingApiFactory.paymentStructureTierUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API billing/paymentStructureTierDelete
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    paymentStructureTierDelete: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            BillingApiFactory.paymentStructureTierDelete(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API billing/paymentStructureTierGet
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    paymentStructureTierGet: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            BillingApiFactory.paymentStructureTierGet(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API billing/paymentStructureTierList
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    paymentStructureTierList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            BillingApiFactory.paymentStructureTierList(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API billing/paymentTenantBillOfMaterialCreate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    paymentTenantBillOfMaterialCreate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            BillingApiFactory.paymentTenantBillOfMaterialUpdate(params, message, clientSession, function (err, data) { 
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API billing/paymentTenantBillOfMaterialUpdate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    paymentTenantBillOfMaterialUpdate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            BillingApiFactory.paymentTenantBillOfMaterialUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API billing/paymentTenantBillOfMaterialGet
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    paymentTenantBillOfMaterialGet: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            BillingApiFactory.paymentTenantBillOfMaterialGet(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API billing/paymentTenantBillOfMaterialList
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    paymentTenantBillOfMaterialList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            BillingApiFactory.paymentTenantBillOfMaterialList(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API billing/paymentTenantBillOfMaterialApprove
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    paymentTenantBillOfMaterialApprove: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            params.status = PaymentTenantBillOfMaterial.constants().STATUS_APPROVED;
            BillingApiFactory.paymentTenantBillOfMaterialSetStatus(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API billing/paymentTenantBillOfMaterialReject
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    paymentTenantBillOfMaterialReject: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            params.status = PaymentTenantBillOfMaterial.constants().STATUS_UNAPPROVED;
            BillingApiFactory.paymentTenantBillOfMaterialSetStatus(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API billing/paymentTenantBillOfMaterialPay
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    paymentTenantBillOfMaterialPay: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            BillingApiFactory.paymentTenantBillOfMaterialPay(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API billing/paymentResourceBillOfMaterialCreate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    paymentResourceBillOfMaterialCreate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            BillingApiFactory.paymentResourceBillOfMaterialUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API billing/paymentResourceBillOfMaterialUpdate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    paymentResourceBillOfMaterialUpdate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            BillingApiFactory.paymentResourceBillOfMaterialUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API billing/paymentResourceBillOfMaterialGet
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    paymentResourceBillOfMaterialGet: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            BillingApiFactory.paymentResourceBillOfMaterialGet(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API billing/paymentResourceBillOfMaterialList
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    paymentResourceBillOfMaterialList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            BillingApiFactory.paymentResourceBillOfMaterialList(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API billing/paymentResourceBillOfMaterialApprove
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    paymentResourceBillOfMaterialApprove: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            params.status = PaymentResourceBillOfMaterial.constants().STATUS_APPROVED;
            BillingApiFactory.paymentResourceBillOfMaterialSetStatus(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API billing/paymentResourceBillOfMaterialReject
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    paymentResourceBillOfMaterialReject: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            params.status = PaymentResourceBillOfMaterial.constants().STATUS_UNAPPROVED;
            BillingApiFactory.paymentResourceBillOfMaterialSetStatus(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API billing/paymentResourceBillOfMaterialPay
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    paymentResourceBillOfMaterialPay: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            BillingApiFactory.paymentResourceBillOfMaterialPay(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API billing/paymentActionCreate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    paymentActionCreate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            BillingApiFactory.paymentActionUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API billing/paymentActionUpdate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    paymentActionUpdate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            BillingApiFactory.paymentActionUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API billing/paymentActionGet
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    paymentActionGet: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            BillingApiFactory.paymentActionGet(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API billing/paymentActionList
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    paymentActionList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            BillingApiFactory.paymentActionList(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API billing/paymentActionActivate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    paymentActionActivate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            params.status = PaymentAction.constants().STATUS_ACTIVE;
            BillingApiFactory.paymentActionSetStatus(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API billing/paymentActionDeactivate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    paymentActionDeactivate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            params.status = PaymentAction.constants().STATUS_INACTIVE;
            BillingApiFactory.paymentActionSetStatus(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API billing/batchBillCalculation
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    batchBillCalculation: function (message, clientSession) {
        try {
            BillingService.batchBillCalculation(function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

};