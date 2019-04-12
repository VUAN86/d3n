var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var RdbmsService = require('nodejs-database').getInstance(Config).RdbmsService;
var PaymentStructure = RdbmsService.Models.Billing.PaymentStructure;
var PaymentStructureTier = RdbmsService.Models.Billing.PaymentStructureTier;
var PaymentAction = RdbmsService.Models.Billing.PaymentAction;
var PaymentTenantBillOfMaterial = RdbmsService.Models.Billing.PaymentTenantBillOfMaterial;
var PaymentResourceBillOfMaterial = RdbmsService.Models.Billing.PaymentResourceBillOfMaterial;
var Workorder = RdbmsService.Models.Workorder.Workorder;
var WorkorderHasTenant = RdbmsService.Models.Workorder.WorkorderHasTenant;
var Question = RdbmsService.Models.Question.Question;
var QuestionTranslation = RdbmsService.Models.Question.QuestionTranslation;
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var CrudHelper = AutoMapperInstance.CrudHelper;
var logger = require('nodejs-logger')();

module.exports = {

    /**
     * Batch calculation and creation of bills grouped by users and workorders
     * @param {Function} callback
     * @returns {*}
     */
    batchBillCalculation: function (callback) {

        function _getUnitsByPaymentStructureTier(paymentStructure, billableItemCount) {
            var units = {
                amountMoneyEur: 0,
                amountBonus: 0,
                amountCredits: 0
            };
            _.forEach(paymentStructure.paymentStructureTiers, function (paymentStructureTier) {
                if (paymentStructureTier.quantityMin <= billableItemCount) {
                    units = {
                        amountMoneyEur: paymentStructureTier.amountMoneyEur,
                        amountBonus: paymentStructureTier.amountBonus,
                        amountCredits: paymentStructureTier.amountCredits
                    };
                }
            });
            return units;
        }

        // Select actions grouped by users and workorders
        logger.info('Batch billing calculation started.');

        try {
            return PaymentAction.findAll({
                attributes: [
                    PaymentAction.tableAttributes.resourceId.field,
                    PaymentAction.tableAttributes.workorderId.field,
                    PaymentAction.tableAttributes.type.field,
                ],
                where: {
                    isBillable: true,
                    paymentTenantBillOfMaterialId: { $eq: null },
                    paymentResourceBillOfMaterialId: { $eq: null },
                    status: PaymentAction.constants().STATUS_POTENTIAL
                },
                group: [
                    PaymentAction.tableAttributes.resourceId.field,
                    PaymentAction.tableAttributes.workorderId.field,
                    PaymentAction.tableAttributes.type.field,
                ],
                order: [
                    PaymentAction.tableAttributes.resourceId.field,
                    PaymentAction.tableAttributes.workorderId.field
                ],
                subQuery: false
            }).then(function (paymentActions) {
                if (paymentActions.length === 0) {
                    logger.info('Batch billing calculation canceled: no payment actions to calculate.');
                    return setImmediate(callback);
                }
                return async.mapSeries(paymentActions, function (paymentAction, nextAction) {
                    var paymentActionItem = paymentAction.get({ plain: true });
                    logger.info('  calculating for user ' + paymentActionItem.resourceId + ', workorder ' + paymentActionItem.workorderId.toString());
                    var workorderItem;
                    var paymentStructure;
                    var billableItemCount = 0;
                    var paymentResourceBillOfMaterial = {
                        resourceId: paymentActionItem.resourceId,
                        workorderId: paymentActionItem.workorderId,
                        paymentStructureId: paymentActionItem.paymentStructureId,
                        amountMoneyEur: 0,
                        amountBonus: 0,
                        amountCredits: 0,
                        status: PaymentResourceBillOfMaterial.constants().STATUS_UNAPPROVED
                    };
                    return async.series([
                        // Get workorder
                        function (next) {
                            return Workorder.findOne({
                                where: {
                                    id: paymentAction.workorderId
                                },
                                include: [
                                    { model: PaymentStructure, as: 'questionCreatePaymentStructure', required: false, include: { model: PaymentStructureTier, required: false } },
                                    { model: PaymentStructure, as: 'questionReviewPaymentStructure', required: false, include: { model: PaymentStructureTier, required: false } },
                                    { model: PaymentStructure, as: 'translationCreatePaymentStructure', required: false, include: { model: PaymentStructureTier, required: false } },
                                    { model: PaymentStructure, as: 'translationReviewPaymentStructure', required: false, include: { model: PaymentStructureTier, required: false } },
                                ],
                                subQuery: false
                            }).then(function (workorder) {
                                workorderItem = workorder.get({ plain: true });
                                return next();
                            }).catch(function (err) {
                                return CrudHelper.callbackError(err, next);
                            });
                        },
                        // Count questions/question translations per workorder
                        function (next) {
                            var model;
                            var status;
                            var attribute;
                            if (!_.isNull(workorderItem.questionCreatePaymentStructure) || !_.isNull(workorderItem.questionReviewPaymentStructure)) {
                                model = Question;
                                attribute = PaymentAction.tableAttributes.questionId.field;
                                status = Question.constants().STATUS_DRAFT;
                                paymentStructure = workorderItem.questionCreatePaymentStructure;
                                if (!_.isNull(workorderItem.questionReviewPaymentStructure)) {
                                    status = Question.constants().STATUS_REVIEW;
                                    paymentStructure = workorderItem.questionReviewPaymentStructure;
                                }
                            } else if (!_.isNull(workorderItem.translationCreatePaymentStructure) || !_.isNull(workorderItem.translationReviewPaymentStructure)) {
                                model = QuestionTranslation;
                                attribute = PaymentAction.tableAttributes.questionTranlationId.field;
                                status = QuestionTranslation.constants().STATUS_DRAFT;
                                paymentStructure = workorderItem.translationCreatePaymentStructure;
                                if (!_.isNull(workorderItem.translationReviewPaymentStructure)) {
                                    status = QuestionTranslation.constants().STATUS_REVIEW;
                                    paymentStructure = workorderItem.translationReviewPaymentStructure;
                                }
                            }
                            // Group by attribute -> count rows
                            return PaymentAction.count({
                                attributes: [attribute],
                                group: [attribute],
                                include: [
                                    { model: model, required: true, attributes: [], where: { status: status } },
                                ],
                                where: {
                                    isBillable: true,
                                    paymentTenantBillOfMaterialId: { $eq: null },
                                    paymentResourceBillOfMaterialId: { $eq: null },
                                    status: PaymentAction.constants().STATUS_POTENTIAL,
                                    resourceId: paymentActionItem.resourceId,
                                    workorderId: paymentActionItem.workorderId
                                },
                            }).then(function (total) {
                                billableItemCount = total.length;
                                return next();
                            }).catch(function (err) {
                                return CrudHelper.callbackError(err, next);
                            });
                        },
                        // Calculate bill amounts, bonuses, credits
                        function (next) {
                            try {
                                var units = _getUnitsByPaymentStructureTier(paymentStructure, billableItemCount);
                                paymentResourceBillOfMaterial.amountMoneyEur = billableItemCount * units.amountMoneyEur;
                                paymentResourceBillOfMaterial.amountBonus = billableItemCount * units.amountBonus;
                                paymentResourceBillOfMaterial.amountCredits = billableItemCount * units.amountCredits;
                                return next();
                            } catch (ex) {
                                return next(ex);
                            }
                        },
                        // Create bill per user and workorder
                        function (next) {
                            PaymentResourceBillOfMaterial.create(paymentResourceBillOfMaterial).then(function (paymentResourceBillOfMaterialRecord) {
                                paymentResourceBillOfMaterial.id = paymentResourceBillOfMaterialRecord.get({ plain: true }).id;
                                return next();
                            }).catch(function (err) {
                                return CrudHelper.callbackError(err, next);
                            });
                        },
                        // Apply bill for payment actions per user and workorder
                        function (next) {
                            return PaymentAction.update({ paymentResourceBillOfMaterialId: paymentResourceBillOfMaterial.id },
                                {
                                    where: {
                                        isBillable: true,
                                        paymentTenantBillOfMaterialId: { $eq: null },
                                        paymentResourceBillOfMaterialId: { $eq: null },
                                        status: PaymentAction.constants().STATUS_POTENTIAL,
                                        resourceId: paymentActionItem.resourceId,
                                        workorderId: paymentActionItem.workorderId
                                    }
                                }).then(function (count) {
                                    return next();
                                }).catch(function (err) {
                                    return CrudHelper.callbackError(err, next);
                                });
                        },
                    ], function (err) {
                        return nextAction(err);
                    });
                }, function (err) {
                    if (err) {
                        logger.error('Batch billing calculation failed.');
                        return setImmediate(callback, err);
                    }
                    logger.info('Batch billing calculation done.');
                    return setImmediate(callback);
                });
            }).catch(function (err) {
                return CrudHelper.callbackError(err, callback);
            });
        } catch (ex) {
            return setImmediate(callback, ex);
        }
    },

};