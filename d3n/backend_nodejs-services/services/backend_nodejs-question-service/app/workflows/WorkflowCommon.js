var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var Question = Database.RdbmsService.Models.Question.Question;
var Workorder = Database.RdbmsService.Models.Workorder.Workorder;
var QuestionTranslation = Database.RdbmsService.Models.Question.QuestionTranslation;
var QuestionOrTranslationReview = Database.RdbmsService.Models.Question.QuestionOrTranslationReview;
var QuestionOrTranslationIssue = Database.RdbmsService.Models.Question.QuestionOrTranslationIssue;
var QuestionOrTranslationEventLog = Database.RdbmsService.Models.Question.QuestionOrTranslationEventLog;
var AnalyticEvent = require('../services/analyticEvent.js');

module.exports = {
    getQuestionVersion: function (id, cb) {
        Question.findOne({ where: { id: id } }).then(function (dbItem) {
            try {
                if (!dbItem) {
                    return cb(Errors.DatabaseApi.NoRecordFound);
                }

                return cb(false, dbItem.get({ plain: true }).version);
            } catch (e) {
                return cb(e);
            }
        }).catch(function (err) {
            return cb(err);
        });
    },
    getQuestionTranslationVersion: function (id, cb) {
        QuestionTranslation.findOne({ where: { id: id } }).then(function (dbItem) {
            try {
                if (!dbItem) {
                    return cb(Errors.DatabaseApi.NoRecordFound);
                }

                return cb(false, dbItem.get({ plain: true }).version);
            } catch (e) {
                return cb(e);
            }
        }).catch(function (err) {
            return cb(err);
        });
    },

    getQuestionPublishedVersion: function (id, cb) {
        Question.findOne({ where: { id: id } }).then(function (dbItem) {
            try {
                if (!dbItem) {
                    return cb(Errors.DatabaseApi.NoRecordFound);
                }

                return cb(false, dbItem.get({ plain: true }).publishedVersion);
            } catch (e) {
                return cb(e);
            }
        }).catch(function (err) {
            return cb(err);
        });
    },
    getQuestionTranslationPublishedVersion: function (id, cb) {
        QuestionTranslation.findOne({ where: { id: id } }).then(function (dbItem) {
            try {
                if (!dbItem) {
                    return cb(Errors.DatabaseApi.NoRecordFound);
                }

                return cb(false, dbItem.get({ plain: true }).publishedVersion);
            } catch (e) {
                return cb(e);
            }
        }).catch(function (err) {
            return cb(err);
        });
    },
    getQuestionStatus: function (id, cb) {
        Question.findOne({where: {id: id}}).then(function (dbItem) {
            try {
                if (!dbItem) {
                    return cb(Errors.DatabaseApi.NoRecordFound);
                }

                return cb(false, dbItem.get({plain: true}).status);
            } catch (e) {
                return cb(e);
            }
        }).catch(function (err) {
            return cb(err);
        });
    },
    getQuestionTranslationStatus: function (id, cb) {
        QuestionTranslation.findOne({where: {id: id}}).then(function (dbItem) {
            try {
                if (!dbItem) {
                    return cb(Errors.DatabaseApi.NoRecordFound);
                }

                return cb(false, dbItem.get({plain: true}).status);
            } catch (e) {
                return cb(e);
            }
        }).catch(function (err) {
            return cb(err);
        });
    },
    
    addReviewAndEventLog: function (dataReview, dataEventLog, cb) {
        var self = this;
        var reviewId;
        async.series([
            // add review
            function (next) {
                try {
                    self.addReview(dataReview, function (err, id) {
                        if (err) {
                            return next(err);
                        }
                        reviewId = id;
                        return next();
                    });
                } catch (e) {
                    return setImmediate(next, e);
                }
            },

            // add event log
            function (next) {
                try {
                    var data = _.assign({}, dataEventLog);
                    data.reviewId = reviewId;
                    self.addEventLog(data, next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            }
        ], cb);
    },
    addIssueAndEventLog: function (dataIssue, dataEventLog, cb) {
        var self = this;
        var issueId;
        async.series([
            // add review
            function (next) {
                try {
                    self.addIssue(dataIssue, function (err, id) {
                        if (err) {
                            return next(err);
                        }
                        issueId = id;
                        return next();
                    });
                } catch (e) {
                    return setImmediate(next, e);
                }
            },

            // add event log
            function (next) {
                try {
                    var data = _.assign({}, dataEventLog);
                    data.issueId = issueId;
                    self.addEventLog(data, next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            }
        ], cb);
    },

    addReview: function (data, cb) {
        try {
            QuestionOrTranslationReview.create(data).then(function(dbItem) {
                try {
                    return cb(false, dbItem.get({ plain: true }).id);
                } catch (e) {
                    return cb(e);
                }
            }).catch(function (err) {
                return cb(err);
            });
        } catch (e) {
            return setImmediate(cb, e);
        }
    },
    addIssue: function (data, cb) {
        try {
            QuestionOrTranslationIssue.create(data).then(function(dbItem) {
                try {
                    return cb(false, dbItem.get({ plain: true }).id);
                } catch (e) {
                    return cb(e);
                }
            }).catch(function (err) {
                return cb(err);
            });
        } catch (e) {
            return setImmediate(cb, e);
        }
    },

    addEventLog: function (data, cb) {
        try {
            var _data = _.assign({}, data);
            if (_.isUndefined(_data.createDate)) {
                _data.createDate = DateUtils.isoNow();
            }
            QuestionOrTranslationEventLog.create(_data).then(function() {
                return cb();
            }).catch(function (err) {
                return cb(err);
            });
        } catch (e) {
            return setImmediate(cb, e);
        }
    },
    
    isFirstTranslation: function (translationId, cb) {
        try {
            var questionId = null;
            var firstTranslationId = null;
            async.series([
                function (next) {
                    QuestionTranslation.findOne({where: {id: translationId}}).then(function (dbItem) {
                        try {
                            questionId = dbItem.get({plain: true}).questionId;
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    }).catch(function (err) {
                        return next(err);
                    });
                },

                function (next) {
                    var options = {
                        where: {
                            questionId: questionId
                        },
                        order: 'id ASC',
                        limit: 1
                    };

                    QuestionTranslation.findAll(options).then(function (dbItems) {
                        try {
                            firstTranslationId = dbItems[0].get({plain: true}).id;
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    }).catch(function (err) {
                        return next(err);
                    });

                }
            ], function (err) {
                if (err) {
                    return cb(err);
                }

                return cb(false, (parseInt(firstTranslationId) === parseInt(translationId)), questionId);
            });
        } catch (e) {
            return setImmediate(cb, e);
        }
    },
    
    closeWorkorder: function (questionId, cb) {
        try {
            var workorderId = null;
            var workorderClose = false;
            async.series([
                // get question entry, save workorderId
                function (next) {
                    try {
                        Question.findOne({where: {id: questionId}}).then(function (dbItem) {
                            try {
                                if (!dbItem) {
                                    return next(Errors.DatabaseApi.NoRecordFound);
                                }
                                workorderId = dbItem.get({plain: true}).workorderId;
                                if(!workorderId) {
                                    return next(new Error('skip'));
                                }
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        }).catch(function (err) {
                            return next(err);
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // list questions of workorder that are not approved (at least one is enough)
                function (next) {
                    try {
                        var where = {
                            workorderId: workorderId,
                            status: { '$ne': Question.constants().STATUS_APPROVED }
                        };
                        Question.count({where: where}).then(function (cnt) {
                            try {
                                if (cnt === 0) {
                                    return next();
                                }
                                return next(new Error('skip'));
                            } catch (e) {
                                return next(e);
                            }
                        }).catch(function (err) {
                            return next(err);
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // if all question are approved, close workorder
                function (next) {
                    try {
                        Workorder.update({ status: Workorder.constants().STATUS_CLOSED }, { where: { id: workorderId } }).then(function (count) {
                            if (count[0] === 0) {
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            return next();
                        }).catch(function (err) {
                            return next(err);
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                }
            ], function (err) {
                if (err) {
                    if (err.message === 'skip') {
                        return cb();
                    }
                    return cb(err);
                }
                return cb(err);
            });
            
        } catch (e) {
            return setImmediate(cb, e);
        }
    },
    
    deleteQuestionReviews: function (questionId, cb) {
        try {
            QuestionOrTranslationReview.update({ deleted: 1 }, { where: { questionId: questionId } }).then(function (count) {
                return cb();
            }).catch(function (err) {
                return cb(err);
            });
        } catch (e) {
            return setImmediate(cb, e);
        }
    },
    
    deleteQuestionTranslationReviews: function (questionTranslationId, cb) {
        try {
            QuestionOrTranslationReview.update({ deleted: 1 }, { where: { questionTranslationId: questionTranslationId } }).then(function (count) {
                return cb();
            }).catch(function (err) {
                return cb(err);
            });
        } catch (e) {
            return setImmediate(cb, e);
        }
    },
    
    addQuestionAnalyticEvent: function (clientSession, eventData, cb) {
        try {
            var clientInfo = {
                userId: clientSession.getUserId(),
                appId: clientSession.getClientInfo().appConfig.appId,
                sessionIp: clientSession.getClientInfo().ip,
                sessionId: clientSession.getId()
            };
            AnalyticEvent.addQuestionEvent(clientInfo, eventData, cb);
        } catch (e) {
            return setImmediate(cb, e);
        }
    }
    
    
};
