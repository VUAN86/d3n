var _ = require('lodash');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var ProfileData = require('./../config/profile.data.js');
var DataIds = require('./../config/_id.data.js');
var LiveMessageData = require('./../config/liveMessage.data.js');
var ShouldHelper = require('nodejs-automapper').getInstance(Config).ShouldHelper;

describe('WS LiveMessage API', function () {
    this.timeout(10000);
    global.wsHelper.series().forEach(function (serie) {
        
        describe('[' + serie + '] ' + 'create', function () {
            it('[' + serie + '] ' + 'liveMessageCreate create succes', function (done) {
                var content = _.cloneDeep(LiveMessageData.LIVE_MESSAGE_TEST);
                content.id = null;
                global.wsHelper.apiSecureSucc(serie, 'messageCenter/liveMessageCreate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var liveMessage = responseContent.liveMessage;
                    ShouldHelper.deepEqual(LiveMessageData.LIVE_MESSAGE_TEST, liveMessage, ['id', 'createDate', 'updateDate']);
                    should(liveMessage).has.property("createDate").which.is.not.empty();
                }, done);
            });
        });
        
        describe('[' + serie + '] ' + 'update', function () {
            it('[' + serie + '] ' + 'liveMessageUpdate update succes', function (done) {
                var content = {
                    id: DataIds.LIVE_MESSAGE_2_ID,
                    content: 'updated content'
                };
                global.wsHelper.apiSecureSucc(serie, 'messageCenter/liveMessageUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var liveMessage = responseContent.liveMessage;
                    ShouldHelper.deepEqual(LiveMessageData.LIVE_MESSAGE_2, liveMessage, ['id', 'createDate', 'updateDate', 'content']);
                    should(liveMessage).has.property("updateDate").which.is.not.empty();
                    should(liveMessage).has.property("content").which.is.equal(content.content);
                }, done);
            });
            it('[' + serie + '] ' + 'liveMessageUpdate no result found', function (done) {
                var content = {
                    id: DataIds.LIVE_MESSAGE_ID_TEST,
                    content: 'updated content'
                };
                global.wsHelper.apiSecureFail(serie, 'messageCenter/liveMessageGet', ProfileData.CLIENT_INFO_1, content, Errors.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'list', function () {
            it('[' + serie + '] ' + 'liveMessageList get all results', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    orderBy: [{ field: 'content', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'messageCenter/liveMessageList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent).has.property('items');
                    should(responseContent.items).has.property('length');
                    should(responseContent.items.length).be.aboveOrEqual(1);
                }, done);
            });
            it('[' + serie + '] ' + 'liveMessageList get one result', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.LIVE_MESSAGE_1_ID },
                    orderBy: [{ field: 'content', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'messageCenter/liveMessageList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent).has.property('items').which.has.length(1);
                    should(responseContent.items[0].id).be.equal(DataIds.LIVE_MESSAGE_1_ID);
                }, done);
            });
            it('[' + serie + '] ' + 'liveMessageList get no results', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.LIVE_MESSAGE_ID_TEST },
                    orderBy: [{ field: 'content', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'messageCenter/liveMessageList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent).has.property('items').which.has.length(0);
                }, done);
            });
        });
        
        describe('[' + serie + '] ' + 'get', function () {
            it('[' + serie + '] ' + 'liveMessageGet get result', function (done) {
                var content = { id: DataIds.LIVE_MESSAGE_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'messageCenter/liveMessageGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                   should(responseContent.liveMessage.applicationId).be.equal(LiveMessageData.LIVE_MESSAGE_1.applicationId);
                }, done);
            });
            it('[' + serie + '] ' + 'liveMessageGet no result found', function (done) {
                var content = { id: DataIds.LIVE_MESSAGE_ID_TEST };
                global.wsHelper.apiSecureFail(serie, 'messageCenter/liveMessageGet', ProfileData.CLIENT_INFO_1, content, Errors.NoRecordFound, done);
            });
        });
        
        describe('[' + serie + '] ' + 'send', function () {
            it('[' + serie + '] ' + 'liveMessageSend send succes', function (done) {
                var content = {
                    id: DataIds.LIVE_MESSAGE_1_ID
                };
                global.wsHelper.apiSecureSucc(serie, 'messageCenter/liveMessageSend', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var AerospikeLiveMessage = require('nodejs-aerospike').getInstance(Config).KeyvalueService.Models.AerospikeLiveMessage;
                    AerospikeLiveMessage.findOne(content, function(err, liveMessage) {
                        should.not.exist(err);
                        should(liveMessage).has.property('id').which.is.equal(content.id);
                    });
                }, done);
            });
        });
    });
});