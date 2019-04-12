let _ = require('lodash');
let async = require('async');
let Config = require('./../config/config.js');
let Errors = require('./../config/errors.js');
let AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
let AutoMapper = AutoMapperInstance.AutoMapper;
let CrudHelper = AutoMapperInstance.CrudHelper;
let DateUtils = require('nodejs-utils').DateUtils;
let TenantService = require('../services/tenantService.js');
let ProfileService = require('../services/profileService.js');
let checkAccountsMoneyService = require('../services/checkAccountsMoneyService');
let Database = require('nodejs-database').getInstance(Config);
let KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
let AerospikeGame = KeyvalueService.Models.AerospikeGame;
let AerospikeGameInstances = KeyvalueService.Models.AerospikeGameInstances;
let AerospikeGameInstanceShownAds = KeyvalueService.Models.AerospikeGameInstanceShownAds;
let AerospikePoolLanguageIndex = KeyvalueService.Models.AerospikePoolLanguageIndex;
let AerospikePoolLanguageIndexMeta = KeyvalueService.Models.AerospikePoolLanguageIndexMeta;
let Game = Database.RdbmsService.Models.Game.Game;
let GameModule = Database.RdbmsService.Models.Game.GameModule;
let GameHasRegionalSetting = Database.RdbmsService.Models.Game.GameHasRegionalSetting;
let GameHasPool = Database.RdbmsService.Models.Game.GameHasPool;
let GameHasQuestionTemplate = Database.RdbmsService.Models.Game.GameHasQuestionTemplate;
let GameHasWinningComponent = Database.RdbmsService.Models.Game.GameHasWinningComponent;
let GamePoolValidationResult = Database.RdbmsService.Models.Game.GamePoolValidationResult;
let ApplicationHasGame = Database.RdbmsService.Models.Application.ApplicationHasGame;
let Application = Database.RdbmsService.Models.Application.Application;
let Pool = Database.RdbmsService.Models.Question.Pool;
let Question = Database.RdbmsService.Models.Question.Question;
let QuestionTranslation = Database.RdbmsService.Models.Question.QuestionTranslation;
let AssignedPools = Database.RdbmsService.Models.Question.Pool;
let QuestionPools = Database.RdbmsService.Models.Question.Pool;
let PoolHasQuestion = Database.RdbmsService.Models.Question.PoolHasQuestion;
let RegionalSetting = Database.RdbmsService.Models.Question.RegionalSetting;
let RegionalSettingHasLanguage = Database.RdbmsService.Models.Question.RegionalSettingHasLanguage;
let Language = Database.RdbmsService.Models.Question.Language;
let PoolHasTenant = Database.RdbmsService.Models.Question.PoolHasTenant;
let Advertisement = Database.RdbmsService.Models.AdvertisementManager.Advertisement;
let Voucher = Database.RdbmsService.Models.VoucherManager.Voucher;
let WinningComponent = Database.RdbmsService.Models.WinningManager.WinningComponent;
let logger = require('nodejs-logger')();

let INSTANCE_MAPPINGS = {
    gameModel: [
        {
            destination: '$root',
            model: Game
        },
        {
            destination: 'regionalSettingsIds',
            model: GameHasRegionalSetting,
            attribute: GameHasRegionalSetting.tableAttributes.regionalSettingId
        },
        {
            destination: 'poolsIds',
            model: GameHasPool,
            attribute: GameHasPool.tableAttributes.poolId
        },
        {
            destination: 'applicationsIds',
            model: ApplicationHasGame,
            attribute: ApplicationHasGame.tableAttributes.applicationId
        },
        {
            destination: 'questionTemplates',
            model: GameHasQuestionTemplate,
            attributes: [
                { questionTemplateId: GameHasQuestionTemplate.tableAttributes.questionTemplateId },
                { amount: GameHasQuestionTemplate.tableAttributes.amount },
                { order: GameHasQuestionTemplate.tableAttributes.order }
            ]
        },
        {
            destination: 'winningComponents',
            model: GameHasWinningComponent,
            attributes: [
                { winningComponentId: GameHasWinningComponent.tableAttributes.winningComponentId },
                { isFree: GameHasWinningComponent.tableAttributes.isFree },
                { rightAnswerPercentage: GameHasWinningComponent.tableAttributes.rightAnswerPercentage },
                { entryFeeAmount: GameHasWinningComponent.tableAttributes.entryFeeAmount },
                { entryFeeCurrency: GameHasWinningComponent.tableAttributes.entryFeeCurrency }
            ]
        }
    ],
    gameCreateModel: [
        {
            destination: '$root',
            model: Game
        },
        {
            destination: 'regionalSettingsIds',
            model: GameHasRegionalSetting,
            attribute: GameHasRegionalSetting.tableAttributes.regionalSettingId
        },
        {
            destination: 'applicationsIds',
            model: ApplicationHasGame,
            attribute: ApplicationHasGame.tableAttributes.applicationId
        },
        {
            destination: 'poolsIds',
            model: GameHasPool,
            attribute: GameHasPool.tableAttributes.poolId
        },
        {
            destination: 'questionTemplates',
            model: GameHasQuestionTemplate,
            attributes: [
                { questionTemplateId: GameHasQuestionTemplate.tableAttributes.questionTemplateId },
                { amount: GameHasQuestionTemplate.tableAttributes.amount },
                { order: GameHasQuestionTemplate.tableAttributes.order }
            ]
        },
        {
            destination: 'winningComponents',
            model: GameHasWinningComponent,
            attributes: [
                { winningComponentId: GameHasWinningComponent.tableAttributes.winningComponentId },
                { isFree: GameHasWinningComponent.tableAttributes.isFree },
                { rightAnswerPercentage: GameHasWinningComponent.tableAttributes.rightAnswerPercentage },
                { entryFeeAmount: GameHasWinningComponent.tableAttributes.entryFeeAmount },
                { entryFeeCurrency: GameHasWinningComponent.tableAttributes.entryFeeCurrency }
            ]
        }
    ],
    gamePublishModel: [
        {
            destination: '$root',
            model: Game
        },
        {
            destination: 'questionPools',
            model: Pool,
            alias: 'questionPools',
            attribute: Pool.tableAttributes.id,
            order: false
        },
        {
            destination: 'assignedPools',
            model: Pool,
            alias: 'assignedPools',
            attribute: Pool.tableAttributes.id,
            order: false
        },
        {
            destination: 'assignedPoolsNames',
            model: Pool,
            alias: 'assignedPools',
            attribute: Pool.tableAttributes.name,
            order: false
        },
        {
            destination: 'assignedPoolsIcons',
            model: Pool,
            alias: 'assignedPools',
            attribute: Pool.tableAttributes.iconId,
            order: false
        },
        {
            destination: 'assignedPoolsColors',
            model: Pool,
            alias: 'assignedPools',
            attribute: Pool.tableAttributes.color,
            order: false
        },
        {
            destination: 'playingRegions',
            model: RegionalSetting,
            alias: 'playingRegions',
            attribute: RegionalSetting.tableAttributes.iso
        },
        {
            destination: 'playingLanguages',
            model: Language,
            alias: 'playingLanguages',
            attribute: Language.tableAttributes.iso
        }
    ],
    gamePublishTypeConfigurationModel: [
        {
            destination: '$root',
            model: Game
        }
    ]
};
INSTANCE_MAPPINGS.gameUpdateModel = INSTANCE_MAPPINGS.gameCreateModel;


module.exports = {

    gameUpdate: function (params, message, clientSession, callback) {
        if (_.has(params, 'startDate') && !_.isUndefined(params.startDate) && !_.isNull(params.startDate) &&
            _.has(params, 'endDate') && !_.isUndefined(params.endDate) && !_.isNull(params.endDate) &&
            new Date(params.startDate).getTime() >= new Date(params.endDate).getTime()) {
            logger.error('gameApiFactory:gameUpdate: validation failed ', JSON.stringify(params));
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        // validate promotion fields
        if (params.isPromotion === 1 && (_.isEmpty(params.promotionStartDate) || _.isEmpty(params.promotionEndDate))) {
            logger.error('gameApiFactory:gameUpdate: validation failed on promotion fields ', JSON.stringify(params));
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        let self = this;
        try {
            let create = true;
            let gameId = undefined;
            let mapBySchema = 'gameCreateModel';
            if (_.has(params, 'id') && params.id) {
                create = false;
                gameId = params.id;
                mapBySchema = 'gameUpdateModel';
            }
            if (params.gameType == 'tournament' && params.gameTypeConfiguration && params.gameTypeConfiguration.gameTournament) {
                params.gameTypeConfiguration.gameTournament.minimumJackpotGarantie = params.gameTypeConfiguration.gameTournament.minimumJackpotGarantie || 0;
            }

            if (params.gameTypeConfiguration && params.gameTypeConfiguration.gameQuickQuiz && params.gameTypeConfiguration.gameQuickQuiz.isSpecialGame){
                params.gameTypeConfiguration.gameQuickQuiz.isSpecialGame = false;
            }

            let gameItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, params, true);
            let gameItemAssociates = undefined;
            let sessionTenantId;
            async.series([
                // get tenantId from global session
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
                        gameItem.createDate = _.now();
                        gameItem.tenantId = sessionTenantId;
                        gameItem.creatorResourceId = clientSession.getUserId();
                        return Game.create(gameItem).then(function (result) {
                            gameId = result.get({ plain: true }).id;
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } else {
                        Game.update(gameItem, { where: { id: gameItem.id, tenantId: sessionTenantId }, individualHooks: true }).then(function (count) {
                            if (_.isArray(count) && count[0] === 0) {
                                return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, next);
                            }
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    }
                },

                // if no regional settings set tenant regional settings
                function (next) {
                    try {
                        let addDefaultRegionalSettings = false;
                        if (create) {
                            if (!_.has(params, 'regionalSettingsIds') || !params.regionalSettingsIds.length) {
                                addDefaultRegionalSettings = true;
                            }
                        } else if (_.has(params, 'regionalSettingsIds') && !params.regionalSettingsIds.length) {
                            addDefaultRegionalSettings = true;
                        }
                        if (!addDefaultRegionalSettings) {
                            return setImmediate(next);
                        }

                        Language.findAll({
                            where: {
                                tenantId: sessionTenantId
                            },
                            include: [
                                { model: RegionalSetting, required: true, attributes: [RegionalSetting.tableAttributes.id.field] }
                            ]
                        }).then(function (dbItems) {
                            try {
                                let regionalSettingsIds = [];
                                for (let i = 0; i < dbItems.length; i++) {
                                    regionalSettingsIds.push(dbItems[i].get({ plain: true }).id);
                                }
                                params.regionalSettingsIds = _.uniq(regionalSettingsIds);
                                return next();
                            } catch (ex) {
                                return next(ex);
                            }
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },

                // Populate game associated entity values
                function (next) {
                    try {
                        gameItemAssociates = AutoMapper.mapAssociatesDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, params, {
                            field: 'gameId',
                            value: gameId
                        });
                        return setImmediate(next, null);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Delete child entities for every updateable associated entity
                function (next) {
                    if (create || _.isUndefined(gameItemAssociates)) {
                        return setImmediate(next, null);
                    }
                    async.mapSeries(gameItemAssociates, function (itemAssociate, remove) {
                        // if (itemAssociate.values.length === 0) {
                        //    return remove();
                        // }
                        itemAssociate.model.destroy({ where: { gameId: gameId } }).then(function (count) {
                            return remove();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, remove);
                        });
                    }, next);
                },
                // Bulk insert populated values for every updateable associated entity
                function (next) {
                    if (_.isUndefined(gameItemAssociates)) {
                        return setImmediate(next, null);
                    }
                    async.mapSeries(gameItemAssociates, function (itemAssociate, create) {
                        if (itemAssociate.values.length === 0) {
                            return create();
                        }
                        itemAssociate.model.bulkCreate(itemAssociate.values).then(function (records) {
                            if (records.length !== itemAssociate.values.length) {
                                return create(Errors.QuestionApi.FatalError);
                            }
                            return create();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, create);
                        });
                    }, next);
                },
                // schedule game, if needed
                function (next) {
                    try {
                        if (create) {
                            return setImmediate(next);
                        }
                        return Game.findOne({ where: { id: gameId } }).then(function (dbGame) {
                            try {
                                let game = dbGame.get({ plain: true });
                                if (game.gameType !== Game.constants().GAME_TYPE_TOURNAMENT ||
                                    game.status !== Game.constants().STATUS_ACTIVE ||
                                    _.isEmpty(game.repetition)) {
                                    return next();
                                }
                                let tournamentType = game.gameTypeConfiguration.gameTournament.type;
                                if (['live', 'normal'].indexOf(tournamentType) === -1) {
                                    return next();
                                }
                                return clientSession.getConnectionService()._schedulerService.scheduleLiveTournament({
                                    id: game.id,
                                    repetition: _.assign({}, game.repetition),
                                    eventsData: {
                                        gameId: game.id,
                                        tenantId: clientSession.getTenantId(),
                                        appId: clientSession.getAppId()
                                    },
                                    type: tournamentType
                                }, next);
                            } catch (e) {
                                return next(e);
                            }
                        }).catch(function (err) {
                            return next(err);
                        });
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                }

            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                params.id = gameId;
                return self.gameGet(params, message, clientSession, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
       * Set Game status
       * @param {{}} params
       * @param {{}} callback
       * @returns {*}
       */
    gameSetStatus: function (params, callback) {
        let self = this;
        if (!params.hasOwnProperty('status') || (
            params.status !== Game.constants().STATUS_ACTIVE &&
            params.status !== Game.constants().STATUS_INACTIVE &&
            params.status !== Game.constants().STATUS_DIRTY &&
            params.status !== Game.constants().STATUS_ARCHIVED)) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        try {
            async.series([
                // Check current status
                function (next) {
                    return Game.findOne({ where: { id: params.id } }).then(function (game) {
                        if (!game) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        // Only undeployed and inactive (but not archived) games can be archived/activated
                        if (params.status === Game.constants().STATUS_ARCHIVED || params.status === Game.constants().STATUS_ACTIVE) {
                            if (game.status === Game.constants().STATUS_ARCHIVED) {
                                return next(Errors.GameApi.GameIsArchived);
                            }
                        }
                        // Only undeployed and active games can be deactivated
                        if (params.status === Game.constants().STATUS_INACTIVE) {
                            if (game.status === Game.constants().STATUS_INACTIVE) {
                                return next(Errors.GameApi.GameIsDeactivated);
                            }
                            if (game.status === Game.constants().STATUS_ARCHIVED) {
                                return next(Errors.GameApi.GameIsArchived);
                            }
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Update status
                function (next) {
                    return Game.update({ status: params.status }, { where: { id: params.id }, individualHooks: true }).then(function (count) {
                        if (_.isArray(count) && count[0] === 0) {
                            return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
                        }
                        return CrudHelper.callbackSuccess(null, callback);
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, callback);
                    });
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

    /**
       * Close Game, disactivate it inside Appication container
       * @param {{}} params
       * @param {{}} callback
       * @returns {*}
       */
    gameClose: function (params, callback) {
        let self = this;
        try {
            async.series([
                // Update Game: set isClosed = 1
                function (serie) {
                    return Game.update({ isClosed: 1 }, { where: { id: params.id }, individualHooks: true }).then(function (count) {
                        if (_.isArray(count) && count[0] === 0) {
                            return serie(Errors.DatabaseApi.NoRecordFound);
                        }
                        return serie();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, serie);
                    });
                },
                // Disactivate Game inside Application container
                function (serie) {
                    return Game.update({ status: Game.constants().STATUS_INACTIVE }, { where: { id: params.id }, individualHooks: true }).then(function (count) {
                        return serie();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, serie);
                    });
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

    gameDelete: function (params, clientSession, callback) {
        let self = this;
        try {
            async.mapSeries([
                GameHasRegionalSetting,
                GameHasPool,
                GameHasQuestionTemplate
            ], function (model, next) {
                try {
                    return model.destroy({ where: { gameId: params.id } }).then(function (count) {
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
                return Game.destroy({ where: { id: params.id } }).then(function (count) {
                    if (count === 0) {
                        return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
                    }

                    // unschedule game
                    clientSession.getConnectionService()._schedulerService.unscheduleLiveTournament(params.id, function (errUnschedule) {
                        if (errUnschedule) {
                            return CrudHelper.callbackError(errUnschedule, callback);
                        }
                        return CrudHelper.callbackSuccess(null, callback);
                    });
                }).catch(function (err) {
                    return CrudHelper.callbackError(err, callback);
                });
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    gameGet: function (params, message, clientSession, callback) {
        try {
            let response = {};
            let responseItem = 'game';
            let mapBySchema = 'gameModel';
            let sessionTenantId;
            let gameItem;
            async.series([
                // Get tenant id from session
                function (next) {
                    return TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if (err) {
                            return next(err);
                        }
                        sessionTenantId = tenantId;
                        return next();
                    });
                },
                // Get game by id within tenant
                function (next) {
                    return Game.findOne({
                        where: { id: params.id, tenantId: sessionTenantId }
                    }).then(function (game) {
                        if (!game) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        gameItem = game;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Populate paged winning component ids
                function (next) {
                    return GameHasWinningComponent.findAndCountAll({
                        where: { gameId: params.id },
                        limit: Config.rdbms.limit,
                        offset: 0
                    }).then(function (gameWinningComponents) {
                        gameItem.gameHasWinningComponents = gameWinningComponents;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Populate paged regional setting ids
                function (next) {
                    return GameHasRegionalSetting.findAndCountAll({
                        where: { gameId: params.id },
                        limit: Config.rdbms.limit,
                        offset: 0
                    }).then(function (regionalSettings) {
                        gameItem.gameHasRegionalSettings = regionalSettings;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Populate paged pool ids
                function (next) {
                    return GameHasPool.findAndCountAll({
                        where: { gameId: params.id },
                        limit: Config.rdbms.limit,
                        offset: 0
                    }).then(function (gamePools) {
                        gameItem.gameHasPools = gamePools;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Populate paged application ids
                function (next) {
                    return ApplicationHasGame.findAndCountAll({
                        where: { gameId: params.id },
                        limit: Config.rdbms.limit,
                        offset: 0
                    }).then(function (gameApplications) {
                        gameItem.applicationHasGames = gameApplications;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Populate paged question template attributes
                function (next) {
                    return GameHasQuestionTemplate.findAndCountAll({
                        where: { gameId: params.id },
                        limit: Config.rdbms.limit,
                        offset: 0
                    }).then(function (gameQuestionTemplates) {
                        gameItem.gameHasQuestionTemplates = gameQuestionTemplates;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                response[responseItem] = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, gameItem);
                return CrudHelper.callbackSuccess(response, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    gameInstancesGet: function (params, message, clientSession, callback) {
        try {
            let response = {};
            let responseItem = 'instances';
            AerospikeGameInstances.findInstances(params, function (err, game) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                response[responseItem] =  game;
                return CrudHelper.callbackSuccess(response, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    gameInstanceShownAdsGet: function (params, message, clientSession, callback) {
        try {
            let response = {};
            let responseItem = 'ads';
            AerospikeGameInstanceShownAds.findAds(params, function (err, ads) {
                if (err) {
                    if (err === "ERR_ENTRY_NOT_FOUND"){
                        ads = [];
                    } else {
                        return CrudHelper.callbackError(err, callback);
                    }
                }
                response[responseItem] =  ads;
                return CrudHelper.callbackSuccess(response, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    gameList: function (args, callback) {
        try {
            let params = args.params;
            let clientInfo = args.clientInfo;
            let mapBySchema = 'gameModel';
            let orderBy = CrudHelper.orderBy(params, Game);
            let searchBy = CrudHelper.searchBy(params, Game);
            searchBy.tenantId = clientInfo.getTenantId();
            let include = CrudHelper.includeDefined(Game, [
                ApplicationHasGame,
                GameHasRegionalSetting,
                GameHasPool,
                GameHasQuestionTemplate,
                GameHasWinningComponent
            ], params, mapBySchema, INSTANCE_MAPPINGS);
            include = CrudHelper.includeNoAttributes(include, Game);
            let attributes = CrudHelper.distinctAttributes(Game.tableAttributes.id);
            let gameItems = [];
            let total = 0;
            async.series([
                // Populate total game count
                function (next) {
                    return Game.count({
                        where: searchBy,
                        include: include,
                        distinct: true,
                        subQuery: false
                    }).then(function (count) {
                        total = count;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Populate game ids (involve base model and nested models only with defined search criterias)
                function (next) {
                    return Game.findAll({
                        limit: params.limit === 0 ? null : params.limit,
                        offset: params.limit === 0 ? null : params.offset,
                        attributes: attributes,
                        where: searchBy,
                        include: include,
                        order: orderBy,
                        subQuery: false,
                        raw: true
                    }).then(function (games) {
                        if (!_.has(searchBy, 'id')) {
                            let gamesIds = [];
                            _.forEach(games, function (game) {
                                gamesIds.push(game.id);
                            });
                            searchBy = {
                                id: { $in: gamesIds }
                            };
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Populate paged game list without nested items
                function (next) {
                    if (total === 0) {
                        return setImmediate(next, null);
                    }
                    return Game.findAll({
                        where: searchBy,
                        order: orderBy,
                        subQuery: false
                    }).then(function (games) {
                        gameItems = games;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Populate nested items for each game separately
                function (next) {
                    if (total === 0) {
                        return setImmediate(next, null);
                    }
                    return async.mapSeries(gameItems, function (gameItem, nextGame) {
                        return async.series([
                            // Populate paged winning component ids
                            function (nextItem) {
                                let where = { gameId: gameItem.id };
                                if (_.has(params, 'searchBy') && _.has(params.searchBy, 'winningComponents')) {
                                    let whereWinningComponents = [];
                                    _.forEach(params.searchBy.winningComponents, function (winningComponent) {
                                        whereWinningComponents.push({
                                            $and: [
                                                { winningComponentId: winningComponent.winningComponentId },
                                                { isFree: winningComponent.isFree },
                                                { rightAnswerPercentage: winningComponent.rightAnswerPercentage },
                                                { entryFeeAmount: winningComponent.entryFeeAmount },
                                                { entryFeeCurrency: winningComponent.entryFeeCurrency }
                                            ]
                                        });
                                    });
                                    where = { $and: [where, { $or: whereWinningComponents }] };
                                }
                                return GameHasWinningComponent.findAndCountAll({
                                    where: where,
                                    limit: Config.rdbms.limit,
                                    offset: 0
                                }).then(function (gameWinningComponents) {
                                    gameItem.gameHasWinningComponents = gameWinningComponents;
                                    return nextItem();
                                }).catch(function (err) {
                                    return CrudHelper.callbackError(err, nextItem);
                                });
                            },
                            // Populate paged regional setting ids
                            function (nextItem) {
                                let where = { gameId: gameItem.id };
                                if (_.has(params, 'searchBy') && _.has(params.searchBy, 'regionalSettingsIds')) {
                                    where.regionalSettingId = { $in: params.searchBy.regionalSettingsIds };
                                }
                                return GameHasRegionalSetting.findAndCountAll({
                                    where: where,
                                    limit: Config.rdbms.limit,
                                    offset: 0
                                }).then(function (regionalSettings) {
                                    gameItem.gameHasRegionalSettings = regionalSettings;
                                    return nextItem();
                                }).catch(function (err) {
                                    return CrudHelper.callbackError(err, nextItem);
                                });
                            },
                            // Populate paged pool ids
                            function (nextItem) {
                                let where = { gameId: gameItem.id };
                                if (_.has(params, 'searchBy') && _.has(params.searchBy, 'poolsIds')) {
                                    where.poolId = { $in: params.searchBy.poolsIds };
                                }
                                return GameHasPool.findAndCountAll({
                                    where: where,
                                    limit: Config.rdbms.limit,
                                    offset: 0
                                }).then(function (gamePools) {
                                    gameItem.gameHasPools = gamePools;
                                    return nextItem();
                                }).catch(function (err) {
                                    return CrudHelper.callbackError(err, nextItem);
                                });
                            },
                            // Populate paged application ids
                            function (nextItem) {
                                let where = { gameId: gameItem.id };
                                if (_.has(params, 'searchBy') && _.has(params.searchBy, 'applicationsIds')) {
                                    where.applicationId = { $in: params.searchBy.applicationsIds };
                                }
                                return ApplicationHasGame.findAndCountAll({
                                    where: where,
                                    limit: Config.rdbms.limit,
                                    offset: 0
                                }).then(function (gameApplications) {
                                    gameItem.applicationHasGames = gameApplications;
                                    return nextItem();
                                }).catch(function (err) {
                                    return CrudHelper.callbackError(err, nextItem);
                                });
                            },
                            // Populate paged question template attributes
                            function (nextItem) {
                                let where = { gameId: gameItem.id };
                                if (_.has(params, 'searchBy') && _.has(params.searchBy, 'questionTemplates')) {
                                    let whereQuestionTemplates = [];
                                    _.forEach(params.searchBy.questionTemplates, function (questionTemplate) {
                                        whereQuestionTemplates.push({
                                            $and: [
                                                { questionTemplateId: questionTemplate.questionTemplateId },
                                                { amount: questionTemplate.amount },
                                                { order: questionTemplate.order }
                                            ]
                                        });
                                    });
                                    where = { $and: [where, { $or: whereQuestionTemplates }] };
                                }
                                return GameHasQuestionTemplate.findAndCountAll({
                                    where: where,
                                    limit: Config.rdbms.limit,
                                    offset: 0
                                }).then(function (gameQuestionTemplates) {
                                    gameItem.gameHasQuestionTemplates = gameQuestionTemplates;
                                    return nextItem();
                                }).catch(function (err) {
                                    return CrudHelper.callbackError(err, nextItem);
                                });
                            }
                        ], nextGame);
                    }, next);
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                let gameItemsMapped = AutoMapper.mapListDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, gameItems);
                return CrudHelper.callbackSuccess({
                    items: gameItemsMapped,
                    limit: params.limit,
                    offset: params.offset,
                    total: total
                }, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    gameModuleList: function (params, callback) {
        try {
            let mapBySchema = 'gameModuleModel';
            let attributes = _.keys(GameModule.attributes);
            let orderBy = CrudHelper.orderBy(params, GameModule);
            let searchBy = CrudHelper.searchBy(params, GameModule);
            return GameModule.findAndCountAll({
                limit: null,
                offset: null,
                where: searchBy,
                order: orderBy,
                attributes: attributes,
                subQuery: false
            }).then(function (games) {
                let gameItems = AutoMapper.mapListDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, games.rows);
                return CrudHelper.callbackSuccess({
                    items: gameItems,
                    total: games.count
                }, callback);
            }).catch(function (err) {
                return CrudHelper.callbackError(err, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    gameGetForPublish: function (args, callback) {
        try {
            let params = args.params;
            let validate = args.validate;
            let clientInfo = args.clientInfo;

            let game = null;
            let sessionTenantId = clientInfo.getTenantId();
            async.series([

                // Map Game base configuration
                function (next) {
                    try {
                        return Game.findOne({
                            where: { id: params.id, tenantId: sessionTenantId }
                        }).then(function (gameItem) {
                            try {
                                if (!gameItem) {
                                    return next(Errors.GameApi.GamePublishingNoGameFound);
                                }
                                if (gameItem.status === Game.constants().STATUS_ARCHIVED) {
                                    return next(Errors.GameApi.GameIsArchived);
                                }
                                game = gameItem;
                                logger.debug('TOUR gameItem: ' + gameItem);

                                // Custom Game mapper object
                                game.properties = {
                                    tenantId: sessionTenantId.toString()
                                };
                                game.properties.gameId = gameItem.id.toString();
                                game.properties.numberOfQuestions = gameItem.numberOfQuestionsToPlay;
                                game.complexityStructure = _.values(gameItem.get({ plain: true }).complexityLevel);
                                if (validate && ((game.complexitySpread === Game.constants().COMPLEXITY_SPREAD_FIXED && _.sum(game.complexityStructure) !== game.properties.numberOfQuestions) ||
                                    (game.complexitySpread === Game.constants().COMPLEXITY_SPREAD_PERCENTAGE && _.sum(game.complexityStructure) !== 100))) {
                                    return next(Errors.GameApi.GamePublishingInvalidComplexityStructure);
                                }
                                if (gameItem.gameType === Game.constants().GAME_TYPE_QUICK_QUIZ) {
                                    game.properties.type = 'QUIZ24';
                                } else if (gameItem.gameType === Game.constants().GAME_TYPE_DUEL) {
                                    game.properties.type = 'DUEL';
                                } else if (gameItem.gameType === Game.constants().GAME_TYPE_TOURNAMENT) {
                                    if (game.gameTypeConfiguration.gameTournament.type === 'live') {
                                        game.properties.type = 'LIVE_TOURNAMENT';
                                    } else if (game.gameTypeConfiguration.gameTournament.type === 'user') {
                                        game.properties.type = 'USER_TOURNAMENT';
                                    } else if (game.gameTypeConfiguration.gameTournament.type === 'user_live') {
                                        game.properties.type = 'USER_LIVE_TOURNAMENT';
                                    } else if (game.gameTypeConfiguration.gameTournament.type === 'normal') {
                                        game.properties.type = 'TOURNAMENT';
                                    }
                                    // PLAYOFF_TOURNAMENT - to be supported later
                                    // TV_LIVE_EVENT - to be supported later
                                    // BETS - to be supported later
                                }
                                if (game.questionSelection === 'randomPerPlayer') {
                                    game.properties.questionSelection = 'random_per_player';
                                } else if (game.questionSelection === 'sameForEachPlayer') {
                                    game.properties.questionSelection = 'same_for_each_player';
                                }
                                game.properties.imageId = gameItem.imageId;
                                game.properties.isMultiplePurchaseAllowed = gameItem.isMultiplePurchaseAllowed === 1;
                                game.properties.isFree = gameItem.entryFeeDecidedByPlayer === 0 && gameItem.gameEntryAmount === 0;
                                game.properties.isSpecialPrices = gameItem.resultConfiguration.specialPrice;
                                game.properties.isOffline = false;
                                game.properties.isRegionalLimitationEnabled = gameItem.isRegionalLimitationEnabled === 1;
                                game.properties.questionOverwriteUsage = gameItem.overWriteUsage;
                                game.properties.instantAnswerFeedback = gameItem.instantAnswerFeedback === 1;
                                game.properties.instantAnswerFeedbackDelay = _.has(gameItem, 'instantAnswerFeedbackDelay') ? gameItem.instantAnswerFeedbackDelay : Game.constants().DEFAULT_INSTANT_ANSWER_FEEDBACK_DELAY;
                                game.properties.startDateTime = DateUtils.isoPublish(gameItem.startDate);
                                game.properties.endDateTime = DateUtils.isoPublish(gameItem.endDate);
                                game.properties.entryFeeAmount = gameItem.gameEntryAmount;
                                game.properties.entryFeeCurrency = gameItem.gameEntryCurrency;
                                game.properties.entryFeeType = game.playerEntryFeeType === 'fixed' ? 'FIXED' : 'STEP';
                                game.properties.entryFeeSettings = gameItem.playerEntryFeeSettings;
                                game.properties.entryFeeValues = gameItem.playerEntryFeeValues;
                                game.properties.entryFeeDecidedByPlayer = gameItem.entryFeeDecidedByPlayer === 1;
                                game.properties.isPromotion = gameItem.isPromotion === 1;
                                game.properties.promotionStartDate = DateUtils.isoPublish(gameItem.promotionStartDate);
                                game.properties.promotionEndDate = DateUtils.isoPublish(gameItem.promotionEndDate);
                                game.properties.loadingScreen = _.has(gameItem.resultConfiguration, 'loadingScreen') ? gameItem.resultConfiguration.loadingScreen : false;
                                if (game.properties.loadingScreen && _.has(gameItem.resultConfiguration, 'loadingScreenProviderId')){
                                    game.properties.loadingScreenProviderId = gameItem.resultConfiguration.loadingScreenProviderId;
                                }
                                if (game.properties.loadingScreen && _.has(gameItem.resultConfiguration, 'loadingScreenDuration')){
                                    game.properties.loadingScreenDuration = gameItem.resultConfiguration.loadingScreenDuration;
                                }

                                if (validate && !game.properties.isFree && gameItem.entryFeeDecidedByPlayer === 0 && !game.properties.entryFeeAmount) {
                                    return next(Errors.GameApi.GamePublishingNoEntryFeeAmount);
                                } else if (validate && gameItem.entryFeeDecidedByPlayer === 1 && game.properties.entryFeeType === 'FIXED' && !game.properties.entryFeeValues) {
                                    return next(Errors.GameApi.GamePublishingNoEntryFeeAmount);
                                } else if (validate && gameItem.entryFeeDecidedByPlayer === 1 && game.properties.entryFeeType === 'STEP' && !game.properties.entryFeeSettings) {
                                    return next(Errors.GameApi.GamePublishingNoEntryFeeAmount);
                                }

                                if (gameItem.advertisement === 1) {
                                    if (gameItem.adsFrequency === Game.constants().ADS_FREQUENCY_BEFORE_THE_GAME) {
                                        game.properties.advertisementFrequency = 'BEFORE_GAME';
                                    } else if (gameItem.adsFrequency === Game.constants().ADS_FREQUENCY_AFTER_THE_GAME) {
                                        game.properties.advertisementFrequency = 'AFTER_GAME';
                                    } else if (gameItem.adsFrequency === Game.constants().ADS_FREQUENCY_AFTER_EACH_QUESTION) {
                                        game.properties.advertisementFrequency = 'AFTER_EACH_QUESTION';
                                    } else if (gameItem.adsFrequency === Game.constants().ADS_FREQUENCY_AFTER_X_QUESTION) {
                                        game.properties.advertisementFrequency = 'AFTER_EVERY_X_QUESTION';
                                    }
                                    game.properties.advFreqXQuestionDefinition = gameItem.adsFrequencyAmount;
                                    game.properties.advertisementProviderId = gameItem.advertisementProviderId;
                                    game.properties.advertisementSkipping = _.has(gameItem.resultConfiguration, 'advertisementSkipping') ? gameItem.resultConfiguration.advertisementSkipping : false;
                                    game.properties.advertisementDuration = gameItem.waitTimeBetweenQuestions;
                                }
                                game.properties.userCanOverridePools = gameItem.userCanOverridePools === 1;
                                game.properties.hideCategories = gameItem.hideCategories === 1;
                                if (_.has(gameItem, 'minimumGameLevel')) {
                                    game.properties.handicap = gameItem.minimumGameLevel;
                                }
                                game.properties.winningComponents = [];

                                // Custom Game.typeConfiguration mapper object: map only defined attributes by json schema
                                game.properties.typeConfiguration = AutoMapper.mapObjectDefinedBySchema('gamePublishTypeConfigurationModel', INSTANCE_MAPPINGS, gameItem.gameTypeConfiguration);

                                // Custom Game.jokerConfiguration mapper object
                                game.properties.jokerConfiguration = gameItem.jokerConfiguration;
                                if (_.isString(game.jokerConfiguration)) {
                                    try {
                                        // Parse into object
                                        game.properties.jokerConfiguration = JSON.parse(game.jokerConfiguration);
                                    } catch (ex) {
                                        return next(Errors.GameApi.GamePublishingInvalidJokerConfiguration);
                                    }
                                }
                                if (validate) {
                                    _.forEach(gameItem.jokerConfiguration, function (item) {
                                        // Proceed to validation only if joker type configuration is enabled
                                        if (item.enabled) {
                                            // Currency is mandatory if price is specified
                                            if (item.price && (!_.has(item, 'currency') || _.isEmpty(item.currency))) {
                                                return next(Errors.GameApi.GamePublishingInvalidJokerConfiguration);
                                            }
                                            // Display time is mandatory for IMMEDIATE_ANSWER joker
                                            if (item.type === 'IMMEDIATE_ANSWER' && !item.displayTime) {
                                                return next(Errors.GameApi.GamePublishingInvalidJokerConfiguration);
                                            }
                                        }
                                    });
                                }

                                // Custom Game.resultConfiguration mapper object
                                game.properties.resultConfiguration = {};
                                game.properties.resultConfiguration.pointCalculator = gameItem.resultConfiguration.pointCalculator;
                                game.properties.resultConfiguration.alternativeBonusPointsPerCorrectAnswer = gameItem.resultConfiguration.alternativeBonusPointsPerCorrect;
                                game.properties.resultConfiguration.bonusPointsPerCorrectAnswerForUnpaid = gameItem.resultConfiguration.bonusPointsPerRightAnswer;
                                game.properties.resultConfiguration.bonusPointsPerGamePointForPaid = gameItem.resultConfiguration.calculatingBonuspointsPerRightAnswer;
                                game.properties.resultConfiguration.bonusPointsForAllCorrectAnswers = gameItem.resultConfiguration.bonusPointsForAllCorrectAnswers;
                                game.properties.resultConfiguration.treatPaidLikeUnpaid = gameItem.resultConfiguration.treatPaidLikeUnpaid;
                                game.properties.resultConfiguration.correctAnswerPointCalculationType = gameItem.resultConfiguration.pointCalculationRightAnswer;
                                if (gameItem.questionComplexity) {
                                    game.properties.resultConfiguration.correctAnswerQuestionComplexityGamePoints = _.map(gameItem.questionComplexity, function (complexityEntry) {
                                        return { level: complexityEntry.level, points: complexityEntry.number };
                                    });
                                }
                                // game.properties.resultConfiguration.correctAnswerFixedValueGamePoints = gameItem.resultConfiguration.calculatingBonuspointsPerRightAnswer;
                                game.properties.resultConfiguration.duelGroupWinnerPayoutType = gameItem.resultConfiguration.duelGroupWinnerPayoutType;
                                game.properties.resultConfiguration.duelGroupWinnerPayoutFixedValueAmount = gameItem.resultConfiguration.fixedValueAmount;
                                game.properties.resultConfiguration.duelGroupWinnerPayoutFixedValueCurrency = gameItem.resultConfiguration.fixedValueCurrency;
                                game.properties.resultConfiguration.duelGroupWinnerPayoutJackpotPercent = gameItem.resultConfiguration.percentageOfJackpot;
                                game.properties.resultConfiguration.duelGroupPrizePayoutType = gameItem.resultConfiguration.groupPrizePaidOutTo;
                                game.properties.resultConfiguration.duelGroupPrizePaidOutToEveryXWinner = gameItem.resultConfiguration.payoutToEveryXWinner;
                                if (_.has(gameItem.gameTypeConfiguration, 'gameDuel')) {
                                    game.properties.timeToAcceptInvites = gameItem.gameTypeConfiguration.gameDuel.timeToAcceptInvites;
                                }
                                if (_.has(gameItem.gameTypeConfiguration, 'gameTournament')) {
                                    game.properties.resultConfiguration.jackpotGame = gameItem.gameTypeConfiguration.gameTournament.isJackpotGame;
                                    game.properties.resultConfiguration.jackpotCalculateByEntryFee = gameItem.gameTypeConfiguration.gameTournament.isJackpotCalculateByEntryFee;
                                    game.properties.resultConfiguration.minimumJackpotAmount = gameItem.gameTypeConfiguration.gameTournament.minimumJackpotGarantie || 0;
                                    game.properties.resultConfiguration.targetJackpotAmount = gameItem.gameTypeConfiguration.gameTournament.targetJackpotAmount;
                                    game.properties.resultConfiguration.targetJackpotCurrency = gameItem.gameTypeConfiguration.gameTournament.targetJackpotCurrency;
                                    // Tournament handicap structure:
                                    // [20, 40, 60] ->
                                    // [{ handicapRangeId: 1, handicapFrom: 0, handicapTo: 20 }]
                                    // [{ handicapRangeId: 2, handicapFrom: 20, handicapTo: 40 }]
                                    // [{ handicapRangeId: 3, handicapFrom: 40, handicapTo: 60 }]
                                    // [{ handicapRangeId: 4, handicapFrom: 60, handicapTo: 100 }]
                                    let rangeId = 1;
                                    let handicapFrom = 0;
                                    game.properties.resultConfiguration.tournamentHandicapStructure = [];
                                    _.forEach(gameItem.gameTypeConfiguration.gameTournament.gameLevelStructure, function (structure) {
                                        game.properties.resultConfiguration.tournamentHandicapStructure.push({ handicapRangeId: rangeId++, handicapFrom: handicapFrom, handicapTo: structure });
                                        handicapFrom = structure;
                                    });
                                    game.properties.resultConfiguration.tournamentHandicapStructure.push({ handicapRangeId: rangeId++, handicapFrom: handicapFrom, handicapTo: 100 });
                                }

                                // export quickResponseBonuspoints and quickResponseBonusPointsMs
                                if ([Game.constants().GAME_TYPE_TOURNAMENT, Game.constants().GAME_TYPE_DUEL].indexOf(gameItem.gameType) >= 0) {
                                    let map = {};
                                    map[Game.constants().GAME_TYPE_TOURNAMENT] = 'gameTournament';
                                    map[Game.constants().GAME_TYPE_DUEL] = 'gameDuel';

                                    let key = map[gameItem.gameType];
                                    if (_.has(gameItem.gameTypeConfiguration[key], 'quickResponseBonuspoints')) {
                                        game.properties.resultConfiguration.quickResponseBonusPointsAmount = gameItem.gameTypeConfiguration[key].quickResponseBonuspoints;
                                    }
                                    if (_.has(gameItem.gameTypeConfiguration[key], 'quickResponseTimeLimitMs')) {
                                        game.properties.resultConfiguration.quickResponseBonusPointsMs = gameItem.gameTypeConfiguration[key].quickResponseTimeLimitMs;
                                    }
                                }
                                game.properties.resultConfiguration.tournamentPayoutStructure = [];

                                if (gameItem.resultConfiguration &&
                                    gameItem.resultConfiguration.payoutStructureUsed &&
                                    typeof gameItem.resultConfiguration.payoutStructureUsed === 'string') {
                                    const tournamentPayoutStructure = gameItem.resultConfiguration.payoutStructureUsed.split('-');
                                    for (const payout of tournamentPayoutStructure) {
                                        game.properties.resultConfiguration.tournamentPayoutStructure.push(+payout);
                                    }
                                }
                                game.properties.resultConfiguration.winningComponentCorrectAnswersPercent = 100;
                                if (gameItem.resultConfiguration.winningComponentRightAnswersAmount && gameItem.numberOfQuestionsToPlay) {
                                    game.properties.resultConfiguration.winningComponentCorrectAnswersPercent =
                                        Math.round(gameItem.resultConfiguration.winningComponentRightAnswersAmount / gameItem.numberOfQuestionsToPlay);
                                }
                                game.properties.resultConfiguration.specialPrize = gameItem.resultConfiguration.specialPrize;
                                game.properties.resultConfiguration.specialPrizeCorrectAnswersPercent = gameItem.resultConfiguration.specialPrizeRightAnswersAmount;
                                game.properties.resultConfiguration.specialPrizeVoucherId = gameItem.resultConfiguration.specialPrizeVoucherId;
                                if (gameItem.resultConfiguration.winningRule) {
                                    game.properties.resultConfiguration.specialPrizeWinningRule = gameItem.resultConfiguration.winningRule === 'theFirstXWinnerWins'
                                        ? 'firstXPlayers'
                                        : gameItem.resultConfiguration.winningRule;
                                }
                                game.properties.resultConfiguration.specialPrizeWinningRuleAmount = gameItem.resultConfiguration.winningRuleAmount;

                                return next();
                            } catch (ex) {
                                logger.error('gameApiFactory:gameGetForPublish exception by processing game configuration', ex);
                                return next(ex);
                            }
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        logger.error('gameApiFactory:gameGetForPublish exception by getting game', ex);
                        return setImmediate(next, ex);
                    }
                },

                // Application level attributes
                function (next) {
                    try {
                        return Application.findAll({
                            where: { tenantId: sessionTenantId },
                            include: [
                                {
                                    model: ApplicationHasGame, required: true, where: { gameId: game.id }
                                }
                            ]
                        }).then(function (applicationItems) {
                            try {
                                if (validate && (!applicationItems || applicationItems.length === 0)) {
                                    return next(Errors.GameApi.GamePublishingNoApplicationFound);
                                }
                                game.properties.applications = [];
                                if (!applicationItems || applicationItems.length === 0) {
                                    return next();
                                }
                                _.forEach(applicationItems, function (applicationItem) {
                                    let application = applicationItem.get({ plain: true });
                                    let handicap = game.properties.handicap;
                                    if (_.isObject(application.configuration) && _.has(application.configuration, 'minimumGameLevel')) {
                                        handicap = application.configuration.minimumGameLevel;
                                    }
                                    game.properties.applications.push({
                                        appId: application.id.toString(),
                                        handicap: handicap
                                    });
                                });
                                return next();
                            } catch (ex) {
                                logger.error('gameApiFactory:gameGetForPublish exception by processing application configuration', ex);
                                return next(ex);
                            }
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        logger.error('gameApiFactory:gameGetForPublish exception by getting application', ex);
                        return setImmediate(next, ex);
                    }
                },

                // Assigned Pools
                function (next) {
                    try {
                        return Pool.findAll({
                            include: [
                                { model: GameHasPool, required: true, where: { gameId: game.id } }
                            ],
                            subQuery: false
                        }).then(function (pools) {
                            try {
                                if (validate && !pools) {
                                    return next(Errors.GameApi.GamePublishingNoPoolFound);
                                }
                                if (!pools) {
                                    return next();
                                }
                                game.assignedPools = pools;
                                game.assignedPoolsNames = pools;
                                game.assignedPoolsIcons = pools;
                                game.assignedPoolsColors = pools;
                                return next();
                            } catch (ex) {
                                logger.error('gameApiFactory:gameGetForPublish exception by processing assigned pool configuration', ex);
                                return next(ex);
                            }
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        logger.error('gameApiFactory:gameGetForPublish exception by getting assigned pools', ex);
                        return setImmediate(next, ex);
                    }
                },

                // Question Pools
                function (next) {
                    try {
                        let assignedPoolIds = _.map(game.assignedPools, function (pool) {
                            return pool.get({ plain: true }).id;
                        });
                        if (!game.properties.applications || game.properties.applications.length === 0) {
                            return setImmediate(next);
                        }
                        return Pool.findAll({
                            where: { parentPoolId: assignedPoolIds },
                            include: [
                                { model: PoolHasTenant, required: true, where: { tenantId: sessionTenantId } }
                            ],
                            subQuery: false
                        }).then(function (pools) {
                            try {
                                if (validate && !pools) {
                                    return next(Errors.GameApi.GamePublishingNoPoolFound);
                                }
                                if (!pools) {
                                    return next();
                                }
                                game.questionPools = _.union(game.assignedPools, pools);
                                return next();
                            } catch (ex) {
                                logger.error('gameApiFactory:gameGetForPublish exception by processing question pool configuration', ex);
                                return next(ex);
                            }
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        logger.error('gameApiFactory:gameGetForPublish exception by getting question pools', ex);
                        return setImmediate(next, ex);
                    }
                },

                // Regional Settings
                function (next) {
                    try {
                        return RegionalSetting.findAll({
                            include: [
                                { model: GameHasRegionalSetting, required: true, where: { gameId: game.id } },
                                { model: Language, required: true },
                                { model: RegionalSettingHasLanguage, required: true, include: [{ model: Language, required: true }] }
                            ],
                            subQuery: false
                        }).then(function (regionalSettings) {
                            try {
                                if (validate && !regionalSettings) {
                                    return next(Errors.GameApi.GamePublishingNoRegionalSettingFound);
                                }
                                if (!regionalSettings) {
                                    return next();
                                }
                                game.playingRegions = regionalSettings;
                                game.playingLanguages = [];
                                _.forEach(regionalSettings, function (regionalSetting) {
                                    let regionalSettingPlain = regionalSetting.get({ plain: true });
                                    game.playingLanguages.push(regionalSettingPlain.language);
                                    _.forEach(regionalSettingPlain.regionalSettingHasLanguages, function (language) {
                                        if (!_.includes(_.map(game.playingLanguages, 'iso'), language.language.iso)) {
                                            game.playingLanguages.push(language.language);
                                        }
                                    });
                                });
                                if (validate && game.playingLanguages.length === 0) {
                                    return next(Errors.GameApi.GamePublishingNoLanguageFound);
                                }
                                return next();
                            } catch (ex) {
                                logger.error('gameApiFactory:gameGetForPublish exception by processing regional setting configuration', ex);
                                return next(ex);
                            }
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        logger.error('gameApiFactory:gameGetForPublish exception by getting regional settings', ex);
                        return setImmediate(next, ex);
                    }
                },

                // Question Template level attributes
                function (next) {
                    try {
                        return GameHasQuestionTemplate.findAll({
                            where: { gameId: game.id },
                            order: [['order', 'ASC']]
                        }).then(function (questionTemplates) {
                            try {
                                if (validate && !questionTemplates) {
                                    return next(Errors.GameApi.GamePublishingNoQuestionTemplateFound);
                                }
                                if (!questionTemplates) {
                                    return next();
                                }
                                game.amountOfQuestions = _.map(questionTemplates, function (template) {
                                    return template.get({ plain: true }).amount;
                                });
                                game.questionTypes = _.map(questionTemplates, function (template) {
                                    return template.get({ plain: true }).questionTemplateId.toString();
                                });
                                if (validate && ((game.questionTypeSpread === Game.constants().QUESTION_TYPE_SPREAD_FIXED && game.properties.numberOfQuestions > 0 && _.sum(game.amountOfQuestions) !== game.properties.numberOfQuestions) ||
                                    (game.questionTypeSpread === Game.constants().QUESTION_TYPE_SPREAD_PERCENTAGE && _.sum(game.amountOfQuestions) !== 100))) {
                                    return next(Errors.GameApi.GamePublishingInvalidAmountOfQuestions);
                                }
                                return next();
                            } catch (ex) {
                                logger.error('gameApiFactory:gameGetForPublish exception by processing question type configuration', ex);
                                return next(ex);
                            }
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        logger.error('gameApiFactory:gameGetForPublish exception by getting question types', ex);
                        return setImmediate(next, ex);
                    }
                },

                // All question templates of game should be accessible under pools
                function (next) {
                    try {
                        if (!validate) {
                            return setImmediate(next);
                        }
                        let sqlQuery =
                            'SELECT COUNT(*) AS count' +
                            '  FROM game_has_question_template ghqt' +
                            ' WHERE ghqt.gameId = :gameId' +
                            '   AND NOT EXISTS (' +
                            '       SELECT 1' +
                            '         FROM game_has_pool ghp' +
                            '        INNER JOIN pool_has_question phq ON phq.poolId = ghp.poolId' +
                            '        INNER JOIN question q ON q.id = phq.questionId' +
                            '        WHERE ghp.gameId = :gameId' +
                            '          AND q.questionTemplateId = ghqt.questionTemplateId)' +
                            '   AND NOT EXISTS (' +
                            '       SELECT 1' +
                            '          FROM game_has_pool ghp' +
                            '         INNER JOIN pool p ON p.parentPoolId = ghp.poolId' +
                            '         INNER JOIN pool_has_question phq ON phq.poolId = p.id' +
                            '         INNER JOIN question q ON q.id = phq.questionId' +
                            '         INNER JOIN pool_has_tenant pht ON pht.poolId = p.id AND pht.tenantId = :tenantId' +
                            '         WHERE ghp.gameId = :gameId' +
                            '           AND q.questionTemplateId = ghqt.questionTemplateId);';
                        return CrudHelper.rawQuery(sqlQuery, { gameId: params.id, tenantId: sessionTenantId }, function (err, data) {
                            try {
                                if (data[0].count > 0) {
                                    return next(Errors.GameApi.GamePublishingNoQuestionTemplateFound);
                                }
                                return next();
                            } catch (ex) {
                                logger.error('gameApiFactory:gameGetForPublish exception by processing question template configuration', ex);
                                return next(ex);
                            }
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        logger.error('gameApiFactory:gameGetForPublish exception by evaluating question template accessibility', ex);
                        return setImmediate(next, ex);
                    }
                },

                // Advertisements
                function (next) {
                    try {
                        if (game.advertisement === 0 || !game.advertisementProviderId) {
                            return setImmediate(next);
                        }
                        return Advertisement.findAll({
                            where: { advertisementProviderId: game.advertisementProviderId }
                        }).then(function (advertisements) {
                            try {
                                if (validate && !advertisements) {
                                    return next(Errors.GameApi.GamePublishingNoAdvertisementFound);
                                }
                                if (!advertisements) {
                                    return next();
                                }
                                game.advertisementIds = _.map(advertisements, function (advertisement) {
                                    return advertisement.get({ plain: true }).id.toString();
                                });
                                return next();
                            } catch (ex) {
                                logger.error('gameApiFactory:gameGetForPublish exception by processing advertisement configuration', ex);
                                return next(ex);
                            }
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        logger.error('gameApiFactory:gameGetForPublish exception by getting advertisements', ex);
                        return setImmediate(next, ex);
                    }
                },

                // Vouchers
                function (next) {
                    try {
                        if (!_.has(game.properties.resultConfiguration, 'voucherProviderId') || !game.properties.resultConfiguration.voucherProviderId) {
                            return setImmediate(next);
                        }
                        return Voucher.findAll({
                            where: { voucherProviderId: game.properties.resultConfiguration.voucherProviderId }
                        }).then(function (vouchers) {
                            try {
                                if (validate && !vouchers) {
                                    return next(Errors.GameApi.GamePublishingNoVoucherFound);
                                }
                                if (!vouchers) {
                                    return next();
                                }
                                game.voucherIds = _.map(vouchers, function (voucher) {
                                    return voucher.get({ plain: true }).id.toString();
                                });
                                return next();
                            } catch (ex) {
                                logger.error('gameApiFactory:gameGetForPublish exception by processing voucher configuration', ex);
                                return next(ex);
                            }
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        logger.error('gameApiFactory:gameGetForPublish exception by getting vouchers', ex);
                        return setImmediate(next, ex);
                    }
                },

                // Associated Free/Paid Winning Component List
                function (next) {
                    try {
                        return GameHasWinningComponent.findAll({
                            where: { gameId: game.id }
                        }).then(function (gameHasWinningComponents) {
                            try {
                                if (!gameHasWinningComponents) {
                                    return next();
                                }
                                game.freeWinningComponentIds = _.map(_.filter(gameHasWinningComponents, { isFree: 1 }), function (winningComponent) {
                                    return winningComponent.get({ plain: true }).winningComponentId.toString();
                                });
                                game.paidWinningComponentIds = _.map(_.filter(gameHasWinningComponents, { isFree: 0 }), function (winningComponent) {
                                    return winningComponent.get({ plain: true }).winningComponentId.toString();
                                });
                                _.forEach(gameHasWinningComponents, function (winningComponent) {
                                    let winningComponentPlain = winningComponent.get({ plain: true });
                                    let winningComponentItem = {
                                        winningComponentId: winningComponent.winningComponentId.toString(),
                                        isPaid: winningComponent.isFree === 0,
                                        rightAnswerPercentage: winningComponent.rightAnswerPercentage,
                                        amount: winningComponent.entryFeeAmount,
                                        currency: winningComponent.entryFeeCurrency
                                    };
                                    game.properties.winningComponents.push(winningComponentItem);
                                });
                                return next();
                            } catch (ex) {
                                logger.error('gameApiFactory:gameGetForPublish exception by processing winning component configuration', ex);
                                return next(ex);
                            }
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        logger.error('gameApiFactory:gameGetForPublish exception by getting winning components', ex);
                        return setImmediate(next, ex);
                    }
                },
                function (next) {
                    try {
                        if (game.properties.winningComponents.length > 0) {
                            if (_.has(game.properties.shortPrize, 'probability')) {
                                delete game.properties.shortPrize.probability;
                            }
                            if (_.has(game.properties.shortPrize, 'winningOptionId')) {
                                delete game.properties.shortPrize.winningOptionId;
                            }
                            if (_.has(game.properties.shortPrize, 'amount')) {
                                game.properties.shortPrize.amount = Number(game.properties.shortPrize.amount);
                                if (isNaN(game.properties.shortPrize.amount)) {
                                    game.properties.shortPrize.amount = 0;
                                }
                            }
                        }
                        return setImmediate(next);
                    } catch (ex) {
                        logger.error('gameApiFactory:gameGetForPublish exception by processing winning component configuration', ex);
                        return setImmediate(next, ex);
                    }
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                let gameItem = AutoMapper.mapDefinedBySchema('gamePublishModel', INSTANCE_MAPPINGS, game);
                logger.info('GameApiFactory.gameGetForPublish', JSON.stringify(gameItem));
                return CrudHelper.callbackSuccess(gameItem, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    gamePublish: function (args, callback) {
        let params = args.params;
        let schedulerService = args.schedulerService;
        let clientInfo = args.clientInfo;
        if (!params.hasOwnProperty('id')) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        let self = this;
        try {
            let game;
            async.series([

                function (next) {
                    try {
                        return self.gameGetForPublish({
                            params: params,
                            clientInfo: clientInfo,
                            validate: true
                        }, function (err, gamePublish) {
                            if (err) {
                                return next(err);
                            }
                            game = gamePublish;
                            logger.debug('TOUR ' + JSON.stringify(game));
                            game.id = params.id;
                            return next();
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                function (next) {
                    if (game.type === 'TOURNAMENT') {
                        return checkAccountsMoneyService.checkGameBalance(game, (err, result) => {
                            if (err) {
                                return next(err);
                            }
                            if (!result) {
                                return next(Errors.GameApi.GamePublishingNotEnoughMoney);
                            }
                            return next();
                        });
                    }
                    return next();
                },
                function (next) { // create repetition for normal tournament
                    if (game.type === 'TOURNAMENT') {
                        try {
                            return Game.findOne({ where: { id: params.id } }).then(function (dbGame) {
                                try {
                                    let game = dbGame.get({ plain: true });
                                    // continue only if the repetition wasn't set yet
                                    if (game.repetition) {
                                        return next();
                                    }
                                    // the repetition for normal tournament should create only 1 instance only once
                                    let repetitionStartTimeDate = new Date(_.now() + 5 * 60 * 1000);
                                    let repetitionEndDate = new Date(_.now() + 10 * 60 * 1000);
                                    let gameItem = {
                                        startDate: new Date(_.now()).toISOString(),
                                        repetition: {
                                            startDate: new Date(_.now()).toISOString(),
                                            unit: 'day',
                                            repeat: 1,
                                            'repeatOn': repetitionStartTimeDate.getUTCHours() + ':' + repetitionStartTimeDate.getUTCMinutes(),
                                            'endDate': repetitionEndDate.toISOString(),
                                            'exceptDays': [],
                                            'offsetOpenRegistration': {
                                                'unit': 'minute',
                                                'offset': 2
                                            },
                                            offsetStartGame: {
                                                'unit': 'minute',
                                                'offset': 1
                                            }
                                        },
                                        id: game.id
                                    };
                                    return Game.update(gameItem, { where: { id: gameItem.id } }).then(function (count) {
                                        if (_.isArray(count) && count[0] === 0) {
                                            return next(Errors.DatabaseApi.NoRecordFound);
                                        }
                                        return next();
                                    }).catch(function (err) {
                                        return CrudHelper.callbackError(err, next);
                                    });
                                } catch (e) {
                                    return next(e);
                                }
                            }).catch(function (err) {
                                return next(err);
                            });
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    } else {
                        return next();
                    }
                },

                // Publish winning components
                function (next) {
                    try {
                        return async.mapSeries(game.winningComponents, function (winningComponent, nextWinningComponent) {
                            return AerospikeGame.publishWinningComponent({ id: winningComponent.winningComponentId }, winningComponent, nextWinningComponent);
                        }, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },

                // Publish international question multilanguage indexes (only for questions, that have multiple translations filtered by game playing languages)
                function (next) {
                    logger.debug('TOUR Publish international question multilanguage indexes (only for questions, that have multiple translations filtered by game playing languages)');
                    try {
                        let gamePools;
                        if (_.has(game, 'assignedPools')) {
                            gamePools = game.assignedPools;
                        }
                        if (_.has(game, 'questionPools')) {
                            gamePools = game.questionPools;
                        }
                        let gamePoolsIds = gamePools.items.join(',');
                        let gameLanguages = '\'' + game.playingLanguages.items.join('\',\'') + '\'';
                        // Initialize traslation parameters
                        let translationParams = {
                            poolId: null,
                            complexity: null,
                            type: null,
                            index: {}
                        };
                        let questionTranslations = [];
                        let publishLanguageIndex = {};
                        // Select game pools -> questions -> translations/languages:
                        // 1. Gather translations/languages for each pool-question
                        // 2. Calculate language index combinations and mappings<->meta
                        // 3. Publish language indexes and update mapping for any involved translation

                        // 1. Gather translations/languages for each pool-question
                        let sqlQuery =
                            'SELECT DISTINCT phq.poolId,' +
                            '                q.id AS questionId,' +
                            '                q.questionTemplateId,' +
                            '                q.complexity,' +
                            '                qt.id AS questionTranslationId,' +
                            '                qt.publishIndex,' +
                            '                qt.publishLanguageIndex,' +
                            '                qtl.iso' +
                            '  FROM pool_has_question phq' +
                            ' INNER JOIN question q ON q.id = phq.questionId AND q.isInternational = 1' +
                            ' INNER JOIN question_translation qt ON qt.questionId = q.id' +
                            ' INNER JOIN language qtl ON qtl.id = qt.languageId' +
                            ' WHERE phq.poolId IN (' + gamePoolsIds + ')' +
                            '    AND (SELECT COUNT(*)' +
                            '           FROM question_translation qtCnt' +
                            '          INNER JOIN language qtlCnt ON qtlCnt.id = qtCnt.languageId AND qtlCnt.iso IN (' + gameLanguages + ')' +
                            '          WHERE qtCnt.questionId = q.id) > 1' +
                            ' ORDER BY phq.poolId, q.id, qtl.iso;';

                        // 2. Calculate language index combinations
                        function _combinations(array, n) {
                            let combinations = [];
                            let func = function (combination, i) {
                                if (combination.length < n) {
                                    _.findIndex(array, function (v, i) {
                                        func(combination.concat([v]), i + 1);
                                    }, i);
                                } else {
                                    combinations.push(combination);
                                }
                            };
                            func([], 0);
                            return combinations;
                        }

                        // 3. Calculate all language combinations for previous pool (language1+language2..+languageN) -> publish
                        function publishLanguageIndexes(questionTranslation, publishCallback) {
                            try {
                                let languageIndexCombinations = [];
                                let languages = _.uniq(_.keys(translationParams.index)).sort();
                                let languageCombinations = _.flatMap(languages, function (value, index, collection) {
                                    return _combinations(collection, index + 1);
                                });
                                languageIndexCombinations = _.map(languageCombinations, function (languageCombination) {
                                    if (languageCombination.length > 1) {
                                        return languageCombination.join('+');
                                    }
                                });
                                _.remove(languageIndexCombinations, function (languageIndexCombination) {
                                    return _.isUndefined(languageIndexCombination);
                                });
                                logger.debug('gamePublish.publishLanguageIndexes: languageIndexCombinations', languageIndexCombinations);
                                // If next pool-question-type: iterate language index combinations for previous pool and publish language indexes
                                return async.mapSeries(languageIndexCombinations, function (languageIndex, nextLanguageIndex) {
                                    return async.series([
                                        // Get existing language index of translation or increase meta
                                        function (internationalNext) {
                                            try {
                                                translationParams.languageIndex = languageIndex;
                                                // if (_.isUndefined(publishLanguageIndex[languageIndex])) {
                                                    return AerospikePoolLanguageIndexMeta.increment(translationParams, function (err, poolMeta) {
                                                        if (err) {
                                                            return internationalNext(err);
                                                        }
                                                        translationParams.internationalIndex = poolMeta - 1;
                                                        publishLanguageIndex[languageIndex] = poolMeta - 1;
                                                        return internationalNext();
                                                    });
                                                // } else {
                                                //     logger.debug('gamePublish.publishLanguageIndexesMeta: 2');
                                                //     translationParams.internationalIndex = publishLanguageIndex[languageIndex];
                                                //     return internationalNext();
                                                // }
                                            } catch (ex) {
                                                logger.error("gamePublish.publishLanguageIndexes: exception by getting language index of translation", ex);
                                                return internationalNext(ex);
                                            }
                                        },
                                        // Deploy translation language index to Aerospike.
                                        function (internationalNext) {
                                            try {
                                                translationParams.multiIndex = translationParams.index;
                                                logger.debug('gamePublish.publishLanguageIndexes: translationParams', translationParams);
                                                return AerospikePoolLanguageIndex.create(translationParams, internationalNext);
                                            } catch (ex) {
                                                logger.error('gamePublish.publishLanguageIndexes: exception by deploying language index to aerospike', ex);
                                                return internationalNext(ex);
                                            }
                                        }
                                    ], nextLanguageIndex);
                                }, function (err) {
                                    if (err) {
                                        return publishCallback(err);
                                    }
                                    // Update language index mapping to translations of previous pool
                                    logger.debug('gamePublish.publishLanguageIndexes: questionTranslations <- publishLanguageIndex', questionTranslations, publishLanguageIndex);
                                    return async.mapSeries(questionTranslations, function (questionTranslationId, nextQuestionTranslationId) {
                                        try {
                                            return QuestionTranslation.update({ publishLanguageIndex: publishLanguageIndex }, { where: { id: questionTranslationId } }).then(function (count) {
                                                if (_.isArray(count) && count[0] === 0) {
                                                    return nextQuestionTranslationId(Errors.DatabaseApi.NoRecordFound);
                                                }
                                                return nextQuestionTranslationId();
                                            }).catch(function (err) {
                                                return CrudHelper.callbackError(err, nextQuestionTranslationId);
                                            });
                                        } catch (ex) {
                                            logger.error('gamePublish.publishLanguageIndexes: exception by updating publishLanguageIndex of question translation', ex);
                                            return nextQuestionTranslationId(ex);
                                        }
                                    }, function (err) {
                                        try {
                                            if (err) {
                                                return publishCallback(err);
                                            }
                                            // Finish if last pool-question-type entry processed
                                            if (!questionTranslation) {
                                                return publishCallback();
                                            }
                                            // Reset traslation parameters to current pool-question-type entry
                                            translationParams = {
                                                poolId: questionTranslation.poolId.toString(),
                                                complexity: questionTranslation.complexity,
                                                type: questionTranslation.questionTemplateId.toString(),
                                                index: {}
                                            };
                                            // Process next language/translation of current pool-question-type entry
                                            translationParams.index[questionTranslation.iso] = questionTranslation.publishIndex;
                                            // Reset involved translations ids, add next translation id
                                            questionTranslations = [];
                                            questionTranslations.push(questionTranslation.questionTranslationId);
                                            return publishCallback();
                                        } catch (ex) {
                                            logger.error('gamePublish.publishLanguageIndexes: exception by populating question translation ids', ex);
                                            return publishCallback(ex);
                                        }
                                    });
                                });
                            } catch (ex) {
                                logger.error('gamePublish.publishLanguageIndexes: exception by language indexes calculation', ex);
                                return setImmediate(publishCallback, ex);
                            }
                        }

                        // Process steps together
                        return CrudHelper.rawQuery(sqlQuery, {}, function (err, data) {
                            return async.mapSeries(data, function (questionTranslation, nextQuestionTranslation) {
                                try {
                                    if (_.isNull(questionTranslation.publishIndex)) {
                                        return nextQuestionTranslation(Errors.GameApi.GamePublishingNoQuestionTranslationPublished);
                                    }
                                    // Process to first pool-question-type
                                    if (_.isNull(translationParams.poolId) &&
                                        _.isNull(translationParams.complexity) &&
                                        _.isNull(translationParams.type)) {
                                        translationParams.poolId = questionTranslation.poolId.toString();
                                        translationParams.complexity = questionTranslation.complexity;
                                        translationParams.type = questionTranslation.questionTemplateId.toString();
                                    }
                                    // 2. If current pool-question-type: process next language/translation of current pool
                                    if (translationParams.poolId === questionTranslation.poolId.toString() &&
                                        translationParams.complexity === questionTranslation.complexity &&
                                        translationParams.type === questionTranslation.questionTemplateId.toString()) {
                                        // Update translation index mapping to single language
                                        translationParams.index[questionTranslation.iso] = questionTranslation.publishIndex;
                                        // Populate involved translations ids
                                        questionTranslations.push(questionTranslation.questionTranslationId);
                                        // Merge language index mapping for all involved translation entries
                                        if (questionTranslation.publishLanguageIndex) {
                                            try {
                                                publishLanguageIndex = _.assign(publishLanguageIndex, JSON.parse(questionTranslation.publishLanguageIndex));
                                            } catch (ex) {
                                                logger.debug('gamePublish.publishLanguageIndexes: cannot parse questionTranslation.publishLanguageIndex, did reset', questionTranslation.publishLanguageIndex);
                                                publishLanguageIndex = {};
                                            }
                                        }
                                        return nextQuestionTranslation();
                                    }
                                    // 3. If next pool-question-type: calculate all language combinations for previous pool (language1+language2..+languageN) -> publish
                                    return publishLanguageIndexes(questionTranslation, nextQuestionTranslation);
                                } catch (ex) {
                                    logger.error('gamePublish.publishLanguageIndexes: exception by processing question translation', ex);
                                    return nextQuestionTranslation(ex);
                                }
                            }, function (err) {
                                try {
                                    if (err) {
                                        return next(err);
                                    }
                                    // 3. If last pool-question-type: calculate all language combinations for last pool (language1+language2..+languageN) -> publish
                                    return publishLanguageIndexes(null, next);
                                } catch (ex) {
                                    logger.error('gamePublish.publishLanguageIndexes: exception by publishing language indexes', ex);
                                    return next(ex);
                                }
                            });
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        logger.error('gamePublish: exception by preparing language indexes', ex);
                        return setImmediate(next, ex);
                    }
                },

                // Publish game
                function (next) {
                    try {
                        logger.debug('TOUR in Publish game: ' + JSON.stringify(game));
                        return AerospikeGame.publishGame(game, next);
                    } catch (ex) {
                        logger.error('gamePublish: exception by publishing game entry and filters', ex);
                        return setImmediate(next, ex);
                    }
                },

                function (next) {
                    try {
                        gameParams = {};
                        gameParams.id = params.id;
                        gameParams.status = Game.constants().STATUS_ACTIVE;
                        return self.gameSetStatus(gameParams, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },

                // schedule game, if needed
                function (next) {
                    if (!schedulerService) {
                        return setImmediate(next);
                    }

                    try {
                        Game.findOne({ where: { id: params.id } }).then(function (dbGame) {
                            try {
                                let game = dbGame.get({ plain: true });
                                if (game.gameType !== Game.constants().GAME_TYPE_TOURNAMENT ||
                                    game.status !== Game.constants().STATUS_ACTIVE ||
                                    _.isEmpty(game.repetition)) {
                                    return next();
                                }
                                let tournamentType = game.gameTypeConfiguration.gameTournament.type;
                                if (['live', 'normal'].indexOf(tournamentType) === -1) {
                                    return next();
                                }
                                return schedulerService.scheduleLiveTournament({
                                    id: game.id,
                                    repetition: _.assign({}, game.repetition),
                                    eventsData: {
                                        gameId: game.id,
                                        tenantId: clientInfo.getTenantId(),
                                        appId: clientInfo.getAppId()
                                    },
                                    type: tournamentType
                                }, next);
                            } catch (e) {
                                return next(e);
                            }
                        }).catch(function (err) {
                            return next(err);
                        });
                    } catch (e) {
                        return setImmediate(next, e);
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

    gameUnpublish: function (args, callback) {
        let params = args.params;
        let schedulerService = args.schedulerService;
        let clientInfo = args.clientInfo;


        if (!params.hasOwnProperty('id')) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        let self = this;
        try {
            let game;
            async.series([

                function (next) {
                    try {
                        return self.gameGetForPublish({
                            params: params,
                            clientInfo: clientInfo,
                            validate: false
                        }, function (err, gamePublish) {
                            if (err) {
                                return next(err);
                            }
                            game = gamePublish;
                            game.id = params.id;
                            return next();
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },

                function (next) {
                    try {
                        return AerospikeGame.unpublishGame(game, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },

                function (next) {
                    try {
                        let gameParams = {};
                        gameParams.id = params.id;
                        gameParams.status = Game.constants().STATUS_INACTIVE;
                        return self.gameSetStatus(gameParams, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // unschedule events
                function (next) {
                    if (!schedulerService) {
                        return setImmediate(next);
                    }
                    try {
                        return schedulerService.unscheduleLiveTournament(params.id, next);
                    } catch (e) {
                        return setImmediate(next, e);
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

    gameValidateById: function (params, clientSession, callback) {
        let self = this;

        try {
            if (!params.hasOwnProperty('id')) {
                return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
            }

            // Should be checking also the language.tenantId here?
            let sqlQuery =
                'SELECT g.id AS id, g.complexityLevel, g.complexitySpread, g.isInternationalGame, ' +
                '   GROUP_CONCAT(DISTINCT l.id SEPARATOR ",") AS languageIds, ' +
                '   GROUP_CONCAT(DISTINCT ghqt.questionTemplateId SEPARATOR ",") as questionTemplateIds, ' +
                '   GROUP_CONCAT(DISTINCT ghp.poolId SEPARATOR ",") as poolIds, ' +
                '   GROUP_CONCAT(DISTINCT ghrs.regionalSettingId SEPARATOR ",") as regionalSettingIds ' +
                'FROM game g ' +
                'INNER JOIN game_has_question_template ghqt ON ghqt.gameId = g.id ' +
                'INNER JOIN game_has_regional_setting ghrs on ghrs.gameId = g.id ' +
                'INNER JOIN regional_setting_has_language rshl on rshl.regionalSettingId = ghrs.regionalSettingId ' +
                'INNER JOIN language l on l.id = rshl.languageId ' +
                'INNER JOIN game_has_pool ghp on g.id = ghp.gameId ' +
                'WHERE g.id = :gameId ' +
                'GROUP BY g.id';

            CrudHelper.rawQuery(sqlQuery, { gameId: params.id }, function (err, data) {
                if (err) {
                    logger.error('gameApiFactory:gameValidateById fatal error retrieving game configuration', err, params);
                    return CrudHelper.callbackError(err, callback);
                }

                if (_.isEmpty(data)) {
                    logger.error('gameApiFactory:gameValidateById - game not found', params);
                    return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
                }

                game = _.head(data);

                logger.info('gameApiFactory:gameValidateById - retrieved the following game configuration data', game);

                return self.gameValidateInternal(game, function (err, result) {
                    if (err) {
                        return CrudHelper.callbackError(err, callback);
                    }
                    return CrudHelper.callbackSuccess(result.data, callback);
                });
            });
        } catch (ex) {
            logger.error('gameApiFactory:gameValidate exception retrieving game configuration', ex);
            return CrudHelper.callbackError(err, callback);
        }
    },

    /**
       * Validate if there are enough question of a certain type as setup in game configuration
       * so that the user can play the game
       */
    gameValidate: function (params, clientSession, callback) {
        let self = this;
        try {
            return self.gameValidateInternal(params, function (err, result) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess(result.data, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
       * Validate if there are enough question of a certain type as setup in game configuration
       * so that the user can play the game, check if game should be deactivated
       */
    gameValidateInternal: function (params, callback) {
        let self = this;

        try {
            let game = params;
            let gameQuestionConfiguration = [],
                validationErrors = [],
                validationWarnings = [];
            let minimumQuestions = Config.game.minimumQuestions;

            logger.info('gameApiFactory:gameValidate - game configuration', JSON.stringify(game));

            let gameConfigurationTemplate = {
                // Field to be filled in with each combination of complexity, template, regional settings, language
                poolId: false,
                complexityLevel: false,
                questionTemplateId: false,
                languageId: false,
                stats: {
                    required: 0, // setup with actual number of required questions for the given complexity level
                    available: 0
                }
            };

            let gameComplexityLevel = game.complexityLevel;
            if (_.isString(game.complexityLevel)) {
                // Parse into object
                gameComplexityLevel = JSON.parse(game.complexityLevel);
            }

            let languageIds = game.languageIds;
            if (_.isString(game.languageIds)) {
                // Parse into object
                languageIds = game.languageIds.split(',');
            }

            let questionTemplateIds = game.questionTemplateIds;
            if (_.isString(game.questionTemplateIds)) {
                // Parse into object
                questionTemplateIds = game.questionTemplateIds.split(',');
            }

            let regionalSettingsIds = game.regionalSettingIds;
            if (_.isString(game.regionalSettingIds)) {
                // Parse into array
                regionalSettingsIds = game.regionalSettingIds.split(',');
            }

            let poolIds = game.poolIds;
            if (_.isString(game.poolIds)) {
                // Parse into array
                poolIds = game.poolIds.split(',');
            }

            // No record found, no admin setup return 404
            _.forEach(gameComplexityLevel, function (complexityValue, complexityKey) {
                let requiredQuestions = 0;

                if (complexityValue > 0) {
                    if (game.complexitySpread == 'fixed') {
                        requiredQuestions = complexityValue;
                    } else if (game.complexitySpread == 'percentage') {
                        requiredQuestions = Math.ceil((minimumQuestions * complexityValue) / 100);
                    }
                }

                if (requiredQuestions <= 0) return;

                _.forEach(poolIds, function (poolId) {
                    _.forEach(languageIds, function (languageId) {
                        _.forEach(questionTemplateIds, function (templateId) {
                            let newGameConfiguration = _.cloneDeep(gameConfigurationTemplate);

                            newGameConfiguration.poolId = poolId;
                            newGameConfiguration.complexityLevel = complexityKey;
                            newGameConfiguration.languageId = languageId;
                            newGameConfiguration.questionTemplateId = templateId;
                            newGameConfiguration.stats.required = requiredQuestions;

                            gameQuestionConfiguration.push(newGameConfiguration);
                        });
                    });
                });
            });

            let deactivate = true;
            async.series([
                // Validate if the game has basic properties setup such as regionalSettings or complexity
                function (next) {
                    try {
                        if (_.isEmpty(gameQuestionConfiguration)) {
                            validationErrors.push({
                                'type': 'game_configuration_empty',
                                'data': {
                                    game: game
                                },
                                'message': 'Empty game configuration'
                            });

                            return next(Errors.GameApi.GameConfigurationInvalid);
                        }
                        return setImmediate(next);
                    } catch (ex) {
                        logger.error('gameApiFactory:gameValidate - empty game configuration', game);
                        return next(Errors.QuestionApi.FatalError);
                    }
                },

                // Validate if there are enough questions with a given complexity level for a given region
                function (next) {
                    try {
                        let complexityIdList = {},
                            languageIdList = {},
                            templateIdList = {};

                        _.forEach(gameQuestionConfiguration, function (value) {
                            complexityIdList[value.complexityLevel] = value.complexityLevel;
                            languageIdList[value.languageId] = value.languageId;
                            templateIdList[value.questionTemplateId] = value.questionTemplateId;
                        });

                        let sqlQuery =
                            'SELECT q.id, q.complexity as complexity, q.questionTemplateId as questionTemplateId, ' +
                            '  GROUP_CONCAT(DISTINCT qhrs.regionalSettingId SEPARATOR ",") as regionalSettingIds, ' +
                            '  GROUP_CONCAT(DISTINCT qt.languageId SEPARATOR ",") as languageIds, ' +
                            '  GROUP_CONCAT(DISTINCT phq.poolId SEPARATOR ",") as poolIds ' +
                            'FROM question q ' +
                            '  INNER JOIN pool_has_question phq on phq.questionId = q.id ' +
                            '  INNER JOIN question_translation qt on qt.questionId = q.id ' +
                            '  LEFT JOIN question_has_regional_setting qhrs on qhrs.questionId = q.id ' +
                            'WHERE q.complexity IN (:complexityIdList) ' +
                            '  AND phq.poolId IN (:poolIdList) ' +
                            '  AND q.questionTemplateId IN (:templateIdList) ' +
                            '  AND qt.languageId IN (:languageIdList) ' +
                            'GROUP BY q.id';

                        CrudHelper.rawQuery(
                            sqlQuery,
                            {
                                poolIdList: poolIds,
                                complexityIdList: _.keys(complexityIdList),
                                templateIdList: _.keys(templateIdList),
                                languageIdList: _.keys(languageIdList)
                            },
                            function (err, data) {
                                if (err) {
                                    logger.error('gameApiFactory:gameValidate fatal error searching for questions', err);
                                    return next(Errors.QuestionApi.FatalError);
                                }

                                let questionLanguageIds,
                                    questionPoolIds,
                                    questionRegionalSettingsIds;

                                _.forEach(data, function (resultRow) {
                                    _.forEach(gameQuestionConfiguration, function (configRow, key) {
                                        if (configRow.complexityLevel == resultRow.complexity &&
                                            configRow.questionTemplateId == resultRow.questionTemplateId) {
                                            questionLanguageIds = resultRow.languageIds.split(',');
                                            questionPoolIds = resultRow.poolIds.split(',');

                                            // Search if question has appropriate translation
                                            if (_.findIndex(questionLanguageIds, function (val) { return val == configRow.languageId; }) === -1) return;

                                            // Search if question has required pool
                                            if (_.findIndex(questionPoolIds, function (val) { return val == configRow.poolId; }) === -1) return;

                                            // Do not count questions with no regional settings setup
                                            if (_.isNull(resultRow.regionalSettingIds)) {
                                                logger.warn('gameApiFactory:gameValidate skipping question with region missing, question info', resultRow);
                                                return;
                                            }

                                            // Check if either game/question is international or
                                            questionRegionalSettingsIds = resultRow.regionalSettingIds.split(',');
                                            if ((game.isInternationalGame && (questionLanguageIds.length > 1 || questionRegionalSettingsIds.length > 1)) ||
                                                _.intersectionBy(regionalSettingsIds, questionRegionalSettingsIds, _.isEqual).length > 0) {
                                                gameQuestionConfiguration[key].stats.available++;
                                            }
                                        }
                                    });
                                });

                                return next(false);
                            }
                        );
                    } catch (ex) {
                        logger.error('gameApiFactory:gameValidate couting questions exception', ex);
                        return next(Errors.QuestionApi.FatalError);
                    }
                },
                // Validate if the game has basic properties setup such as regionalSettings or complexity
                function (next) {
                    try {
                        _.forEach(gameQuestionConfiguration, function (value) {
                            if (value.stats.available <= value.stats.required) {
                                validationErrors.push({
                                    'type': 'game_configuration_invalid',
                                    'result': {
                                        'game': game,
                                        configuration: value
                                    },
                                    'message': 'Invalid game configuration'
                                });
                            } else {
                                deactivate = false;
                            }
                        });

                        return next();
                    } catch (ex) {
                        return next(Errors.QuestionApi.FatalError);
                    }
                },
                // Push errors also into the validation log
                function (next) {
                    try {
                        if (!_.has(game, 'id')) {
                            // If the game does not have an id we don't log the validation errors encountered
                            return setImmediate(next, false);
                        }
                        validationErrorLog = [];

                        _.forEach(validationErrors, function (value) {
                            validationErrorLog.push({
                                gameId: game.id,
                                result: JSON.stringify(value.result),
                                warningMessage: value.message,
                                createDate: _.now()
                            });
                        });

                        GamePoolValidationResult.bulkCreate(validationErrorLog)
                            .then(function () {
                                return next();
                            })
                            .catch(function (err) {
                                logger.error('gameApiFactory.gameValidate - could not bulk create log entries', err);
                                return CrudHelper.callbackError(err, next);
                            });
                    } catch (ex) {
                        return next(Errors.QuestionApi.FatalError);
                    }
                }

            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }

                CrudHelper.callbackSuccess({
                    data:
                        {
                            errors: validationErrors,
                            warnings: validationWarnings,
                            statistics: {
                                game: game,
                                questions: gameQuestionConfiguration
                            }
                        },
                    deactivate: deactivate
                }, callback);
            });
        } catch (ex) {
            logger.error('gameApiFactory.gameValidate - exception', ex);
            return CrudHelper.callbackError(ex, callback);
        }
    }

};
