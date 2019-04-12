var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var AutoMapper = AutoMapperInstance.AutoMapper;
var CrudHelper = AutoMapperInstance.CrudHelper;
var Database = require('nodejs-database').getInstance(Config);
var RegionalSetting = Database.RdbmsService.Models.Question.RegionalSetting;
var RegionalSettingHasLanguage = Database.RdbmsService.Models.Question.RegionalSettingHasLanguage;
var Language = Database.RdbmsService.Models.Question.Language;
var WorkorderHasRegionalSetting = Database.RdbmsService.Models.Workorder.WorkorderHasRegionalSetting;
var QuestionHasRegionalSetting = Database.RdbmsService.Models.Question.QuestionHasRegionalSetting;
var TenantService = require('../services/tenantService.js');

var INSTANCE_MAPPINGS = {
    'regionalSettingModel': [
        {
            destination: '$root',
            model: RegionalSetting
        },
        {
            destination: 'languagesIds',
            model: RegionalSettingHasLanguage,
            attribute: RegionalSettingHasLanguage.tableAttributes.languageId,
            limit: 0
        }
    ],
    'regionalSettingCreateModel': [
        {
            destination: '$root',
            model: RegionalSetting
        },
        {
            destination: 'languagesIds',
            model: RegionalSettingHasLanguage,
            attribute: RegionalSettingHasLanguage.tableAttributes.languageId
        }
    ],
    'languageModel': [
        {
            destination: '$root',
            model: Language
        }
    ],
    'languageCreateModel': [
        {
            destination: '$root',
            model: Language
        }
    ],
};
INSTANCE_MAPPINGS['regionalSettingUpdateModel'] = INSTANCE_MAPPINGS['regionalSettingCreateModel'];
INSTANCE_MAPPINGS['languageUpdateModel'] = INSTANCE_MAPPINGS['languageCreateModel'];

module.exports = {

    /**
     *
     * @param {{}} params
     * @param {{}} callback
     * @returns {*}
     */
    regionalSettingUpdate: function (params, message, clientSession, callback) {
        var self = this;
        try {
            var create = true;
            var regionalSettingId = undefined;
            var mapBySchema = 'regionalSettingCreateModel';
            if (_.has(params, 'id') && params.id) {
                create = false;
                regionalSettingId = params.id;
                mapBySchema = 'regionalSettingUpdateModel';
            }
            var regionalSettingItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, params, true);
            var regionalSettingItemAssociates = undefined;
            async.series([
                // Create/update main entity
                function (next) {
                    if (create) {
                        if (!_.has(regionalSettingItem, 'createDate')) {
                            regionalSettingItem.createDate = _.now();
                        }
                        return RegionalSetting.create(regionalSettingItem).then(function (regionalSetting) {
                            regionalSettingId = regionalSetting.get({ plain: true }).id;
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } else {
                        return RegionalSetting.update(regionalSettingItem, { where: { id: regionalSettingItem.id } }).then(function (count) {
                            if (count[0] === 0) {
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    }
                },
                // Populate regionalSetting associated entity values
                function (next) {
                    if (!create) {
                        return setImmediate(next, null);
                    }
                    try {
                        regionalSettingItemAssociates = AutoMapper.mapAssociatesDefinedBySchema(
                            mapBySchema, INSTANCE_MAPPINGS, params, {
                                field: 'regionalSettingId',
                                value: regionalSettingId,
                            }
                        );
                        return setImmediate(next, null);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Delete child entities for every updateable associated entity
                function (next) {
                    if (create || _.isUndefined(regionalSettingItemAssociates)) {
                        return setImmediate(next, null);
                    }
                    async.mapSeries(regionalSettingItemAssociates, function (itemAssociate, remove) {
                        return itemAssociate.model.destroy({ where: { regionalSettingId: regionalSettingId } }).then(function (count) {
                            return remove();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, remove);
                        });
                    }, next);
                },
                // Bulk insert populated values for every updateable associated entity
                function (next) {
                    if (_.isUndefined(regionalSettingItemAssociates)) {
                        return setImmediate(next, null);
                    }
                    async.mapSeries(regionalSettingItemAssociates, function (itemAssociate, create) {
                        if (itemAssociate.values.length === 0) {
                            return create();
                        }
                        return itemAssociate.model.bulkCreate(itemAssociate.values).then(function (records) {
                            if (records.length !== itemAssociate.values.length) {
                                return create(Errors.QuestionApi.FatalError);
                            }
                            return create();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, create);
                        });
                    }, next);
                },
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                params.id = regionalSettingId;
                return self.regionalSettingGet(params, message, clientSession, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     *
     * @param {{}} params
     * @param {{}} callback
     * @returns {*}
     */
    regionalSettingAddLanguage: function (params, callback) {
        var self = this;
        try {
            return RegionalSettingHasLanguage.create(params).then(function (createdRecord) {
                if (!createdRecord) {
                    return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
                }
                return CrudHelper.callbackSuccess(null, callback);
            }).catch(function (err) {
                return CrudHelper.callbackError(err, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     *
     * @param {{}} params
     * @param {{}} callback
     * @returns {*}
     */
    regionalSettingRemoveLanguage: function (params, callback) {
        var self = this;
        try {
            return RegionalSettingHasLanguage.destroy({
                where: {
                    regionalSettingId: params.regionalSettingId,
                    languageId: params.languageId
                }
            }).then(function (count) {
                if (count === 0) {
                    return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
                }
                return CrudHelper.callbackSuccess(null, callback);
            }).catch(function (err) {
                return CrudHelper.callbackError(err, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    regionalSettingDelete: function (params, callback) {
        try {
            async.mapSeries([
                RegionalSettingHasLanguage,
            ], function (model, next) {
                try {
                    return model.destroy({ where: { regionalSettingId: params.id } }).then(function (count) {
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                } catch (ex) {
                    return setImmediate(next, ex);
                }
            }, function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return RegionalSetting.destroy({ where: { id: params.id } }).then(function (count) {
                    if (count === 0) {
                        return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
                    }
                    return CrudHelper.callbackSuccess(null, callback);
                }).catch(function (err) {
                    return CrudHelper.callbackError(err, callback);
                });
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    regionalSettingGet: function (params, message, clientSession, callback) {
        var sessionTenantId;
        var questionRatingItem;
        try {
            var include = CrudHelper.include(RegionalSetting, [
                RegionalSettingHasLanguage
            ]);
            return async.series([
                // Get tenant id from client session object
                function (next) {
                    TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if (err) {
                            return next(err);
                        }
                        sessionTenantId = tenantId;
                        return next();
                    });
                },
                // Get regional setting record
                function (next) {
                    include = CrudHelper.includeWhere(include, Language, { tenantId: sessionTenantId }, true);
                    return RegionalSetting.findOne({
                        where: { id: params.id },
                        include: include,
                        subQuery: false
                    }).then(function (regionalSetting) {
                        if (!regionalSetting) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        regionalSettingItem = AutoMapper.mapDefinedBySchema('regionalSettingModel', INSTANCE_MAPPINGS, regionalSetting);
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess({ regionalSetting: regionalSettingItem }, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    regionalSettingList: function (params, message, clientSession, isInternal, callback) {
        try {
            var mapBySchema = 'regionalSettingModel';
            var attributes = _.keys(RegionalSetting.attributes);
            var orderBy = CrudHelper.orderBy(params, RegionalSetting);
            var searchBy = CrudHelper.searchBy(params, RegionalSetting);
            var include = CrudHelper.includeDefined(RegionalSetting, [
                Language,
                RegionalSettingHasLanguage
            ], params, mapBySchema, INSTANCE_MAPPINGS);
            var sessionTenantId;
            var regionalSettingItems = [];
            var total = 0;
            async.series([
                // Get tenantId from global session
                function (next) {
                    if (isInternal) {
                        return next();
                    } else {
                        TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                            if (err) {
                                return next(err);
                            }
                            sessionTenantId = tenantId;
                            return next();
                        });
                    }
                },
                // Populate total count and regional setting ids (involve base model and nested models only with defined search criterias)
                function (next) {
                    if (!isInternal)
                        include = CrudHelper.includeWhere(include, Language, {tenantId: sessionTenantId}, true);
                    return RegionalSetting.findAndCountAll({
                        limit: params.limit === 0 ? null : params.limit,
                        offset: params.limit === 0 ? null : params.offset,
                        where: searchBy,
                        order: orderBy,
                        include: include,
                        subQuery: false
                    }).then(function (regionalSettings) {
                        total = regionalSettings.count;
                        if (!_.has(searchBy, 'id')) {
                            var regionalSettingsIds = [];
                            _.forEach(regionalSettings.rows, function (regionalSetting) {
                                regionalSettingsIds.push(regionalSetting.id);
                            });
                            searchBy = {
                                id: { '$in': regionalSettingsIds }
                            };
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Populate model list with all nested model elements
                function (next) {
                    if (total === 0) {
                        return setImmediate(next, null);
                    }
                    include = CrudHelper.include(RegionalSetting, [
                        RegionalSettingHasLanguage
                    ], params, mapBySchema, INSTANCE_MAPPINGS);
                    if (!isInternal)
                        include = CrudHelper.includeWhere(include, Language, { tenantId: sessionTenantId }, true);
                    return RegionalSetting.findAll({
                        where: searchBy,
                        order: orderBy,
                        attributes: attributes,
                        include: include,
                        subQuery: false
                    }).then(function (regionalSettings) {
                        var instanceMappings = AutoMapper.limitedMappings(INSTANCE_MAPPINGS);
                        regionalSettingItems = AutoMapper.mapListDefinedBySchema(mapBySchema, instanceMappings, regionalSettings);
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess({
                    items: regionalSettingItems,
                    limit: params.limit,
                    offset: params.offset,
                    total: total,
                }, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * Set RegionalSetting status
     * @param {{}} params
     * @param {ClientSession} clientSession
     * @param {{}} callback
     * @returns {*}
     */
    regionalSettingSetStatus: function (params, clientSession, callback) {
        var self = this;
        if (!params.hasOwnProperty('status') || (params.status !== 'active' && params.status !== 'inactive')) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        try {
            return RegionalSetting.update({ status: params.status }, { where: { id: params.id } }).then(function (count) {
                if (count[0] === 0) {
                    return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
                }
                return CrudHelper.callbackSuccess(null, callback);
            }).catch(function (err) {
                return CrudHelper.callbackError(err, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     *
     * @param {{}} params
     * @param {{}} callback
     * @returns {*}
     */
    languageUpdate: function (params, message, clientSession, callback) {
        var self = this;
        try {
            var create = true;
            var languageId = undefined;
            var mapBySchema = 'languageCreateModel';
            if (_.has(params, 'id') && params.id) {
                create = false;
                languageId = params.id;
                mapBySchema = 'languageUpdateModel';
            }
            var languageItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, params, true);
            var sessionTenantId;
            async.series([
                // Get tenantId from global session
                function (next) {
                    TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if (err) {
                            return next(err);
                        }
                        sessionTenantId = tenantId;
                        return next();
                    });
                },
                // Create/update main entity
                function (next) {
                    if (create) {
                        //console.log('\n\n######tenantId:', sessionTenantId, '\n\n'); 
                        languageItem.tenantId = sessionTenantId;
                        return Language.create(languageItem).then(function (language) {
                            languageId = language.get({ plain: true }).id;
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } else {
                        return Language.update(languageItem, { where: { id: languageItem.id, tenantId: sessionTenantId } }).then(function (count) {
                            if (count[0] === 0) {
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    }
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                params.id = languageId;
                return self.languageGet(params, message, clientSession, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    languageDelete: function (params, callback) {
        try {
            return Language.destroy({ where: { id: params.id } }).then(function (count) {
                if (count === 0) {
                    return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
                }
                return CrudHelper.callbackSuccess(null, callback);
            }).catch(function (err) {
                return CrudHelper.callbackError(err, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    languageGet: function (params, message, clientSession, callback) {
        try {
            TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return Language.findOne({
                    where: { id: params.id, tenantId: tenantId }
                }).then(function (language) {
                    if (!language) {
                        return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
                    }
                    var languageItem = AutoMapper.mapDefinedBySchema('languageModel', INSTANCE_MAPPINGS, language);
                    return CrudHelper.callbackSuccess({ language: languageItem }, callback);
                }).catch(function (err) {
                    return CrudHelper.callbackError(err, callback);
                });
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    languageList: function (params, message, clientSession, isInternal, callback) {
        try {
            var orderBy = CrudHelper.orderBy(params, Language);
            var searchBy = CrudHelper.searchBy(params, Language);
            if (!isInternal){
                TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                    if (err) {
                        return CrudHelper.callbackError(err, callback);
                    }
                    searchBy.tenantId = tenantId;
                    return Language.findAndCountAll({
                        limit: params.limit === 0 ? null : params.limit,
                        offset: params.limit === 0 ? null : params.offset,
                        where: searchBy,
                        order: orderBy
                    }).then(function (languages) {
                        var languageItems = AutoMapper.mapListDefinedBySchema('languageModel', INSTANCE_MAPPINGS, languages.rows);
                        return CrudHelper.callbackSuccess({
                            items: languageItems,
                            limit: params.limit,
                            offset: params.offset,
                            total: languages.count,
                        }, callback);
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, callback);
                    });
                });
            } else {
                Language.findAndCountAll({
                    limit: params.limit === 0 ? null : params.limit,
                    offset: params.limit === 0 ? null : params.offset,
                    where: searchBy,
                    order: orderBy
                }).then(function (languages) {
                    var languageItems = AutoMapper.mapListDefinedBySchema('languageModel', INSTANCE_MAPPINGS, languages.rows);
                    return CrudHelper.callbackSuccess({
                        items: languageItems,
                        limit: params.limit,
                        offset: params.offset,
                        total: languages.count,
                    }, callback);
                }).catch(function (err) {
                    return CrudHelper.callbackError(err, callback);
                });
            }

        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    enumList: function (params, callback) {
        try {
            var data = AutoMapper.getEnumMappedBySchema(params.model, INSTANCE_MAPPINGS, params.field);
            return CrudHelper.callbackSuccess({ items: data }, callback);
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

};