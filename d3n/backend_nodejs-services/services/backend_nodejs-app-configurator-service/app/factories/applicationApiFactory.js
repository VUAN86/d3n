var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var AutoMapper = AutoMapperInstance.AutoMapper;
var CrudHelper = AutoMapperInstance.CrudHelper;
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var Tenant = Database.RdbmsService.Models.Tenant.Tenant;
var Application = Database.RdbmsService.Models.Application.Application;
var ApplicationAnimation = Database.RdbmsService.Models.Application.ApplicationAnimation;
var ApplicationCharacter = Database.RdbmsService.Models.Application.ApplicationCharacter;
var PaymentType = Database.RdbmsService.Models.Billing.PaymentType;
var ApplicationHasPaymentType = Database.RdbmsService.Models.Application.ApplicationHasPaymentType;
var ApplicationHasRegionalSetting = Database.RdbmsService.Models.Application.ApplicationHasRegionalSetting;
var RegionalSettingHasLanguage = Database.RdbmsService.Models.Question.RegionalSettingHasLanguage;
var RegionalSetting = Database.RdbmsService.Models.Question.RegionalSetting;
var Language = Database.RdbmsService.Models.Question.Language;
var ApplicationHasApplicationAnimation = Database.RdbmsService.Models.Application.ApplicationHasApplicationAnimation;
var ApplicationHasApplicationCharacter = Database.RdbmsService.Models.Application.ApplicationHasApplicationCharacter;
var ApplicationHasGame = Database.RdbmsService.Models.Application.ApplicationHasGame;
var Game = Database.RdbmsService.Models.Game.Game;
var ApplicationHasGameModule = Database.RdbmsService.Models.Application.ApplicationHasGameModule;
var GameModule = Database.RdbmsService.Models.Game.GameModule;
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikeApp = KeyvalueService.Models.AerospikeApp;
var AerospikeGlobalClientSession = KeyvalueService.Models.AerospikeGlobalClientSession;
var GameApiFactory = require('./gameApiFactory.js');
var TenantService = require('../services/tenantService.js');
var logger = require('nodejs-logger')();
var S3Client = require('nodejs-s3client');
var s3ClientAppConfig = S3Client.getInstance(_.merge(_.merge({}, S3Client.Config), {amazon_s3: {bucket: Config.appConfigBucket}})).Storage;

// schema validation is used in order to validate app publishing structure
var jjv = require('jjv')();
var protocolSchemas = require('nodejs-protocol-schemas');
var schemas = protocolSchemas.schemas;
jjv.addSchema('applicationPublishModel', schemas['application'].definitions.applicationPublishModel);

var INSTANCE_MAPPINGS = {
    'applicationModel': [
        {
            destination: '$root',
            model: Application
        },
        {
            destination: 'paymentTypesIds',
            model: ApplicationHasPaymentType,
            attribute: ApplicationHasPaymentType.tableAttributes.paymentTypeId
        },
        {
            destination: 'regionalSettingsIds',
            model: ApplicationHasRegionalSetting,
            attribute: ApplicationHasRegionalSetting.tableAttributes.regionalSettingId
        },
        {
            destination: 'activeAnimationsIds',
            model: ApplicationHasApplicationAnimation,
            attribute: ApplicationHasApplicationAnimation.tableAttributes.applicationAnimationId
        },
        {
            destination: 'activeCharactersIds',
            model: ApplicationHasApplicationCharacter,
            attribute: ApplicationHasApplicationCharacter.tableAttributes.applicationCharacterId
        },
        {
            destination: 'gamesIds',
            model: ApplicationHasGame,
            attribute: ApplicationHasGame.tableAttributes.gameId
        },
        {
            destination: 'gameModulesIds',
            model: ApplicationHasGameModule,
            attribute: ApplicationHasGameModule.tableAttributes.gameModuleId
        },
    ],
    'applicationCreateModel': [
        {
            destination: '$root',
            model: Application
        },
        {
            destination: 'paymentTypesIds',
            model: ApplicationHasPaymentType,
            attribute: ApplicationHasPaymentType.tableAttributes.paymentTypeId
        },
        {
            destination: 'regionalSettingsIds',
            model: ApplicationHasRegionalSetting,
            attribute: ApplicationHasRegionalSetting.tableAttributes.regionalSettingId
        },
    ],
    'gameModuleModel': [
        {
            destination: '$root',
            model: GameModule
        }
    ],
    'applicationAnimationModel': [
        {
            destination: '$root',
            model: ApplicationAnimation
        }
    ],
    'applicationCharacterModel': [
        {
            destination: '$root',
            model: ApplicationCharacter
        }
    ],
    'applicationPublishAppModel': [
        {
            destination: '$root',
            model: Application
        }
    ],
    'applicationPublishF4mModel': [],
    'applicationPublishEnvironmentModel': [],
    'applicationPublishTenantModel': [
        {
            destination: '$root',
            model: Tenant
        }
    ],
    'applicationPublishModel': [],
};
INSTANCE_MAPPINGS['applicationUpdateModel'] = INSTANCE_MAPPINGS['applicationCreateModel'];

module.exports = {

    applicationUpdate: function (params, message, clientSession, callback) {
        var self = this;
        try {
            var create = true;
            var applicationId = undefined;
            var mapBySchema = 'applicationCreateModel';
            if (_.has(params, 'id') && params.id) {
                create = false;
                applicationId = params.id;
                mapBySchema = 'applicationUpdateModel';
            }
            var newItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, params, true);
            var applicationItemAssociates = undefined;
            var sessionTenantId;
            async.series([
                // get tenantId from global session
                function (next) {
                    TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if (err) {
                            logger.error('applicationUpdate getSessionTenantId error: ', err);
                            return next(err);
                        }
                        sessionTenantId = tenantId;
                        return next();
                    });
                },
                // Create/update main entity
                function (next) {
                    if (create) {
                        newItem.tenantId = sessionTenantId;
                        if (!_.has(newItem, 'createDate')) {
                            newItem.createDate = _.now();
                        }
                        // Creator not set -> set current user
                        if (!_.has(newItem, 'creatorResourceId')) {
                            newItem.creatorResourceId = clientSession.getUserId();
                        }
                        
                        // if fyber data not provided set default data
                        if (!_.has(newItem, 'configuration.fyber')) {
                            newItem.configuration.fyber = Config.fyber;
                        }
                        
                        return Application.create(newItem).then(function (result) {
                            applicationId = result.get({ plain: true }).id;
                            return next();
                        }).catch(function (err) {
                            logger.error('applicationUpdate Application.create error: ', err);
                            return next(CrudHelper.checkViolation(err, { uniqueKeyConstraintViolation: Errors.ApplicationApi.AppNameNotUniquePerTenant }));
                        });
                    } else {
                        // ensure item exists and is not deployed
                        return Application.findOne({
                            where: { id: newItem.id, tenantId: sessionTenantId}
                        }).then(function (app) {
                            if (!app) {
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            // everything ok, update item
                            Application.update(newItem, { where: { id: newItem.id }, individualHooks: true }).then(function (count) {
                                if (count[0] === 0) {
                                    return next(Errors.DatabaseApi.NoRecordFound);
                                }
                                return next();
                            }).catch(function (err) {
                                logger.error('applicationUpdate Application.update error: ', err);
                                return next(CrudHelper.checkViolation(err, { uniqueKeyConstraintViolation: Errors.ApplicationApi.AppNameNotUniquePerTenant }));
                            });
                        }).catch(function (err) {
                            logger.error('applicationUpdate 1 error: ', err);
                            return CrudHelper.callbackError(err, next);
                        });
                    }
                },
                // Populate application associated entity values
                function (next) {
                    try {
                        applicationItemAssociates = AutoMapper.mapAssociatesDefinedBySchema(
                            mapBySchema, INSTANCE_MAPPINGS, params, {
                                field: 'applicationId',
                                value: applicationId,
                            }
                        );
                        return setImmediate(next, null);
                    } catch (ex) {
                        logger.error('applicationUpdate AutoMapper.mapAssociatesDefinedBySchema error: ', ex);
                        return setImmediate(next, ex);
                    }
                },
                // Delete child entities for every updateable associated entity
                function (next) {
                    if (create || _.isUndefined(applicationItemAssociates)) {
                        return setImmediate(next, null);
                    }
                    async.mapSeries(applicationItemAssociates, function (itemAssociate, remove) {
                        //if (itemAssociate.values.length === 0) {
                        //    return remove();
                        //}
                        itemAssociate.model.destroy({ where: { applicationId: applicationId } }).then(function (count) {
                            return remove();
                        }).catch(function (err) {
                            logger.error('applicationUpdate itemAssociate.model.destroy error: ', ex);
                            return CrudHelper.callbackError(err, remove);
                        });
                    }, next);
                },
                // Bulk insert populated values for every updateable associated entity
                function (next) {
                    if (_.isUndefined(applicationItemAssociates)) {
                        return setImmediate(next, null);
                    }
                    async.mapSeries(applicationItemAssociates, function (itemAssociate, create) {
                        if (itemAssociate.values.length === 0) {
                            return create();
                        }
                        itemAssociate.model.bulkCreate(itemAssociate.values).then(function (records) {
                            if (records.length !== itemAssociate.values.length) {
                                return create(Errors.QuestionApi.FatalError);
                            }
                            return create();
                        }).catch(function (err) {
                            logger.error('applicationUpdate itemAssociate.model.bulkCreate error: ', ex);
                            return CrudHelper.callbackError(err, create);
                        });
                    }, next);
                },
            ], function (err) {
                if (err) {
                    logger.error('applicationUpdate 2 error: ', ex);
                    return CrudHelper.callbackError(err, callback);
                }
                params.id = applicationId;
                return self.applicationGet(params, message, clientSession, callback);
            });
        } catch (ex) {
            logger.error('applicationUpdate error: ', ex);
            return CrudHelper.callbackError(ex, callback);
        }
    },

    applicationGet: function (params, message, clientSession, callback) {
        try {
            var response = {};
            var responseItem = 'application';
            var mapBySchema = 'applicationModel';
            var include = CrudHelper.include(Application, [
                ApplicationHasApplicationAnimation,
                ApplicationHasApplicationCharacter,
                ApplicationHasGame,
                ApplicationHasGameModule,
                ApplicationHasPaymentType,
                ApplicationHasRegionalSetting,
            ], params, mapBySchema, INSTANCE_MAPPINGS);
            include = CrudHelper.includeWhere(include, ApplicationHasApplicationAnimation, {
                status: ApplicationHasApplicationAnimation.constants().STATUS_ACTIVE
            }, false);
            include = CrudHelper.includeWhere(include, ApplicationHasApplicationCharacter, {
                status: ApplicationHasApplicationCharacter.constants().STATUS_ACTIVE
            }, false);
            include = CrudHelper.includeWhere(include, ApplicationHasGameModule, {
                status: ApplicationHasGameModule.constants().STATUS_ACTIVE
            }, false);

            TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return Application.findOne({
                    where: { id: params.id, tenantId: tenantId },
                    include: include,
                    subQuery: false
                }).then(function (application) {
                    if (!application) {
                        return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
                    }
                    var applicationItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, application);
                    response[responseItem] = applicationItem;
                    return CrudHelper.callbackSuccess(response, callback);
                }).catch(function (err) {
                    return CrudHelper.callbackError(err, callback);
                });
            });

        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    applicationList: function (params, message, clientSession, callback) {
        try {
            var mapBySchema = 'applicationModel';
            var attributes = _.keys(Application.attributes);
            var orderBy = CrudHelper.orderBy(params, Application);
            var searchBy = CrudHelper.searchBy(params, Application);
            var applicationItems = [];
            var total = 0;
            async.series([
                // Get tenantId from global session
                function (next) {
                    TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if (err) {
                            return next(err);
                        }
                        searchBy.tenantId = tenantId;
                        return next();
                    });
                },
                // Populate total count and regional setting ids (involve base model and nested models only with defined search criterias)
                function (next) {
                    var include = CrudHelper.includeDefined(Application, [
                        ApplicationHasApplicationAnimation,
                        ApplicationHasApplicationCharacter,
                        ApplicationHasGame,
                        ApplicationHasGameModule,
                        ApplicationHasPaymentType,
                        ApplicationHasRegionalSetting,
                    ], params, mapBySchema, INSTANCE_MAPPINGS);
                    return Application.findAndCountAll({
                        limit: params.limit === 0 ? null : params.limit,
                        offset: params.limit === 0 ? null : params.offset,
                        where: searchBy,
                        order: orderBy,
                        include: include,
                        subQuery: false
                    }).then(function (applications) {
                        total = applications.count;
                        if (!_.has(searchBy, 'id')) {
                            var applicationsIds = [];
                            _.forEach(applications.rows, function (application) {
                                applicationsIds.push(application.id);
                            });
                            searchBy = {
                                id: { '$in': applicationsIds }
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
                        return next();
                    }
                    var include = CrudHelper.include(Application, [
                        ApplicationHasApplicationAnimation,
                        ApplicationHasApplicationCharacter,
                        ApplicationHasGame,
                        ApplicationHasGameModule,
                        ApplicationHasPaymentType,
                        ApplicationHasRegionalSetting,
                    ], params, mapBySchema, INSTANCE_MAPPINGS);
                    include = CrudHelper.includeWhere(include, ApplicationHasApplicationAnimation, {
                        status: ApplicationHasApplicationAnimation.constants().STATUS_ACTIVE
                    }, false);
                    include = CrudHelper.includeWhere(include, ApplicationHasApplicationCharacter, {
                        status: ApplicationHasApplicationCharacter.constants().STATUS_ACTIVE
                    }, false);
                    include = CrudHelper.includeWhere(include, ApplicationHasGameModule, {
                        status: ApplicationHasGameModule.constants().STATUS_ACTIVE
                    }, false);
                    return Application.findAll({
                        where: searchBy,
                        order: orderBy,
                        attributes: attributes,
                        include: include,
                        subQuery: false
                    }).then(function (applications) {
                        applicationItems = AutoMapper.mapListDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, applications);
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
                    items: applicationItems,
                    limit: params.limit,
                    offset: params.offset,
                    total: total,
                }, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    applicationDelete: function (params, callback) {
        try {
            async.mapSeries([
                ApplicationHasApplicationAnimation,
                ApplicationHasApplicationCharacter,
                ApplicationHasGame,
                ApplicationHasGameModule,
                ApplicationHasPaymentType,
                ApplicationHasRegionalSetting,
            ], function (model, next) {
                try {
                    return model.destroy({ where: { applicationId: params.id } }).then(function (count) {
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
                return Application.destroy({ where: { id: params.id } }).then(function (count) {
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

    /**
     * Set Application status
     * @param {{}} params
     * @param {{}} callback
     * @returns {*}
     */
    applicationSetStatus: function (params, callback) {
        var self = this;
        if (!params.hasOwnProperty('status') || (
            params.status !== Application.constants().STATUS_ACTIVE &&
            params.status !== Application.constants().STATUS_INACTIVE &&
            params.status !== Application.constants().STATUS_DIRTY &&
            params.status !== Application.constants().STATUS_ARCHIVED)) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        try {
            async.series([
                // Check current status
                function (next) {
                    return Application.findOne({ where: { id: params.id } }).then(function (application) {
                        if (!application) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        // Only undeployed and inactive (but not archived) applications can be archived/activated
                        if (params.status === Application.constants().STATUS_ARCHIVED || params.status === Application.constants().STATUS_ACTIVE) {
                            if (application.status === Application.constants().STATUS_ARCHIVED) {
                                return next(Errors.ApplicationApi.AppIsArchived);
                            }
                        }
                        // Only undeployed and active games can be deactivated
                        if (params.status === Application.constants().STATUS_INACTIVE) {
                            if (application.status === Application.constants().STATUS_INACTIVE) {
                                return next(Errors.ApplicationApi.AppIsDeactivated);
                            }
                            if (application.status === Application.constants().STATUS_ARCHIVED) {
                                return next(Errors.ApplicationApi.AppIsArchived);
                            }
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Update status
                function (next) {
                    return Application.update({ status: params.status }, { where: { id: params.id }, individualHooks: true }).then(function (count) {
                        if (count[0] === 0) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
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

    /**
     * List Application animations
     * @param {{}} params
     * @param {{}} callback
     * @returns {*}
     */
    applicationAnimationList: function (params, message, clientSession, callback) {
        try {
            var orderBy = CrudHelper.orderBy(params, ApplicationAnimation);
            var searchBy = CrudHelper.searchBy(params, ApplicationAnimation);
            TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                var include = CrudHelper.include(ApplicationAnimation, [
                    ApplicationHasApplicationAnimation
                ]);
                include = CrudHelper.includeNestedWhere(include, ApplicationHasApplicationAnimation, Application, { tenantId: tenantId }, true);
                return ApplicationAnimation.findAndCountAll({
                    limit: params.limit === 0 ? null : params.limit,
                    offset: params.limit === 0 ? null : params.offset,
                    where: searchBy,
                    order: orderBy,
                    include: include,
                    distinct: true,
                    subQuery: false
                }).then(function (applicationAnimations) {
                    var applicationAnimationItems = AutoMapper.mapListDefinedBySchema('applicationAnimationModel', INSTANCE_MAPPINGS, applicationAnimations.rows);
                    return CrudHelper.callbackSuccess({
                        items: applicationAnimationItems,
                        limit: params.limit,
                        offset: params.offset,
                        total: applicationAnimations.count,
                    }, callback);
                }).catch(function (err) {
                    return CrudHelper.callbackError(err, callback);
                });
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * List Application characters
     * @param {{}} params
     * @param {{}} callback
     * @returns {*}
     */
    applicationCharacterList: function (params, message, clientSession, callback) {
        try {
            var orderBy = CrudHelper.orderBy(params, ApplicationCharacter);
            var searchBy = CrudHelper.searchBy(params, ApplicationCharacter);
            TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                var include = CrudHelper.include(ApplicationCharacter, [
                    ApplicationHasApplicationCharacter
                ]);
                include = CrudHelper.includeNestedWhere(include, ApplicationHasApplicationCharacter, Application, { tenantId: tenantId }, true);
                return ApplicationCharacter.findAndCountAll({
                    limit: params.limit === 0 ? null : params.limit,
                    offset: params.limit === 0 ? null : params.offset,
                    where: searchBy,
                    order: orderBy,
                    include: include,
                    distinct: true,
                    subQuery: false
                }).then(function (applicationCharacters) {
                    var applicationCharacterItems = AutoMapper.mapListDefinedBySchema('applicationCharacterModel', INSTANCE_MAPPINGS, applicationCharacters.rows);
                    return CrudHelper.callbackSuccess({
                        items: applicationCharacterItems,
                        limit: params.limit,
                        offset: params.offset,
                        total: applicationCharacters.count,
                    }, callback);
                }).catch(function (err) {
                    return CrudHelper.callbackError(err, callback);
                });
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * Set Application Animation status
     * @param {{}} params
     * @param {{}} callback
     * @returns {*}
     */
    applicationAnimationSetStatus: function (params, callback) {
        var self = this;
        if (!params.hasOwnProperty('status') || (params.status !== 'active' && params.status !== 'inactive')) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        try {
            return ApplicationHasApplicationAnimation.update({ status: params.status }, {
                where: {
                    applicationId: params.applicationId,
                    applicationAnimationId: params.applicationAnimationId
                }
            }).then(function (count) {
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
     * Set Application Character status
     * @param {{}} params
     * @param {{}} callback
     * @returns {*}
     */
    applicationCharacterSetStatus: function (params, callback) {
        var self = this;
        if (!params.hasOwnProperty('status') || (params.status !== 'active' && params.status !== 'inactive')) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        try {
            return ApplicationHasApplicationCharacter.update({ status: params.status }, {
                where: {
                    applicationId: params.applicationId,
                    applicationCharacterId: params.applicationCharacterId
                }
            }).then(function (count) {
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
     * Set Application Game Module status
     * @param {{}} params
     * @param {{}} callback
     * @returns {*}
     */
    gameModuleSetStatus: function (params, callback) {
        var self = this;
        if (!params.hasOwnProperty('status') || (params.status !== 'active' && params.status !== 'inactive')) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        try {
            return ApplicationHasGameModule.upsert({
                applicationId: params.applicationId,
                gameModuleId: params.gameModuleId,
                status: params.status
            }).then(function (count) {
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
     * Associate Game to Application
     * @param {{}} params
     * @param {{}} callback
     * @returns {*}
     */
    applicationGameAdd: function (params, callback) {
        var self = this;
        try {
            return ApplicationHasGame.create({ applicationId: params.applicationId, gameId: params.gameId }).then(function (result) {
                return CrudHelper.callbackSuccess(null, callback);
            }).catch(function (err) {
                return CrudHelper.callbackError(err, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * Remove association of Game from Application
     * @param {{}} params
     * @param {{}} callback
     * @returns {*}
     */
    applicationGameRemove: function (params, message, clientSession, callback) {
        var self = this;
        var gameStatus;
        try {
            return async.series([
                // Detect game status
                function (next) {
                    try {
                        return Game.findOne({
                            where: { id: params.gameId }
                        }).then(function (game) {
                            if (!game) {
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            gameStatus = game.get({ plain: true }).status;
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        return next(err);
                    }
                },
                // First unpublish game if it is already activated
                function (next) {
                    try {
                        if (gameStatus === Game.constants().STATUS_ACTIVE || gameStatus === Game.constants().STATUS_DIRTY) {
                            params.id = params.gameId;
                            return GameApiFactory.gameUnpublish(params, message, clientSession, next);
                        }
                        return next();
                    } catch (ex) {
                        return next(ex);
                    }
                },
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                // Unregister game from application
                try {
                    return ApplicationHasGame.destroy({ where: { applicationId: params.applicationId, gameId: params.gameId } }).then(function (count) {
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
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    applicationGetForPublish: function (params, clientSession, callback) {
        try {
            var self = this;

            self.buildGetAppConfigurationWithApp(params.id, function (err, item) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess(item, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    applicationPublish: function (params, clientSession, callback) {
        if (!params.hasOwnProperty('id')) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        var self = this;
        try {
            var application;
            async.series ([

                function (next) {
                    try {
                        return self.applicationGetForPublish(params, clientSession, function (err, applicationPublish) {
                            if (err) {
                                return next(err);
                            }
                            application = applicationPublish;
                            return next();
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },

                function (next) {
                    try {
                        application.id = params.id;
                        return AerospikeApp.publishApplication(application, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },

                function (next) {
                    try {
                        var applicationParams = {};
                        applicationParams.id = params.id;
                        applicationParams.status = Application.constants().STATUS_ACTIVE;
                        return self.applicationSetStatus(applicationParams, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                }
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

    applicationUnpublish: function (params, clientSession, callback) {
        if (!params.hasOwnProperty('id')) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        var self = this;
        try {
            var application = {};
            var appTenantId;
            async.series ([
                function (next) {
                    Application.findOne({
                        where: { id: params.id}
                    }).then(function (app) {
                        if (!app) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }

                        appTenantId = app.tenantId;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },

                function (next) {
                    try {
                        application.id = params.id;
                        return AerospikeApp.unpublishApplication({
                            application: {
                                id: params.id
                            },
                            tenant: {
                                id: appTenantId
                            }
                        }, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },

                function (next) {
                    try {
                        var applicationParams = {};
                        applicationParams.id = params.id;
                        applicationParams.status = Application.constants().STATUS_INACTIVE;
                        return self.applicationSetStatus(applicationParams, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                }
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

    buildGetAppConfigurationWithApp: function (appId, cb) {
        try {
            var self = this;
            var applicationPublishData = {
                tenant: {},
                application: {},
                friendsForMedia: {},
                environment: {}
            };
            var application;
            var tenant;
            var friendsForMedia;
            var environment;
            async.series([
                //Load FriendsForMedia Data
                function (next) {
                    TenantService.retrieveF4mLegalEntities(function (err, f4mLegalEntities) {
                        if (err) {
                            return next(err);
                        }
                        friendsForMedia = _.cloneDeep(f4mLegalEntities);
                        return next();
                    });
                },
                // upload files on S3 and set URLs
                function (next) {
                    try {
                        var keys = ['description', 'termsAndConditions', 'privacyStatement'];
                        self.buildGetAppConfigurationUploadFilesAndSetUrls({
                            keys: keys,
                            friendsForMedia: friendsForMedia
                        }, next);
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                },
                //Load App Data
                function (next) {
                    return Application.findOne({
                        where: { id: appId }
                    }).then(function (applicationItem) {
                        if (!applicationItem) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        if (applicationItem.status === Application.constants().STATUS_ARCHIVED) {
                            return next(Errors.ApplicationApi.AppIsArchived);
                        }
                        application = applicationItem;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // regional settings
                function (next) {
                    application.regionalSettings = [];
                    tenantRegionalSettings(application.tenantId, function (err, result) {
                        if (err) {
                            return next(err);
                        }

                        application.regionalSettings = result;
                        return next();
                    });
                },
                // Load games per type
                function (next) {
                    application.dataValues.gamesPerType = {
                        QUIZ24: 0,
                        DUEL: 0,
                        TOURNAMENT: 0,
                        LIVE_TOURNAMENT: 0,
                        USER_TOURNAMENT: 0,
                        USER_LIVE_TOURNAMENT: 0,
                        PLAYOFF_TOURNAMENT: 0
                    };

                    function _gameType(game) {
                        var type = '';
                        if (game.gameType === Game.constants().GAME_TYPE_QUICK_QUIZ) {
                            type = 'QUIZ24';
                        } else if (game.gameType === Game.constants().GAME_TYPE_DUEL) {
                            type = 'DUEL';
                        } else if (game.gameType === Game.constants().GAME_TYPE_TOURNAMENT) {
                            if (game.gameTypeConfiguration.gameTournament.type === 'live') {
                                type = 'LIVE_TOURNAMENT';
                            } else if (game.gameTypeConfiguration.gameTournament.type === 'user') {
                                type = 'USER_TOURNAMENT';
                            } else if (game.gameTypeConfiguration.gameTournament.type === 'user_live') {
                                type = 'USER_LIVE_TOURNAMENT';
                            } else if (game.gameTypeConfiguration.gameTournament.type === 'normal') {
                                type = 'TOURNAMENT';
                            }
                        }
                        return type;
                    };

                    ApplicationHasGame.findAll({
                        where: {applicationId: application.id},
                        include: [{model: Game, required: true}]
                    }).then(function (applicationHasGames) {
                        try {
                            if (!applicationHasGames) {
                                return next();
                            }
                            for(var i=0; i<applicationHasGames.length; i++) {
                                var game = applicationHasGames[i].get({ plain: true }).game;
                                if (game.status !== Game.constants().STATUS_ACTIVE) {
                                    continue;
                                }

                                var gameType = _gameType(game);
                                if (!_.has(application.dataValues.gamesPerType, gameType)) {
                                    logger.error('gameType not exists:', gameType);
                                    continue;
                                }

                                application.dataValues.gamesPerType[gameType]++;
                            }
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },

                //Load Tenant Data
                function (next) {
                    return Tenant.findOne({
                        where: { id: application.tenantId }
                    }).then(function (tenantItem) {
                        if (!tenantItem) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        tenant = tenantItem;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                //Load Game Modules
                function (next) {
                    return GameModule.findAll({
                        include: [
                            {
                                model: ApplicationHasGameModule, required: true, where: {
                                    applicationId: application.id,
                                    status: ApplicationHasGameModule.constants().STATUS_ACTIVE
                                }
                            }],
                        subQuery: false
                    }).then(function (applicationGameModules) {
                        application.applicationGameModules = applicationGameModules;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                //Load Animations
                function (next) {
                    return ApplicationAnimation.findAll({
                        include: [
                            {
                                model: ApplicationHasApplicationAnimation, required: true, where: {
                                    applicationId: application.id,
                                    status: ApplicationHasApplicationAnimation.constants().STATUS_ACTIVE
                                }
                            }],
                        subQuery: false
                    }).then(function (applicationAnimations) {
                        application.applicationAnimations = applicationAnimations;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                //Load Characters
                function (next) {
                    return ApplicationCharacter.findAll({
                        include: [
                            {
                                model: ApplicationHasApplicationCharacter, required: true, where: {
                                    applicationId: application.id,
                                    status: ApplicationHasApplicationCharacter.constants().STATUS_ACTIVE
                                }
                            }],
                        subQuery: false
                    }).then(function (applicationCharacters) {
                        application.applicationCharacters = applicationCharacters;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // set environment object
                function (next) {
                    try {
                        environment = _.assign({}, _getS3BaseUrls());
                        environment = _.assign(environment, {urlMediaService: Config.urlMediaService});
                        environment = _.assign(environment, {regionalSettings: application.regionalSettings});
                        return setImmediate(next);
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                },
                // set s3 urls
                function (next) {
                    try {
                        var urls = _getS3BaseUrls();
                        application.configuration = _.assign(application.configuration, urls);
                        return setImmediate(next);
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                },

                //Map all the data to application publishing model (using submodels) and validate
                function (next) {
                    try {
                        //Tenant
                        tenant.dataValues.id = tenant.id.toString();
                        applicationPublishData.tenant = AutoMapper.mapDefinedBySchema('applicationPublishTenantModel', INSTANCE_MAPPINGS, tenant);

                        //Application
                        application.dataValues.id = application.id.toString();
                        application.dataValues.releaseDate = DateUtils.isoPublish(application.releaseDate);
                        application.applicationGameModules = AutoMapper.mapListDefinedBySchema('gameModuleModel', INSTANCE_MAPPINGS, application.applicationGameModules);
                        application.applicationAnimations = AutoMapper.mapListDefinedBySchema('applicationAnimationModel', INSTANCE_MAPPINGS, application.applicationAnimations);
                        application.applicationCharacters = AutoMapper.mapListDefinedBySchema('applicationCharacterModel', INSTANCE_MAPPINGS, application.applicationCharacters);
                        applicationPublishData.application = AutoMapper.mapDefinedBySchema('applicationPublishAppModel', INSTANCE_MAPPINGS, application);

                        //F4M configuration
                        applicationPublishData.friendsForMedia = AutoMapper.mapDefinedBySchema('applicationPublishF4mModel', INSTANCE_MAPPINGS, friendsForMedia);

                        // environment
                        applicationPublishData.environment = environment;

                        //Schema validation
                        logger.info('ApplicationApiFactory.buildGetAppConfigurationWithApp before validation', JSON.stringify(applicationPublishData));
                        var err = jjv.validate('applicationPublishModel', applicationPublishData);
                        return next(err);
                    } catch (ex) {
                        return next(ex);
                    }
                }
            ], function (err) {
                if (err) {
                    logger.error('ApplicationApiFactory.buildGetAppConfigurationWithApp error', JSON.stringify(err));
                    return cb(err);
                }
                try {
                    var applicationItem = AutoMapper.mapDefinedBySchema('applicationPublishModel', INSTANCE_MAPPINGS, applicationPublishData);
                    logger.info('ApplicationApiFactory.buildGetAppConfigurationWithApp maped data', JSON.stringify(applicationItem));
                    return cb(false, applicationItem);
                } catch (ex) {
                    logger.error("ApplicationApiFactory.buildGetAppConfigurationWithApp exception", ex);
                    return cb(ex);
                }
            });

        } catch (e) {
            return setImmediate(cb, e);
        }
    },

    buildGetAppConfigurationWithoutApp: function (args, cb) {
        try {
            var self = this;
            var tenantId;
            var transaction;
            if (_.isPlainObject(args)) {
                tenantId = args.tenantId;
                transaction = args.transaction;
            } else {
                tenantId = args;
            }
            
            var applicationPublishData = {
                tenant: {},
                friendsForMedia: {},
                environment: {}
            };
            var tenant;
            var friendsForMedia;
            var environment;

            var _tenantRegionalSettings = [];
            async.series([
                //Load FriendsForMedia Data
                function (next) {
                    friendsForMedia = Config.f4mLegalEntities;
                    return next();
                },
                // upload files on S3 and set URLs
                function (next) {
                    try {
                        var keys = ['description', 'termsAndConditions', 'privacyStatement'];
                        self.buildGetAppConfigurationUploadFilesAndSetUrls({
                            keys: keys,
                            friendsForMedia: friendsForMedia
                        }, next);
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                },
                // regional settings
                function (next) {
                    tenantRegionalSettings(tenantId, function (err, result) {
                        if (err) {
                            return next(err);
                        }

                        _tenantRegionalSettings = result;
                        return next();
                    });
                },
                //Load Tenant Data
                function (next) {
                    var options = {
                        where: { id: tenantId }
                    };
                    if (transaction) {
                        options.transaction = transaction;
                    }
                    return Tenant.findOne(options).then(function (tenantItem) {
                        if (!tenantItem) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        tenant = tenantItem;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // set environment object
                function (next) {
                    try {
                        environment = _.assign({}, _getS3BaseUrls());
                        environment = _.assign(environment, {urlMediaService: Config.urlMediaService});
                        environment = _.assign(environment, {regionalSettings: _tenantRegionalSettings});
                        return setImmediate(next);
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                },

                //Map all the data to application publishing model (using submodels) and validate
                function (next) {
                    try {
                        //Tenant
                        tenant.dataValues.id = tenant.id.toString();
                        applicationPublishData.tenant = AutoMapper.mapDefinedBySchema('applicationPublishTenantModel', INSTANCE_MAPPINGS, tenant);

                        //F4M configuration
                        applicationPublishData.friendsForMedia = AutoMapper.mapDefinedBySchema('applicationPublishF4mModel', INSTANCE_MAPPINGS, friendsForMedia);

                        // environment
                        applicationPublishData.environment = environment;

                        //Schema validation
                        logger.info('ApplicationApiFactory.buildGetAppConfigurationWithoutApp before validation', JSON.stringify(applicationPublishData));
                        var err = jjv.validate('applicationPublishModel', applicationPublishData);
                        return next(err);
                    } catch (ex) {
                        return next(ex);
                    }
                }
            ], function (err) {
                if (err) {
                    logger.error("ApplicationApiFactory.buildGetAppConfigurationWithoutApp error", JSON.stringify(err));
                    return cb(err);
                }
                try {
                    var applicationItem = AutoMapper.mapDefinedBySchema('applicationPublishModel', INSTANCE_MAPPINGS, applicationPublishData);
                    logger.info('ApplicationApiFactory.buildGetAppConfigurationWithoutApp maped', JSON.stringify(applicationItem));
                    return cb(false, applicationItem);
                } catch (ex) {
                    logger.error("ApplicationApiFactory.buildGetAppConfigurationWithoutApp exception", ex);
                    return cb(ex);
                }
            });

        } catch (e) {
            return setImmediate(cb, e);
        }
    },
    
    
    buildGetAppConfigurationUploadFilesAndSetUrls: function (args, cb) {
        try {
            var keys = args.keys;
            var friendsForMedia = args.friendsForMedia;
            
            var locations = {};
            var files = [];
            for(var i=0; i<keys.length; i++) {
                var key = keys[i];
                files.push({
                    fileName: 'appconfig/' + key + '.txt',
                    fileContent: friendsForMedia[key],
                    key: key
                });
            }
            
            async.map(files, function (file, cbItem) {
                try {
                    s3ClientAppConfig.uploadObject(file.fileName, file.fileContent,
                    function () {
                    },
                    function(err, data) {
                        if(err) {
                            logger.error('buildGetAppConfigurationUploadFiles() error on upload:', err, file.fileName);
                        }
                        
                        locations[file.fileName] = data.Location;
                        return cbItem(err);
                    });
                } catch (e) {
                    return setImmediate(cbItem, e);
                }
            }, function (err) {
                try {
                    if(err) {
                        logger.error('buildGetAppConfigurationUploadFiles() error:', err);
                        return cb(err);
                    }
                    
                    // set files URLs
                    for(var i=0; i<files.length; i++) {
                        var file = files[i];
                        friendsForMedia[file.key + 'URL'] = locations[file.fileName];
                        //console.log('\n\n\n>>>here:', file.key + 'URL', locations[file.fileName]);
                    }
                    
                    return cb();
                } catch (e) {
                    return cb(e);
                }
            });
        } catch (e) {
            return setImmediate(cb, e);
        }
    }
};
function _getS3BaseUrls () {
    var obj = {};
    obj.cdnAdvertisement = 'https://' + S3Client.Config.buckets.appConfigurator.advertisement + '.s3.amazonaws.com/';
    obj.cdnMedia = 'https://' + S3Client.Config.buckets.medias.medias + '.s3.amazonaws.com/';
    obj.cdnEncrypted = 'https://' + S3Client.Config.buckets.appConfigurator.blob + '.s3.amazonaws.com/';
    return obj;
}

function tenantRegionalSettings (tenantId, cb) {
    var resultRegionalSettings = [];

    var options = {
        where: {tenantId: tenantId},
        include: [{model: RegionalSetting, required: true}]
    };

    function _loadLanguages(appRegionalSettings, cbLoadLanguages) {
        async.mapSeries(appRegionalSettings, function (regionalSetting, cbItem) {
            try {
                var item = {
                    iso: regionalSetting.iso,
                    name: regionalSetting.name,
                    languages: []
                };
                RegionalSettingHasLanguage.findAll({
                    where: {regionalSettingId: regionalSetting.id},
                    include: [{model: Language, required: true}]
                }).then(function (dbItems) {
                    if (!dbItems.length) {
                        resultRegionalSettings.push(item);
                        return cbItem();
                    }

                    for(var i=0; i<dbItems.length; i++) {
                        var lang = dbItems[i].language;
                        item.languages.push({
                            iso: lang.iso,
                            name: lang.name,
                            isDefault: lang.id === regionalSetting.defaultLanguageId
                        });
                    }
                    resultRegionalSettings.push(item);
                    return cbItem();
                }).catch(function (err) {
                    return CrudHelper.callbackError(err, cbItem);
                });
            } catch (e) {
                return cbItem(e);
            }
        }, cbLoadLanguages);
    };

    Language.findAll(options).then(function (languages) {
        try {
            if (!languages.length) {
                return cb(false, resultRegionalSettings);
            }

            var regionalSettings = [];
            for(var i=0; i<languages.length; i++) {
                var lang = languages[i];
                for(var j=0; j<lang.regionalSettings.length; j++) {
                    var rs = lang.regionalSettings[j];
                    if (rs.status !== RegionalSetting.constants().STATUS_ACTIVE) {
                        continue;
                    }
                    regionalSettings.push(rs);
                }
            }

            regionalSettings = _.uniqBy(regionalSettings, 'id');
            if (!regionalSettings.length) {
                return cb(false, resultRegionalSettings);
            }

            _loadLanguages(regionalSettings, function (err) {
                if (err) {
                    return cb(err);
                }

                return cb(false, resultRegionalSettings);
            });

        } catch (e) {
            return cb(e);
        }
    }).catch(function (err) {
        return CrudHelper.callbackError(err, cb);
    });

};
