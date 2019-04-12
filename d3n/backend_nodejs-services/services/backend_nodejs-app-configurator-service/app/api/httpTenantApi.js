var _ = require('lodash');
var async = require('async');
var logger = require('nodejs-logger')();
var Config = require('./../config/config.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var CrudHelper = AutoMapperInstance.CrudHelper;
var StandardErrors = require('nodejs-errors');
var TenantApiFactory = require('./../factories/tenantManagementConnectorApiFactory.js');
var TenantShopApiFactory = require('./../factories/tenantShopApiFactory.js');

module.exports = {
    tenantAdminContractCreate: function (req, res) {
        try {
            TenantApiFactory.tenantAdminContractCreate(req, res, function (err, data) {
                CrudHelper.handleHttpResponse(err, data, res);
            });
        } catch (ex) {
            CrudHelper.handleHttpResponse(ex, null, res);
        }
    },
    
    tenantGet: function (req, res) {
        try {
            TenantApiFactory.tenantGet(req, res, function (err, data) {
                CrudHelper.handleHttpResponse(err, data, res);
            });
        } catch (ex) {
            CrudHelper.handleHttpResponse(ex, null, res);
        }
    },

    tenantUpdate: function (req, res) {
        try {
            TenantApiFactory.tenantUpdate(req, res, function (err, data) {
                if (!err) {
                    //sync tenant in shop
                    TenantShopApiFactory.tenantUpdate(req, function (err, data) {
                        //do nothing
                    });
                }
                CrudHelper.handleHttpResponse(err, data, res);
            });

            //
        } catch (ex) {
            CrudHelper.handleHttpResponse(ex, null, res);
        }
    },

    tenantConfigurationUpdate: function (req, res) {
        try {
            TenantApiFactory.tenantConfigurationUpdate(req, res, function (err, data) {
                CrudHelper.handleHttpResponse(err, data, res);
            });
        } catch (ex) {
            CrudHelper.handleHttpResponse(ex, null, res);
        }
    },

    tenantPaydentApiKeyUpdate: function (req, res) {
        try {
            TenantApiFactory.tenantPaydentApiKeyUpdate(req, res, function (err, data) {
                CrudHelper.handleHttpResponse(err, data, res);
            });
        } catch (ex) {
            CrudHelper.handleHttpResponse(ex, null, res);
        }
    },

    tenantExchangeCurrencyUpdate: function (req, res) {
        try {
            TenantApiFactory.tenantExchangeCurrencyUpdate(req, res, function (err, data) {
                CrudHelper.handleHttpResponse(err, data, res);
            });
        } catch (ex) {
            CrudHelper.handleHttpResponse(ex, null, res);
        }
    },

    tenantBulkUpdate: function (req, res) {
        try {
            TenantApiFactory.tenantBulkUpdate(req, res, function (err, data) {
                CrudHelper.handleHttpResponse(err, data, res);
            });
        } catch (ex) {
            CrudHelper.handleHttpResponse(ex, null, res);
        }
    },

    tenantList: function (req, res) {
        try {
            TenantApiFactory.tenantList(req, res, function (err, data) {
                CrudHelper.handleHttpResponse(err, data, res);
            });
        } catch (ex) {
            CrudHelper.handleHttpResponse(ex, null, res);
        }
    },

    tenantAuditLogList: function (req, res) {
        try {
            TenantApiFactory.tenantAuditLogList(req, res, function (err, data) {
                CrudHelper.handleHttpResponse(err, data, res);
            });
        } catch (ex) {
            CrudHelper.handleHttpResponse(ex, null, res);
        }
    },

    contractGet: function (req, res) {
        try {
            TenantApiFactory.contractGet(req, res, function (err, data) {
                CrudHelper.handleHttpResponse(err, data, res);
            });
        } catch (ex) {
            CrudHelper.handleHttpResponse(ex, null, res);
        }
    },

    contractCreate: function (req, res) {
        try {
            TenantApiFactory.contractUpdate(req, res, function (err, data) {
                CrudHelper.handleHttpResponse(err, data, res);
            });
        } catch (ex) {
            CrudHelper.handleHttpResponse(ex, null, res);
        }
    },

    contractUpdate: function (req, res) {
        try {
            TenantApiFactory.contractUpdate(req, res, function (err, data) {
                CrudHelper.handleHttpResponse(err, data, res);
            });
        } catch (ex) {
            CrudHelper.handleHttpResponse(ex, null, res);
        }
    },

    contractList: function (req, res) {
        try {
            TenantApiFactory.contractList(req, res, function (err, data) {
                CrudHelper.handleHttpResponse(err, data, res);
            });
        } catch (ex) {
            CrudHelper.handleHttpResponse(ex, null, res);
        }
    },

    contractAuditLogList: function (req, res) {
        try {
            TenantApiFactory.contractAuditLogList(req, res, function (err, data) {
                CrudHelper.handleHttpResponse(err, data, res);
            });
        } catch (ex) {
            CrudHelper.handleHttpResponse(ex, null, res);
        }
    },

    administratorGet: function (req, res) {
        try {
            TenantApiFactory.administratorGet(req, res, function (err, data) {
                CrudHelper.handleHttpResponse(err, data, res);
            });
        } catch (ex) {
            CrudHelper.handleHttpResponse(ex, null, res);
        }
    },

    administratorImpersonate: function (req, res) {
        try {
            TenantApiFactory.administratorImpersonate(req, res, function (err, data) {
                CrudHelper.handleHttpResponse(err, data, res);
            });
        } catch (ex) {
            CrudHelper.handleHttpResponse(ex, null, res);
        }
    },

    administratorCreate: function (req, res) {
        try {
            TenantApiFactory.administratorCreate(req, res, function (err, data) {
                CrudHelper.handleHttpResponse(err, data, res);
            });
        } catch (ex) {
            CrudHelper.handleHttpResponse(ex, null, res);
        }
    },

    administratorDelete: function (req, res) {
        try {
            TenantApiFactory.administratorDelete(req, res, function (err, data) {
                CrudHelper.handleHttpResponse(err, data, res);
            });
        } catch (ex) {
            CrudHelper.handleHttpResponse(ex, null, res);
        }
    },

    administratorAuditLogList: function (req, res) {
        try {
            TenantApiFactory.administratorAuditLogList(req, res, function (err, data) {
                CrudHelper.handleHttpResponse(err, data, res);
            });
        } catch (ex) {
            CrudHelper.handleHttpResponse(ex, null, res);
        }
    },

    f4mConfigurationUpdate: function (req, res) {
        try {
            TenantApiFactory.f4mConfigurationUpdate(req, res, function (err, data) {
                CrudHelper.handleHttpResponse(err, data, res);
            });
        } catch (ex) {
            CrudHelper.handleHttpResponse(ex, null, res);
        }
    },
    
    invoiceCreate: function (req, res) {
        try {
            TenantApiFactory.invoiceUpdate(req, res, function (err, data) {
                CrudHelper.handleHttpResponse(err, data, res);
            });
        } catch (ex) {
            CrudHelper.handleHttpResponse(ex, null, res);
        }
    },
    
    invoiceEndCustomerCreate: function (req, res) {
        try {
            TenantApiFactory.invoiceEndCustomerUpdate(req, res, function (err, data) {
                CrudHelper.handleHttpResponse(err, data, res);
            });
        } catch (ex) {
            CrudHelper.handleHttpResponse(ex, null, res);
        }
    },

    vatAdd: function (req, res) {
        try {
            TenantApiFactory.vatAdd(req, res, function (err, data) {
                CrudHelper.handleHttpResponse(err, data, res);
            });
        } catch (ex) {
            CrudHelper.handleHttpResponse(ex, null, res);
        }
    },
    
    profileGet: function (req, res) {
        try {
            TenantApiFactory.profileGet(req, res, function (err, data) {
                CrudHelper.handleHttpResponse(err, data, res);
            });
        } catch (ex) {
            CrudHelper.handleHttpResponse(ex, null, res);
        }
    }
    
    
};
