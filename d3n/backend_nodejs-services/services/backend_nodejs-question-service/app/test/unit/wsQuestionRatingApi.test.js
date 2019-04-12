var _ = require('lodash');
var async = require('async');
var should = require('should');
var assert = require('assert');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var Database = require('nodejs-database').getInstance(Config);
var DataIds = require('./../config/_id.data.js');
var ProfileData = require('./../config/profile.data.js');
var QuestionRatingData = require('./../config/questionRating.data.js');
var ShouldHelper = require('nodejs-automapper').getInstance(Config).ShouldHelper;

describe('WS QuestionRating API', function () {
    this.timeout(10000);
    global.wsHelper.series().forEach(function (serie) {
        
        describe('[' + serie + '] ' + 'questionRatingList', function () {
            
            it('[' + serie + '] ' + 'list by id', function (done) {
                var reqContent = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: QuestionRatingData.QUESTION_RATING_1.id },
                    orderBy: [{ field: 'id', direction: 'asc' }]
                };
                global.wsHelper.apiSecureCall(serie, 'questionRating/questionRatingList', ProfileData.CLIENT_INFO_1, reqContent, function (err, message) {
                    try {
                        var shouldResponseContent = _.clone(QuestionRatingData.QUESTION_RATING_1);
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
                global.wsHelper.apiSecureCall(serie, 'questionRating/questionRatingList', ProfileData.CLIENT_INFO_1, reqContent, function (err, message) {
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
                global.wsHelper.apiSecureCall(serie, 'questionRating/questionRatingList', ProfileData.CLIENT_INFO_1, reqContent, function (err, message) {
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
        
        describe('[' + serie + '] ' + 'questionRatingGet', function () {
            it('[' + serie + '] ' + 'get - success', function (done) {
                var reqContent = { id: QuestionRatingData.QUESTION_RATING_1.id };
                global.wsHelper.apiSecureCall(serie, 'questionRating/questionRatingGet', ProfileData.CLIENT_INFO_1, reqContent, function (err, message) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(message.getError(), null);
                        assert.notStrictEqual(message.getContent(), null);
                        assert.deepStrictEqual(message.getContent().questionRating, QuestionRatingData.QUESTION_RATING_1);
                        done();
                    } catch (e) {
                        done(e);
                    }
                });
            });
            
            it('[' + serie + '] ' + 'get - error (entity not exists)', function (done) {
                var reqContent = {id: 2345282234};
                global.wsHelper.apiSecureCall(serie, 'questionRating/questionRatingGet', ProfileData.CLIENT_INFO_1, reqContent, function(err, message) {
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
        
        
        describe('[' + serie + '] ' + 'questionRatingUpdate', function () {
            it('[' + serie + '] ' + 'update - success', function (done) {
                var reqContent = _.clone(QuestionRatingData.QUESTION_RATING_1);
                reqContent.rating = 2.5;
                reqContent.reason = 'reason updated';
                
                global.wsHelper.apiSecureCall(serie, 'questionRating/questionRatingUpdate', ProfileData.CLIENT_INFO_1, reqContent, function(err, message) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(message.getError(), null);
                        assert.notStrictEqual(message.getContent(), null);
                        var updatedItem = _.clone(message.getContent().questionRating);
                        ShouldHelper.deepEqual(updatedItem, reqContent, ['resourceId']);
                        done();
                    } catch (e) {
                        done(e);
                    }
                });
            });
            it('[' + serie + '] ' + 'update - error, ID not exists', function (done) {
                var reqContent = _.clone(QuestionRatingData.QUESTION_RATING_1);
                reqContent.id = 45132343;
                global.wsHelper.apiSecureCall(serie, 'questionRating/questionRatingUpdate', ProfileData.CLIENT_INFO_1, reqContent, function(err, message) {
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
                var reqContent = _.clone(QuestionRatingData.QUESTION_RATING_1);
                reqContent.questionId = 12345;
                global.wsHelper.apiSecureCall(serie, 'questionRating/questionRatingUpdate', ProfileData.CLIENT_INFO_1, reqContent, function(err, message) {
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
                var reqContent = _.clone(QuestionRatingData.QUESTION_RATING_NEW);
                
                global.wsHelper.apiSecureCall(serie, 'questionRating/questionRatingCreate', ProfileData.CLIENT_INFO_1, reqContent, function(err, message) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(message.getError(), null);
                        assert.notStrictEqual(message.getContent(), null);
                        var updatedItem = _.clone(message.getContent().questionRating);
                        ShouldHelper.deepEqual(updatedItem, reqContent, ['id', 'resourceId']);
                        done();
                    } catch (e) {
                        done(e);
                    }
                });
            });
            it('[' + serie + '] ' + 'create - error, wrong questionId', function (done) {
                var reqContent = _.clone(QuestionRatingData.QUESTION_RATING_NEW);
                reqContent.questionId = 432546;
                
                global.wsHelper.apiSecureCall(serie, 'questionRating/questionRatingCreate', ProfileData.CLIENT_INFO_1, reqContent, function(err, message) {
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
        
        
        describe('[' + serie + '] ' + 'questionRatingDelete', function () {
            it('[' + serie + '] ' + 'delete - success', function (done) {
                var reqContent = { id: QuestionRatingData.QUESTION_RATING_DELETE.id };
                global.wsHelper.apiSecureCall(serie, 'questionRating/questionRatingDelete', ProfileData.CLIENT_INFO_1, reqContent, function (err, message) {
                    try {
                        assert.ifError(err);
                        assert(message.getError() === null);
                        setTimeout(checkWasDeleted, 500);
                    } catch (e) {
                        done(e);
                    }
                });
                
                
                function checkWasDeleted() {
                    var reqContent = { id: QuestionRatingData.QUESTION_RATING_DELETE.id };
                    global.wsHelper.apiSecureCall(serie, 'questionRating/questionRatingGet', ProfileData.CLIENT_INFO_1, reqContent, function (err, message) {
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
                global.wsHelper.apiSecureCall(serie, 'questionRating/questionRatingDelete', ProfileData.CLIENT_INFO_1, reqContent, function (err, message) {
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
        
        describe('[' + serie + '] ' + ' test aggregate rating', function () {
            it('[' + serie + '] ' + ' test aggregate rating', function (done) {
                
                function upsertQuestionRating(data, cb) {
                    try {
                        global.wsHelper.apiSecureCall(serie, 'questionRating/questionRatingUpdate', ProfileData.CLIENT_INFO_1, data, function(err, message) {
                            try {
                                assert.ifError(err);
                                assert.strictEqual(message.getError(), null);
                                assert.notStrictEqual(message.getContent(), null);
                                return cb(false, message.getContent().questionRating.id);
                            } catch (e) {
                                return cb(e);
                            }
                        });
                    } catch (e) {
                        return setImmediate(cb, e);
                    }
                };
                function deleteQuestionRating(id, cb) {
                    try {
                        global.wsHelper.apiSecureCall(serie, 'questionRating/questionRatingDelete', ProfileData.CLIENT_INFO_1, {id: id}, function(err, message) {
                            try {
                                assert.ifError(err);
                                assert.strictEqual(message.getError(), null);
                                return cb();
                            } catch (e) {
                                return cb(e);
                            }
                        });
                    } catch (e) {
                        return setImmediate(cb, e);
                    }
                };
                
                function getAggQuestionRating(questionId, cb) {
                    try {
                        global.wsHelper.apiSecureCall(serie, 'question/questionGet', ProfileData.CLIENT_INFO_1, {id: questionId}, function (err, message) {
                            try {
                                assert.ifError(err);
                                assert.strictEqual(message.getError(), null);
                                assert.notStrictEqual(message.getContent(), null);
                                cb(false, message.getContent().question.rating);
                            } catch (e) {
                                cb(e);
                            }
                        });
                        
                    } catch (e) {
                        return setImmediate(cb, e);
                    }
                };
                
                
                var qrId1 = null;
                var qrId2 = null;
                async.series([
                    // add record
                    function (next) {
                        var data = _.clone(QuestionRatingData.QUESTION_RATING_AGG);
                        data.rating = 2.5;
                        upsertQuestionRating(data, function (err, qrId) {
                            qrId1 = qrId;
                            return next(err);
                        });
                    }, function (next) {
                        setTimeout(next, 200);
                    },
                    
                    // rating must be 2.5
                    function (next) {
                        getAggQuestionRating(QuestionRatingData.QUESTION_RATING_AGG.questionId, function (err, rating) {
                            try {
                                assert.ifError(err);
                                assert.strictEqual(rating, 2.5);
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        });
                    }, function (next) {
                        setTimeout(next, 200);
                    },
                    
                    // add record
                    function (next) {
                        var data = _.clone(QuestionRatingData.QUESTION_RATING_AGG);
                        data.rating = 4;
                        upsertQuestionRating(data, function (err, qrId) {
                            qrId2 = qrId;
                            return next(err);
                        });
                    },function (next) {
                        setTimeout(next, 200);
                    },
                    
                    // rating must be 6.5, 4+2.5
                    function (next) {
                        getAggQuestionRating(QuestionRatingData.QUESTION_RATING_AGG.questionId, function (err, rating) {
                            try {
                                assert.ifError(err);
                                assert.strictEqual(rating, 6.5);
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        });
                    },function (next) {
                        setTimeout(next, 200);
                    },
                    
                    
                    // update record rating
                    function (next) {
                        var data = _.clone(QuestionRatingData.QUESTION_RATING_AGG);
                        data.id = qrId1;
                        data.rating = 3;
                        upsertQuestionRating(data, function (err) {
                            return next(err);
                        });
                    },function (next) {
                        setTimeout(next, 200);
                    },
                    
                    // rating must be 7, 4+3
                    function (next) {
                        getAggQuestionRating(QuestionRatingData.QUESTION_RATING_AGG.questionId, function (err, rating) {
                            try {
                                assert.ifError(err);
                                assert.strictEqual(rating, 7);
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        });
                    },function (next) {
                        setTimeout(next, 200);
                    },
                    
                    
                    // delete a record
                    function (next) {
                        deleteQuestionRating(qrId1, next);
                    },function (next) {
                        setTimeout(next, 200);
                    },
                    
                    // rating must be 4
                    function (next) {
                        getAggQuestionRating(QuestionRatingData.QUESTION_RATING_AGG.questionId, function (err, rating) {
                            try {
                                assert.ifError(err);
                                assert.strictEqual(rating, 4);
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        });
                    },
                    
                    
                ], function (err) {
                    try {
                        assert.ifError(err);
                        return done();
                    } catch (e) {
                        return done(e);
                    }
                });
            });
        });
        
    });
});