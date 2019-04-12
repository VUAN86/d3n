var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var UserMessageClient = require('nodejs-user-message-client');
var ProfileService = require('./profileService.js');
var RdbmsService = require('nodejs-database').getInstance(Config).RdbmsService;
var Voucher = RdbmsService.Models.VoucherManager.Voucher;
var VoucherProvider = RdbmsService.Models.VoucherManager.VoucherProvider;
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikeVoucherCounter = KeyvalueService.Models.AerospikeVoucherCounter;
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var CrudHelper = AutoMapperInstance.CrudHelper;
var logger = require('nodejs-logger')();

var userMessageClient = new UserMessageClient({
    registryServiceURIs: Config.registryServiceURIs,
    ownerServiceName: 'gameValidityCheckService'
});

module.exports = {

    /**
     * Checks thresholds are reached for active vouchers
     * @param {*} callback
     * @returns {*}
     */
    voucherCheck: function (callback) {
        var self = this;
        var activeVouchers = [];
        var activeAdmins = [];
        try {
            async.series([
                // Gather active vouchers for all tenants
                function (serie) {
                    return Voucher.findAll({
                        where: { status: Voucher.constants().STATUS_ACTIVE },
                        include: [
                            { model: VoucherProvider, required: true }
                        ],
                        subQuery: false
                    }).then(function (vouchers) {
                        _.forEach(vouchers, function (voucher) {
                            var voucherPlain = voucher.get({ plain: true });
                            activeVouchers.push({ id: voucherPlain.id, tenantId: voucherPlain.voucherProvider.tenantId });
                        });
                        return serie();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, serie);
                    });
                },
                // Gather admins for all tenants
                function (serie) {
                    try {
                        return ProfileService.getAdminTenantList(function (err, admins) {
                            activeAdmins = admins;
                            return serie();
                        });
                    } catch (ex) {
                        return serie(ex);
                    }
                },
            ], function (err) {
                if (err) {
                    return setImmediate(callback, err);
                }
                async.everySeries(activeVouchers, function (activeVoucher, every) {
                    logger.info('VoucherCheckService: checking ' + activeVoucher.id);
                    var skip = true;
                    async.series([
                        // Check used amount
                        function (next) {
                            try {
                                return AerospikeVoucherCounter.getUsedAmount({ id: activeVoucher.id }, function (err, usedAmount) {
                                    if (err && err !== Errors.DatabaseApi.NoRecordFound) {
                                        return next(err);
                                    }
                                    if (usedAmount > 0 && usedAmount <= Config.backgroundProcesses.voucherCheckThresholdLimit) {
                                        skip = false;
                                    }
                                    return next();
                                });
                            } catch (ex) {
                                logger.error('VoucherCheckService: AerospikeVoucherCounter.getUsedAmount', ex);
                                return setImmediate(next, ex);
                            }
                        },
                        // Send email to all admins of voucher's tenant
                        function (next) {
                            if (skip) {
                                return setImmediate(next, null);
                            }
                            try {
                                var tenantAdmins = _.map(activeAdmins, function (admin) {
                                    if (admin.tenantId === activeVoucher.tenantId) {
                                        return admin.profileId;
                                    }
                                });
                                return async.everySeries(tenantAdmins, function (tenantAdmin, everyAdmin) {
                                    try {
                                        var sendEmailParams = {
                                            userId: tenantAdmin,
                                            address: null,
                                            subject: Config.userMessage.voucherThresholdReachingEmail.subject,
                                            message: Config.userMessage.voucherThresholdReachingEmail.message,
                                            subject_parameters: [activeVoucher.id.toString()],
                                            message_parameters: [activeVoucher.id.toString()],
                                            waitResponse: Config.umWaitForResponse
                                        };
                                        return userMessageClient.sendEmail(sendEmailParams, function (err, message) {
                                            if (err || (message && message.getError())) {
                                                logger.error('VoucherCheckService: sendEmail', err || message.getError());
                                                return everyAdmin();
                                            }
                                            return everyAdmin();
                                        });
                                    } catch (ex) {
                                        logger.error('VoucherCheckService: sendEmail', ex);
                                        return everyAdmin(Errors.QuestionApi.NoInstanceAvailable);
                                    }
                                }, function (err) {
                                    if (err) {
                                        return next(err);
                                    }
                                    return next(null, true);
                                });
                            } catch (ex) {
                                logger.error('VoucherCheckService: email to admins', ex);
                                return setImmediate(next, ex);
                            }
                        },
                    ], function (err) {
                        if (err) {
                            return every(err);
                        }
                        return every(null, true);
                    });
                }, function (err) {
                    if (err) {
                        return setImmediate(callback, err);
                    }
                    return setImmediate(callback);
                });
            });
        } catch (ex) {
            return setImmediate(callback, ex);
        }
    },

};