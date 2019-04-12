var _ = require('lodash');
var should = require('should');
var assert = require('assert');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var Database = require('nodejs-database').getInstance(Config);
var DataIds = require('./../config/_id.data.js');
var ProfileData = require('./../config/profile.data.js');
var QuestionVotingData = require('./../config/questionVoting.data.js');
var ShouldHelper = require('nodejs-automapper').getInstance(Config).ShouldHelper;

describe('WS QuestionVoting API', function () {
    this.timeout(10000);
    global.wsHelper.series().forEach(function (serie) {
        
        describe('[' + serie + '] ' + 'questionVotingList', function () {
            
            it('[' + serie + '] ' + 'list by id', function (done) {
                var reqContent = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: QuestionVotingData.QUESTION_VOTING_1.id },
                    orderBy: [{ field: 'id', direction: 'asc' }]
                };
                global.wsHelper.apiSecureCall(serie, 'questionVoting/questionVotingList', ProfileData.CLIENT_INFO_1, reqContent, function (err, message) {
                    try {
                        var shouldResponseContent = _.clone(QuestionVotingData.QUESTION_VOTING_1);
                        assert.ifError(err);
                        assert.strictEqual(message.getError(), null);
                        assert.notStrictEqual(message.getContent(), null);
                        assert.strictEqual(message.getContent().items.length, 1);
                        ShouldHelper.deepEqual(shouldResponseContent, message.getContent().items[0], []);
                        done();
                    } catch (e) {
                        done(e);
                    }
                });
            });
            it('[' + serie + '] ' + 'list by rating', function (done) {
                var reqContent = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { rating: 2 },
                    orderBy: [{ field: 'id', direction: 'asc' }]
                };
                global.wsHelper.apiSecureCall(serie, 'questionVoting/questionVotingList', ProfileData.CLIENT_INFO_1, reqContent, function (err, message) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(message.getError(), null);
                        assert.notStrictEqual(message.getContent(), null);
                        
                        var items = message.getContent().items;
                        for(var i=0; i<items.length; i++) {
                            assert.strictEqual(items[i].rating, 2);
                        }
                        
                        done();
                    } catch (e) {
                        done(e);
                    }
                });
            });
            
            it('[' + serie + '] ' + 'list no item', function (done) {
                var reqContent = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: 2345282234 },
                    orderBy: [{ field: 'id', direction: 'asc' }]
                };
                global.wsHelper.apiSecureCall(serie, 'questionVoting/questionVotingList', ProfileData.CLIENT_INFO_1, reqContent, function (err, message) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(message.getError(), null);
                        assert.notStrictEqual(message.getContent(), null);
                        assert.strictEqual(message.getContent().items.length, 0);
                        done();
                    } catch (e) {
                        done(e);
                    }
                });
            });
        });
        
        describe('[' + serie + '] ' + 'questionVotingGet', function () {
            it('[' + serie + '] ' + 'get - success', function (done) {
                var reqContent = { id: QuestionVotingData.QUESTION_VOTING_1.id };
                global.wsHelper.apiSecureCall(serie, 'questionVoting/questionVotingGet', ProfileData.CLIENT_INFO_1, reqContent, function (err, message) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(message.getError(), null);
                        assert.notStrictEqual(message.getContent(), null);
                        ShouldHelper.deepEqual(message.getContent().questionVoting, QuestionVotingData.QUESTION_VOTING_1, ['createDate']);
                        done();
                    } catch (e) {
                        done(e);
                    }
                });
            });
            it('[' + serie + '] ' + 'get - error (entity not exists)', function (done) {
                var reqContent = {id: 2345282234};
                global.wsHelper.apiSecureCall(serie, 'questionVoting/questionVotingGet', ProfileData.CLIENT_INFO_1, reqContent, function(err, message) {
                    try {
                        assert.ifError(err);
                        assert.notStrictEqual(message.getError(), null);
                        assert.strictEqual(message.getError().message, Errors.DatabaseApi.NoRecordFound);
                        done();
                    } catch (e) {
                        done(e);
                    }
                });
            });
        });
        
        describe('[' + serie + '] ' + 'questionVotingUpdate', function () {
            it('[' + serie + '] ' + 'create - success', function (done) {
                var reqContent = _.clone(QuestionVotingData.QUESTION_VOTING_NEW);
                global.wsHelper.apiSecureCall(serie, 'questionVoting/questionVotingCreate', ProfileData.CLIENT_INFO_1, reqContent, function (err, message) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(message.getError(), null);
                        assert.notStrictEqual(message.getContent(), null);
                        var updatedItem = _.clone(message.getContent().questionVoting);
                        ShouldHelper.deepEqual(updatedItem, reqContent, ['id', 'resourceId', 'createDate']);
                        done();
                    } catch (e) {
                        done(e);
                    }
                });
            });
            it('[' + serie + '] ' + 'create - error, ID not exists', function (done) {
                var reqContent = _.clone(QuestionVotingData.QUESTION_VOTING_NEW);
                reqContent.questionId = 45132343;
                global.wsHelper.apiSecureCall(serie, 'questionVoting/questionVotingCreate', ProfileData.CLIENT_INFO_1, reqContent, function (err, message) {
                    try {
                        assert.ifError(err);
                        assert.notStrictEqual(message.getError(), null);
                        done();
                    } catch (e) {
                        done(e);
                    }
                });
            });
            it('[' + serie + '] ' + 'update - success', function (done) {
                var reqContent = _.clone(QuestionVotingData.QUESTION_VOTING_1);
                reqContent.rating = 2.5;
                reqContent.reason = 'reason updated';
                global.wsHelper.apiSecureCall(serie, 'questionVoting/questionVotingUpdate', ProfileData.CLIENT_INFO_1, reqContent, function(err, message) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(message.getError(), null);
                        assert.notStrictEqual(message.getContent(), null);
                        var updatedItem = _.clone(message.getContent().questionVoting);
                        ShouldHelper.deepEqual(updatedItem, reqContent, ['resourceId', 'createDate']);
                        done();
                    } catch (e) {
                        done(e);
                    }
                });
            });
            it('[' + serie + '] ' + 'update - error, ID not exists', function (done) {
                var reqContent = _.clone(QuestionVotingData.QUESTION_VOTING_1);
                reqContent.id = 45132343;
                global.wsHelper.apiSecureCall(serie, 'questionVoting/questionVotingUpdate', ProfileData.CLIENT_INFO_1, reqContent, function(err, message) {
                    try {
                        assert.ifError(err);
                        assert.notStrictEqual(message.getError(), null);
                        done();
                    } catch (e) {
                        done(e);
                    }
                });
            });
            it('[' + serie + '] ' + 'update - error, questionId not exists', function (done) {
                var reqContent = _.clone(QuestionVotingData.QUESTION_VOTING_1);
                reqContent.questionId = 12345;
                global.wsHelper.apiSecureCall(serie, 'questionVoting/questionVotingUpdate', ProfileData.CLIENT_INFO_1, reqContent, function(err, message) {
                    try {
                        assert.ifError(err);
                        assert.notStrictEqual(message.getError(), null);
                        done();
                    } catch (e) {
                        done(e);
                    }
                });
            });
            
            it('[' + serie + '] ' + 'create - success', function (done) {
                var reqContent = _.clone(QuestionVotingData.QUESTION_VOTING_NEW);
                global.wsHelper.apiSecureCall(serie, 'questionVoting/questionVotingUpdate', ProfileData.CLIENT_INFO_1, reqContent, function(err, message) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(message.getError(), null);
                        assert.notStrictEqual(message.getContent(), null);
                        var updatedItem = _.clone(message.getContent().questionVoting);
                        ShouldHelper.deepEqual(updatedItem, reqContent, ['id', 'resourceId', 'createDate']);
                        done();
                    } catch (e) {
                        done(e);
                    }
                });
            });
            it('[' + serie + '] ' + 'create - error, wrong questionId', function (done) {
                var reqContent = _.clone(QuestionVotingData.QUESTION_VOTING_NEW);
                reqContent.questionId = 432546;
                global.wsHelper.apiSecureCall(serie, 'questionVoting/questionVotingUpdate', ProfileData.CLIENT_INFO_1, reqContent, function(err, message) {
                    try {
                        assert.ifError(err);
                        assert(message.getError() !== null);
                        done();
                    } catch (e) {
                        done(e);
                    }
                });
            });
        });
        
        describe('[' + serie + '] ' + 'questionVotingDelete', function () {
            it('[' + serie + '] ' + 'delete - success', function (done) {
                var reqContent = { id: QuestionVotingData.QUESTION_VOTING_DELETE.id };
                global.wsHelper.apiSecureCall(serie, 'questionVoting/questionVotingDelete', ProfileData.CLIENT_INFO_1, reqContent, function (err, message) {
                    try {
                        assert.ifError(err);
                        assert(message.getError() === null);
                        setTimeout(checkWasDeleted, 500);
                    } catch (e) {
                        done(e);
                    }
                });
                function checkWasDeleted() {
                    var reqContent = { id: QuestionVotingData.QUESTION_VOTING_DELETE.id };
                    global.wsHelper.apiSecureCall(serie, 'questionVoting/questionVotingGet', ProfileData.CLIENT_INFO_1, reqContent, function (err, message) {
                        try {
                            assert.ifError(err);
                            assert(message.getError() !== null);
                            assert(message.getError().message === Errors.DatabaseApi.NoRecordFound);
                            return done();
                        } catch (e) {
                            done(e);
                        }
                    });
                };
            });
            it('[' + serie + '] ' + 'delete - error , record not found', function (done) {
                var reqContent = { id: 3454345 };
                global.wsHelper.apiSecureCall(serie, 'questionVoting/questionVotingDelete', ProfileData.CLIENT_INFO_1, reqContent, function (err, message) {
                    try {
                        assert.ifError(err);
                        assert(message.getError() !== null);
                        assert(message.getError().message === Errors.DatabaseApi.NoRecordFound);
                        done();
                    } catch (e) {
                        done(e);
                    }
                });
            });
        });
        
    });
});