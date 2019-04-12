var _ = require('lodash');
var Config = require('./../../config/config.js');
var DataIds = require('./_id.data.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var RdbmsService = Database.RdbmsService;
var LiveMessage = RdbmsService.Models.MessageCenter.LiveMessage;
var Application = RdbmsService.Models.Application.Application;

module.exports = {
    LIVE_MESSAGE_1: {
        id: DataIds.LIVE_MESSAGE_1_ID,
        applicationId: DataIds.APPLICATION_1_ID,
        creatorResourceId: DataIds.LOCAL_USER_ID,
        updaterResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        updateDate: DateUtils.isoNow(),
        content: '{ "content": "content" }',
        sendingCompleteDate: null,
        filter: '{ "filter": "filter" }',
        sms: 1,
        email: 1,
        inApp: 1,
        push: 1,
        incentive: 0,
        incentiveAmount: 0,
        incentiveCurrency: 'CREDIT',
    },
    LIVE_MESSAGE_2: {
        id: DataIds.LIVE_MESSAGE_2_ID,
        applicationId: DataIds.APPLICATION_2_ID,
        creatorResourceId: DataIds.LOCAL_USER_ID,
        updaterResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        updateDate: DateUtils.isoNow(),
        content: '{ "content": "content" }',
        sendingCompleteDate: null,
        filter: '{ "filter": "filter" }',
        sms: 1,
        email: 1,
        inApp: 1,
        push: 1,
        incentive: 0,
        incentiveAmount: 0,
        incentiveCurrency: 'CREDIT',
    },
    LIVE_MESSAGE_TEST: {
        id: DataIds.LIVE_MESSAGE_ID_TEST,
        applicationId: DataIds.APPLICATION_1_ID,
        creatorResourceId: DataIds.LOCAL_USER_ID,
        updaterResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        updateDate: DateUtils.isoNow(),
        content: '{ "content": "content" }',
        sendingCompleteDate: null,
        filter: '{ "filter": "filter" }',
        sms: 1,
        email: 1,
        inApp: 1,
        push: 1,
        incentive: 0,
        incentiveAmount: 0,
        incentiveCurrency: 'CREDIT',
    },

    cleanEntities: function (done) {
        var self = this;
        RdbmsService.load()
            .removeSeries(LiveMessage, [self.LIVE_MESSAGE_1, self.LIVE_MESSAGE_2])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },

    loadEntities: function (done) {
        var self = this;
        RdbmsService.load()
            .createSeries(LiveMessage, [self.LIVE_MESSAGE_1, self.LIVE_MESSAGE_2])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    }
};
