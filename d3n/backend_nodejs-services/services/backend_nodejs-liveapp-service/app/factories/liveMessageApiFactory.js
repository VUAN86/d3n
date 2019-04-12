var _ = require('lodash');
var async = require('async');
var logger = require('nodejs-logger')();
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var AutoMapper = AutoMapperInstance.AutoMapper;
var CrudHelper = AutoMapperInstance.CrudHelper;
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var AerospikeLiveMessage = require('nodejs-aerospike').getInstance(Config).KeyvalueService.Models.AerospikeLiveMessage;
var LiveMessage = Database.RdbmsService.Models.MessageCenter.LiveMessage;
var Application = Database.RdbmsService.Models.Application.Application;

var INSTANCE_MAPPINGS = {
    'liveMessageModel': [
        {
            destination: '$root',
            model: LiveMessage,
        }
    ],
    'liveMessageCreateModel': [
        {
            destination: '$root',
            model: LiveMessage,
        }
    ],
    'liveMessageSendModel': [
        {
            destination: '$root',
            model: LiveMessage,
        }
    ]
}
INSTANCE_MAPPINGS['liveMessageUpdateModel'] = INSTANCE_MAPPINGS['liveMessageCreateModel'];

module.exports = {

    /**
     * Create or update live message
     * @param {{}} params
     * @param {ClientSession} clientSession
     * @param {function} callback
     * @returns {*}
     */
    update: function (params, message, clientSession, callback) {
        var self = this;
        try {
            var create = true;
            var liveMessageId = undefined;
            var mapBySchema = 'liveMessageCreateModel';
            if (_.has(params, 'id') && params.id) {
                create = false;
                liveMessageId = params.id;
                mapBySchema = 'liveMessageUpdateModel';
            }
            var liveMessageItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, params, true);
            async.series([
                // Create/update main entity
                function (next) {
                    liveMessageItem.updaterResourceId = clientSession.getUserId();
                    if (create) {
                        if (!_.has(liveMessageItem, 'createDate')) {
                            liveMessageItem.createDate = _.now();
                        }
                        // Creator not set -> set current user
                        if (!_.has(liveMessageItem, 'creatorResourceId')) {
                            liveMessageItem.creatorResourceId = clientSession.getUserId();
                        }
                        
                        return LiveMessage.create(liveMessageItem).then(function (liveMessage) {
                            liveMessageId = liveMessage.get({ plain: true }).id;

                            //nothing here for now - for future extensibility
                            return setImmediate(next);
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } else {
                        if (!_.has(liveMessageItem, 'updateDate')) {
                            liveMessageItem.updateDate = _.now();
                        }
                        // Updater not set -> set current user
                        if (!_.has(liveMessageItem, 'updaterResourceId')) {
                            liveMessageItem.updaterResourceId = clientSession.getUserId();
                        }

                        return LiveMessage.update(liveMessageItem, { where: { id: liveMessageItem.id }, individualHooks: true }).then(function (count) {
                            if (count[0] === 0) {
                                return setImmediate(next, Errors.NoRecordFound);
                            }
                            return setImmediate(next);
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    }
                },
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                params.id = liveMessageId;
                return self.get(params, message, clientSession, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    get: function (params, message, clientSession, callback) {
        var liveMessageItem;
        try {
            var include = CrudHelper.include(LiveMessage, [
                Application
            ]);
            return async.series([
                // Prevent listing if entry is blocked by another user and block date not expired and less than 2h
                function (next) {
                    return CrudHelper.checkModelEntryBlockedByUser(LiveMessage, params, clientSession, next);
                },
                // Get live message record
                function (next) {
                    return LiveMessage.findOne({
                        where: { id: params.id },
                        include: include
                    }).then(function (liveMessage) {
                        if (!liveMessage) {
                            return next(Errors.NoRecordFound);
                        }
                        liveMessageItem = AutoMapper.mapDefinedBySchema('liveMessageModel', INSTANCE_MAPPINGS, liveMessage);
                        return setImmediate(next);
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess({ liveMessage: liveMessageItem }, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    list: function (params, message, clientSession, callback) {
        var total = 0;
        var liveMessageItems = [];
        var liveMessageTotal = 0;
        var mapBySchema = 'liveMessageModel';
        try {
            var orderBy = CrudHelper.orderBy(params, LiveMessage);
            var searchBy = CrudHelper.searchBy(params, LiveMessage);
            var instanceMappings = _.cloneDeep(INSTANCE_MAPPINGS);
            var rootModel = _.find(instanceMappings[mapBySchema], { destination: '$root' });
            if (_.has(params, 'searchBy') && _.has(params.searchBy, 'fulltext')) {
                searchBy = CrudHelper.searchByFullText(params, LiveMessage, mapBySchema, instanceMappings, clientSession);
            } else if (params._unique) {
                searchBy = CrudHelper.searchByCustom(params, LiveMessage, mapBySchema, instanceMappings, clientSession);
            }
            var include = CrudHelper.include(LiveMessage, [
                Application
            ], params, mapBySchema, instanceMappings);
            return async.series([
                // Get live message records
                function (next) {
                    return LiveMessage.findAndCountAll({
                        limit: params.limit === 0 ? null : params.limit,
                        offset: params.limit === 0 ? null : params.offset,
                        where: searchBy,
                        order: orderBy,
                        include: include,
                        subQuery: false
                    }).then(function (liveMessages) {
                        liveMessageTotal = liveMessages.count;
                        liveMessageItems = AutoMapper.mapListDefinedBySchema(mapBySchema, instanceMappings, liveMessages.rows);
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess({
                    items: liveMessageItems,
                    limit: params.limit,
                    offset: params.offset,
                    total: liveMessageTotal
                }, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    send: function (params, message, clientSession, callback) {
        if (!params.hasOwnProperty('id')) {
            return CrudHelper.callbackError(Errors.ValidationFailed, callback);
        }
        var self = this;
        try {
            var liveMessage;
            async.series ([
                //get live message
                function (next) {
                    try {
                        return getForSend(params, message, clientSession, function (err, liveMessageItem) {
                            if (err) {
                                return next(err);
                            }
                            if (!liveMessageItem) {
                                return next(Errors.NoRecordFound);
                            }
                            liveMessage = liveMessageItem;
                            try {
                                if (liveMessage.content && !_.isObject(liveMessage.content)) {
                                    liveMessage.content = JSON.parse(liveMessage.content);
                                }
                            } catch (ex) {
                                logger.error('liveMessageSend error on JSON parsing: content', liveMessage.content, ex);
                                return next(Errors.ValidationFailed);
                            }
                            try {
                                if (liveMessage.filter && !_.isObject(liveMessage.filter)) {
                                    liveMessage.filter = JSON.parse(liveMessage.filter);
                                }
                            } catch (ex) {
                                logger.error('liveMessageSend error on JSON parsing: filter', liveMessage.filter, ex);
                                return next(Errors.ValidationFailed);
                            }
                            return setImmediate(next);
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },

                //publish live message
                function (next) {
                    try {
                        return AerospikeLiveMessage.publish(liveMessage, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess(null, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },
};

function getForSend(params, message, clientSession, callback) {
    try {
        var liveMessageSend;
        return async.series([
            // Get live message by id
            function (next) {
                var liveMessageItem = {};
                var attributes = _.keys(LiveMessage.attributes);
                var include = CrudHelper.include(LiveMessage, [
                    Application
                ]);
                return LiveMessage.findOne({
                    where: { id: params.id },
                    attributes: attributes,
                    include: include
                }).then(function (liveMessage) {
                    if (!liveMessage) {
                        return next(Errors.NoRecordFound);
                    }
                    // Custom live message mapper object
                    var liveMessagePlain = liveMessage.get({ plain: true });
                    AutoMapper.mapDefined(liveMessagePlain, liveMessageItem)
                        .forMember('id')
                        .forMember('applicationId')
                        .forMember('creatorResourceId')
                        .forMember('updaterResourceId')
                        .forMember('content')
                        .forMember('filter')
                        .forMember('incentiveAmount')
                        .forMember('incentiveCurrency');
                    if (_.has(liveMessagePlain, 'createDate')) {
                        liveMessageItem.createDate = DateUtils.isoPublish(liveMessagePlain.createDate);
                    }
                    if (_.has(liveMessagePlain, 'updateDate')) {
                        liveMessageItem.updateDate = DateUtils.isoPublish(liveMessagePlain.updateDate);
                    }
                    _.each(['sms', 'email', 'inApp', 'push','incentive'], function (param) {
                        liveMessageItem[param] = liveMessagePlain[param] > 0;
                    });
                    liveMessageSend = AutoMapper.mapDefinedBySchema('liveMessageSendModel', INSTANCE_MAPPINGS, liveMessageItem);
                    return next();
                }).catch(function (err) {
                    return CrudHelper.callbackError(err, next);
                });
            },
        ], function (err) {
            if (err) {
                return CrudHelper.callbackError(err, callback);
            }
            logger.info('LiveMessageApiFactory.getForSend', JSON.stringify(liveMessageSend));
            return CrudHelper.callbackSuccess(liveMessageSend, callback);
        });
    } catch (ex) {
        return CrudHelper.callbackError(ex, callback);
    }
}
