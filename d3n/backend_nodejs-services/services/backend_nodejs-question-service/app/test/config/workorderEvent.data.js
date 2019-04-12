var _ = require('lodash');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./_id.data.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var DatabaseErrors = Database.Errors;
var RdbmsService = Database.RdbmsService;
var Workorder = RdbmsService.Models.Workorder.Workorder;
var WorkorderBillingModel = RdbmsService.Models.Workorder.WorkorderBillingModel;
var LanguageModel = RdbmsService.Models.Question.Language;
var RegionalSettingModel = RdbmsService.Models.Question.RegionalSetting;

var WorkorderHasPool = RdbmsService.Models.Workorder.WorkorderHasPool;
var WorkorderHasTenant = RdbmsService.Models.Workorder.WorkorderHasTenant;
var WorkorderHasResource = RdbmsService.Models.Workorder.WorkorderHasResource;
var WorkorderHasRegionalSetting = RdbmsService.Models.Workorder.WorkorderHasRegionalSetting;
var WorkorderHasQuestionExample = RdbmsService.Models.Workorder.WorkorderHasQuestionExample;

var OBJECT = {};

OBJECT.LANGUAGE_1 = {
    id: DataIds.WWMSG.LANGUAGE.ID1,
    iso: 'en',
    name: 'English',
    tenantId: DataIds.TENANT_1_ID
};

OBJECT.REGIONAL_SETTING_1 = {
    id: DataIds.WWMSG.REGIONAL_SETTINGS.ID1,
    iso: 'AW',
    name: 'Regional setting 1',
    status: 'active',
    defaultLanguageId: DataIds.WWMSG.LANGUAGE.ID1
};

OBJECT.WORKORDER_1 = {
    id: DataIds.WWMSG.WORKORDER.ID1,
    ownerResourceId: DataIds.WWMSG.OWNER_RESOURCE.ID1,
    createDate: DateUtils.isoNow(),
    title: 'Title',
    description: 'Description',
    startDate: DateUtils.isoNow(),
    endDate: DateUtils.isoFuture(),
    status: 'draft',
    itemsRequired: 0,
    type: 'question',
    primaryRegionalSettingId: OBJECT.REGIONAL_SETTING_1.id,
};

OBJECT.WORKORDER_NO_RESOURCE = {
    id: DataIds.WWMSG.WORKORDER.ID_NO_RESOURCE,
    ownerResourceId: DataIds.WWMSG.OWNER_RESOURCE.ID1,
    createDate: DateUtils.isoNow(),
    title: 'Title',
    description: 'Description',
    startDate: DateUtils.isoNow(),
    endDate: DateUtils.isoFuture(),
    status: 'draft',
    itemsRequired: 0,
    type: 'question',
    primaryRegionalSettingId: OBJECT.REGIONAL_SETTING_1.id
};

OBJECT.WORKORDER_2_RESOURCES = {
    id: DataIds.WWMSG.WORKORDER.ID_2_RESOURCES,
    ownerResourceId: DataIds.WWMSG.OWNER_RESOURCE.ID1,
    createDate: DateUtils.isoNow(),
    title: 'Title',
    description: 'Description',
    startDate: DateUtils.isoNow(),
    endDate: DateUtils.isoFuture(),
    status: 'draft',
    itemsRequired: 0,
    type: 'question',
    primaryRegionalSettingId: OBJECT.REGIONAL_SETTING_1.id
};


OBJECT.WORKORDER_1_HAS_RESOURCE = {
    workorderId: OBJECT.WORKORDER_1.id,
    resourceId: DataIds.WWMSG.RESOURCE.ID1,
    action: WorkorderHasResource.constants().ACTION_QUESTION_CREATE
};

OBJECT.WORKORDER_2_RESOURCE_HAS_RESOURCE_1 = {
    workorderId: OBJECT.WORKORDER_2_RESOURCES.id,
    resourceId: DataIds.WWMSG.RESOURCE.ID1,
    action: WorkorderHasResource.constants().ACTION_QUESTION_CREATE
};

OBJECT.WORKORDER_2_RESOURCE_HAS_RESOURCE_2 = {
    workorderId: OBJECT.WORKORDER_2_RESOURCES.id,
    resourceId: DataIds.WWMSG.RESOURCE.ID2,
    action: WorkorderHasResource.constants().ACTION_QUESTION_CREATE
};


OBJECT.WORKORDER_1_HAS_POOL = {
    workorderId: DataIds.WWMSG.WORKORDER.ID1,
    poolId: DataIds.POOL_1_ID
},

OBJECT.WORKORDER_NO_RESOURCE_HAS_POOL = {
    workorderId: DataIds.WWMSG.WORKORDER.ID_NO_RESOURCE,
    poolId: DataIds.POOL_1_ID
},

OBJECT.WORKORDER_2_RESOURCES_HAS_POOL = {
    workorderId: DataIds.WWMSG.WORKORDER.ID_2_RESOURCES,
    poolId: DataIds.POOL_1_ID
},


module.exports = {

    clean: function (done) {
        RdbmsService.load()
            .removeSeries(WorkorderHasPool, [OBJECT.WORKORDER_1_HAS_POOL, OBJECT.WORKORDER_NO_RESOURCE_HAS_POOL, OBJECT.WORKORDER_2_RESOURCES_HAS_POOL], 'workorderId')
            .removeSeries(WorkorderHasResource, [OBJECT.WORKORDER_1_HAS_RESOURCE, OBJECT.WORKORDER_2_RESOURCE_HAS_RESOURCE_1, OBJECT.WORKORDER_2_RESOURCE_HAS_RESOURCE_2], 'workorderId')
            .removeSeries(LanguageModel, [OBJECT.LANGUAGE_1])
            .removeSeries(RegionalSettingModel, [OBJECT.REGIONAL_SETTING_1])
            .removeSeries(Workorder, [OBJECT.WORKORDER_1_HAS_POOL_1, OBJECT.WORKORDER_1, OBJECT.WORKORDER_NO_RESOURCE, OBJECT.WORKORDER_2_RESOURCES])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },

    load: function (done) {
        RdbmsService.load()
            .createSeries(LanguageModel, [OBJECT.LANGUAGE_1])
            .createSeries(RegionalSettingModel, [OBJECT.REGIONAL_SETTING_1])
            .createSeries(Workorder, [OBJECT.WORKORDER_1, OBJECT.WORKORDER_NO_RESOURCE, OBJECT.WORKORDER_2_RESOURCES])
            .createSeries(WorkorderHasResource, [OBJECT.WORKORDER_1_HAS_RESOURCE, OBJECT.WORKORDER_2_RESOURCE_HAS_RESOURCE_1, OBJECT.WORKORDER_2_RESOURCE_HAS_RESOURCE_2], 'workorderId')
            .createSeries(WorkorderHasPool, [OBJECT.WORKORDER_1_HAS_POOL, OBJECT.WORKORDER_NO_RESOURCE_HAS_POOL, OBJECT.WORKORDER_2_RESOURCES_HAS_POOL], 'workorderId')
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    }

};
