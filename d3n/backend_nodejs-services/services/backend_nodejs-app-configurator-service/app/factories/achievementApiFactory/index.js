const _ = require('lodash');
const async = require('async');
const Config = require('./../../config/config.js');
const Errors = require('./../../config/errors.js');
const AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
const AutoMapper = AutoMapperInstance.AutoMapper;
const CrudHelper = AutoMapperInstance.CrudHelper;
const DateUtils = require('nodejs-utils').DateUtils;
const TenantService = require('../../services/tenantService.js');
const AchievementService = require('../../services/achievementService.js');
const Database = require('nodejs-database').getInstance(Config);
const Achievement = Database.RdbmsService.Models.AchievementManager.Achievement;
const Badge = Database.RdbmsService.Models.AchievementManager.Badge;
const AchievementHasBadge = Database.RdbmsService.Models.AchievementManager.AchievementHasBadge;
const BadgeHasBadge = Database.RdbmsService.Models.AchievementManager.BadgeHasBadge;
const KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
const AerospikeAchievement = KeyvalueService.Models.AerospikeAchievement;
const AerospikeAchievementList = KeyvalueService.Models.AerospikeAchievementList;
const AerospikeTenantsWithAchievements = KeyvalueService.Models.AerospikeTenantsWithAchievements;
const AerospikeBadge = KeyvalueService.Models.AerospikeBadge;
const AerospikeBadgeList = KeyvalueService.Models.AerospikeBadgeList;
const logger = require('nodejs-logger')();

const INSTANCE_MAPPINGS = {
  achievementModel: [
    {
      destination: '$root',
      model: Achievement
    },
    {
      destination: 'badges',
      model: Badge,
      hasModel: AchievementHasBadge,
      type: AutoMapper.TYPE_OBJECT_LIST,
      limit: 0
    }
  ],
  achievementCreateModel: [
    {
      destination: '$root',
      model: Achievement
    },
    {
      destination: 'badgesIds',
      model: AchievementHasBadge,
      attribute: AchievementHasBadge.tableAttributes.badgeId
    }
  ],
  badgeModel: [
    {
      destination: '$root',
      model: Badge
    },
    {
      destination: 'relatedBadgesIds',
      model: BadgeHasBadge,
      attribute: BadgeHasBadge.tableAttributes.relatedBadgeId,
      type: AutoMapper.TYPE_ATTRIBUTE_LIST,
      limit: 0
    }
  ],
  badgeCreateModel: [
    {
      destination: '$root',
      model: Badge
    },
    {
      destination: 'relatedBadgesIds',
      model: BadgeHasBadge,
      attribute: BadgeHasBadge.tableAttributes.relatedBadgeId,
      type: AutoMapper.TYPE_ATTRIBUTE_LIST,
      limit: 0
    }
  ],
  achievementPublishModel: [
    {
      destination: '$root',
      model: Achievement
    }
  ],
  badgePublishModel: [
    {
      destination: '$root',
      model: Badge
    },
    {
      destination: 'badges',
      model: BadgeHasBadge,
      attribute: BadgeHasBadge.tableAttributes.relatedBadgeId,
      type: AutoMapper.TYPE_ATTRIBUTE_LIST,
      limit: 0
    }
  ]
};
INSTANCE_MAPPINGS.achievementUpdateModel = INSTANCE_MAPPINGS.achievementCreateModel;
INSTANCE_MAPPINGS.badgeUpdateModel = INSTANCE_MAPPINGS.badgeCreateModel;

module.exports = {

  /**
     *
     * @param {{}} params
     * @param {{}} callback
     * @returns {*}
     */
  achievementUpdate: (params, message, clientSession, callback) => {
    const self = this;
    try {
      let create = true;
      let sessionTenantId;
      let achievementId;
      let mapBySchema = 'achievementCreateModel';
      if (_.has(params, 'id') && params.id) {
        create = false;
        achievementId = params.id;
        mapBySchema = 'achievementUpdateModel';
      }
      let achievementAssociates;
      const achievementItem = AutoMapper.mapDefinedBySchema(
        mapBySchema,
        INSTANCE_MAPPINGS,
        params,
        true
      );
      return async.series([
        // Get tenantId from global session
        (next)=>{
          TenantService.getSessionTenantId(message, clientSession, (err, tenantId)=>{
            if (err) {
              return next(err);
            }
            sessionTenantId = tenantId;
            return next();
          });
        },
        // Check current status (only for update), should not be activated/archived
        (next)=>{
          if (create) {
            return setImmediate(next);
          }
          return Achievement.findOne({
            where: { id: params.id, tenantId: sessionTenantId }
          }).then((achievement)=>{
            if (!achievement) {
              return next(Errors.DatabaseApi.NoRecordFound);
            }
            // Only inactive achievements can be updated
            if (achievement.status === Achievement.constants().STATUS_ACTIVE) {
              return next(Errors.AchievementApi.AchievementIsActivated);
            }
            if (achievement.status === Achievement.constants().STATUS_ARCHIVED) {
              return next(Errors.AchievementApi.AchievementIsArchived);
            }
            return next();
          }).catch((err)=>{
            return CrudHelper.callbackError(err, next);
          });
        },
        // Create/update main entity
        (next)=>{
          if (create) {
            achievementItem.tenantId = sessionTenantId;
            return Achievement.create(achievementItem).then((achievement)=>{
              achievementId = achievement.get({ plain: true }).id;
              return next();
            }).catch((err)=>{
              return CrudHelper.callbackError(err, next);
            });
          }
          const options = {
            where: {
              id: achievementItem.id,
              tenantId: sessionTenantId
            },
            individualHooks: true
          };
          return Achievement.update(achievementItem, options).then((count)=>{
            if (count[0] === 0) {
              return next(Errors.DatabaseApi.NoRecordFound);
            }
            return next();
          }).catch((err)=>{
            return CrudHelper.callbackError(err, next);
          });
        },
        // Populate achievement associated entity values
        (next)=>{
          try {
            achievementAssociates = AutoMapper.mapAssociatesDefinedBySchema(
              mapBySchema,
              INSTANCE_MAPPINGS,
              params, {
                field: 'achievementId',
                value: achievementId
              }
            );
            return setImmediate(next, null);
          } catch (ex) {
            return setImmediate(next, ex);
          }
        },
        // Delete child entities for every updateable associated entity
        (next)=>{
          if (create || _.isUndefined(achievementAssociates)) {
            return setImmediate(next, null);
          }
          return async.mapSeries(achievementAssociates, (itemAssociate, remove)=>{
            // if (itemAssociate.values.length === 0) {
            //    return remove();
            // }
            return itemAssociate.model.destroy({
              where: { achievementId: achievementId }
            }).then(()=>{
              return remove();
            }).catch((err)=>{
              return CrudHelper.callbackError(err, remove);
            });
          }, next);
        },
        // Bulk insert populated values for every updateable associated entity
        (next)=>{
          if (_.isUndefined(achievementAssociates)) {
            return setImmediate(next, null);
          }
          return async.mapSeries(achievementAssociates, (itemAssociate, createFunc)=>{
            if (itemAssociate.values.length === 0) {
              return createFunc();
            }
            return itemAssociate.model.bulkCreate(itemAssociate.values).then((records)=>{
              if (records.length !== itemAssociate.values.length) {
                return createFunc(Errors.QuestionApi.FatalError);
              }
              return createFunc();
            }).catch((err)=>{
              return CrudHelper.callbackError(err, createFunc);
            });
          }, next);
        }
      ], (err)=>{
        if (err) {
          return CrudHelper.callbackError(err, callback);
        }
        return self.achievementGet(Object.assign(params, {
          id: achievementId
        }), message, clientSession, callback);
      });
    } catch (ex) {
      return CrudHelper.callbackError(ex, callback);
    }
  },

  /**
     * Set Achievement status
     * @param {{}} params
     * @param {{}} callback
     * @returns {*}
     */
  achievementSetStatus: (params, callback)=>{
    if (params.status === undefined || (
      params.status !== Achievement.constants().STATUS_ACTIVE &&
            params.status !== Achievement.constants().STATUS_INACTIVE &&
            params.status !== Achievement.constants().STATUS_ARCHIVED)) {
      return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
    }
    try {
      return async.series([
        // Check current status
        (next)=>{
          return Achievement.findOne({ where: { id: params.id } }).then((achievement)=>{
            if (!achievement) {
              return next(Errors.DatabaseApi.NoRecordFound);
            }
            // Only undeployed and inactive
            // (but not archived) achievements can be archived/activated
            if (
              params.status === Achievement.constants().STATUS_ARCHIVED ||
                params.status === Achievement.constants().STATUS_ACTIVE
            ) {
              if (achievement.status === Achievement.constants().STATUS_ARCHIVED) {
                return next(Errors.AchievementApi.AchievementIsArchived);
              }
            }
            // Only undeployed and active achievements can be deactivated
            if (params.status === Achievement.constants().STATUS_INACTIVE) {
              if (achievement.status === Achievement.constants().STATUS_INACTIVE) {
                return next(Errors.AchievementApi.AchievementIsDeactivated);
              }
              if (achievement.status === Achievement.constants().STATUS_ARCHIVED) {
                return next(Errors.AchievementApi.AchievementIsArchived);
              }
            }
            return next();
          }).catch((err)=>{
            return CrudHelper.callbackError(err, next);
          });
        },
        // Update status
        (next)=>{
          return Achievement.update(
            { status: params.status },
            { where: { id: params.id }, individualHooks: true }
          ).then((count)=>{
            if (count[0] === 0) {
              return next(Errors.DatabaseApi.NoRecordFound);
            }
            return next();
          }).catch((err)=>{
            return CrudHelper.callbackError(err, next);
          });
        }
      ], (err)=>{
        if (err) {
          return CrudHelper.callbackError(err, callback);
        }
        return CrudHelper.callbackSuccess(null, callback);
      });
    } catch (ex) {
      return CrudHelper.callbackError(ex, callback);
    }
  },

  achievementDelete: (params, clientSession, callback)=>{
    try {
      return async.series([
        // Check current status, should not be activated/archived
        (next)=>{
          try {
            return Achievement.findOne({ where: { id: params.id } }).then((achievement)=>{
              if (!achievement) {
                return next(Errors.DatabaseApi.NoRecordFound);
              }
              // Only inactive achievements can be deleted
              if (achievement.status === Achievement.constants().STATUS_ACTIVE) {
                return next(Errors.AchievementApi.AchievementIsActivated);
              }
              if (achievement.status === Achievement.constants().STATUS_ARCHIVED) {
                return next(Errors.AchievementApi.AchievementIsArchived);
              }
              return next();
            }).catch((err)=>{
              return CrudHelper.callbackError(err, next);
            });
          } catch (ex) {
            return setImmediate(next, ex);
          }
        },
        // Remove achievement
        (next)=>{
          try {
            return Achievement.destroy({ where: { id: params.id } }).then((count)=>{
              if (count === 0) {
                return next(Errors.DatabaseApi.NoRecordFound);
              }
              return next();
            }).catch((err)=>{
              return CrudHelper.callbackError(err, next);
            });
          } catch (ex) {
            return setImmediate(next, ex);
          }
        },

        // publish refresh event
        (next)=>{
          try {
            return clientSession.getConnectionService().getEventService().publish('analytics.achievements.refresh', {}, false, next);
          } catch (e) {
            return setImmediate(next, e);
          }
        }

      ], (err)=>{
        if (err) {
          return CrudHelper.callbackError(err, callback);
        }
        return CrudHelper.callbackSuccess(null, callback);
      });
    } catch (ex) {
      return CrudHelper.callbackError(ex, callback);
    }
  },

  achievementGet: (params, message, clientSession, callback)=>{
    try {
      const response = {};
      const responseItem = 'achievement';
      const mapBySchema = 'achievementModel';
      const attributes = _.keys(Achievement.attributes);
      return TenantService.getSessionTenantId(message, clientSession, (err, tenantId)=>{
        if (err) {
          return CrudHelper.callbackError(err, callback);
        }
        let include = CrudHelper.include(Achievement, [
          AchievementHasBadge
        ]);
        include = CrudHelper.includeNestedWhere(
          include,
          AchievementHasBadge,
          Badge,
          { tenantId: tenantId },
          true
        );
        return Achievement.findOne({
          where: { id: params.id, tenantId: tenantId },
          attributes: attributes,
          include: include,
          subQuery: false
        }).then((achievement)=>{
          if (!achievement) {
            return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
          }
          const achievementItem = AutoMapper.mapDefinedBySchema(
            mapBySchema,
            INSTANCE_MAPPINGS,
            achievement
          );
          response[responseItem] = achievementItem;
          return CrudHelper.callbackSuccess(response, callback);
        }).catch((er)=>{
          return CrudHelper.callbackError(er, callback);
        });
      });
    } catch (ex) {
      return CrudHelper.callbackError(ex, callback);
    }
  },

  achievementList: (params, message, clientSession, callback)=>{
    try {
      const mapBySchema = 'achievementModel';
      const attributes = _.keys(Achievement.attributes);
      const orderBy = CrudHelper.orderBy(params, Achievement);
      let searchBy = CrudHelper.searchBy(params, Achievement);
      let achievementItems = [];
      let total = 0;
      let sessionTenantId;
      return async.series([
        // add tenantId filter
        (next)=>{
          return TenantService.getSessionTenantId(message, clientSession, (err, tenantId)=>{
            if (err) {
              return next(err);
            }
            sessionTenantId = tenantId;
            return next();
          });
        },
        // Populate total count and badge ids
        // (involve base model and nested models only with defined search criterias)
        (next)=>{
          const include = CrudHelper.includeDefined(Achievement, [
            AchievementHasBadge
          ], params, mapBySchema, INSTANCE_MAPPINGS);
          searchBy.tenantId = sessionTenantId;
          return Achievement.findAndCountAll({
            limit: params.limit === 0 ? null : params.limit,
            offset: params.limit === 0 ? null : params.offset,
            where: searchBy,
            order: orderBy,
            include: include,
            distinct: true,
            subQuery: false
          }).then((achievements)=>{
            total = achievements.count;
            if (!_.has(searchBy, 'id')) {
              const achievementsIds = [];
              _.forEach(achievements.rows, (achievement)=>{
                achievementsIds.push(achievement.id);
              });
              searchBy = {
                id: { $in: achievementsIds }
              };
            }
            return next();
          }).catch((err)=>{
            return CrudHelper.callbackError(err, next);
          });
        },
        // Populate model list with all nested model elements
        (next)=>{
          if (total === 0) {
            return setImmediate(next, null);
          }
          let include = CrudHelper.include(Achievement, [
            AchievementHasBadge
          ]);
          include = CrudHelper.includeNestedWhere(
            include,
            AchievementHasBadge,
            Badge,
            { tenantId: sessionTenantId },
            true
          );
          return Achievement.findAll({
            where: searchBy,
            order: orderBy,
            attributes: attributes,
            include: include,
            subQuery: false
          }).then((achievements)=>{
            achievementItems = AutoMapper.mapListDefinedBySchema(
              mapBySchema,
              INSTANCE_MAPPINGS,
              achievements
            );
            return next();
          }).catch((err)=>{
            return CrudHelper.callbackError(err, next);
          });
        }
      ], (err)=>{
        if (err) {
          return CrudHelper.callbackError(err, callback);
        }
        return CrudHelper.callbackSuccess({
          items: achievementItems,
          limit: params.limit,
          offset: params.offset,
          total: total
        }, callback);
      });
    } catch (ex) {
      return CrudHelper.callbackError(ex, callback);
    }
  },

  _achievementMap: (achievement)=>{
    // Custom Achievement mapper object
    const achievementItem = {};
    const achievementPlain = achievement.get({ plain: true });
    AutoMapper.mapDefined(achievementPlain, achievementItem)
      .forMember('id')
      .forMember('tenantId')
      .forMember('name')
      .forMember('description')
      .forMember('status')
      .forMember('image', 'imageId')
      .forMember('timePeriod')
      .forMember('bonusPointsReward')
      .forMember('creditReward')
      .forMember('paymentMultiplier')
      .forMember('accessRules')
      .forMember('messaging');
    if (_.has(achievementPlain, 'isTimeLimit')) {
      achievementItem.timeLimit = achievementPlain.isTimeLimit === 1;
    }
    if (_.has(achievementPlain, 'isReward')) {
      achievementItem.reward = achievementPlain.isReward === 1;
    }
    if (_.has(achievementPlain, 'badges')) {
      achievementItem.badges = _.map(achievementPlain.badges, Achievement.tableAttributes.id);
    }
    const achievementPublish = AutoMapper.mapDefinedBySchema('achievementPublishModel', INSTANCE_MAPPINGS, achievementItem);
    return achievementPublish;
  },

  achievementGetForPublish: (params, message, clientSession, callback)=>{
    const self = this;
    try {
      let achievementPublish;
      let sessionTenantId;
      return async.series([
        // Get tenant id from session
        (next)=>{
          return TenantService.getSessionTenantId(message, clientSession, (err, tenantId)=>{
            if (err) {
              return next(err);
            }
            sessionTenantId = tenantId;
            return next();
          });
        },
        // Get achievement record by tenant
        (next)=>{
          return Achievement.findOne({
            where: { id: params.id, tenantId: sessionTenantId },
            include: { model: AchievementHasBadge, required: true }
          }).then((achievement)=>{
            if (!achievement) {
              return next(Errors.DatabaseApi.NoRecordFound);
            }
            if (
              params.status === Achievement.constants().STATUS_ACTIVE &&
                achievement.status === Achievement.constants().STATUS_ACTIVE
            ) {
              return next(Errors.AchievementApi.AchievementIsActivated);
            }
            if (
              params.status === Achievement.constants().STATUS_INACTIVE &&
                achievement.status === Achievement.constants().STATUS_INACTIVE) {
              return next(Errors.AchievementApi.AchievementIsDeactivated);
            }
            if (achievement.status === Achievement.constants().STATUS_ARCHIVED) {
              return next(Errors.AchievementApi.AchievementIsArchived);
            }
            achievementPublish = self._achievementMap(achievement);
            return next();
          }).catch((err)=>{
            return CrudHelper.callbackError(err, next);
          });
        },
        // Get all tenants, where achievements are activated, including current
        (next)=>{
          const groupBy = CrudHelper.groupBy(Achievement.tableAttributes.tenantId);
          return Achievement.findAll({
            where: {
              status: Achievement.constants().STATUS_ACTIVE,
              id: { $ne: achievementPublish.id }
            },
            attributes: [Achievement.tableAttributes.tenantId.field],
            group: groupBy
          }).then((achievements)=>{
            achievementPublish.tenants = [];
            _.forEach(achievements, (achievement)=>{
              const achievementItem = achievement.get({ plain: true });
              achievementPublish.tenants.push(achievementItem.tenantId);
            });
            if (params.status === Achievement.constants().STATUS_ACTIVE) {
              achievementPublish.tenants = _.union(achievementPublish.tenants, [sessionTenantId]);
            }
            return next();
          }).catch((err)=>{
            return CrudHelper.callbackError(err, next);
          });
        }
      ], (err)=>{
        if (err) {
          return CrudHelper.callbackError(err, callback);
        }
        logger.info('AchievementApiFactory.achievementGetForPublish', JSON.stringify(achievementPublish));
        return CrudHelper.callbackSuccess(achievementPublish, callback);
      });
    } catch (ex) {
      return CrudHelper.callbackError(ex, callback);
    }
  },

  achievementPublish: (params, message, clientSession, callback)=>{
    if (!params.id === undefined) {
      return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
    }
    const self = this;
    try {
      let achievement;
      return async.series([
        // Get achievement data for publishing
        (next)=>{
          try {
            return self.achievementGetForPublish(Object.assign(params, {
              status: Achievement.constants().STATUS_ACTIVE
            }), message, clientSession, (err, achievementPublish)=>{
              if (err) {
                return next(err);
              }
              achievement = achievementPublish;
              achievement.id = params.id;
              return next();
            });
          } catch (ex) {
            return setImmediate(next, ex);
          }
        },
        // Publish achievement
        (next)=>{
          try {
            return AerospikeAchievement.publish(achievement, next);
          } catch (ex) {
            return setImmediate(next, ex);
          }
        },
        // Publish achievement list
        (next)=>{
          try {
            return AerospikeAchievementList.publish(achievement, next);
          } catch (ex) {
            return setImmediate(next, ex);
          }
        },
        // Update tenant list with active achievements
        (next)=>{
          try {
            return AerospikeTenantsWithAchievements.update(achievement, next);
          } catch (ex) {
            return setImmediate(next, ex);
          }
        },
        // Set achievement deployment status
        (next)=>{
          try {
            const achievementParams = {
              id: params.id,
              status: Achievement.constants().STATUS_ACTIVE
            };
            return self.achievementSetStatus(achievementParams, next);
          } catch (ex) {
            return setImmediate(next, ex);
          }
        },
        // publish refresh event
        (next)=>{
          try {
            return clientSession.getConnectionService().getEventService().publish('analytics.achievements.refresh', {}, false, next);
          } catch (e) {
            return setImmediate(next, e);
          }
        }

      ], (err)=>{
        if (err) {
          return CrudHelper.callbackError(err, callback);
        }
        return CrudHelper.callbackSuccess(null, callback);
      });
    } catch (ex) {
      return CrudHelper.callbackError(ex, callback);
    }
  },

  achievementUnpublish: (params, message, clientSession, callback)=>{
    if (!params.id === undefined) {
      return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
    }
    const self = this;
    try {
      let achievement;
      return async.series([
        // Get achievement data for publishing
        (next)=>{
          try {
            return self.achievementGetForPublish(Object.assign(params, {
              status: Achievement.constants().STATUS_INACTIVE
            }), message, clientSession, (err, achievementPublish)=>{
              if (err) {
                return next(err);
              }
              achievement = achievementPublish;
              achievement.id = params.id;
              return next();
            });
          } catch (ex) {
            return setImmediate(next, ex);
          }
        },
        // Unpublish achievement
        (next)=>{
          try {
            return AerospikeAchievement.unpublish(achievement, next);
          } catch (ex) {
            return setImmediate(next, ex);
          }
        },
        // Unpublish achievement list
        (next)=>{
          try {
            return AerospikeAchievementList.unpublish(achievement, next);
          } catch (ex) {
            return setImmediate(next, ex);
          }
        },
        // Update tenant list with active achievements
        (next)=>{
          try {
            return AerospikeTenantsWithAchievements.update(achievement, next);
          } catch (ex) {
            return setImmediate(next, ex);
          }
        },
        // Reset achievement deployment status
        (next)=>{
          try {
            const achievementParams = {};
            achievementParams.id = params.id;
            achievementParams.status = Achievement.constants().STATUS_INACTIVE;
            return self.achievementSetStatus(achievementParams, next);
          } catch (ex) {
            return setImmediate(next, ex);
          }
        },
        // publish refresh event
        (next)=>{
          try {
            return clientSession.getConnectionService().getEventService().publish('analytics.achievements.refresh', {}, false, next);
          } catch (e) {
            return setImmediate(next, e);
          }
        }

      ], (err)=>{
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
     *
     * @param {{}} params
     * @param {{}} callback
     * @returns {*}
     */
  badgeUpdate: (params, message, clientSession, callback)=>{
    const self = this;
    try {
      let create = true;
      let sessionTenantId;
      let badgeId;
      let mapBySchema = 'badgeCreateModel';
      if (_.has(params, 'id') && params.id) {
        create = false;
        badgeId = params.id;
        mapBySchema = 'badgeUpdateModel';
      }
      let badgeAssociates;
      const badgeItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, params, true);
      return async.series([
        // Get tenantId from global session
        (next)=>{
          return TenantService.getSessionTenantId(message, clientSession, (err, tenantId)=>{
            if (err) {
              return next(err);
            }
            sessionTenantId = tenantId;
            return next();
          });
        },
        // Check current status (only for update), should not be activated/archived
        (next)=>{
          if (create) {
            return setImmediate(next);
          }
          return Badge.findOne({
            where: { id: params.id, tenantId: sessionTenantId }
          }).then((badge)=>{
            if (!badge) {
              return next(Errors.DatabaseApi.NoRecordFound);
            }
            // Only inactive badges can be updated
            if (badge.status === Badge.constants().STATUS_ACTIVE) {
              return next(Errors.AchievementApi.BadgeIsActivated);
            }
            if (badge.status === Badge.constants().STATUS_ARCHIVED) {
              return next(Errors.AchievementApi.BadgeIsArchived);
            }
            return next();
          }).catch((err)=>{
            return CrudHelper.callbackError(err, next);
          });
        },
        // Create/update main entity
        (next)=>{
          if (create) {
            badgeItem.tenantId = sessionTenantId;
            return Badge.create(badgeItem).then((badge)=>{
              badgeId = badge.get({ plain: true }).id;
              return next();
            }).catch((err)=>{
              return CrudHelper.callbackError(err, next);
            });
          }
          const options = {
            where: {
              id: badgeItem.id,
              tenantId: sessionTenantId
            },
            individualHooks: true
          };
          return Badge.update(badgeItem, options).then((count)=>{
            if (count[0] === 0) {
              return next(Errors.DatabaseApi.NoRecordFound);
            }
            return next();
          }).catch((err)=>{
            return CrudHelper.callbackError(err, next);
          });
        },
        // Populate achievement associated entity values
        (next)=>{
          try {
            badgeAssociates = AutoMapper.mapAssociatesDefinedBySchema(
              mapBySchema,
              INSTANCE_MAPPINGS,
              params, {
                field: 'badgeId',
                value: badgeId
              }
            );
            return setImmediate(next, null);
          } catch (ex) {
            return setImmediate(next, ex);
          }
        },
        // Delete child entities for every updateable associated entity
        (next)=>{
          if (create || _.isUndefined(badgeAssociates)) {
            return setImmediate(next, null);
          }
          return async.mapSeries(badgeAssociates, (itemAssociate, remove)=>{
            // if (itemAssociate.values.length === 0) {
            //    return remove();
            // }
            return itemAssociate.model.destroy({ where: { badgeId: badgeId } }).then(()=>{
              return remove();
            }).catch((err)=>{
              return CrudHelper.callbackError(err, remove);
            });
          }, next);
        },
        // Bulk insert populated values for every updateable associated entity
        (next)=>{
          if (_.isUndefined(badgeAssociates)) {
            return setImmediate(next, null);
          }
          return async.mapSeries(badgeAssociates, (itemAssociate, createFunc)=>{
            if (itemAssociate.values.length === 0) {
              return createFunc();
            }
            return itemAssociate.model.bulkCreate(itemAssociate.values).then((records)=>{
              if (records.length !== itemAssociate.values.length) {
                return createFunc(Errors.QuestionApi.FatalError);
              }
              return createFunc();
            }).catch((err)=>{
              return CrudHelper.callbackError(err, createFunc);
            });
          }, next);
        }
      ], (err)=>{
        if (err) {
          return CrudHelper.callbackError(err, callback);
        }
        return self.badgeGet(Object.assign(params, {
          id: badgeId
        }), message, clientSession, callback);
      });
    } catch (ex) {
      return CrudHelper.callbackError(ex, callback);
    }
  },

  /**
     * Set Badge status
     * @param {{}} params
     * @param {{}} callback
     * @returns {*}
     */
  badgeSetStatus: (params, callback)=>{
    if (params.status === undefined || (
      params.status !== Badge.constants().STATUS_ACTIVE &&
            params.status !== Badge.constants().STATUS_INACTIVE &&
            params.status !== Badge.constants().STATUS_ARCHIVED)) {
      return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
    }
    try {
      return async.series([
        // Check current deployment status of parent badges
        (next)=>{
          return Achievement.count({
            where: { status: Achievement.constants().STATUS_ACTIVE },
            include: { model: AchievementHasBadge, where: { badgeId: params.id } },
            required: true
          }).then((achievementCount)=>{
            return Badge.count({
              where: { status: Badge.constants().STATUS_ACTIVE },
              include: { model: BadgeHasBadge, alias: 'relatedBadges', where: { relatedBadgeId: params.id } },
              required: true
            }).then((badgeCount)=>{
              const activeParents = achievementCount + badgeCount;
              // Only undependent badges can be deactivated or archived
              if (
                params.status === Badge.constants().STATUS_ARCHIVED ||
                  params.status === Badge.constants().STATUS_INACTIVE
              ) {
                if (activeParents > 0) {
                  return next(Errors.AchievementApi.BadgeIsActivated);
                }
              }
              return next();
            }).catch((err)=>{
              return CrudHelper.callbackError(err, next);
            });
          }).catch((err)=>{
            return CrudHelper.callbackError(err, next);
          });
        },
        // Check current badge provider status
        (next)=>{
          return Badge.findOne({ where: { id: params.id } }).then((badge)=>{
            if (!badge) {
              return next(Errors.DatabaseApi.NoRecordFound);
            }
            // Only undeployed and inactive (but not archived)
            // badge providers can be archived/activated
            if (
              params.status === Badge.constants().STATUS_ARCHIVED ||
                params.status === Badge.constants().STATUS_ACTIVE
            ) {
              if (badge.status === Badge.constants().STATUS_ARCHIVED) {
                return next(Errors.AchievementApi.BadgeIsArchived);
              }
            }
            // Only undeployed and active badge providers can be deactivated
            if (params.status === Badge.constants().STATUS_INACTIVE) {
              if (badge.status === Badge.constants().STATUS_INACTIVE) {
                return next(Errors.AchievementApi.BadgeIsDeactivated);
              }
              if (badge.status === Badge.constants().STATUS_ARCHIVED) {
                return next(Errors.AchievementApi.BadgeIsArchived);
              }
            }
            return next();
          }).catch((err)=>{
            return CrudHelper.callbackError(err, next);
          });
        },
        // Update status
        (next)=>{
          return Badge.update({
            status: params.status
          }, {
            where: { id: params.id }
          }).then((count)=>{
            if (count[0] === 0) {
              return next(Errors.DatabaseApi.NoRecordFound);
            }
            return next();
          }).catch((err)=>{
            return CrudHelper.callbackError(err, next);
          });
        }
      ], (err)=>{
        if (err) {
          return CrudHelper.callbackError(err, callback);
        }
        return CrudHelper.callbackSuccess(null, callback);
      });
    } catch (ex) {
      return CrudHelper.callbackError(ex, callback);
    }
  },

  badgeDelete: (params, callback)=>{
    try {
      return async.series([
        // Check current status, should not be activated/archived
        (next)=>{
          try {
            return Badge.findOne({ where: { id: params.id } }).then((badge)=>{
              if (!badge) {
                return next(Errors.DatabaseApi.NoRecordFound);
              }
              // Only inactive badges can be deleted
              if (badge.status === Badge.constants().STATUS_ACTIVE) {
                return next(Errors.AchievementApi.BadgeIsActivated);
              }
              if (badge.status === Badge.constants().STATUS_ARCHIVED) {
                return next(Errors.AchievementApi.BadgeIsArchived);
              }
              return next();
            }).catch((err)=>{
              return CrudHelper.callbackError(err, next);
            });
          } catch (ex) {
            return setImmediate(next, ex);
          }
        },
        // Remove badge
        (next)=>{
          try {
            return Badge.destroy({ where: { id: params.id } }).then((count)=>{
              if (count === 0) {
                return next(Errors.DatabaseApi.NoRecordFound);
              }
              return next();
            }).catch((err)=>{
              return CrudHelper.callbackError(err, next);
            });
          } catch (ex) {
            return setImmediate(next, ex);
          }
        }
      ], (err)=>{
        if (err) {
          return CrudHelper.callbackError(err, callback);
        }
        return CrudHelper.callbackSuccess(null, callback);
      });
    } catch (ex) {
      return CrudHelper.callbackError(ex, callback);
    }
  },

  badgeGet: (params, message, clientSession, callback)=>{
    try {
      const response = {};
      const responseItem = 'badge';
      const mapBySchema = 'badgeModel';
      const attributes = _.keys(Badge.attributes);
      return TenantService.getSessionTenantId(message, clientSession, (err, tenantId)=>{
        if (err) {
          return CrudHelper.callbackError(err, callback);
        }
        const include = CrudHelper.include(Badge, [
          BadgeHasBadge
        ], params, mapBySchema, INSTANCE_MAPPINGS);
        return Badge.findOne({
          where: { id: params.id, tenantId: tenantId },
          attributes: attributes,
          include: include,
          subQuery: false
        }).then((badge)=>{
          if (!badge) {
            return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
          }
          const badgeItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, badge);
          response[responseItem] = badgeItem;
          return CrudHelper.callbackSuccess(response, callback);
        }).catch((er)=>{
          return CrudHelper.callbackError(er, callback);
        });
      });
    } catch (ex) {
      return CrudHelper.callbackError(ex, callback);
    }
  },

  badgeList: (params, message, clientSession, callback)=>{
    try {
      const mapBySchema = 'badgeModel';
      const attributes = _.keys(Badge.attributes);
      const orderBy = CrudHelper.orderBy(params, Badge);
      let searchBy = CrudHelper.searchBy(params, Badge);
      let badgeItems = [];
      let total = 0;
      let sessionTenantId;
      return async.series([
        // add tenantId filter
        (next)=>{
          return TenantService.getSessionTenantId(message, clientSession, (err, tenantId)=>{
            if (err) {
              return next(err);
            }
            sessionTenantId = tenantId;
            return next();
          });
        },
        // Populate total count and badge ids
        // (involve base model and nested models only with defined search criterias)
        (next)=>{
          const include = CrudHelper.includeDefined(Badge, [
            BadgeHasBadge
          ], params, mapBySchema, INSTANCE_MAPPINGS);
          searchBy.tenantId = sessionTenantId;
          return Badge.findAndCountAll({
            limit: params.limit === 0 ? null : params.limit,
            offset: params.limit === 0 ? null : params.offset,
            where: searchBy,
            order: orderBy,
            include: include,
            distinct: true,
            subQuery: false
          }).then((badges)=>{
            total = badges.count;
            if (!_.has(searchBy, 'id')) {
              const badgesIds = [];
              _.forEach(badges.rows, (badge)=>{
                badgesIds.push(badge.id);
              });
              searchBy = {
                id: { $in: badgesIds }
              };
            }
            return next();
          }).catch((err)=>{
            return CrudHelper.callbackError(err, next);
          });
        },
        // Populate model list with all nested model elements
        (next)=>{
          if (total === 0) {
            return setImmediate(next, null);
          }
          const include = CrudHelper.include(Badge, [
            BadgeHasBadge
          ]);
          return Badge.findAll({
            where: searchBy,
            order: orderBy,
            attributes: attributes,
            include: include,
            subQuery: false
          }).then((badges)=>{
            badgeItems = AutoMapper.mapListDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, badges);
            return next();
          }).catch((err)=>{
            return CrudHelper.callbackError(err, next);
          });
        }
      ], (err)=>{
        if (err) {
          return CrudHelper.callbackError(err, callback);
        }
        return CrudHelper.callbackSuccess({
          items: badgeItems,
          limit: params.limit,
          offset: params.offset,
          total: total
        }, callback);
      });
    } catch (ex) {
      return CrudHelper.callbackError(ex, callback);
    }
  },

  badgeImport: (params, message, clientSession, callback)=>{
    try {
      let badgeItems = [];
      return async.series([
        // Get tenant id from session
        (next)=>{
          return TenantService.getSessionTenantId(message, clientSession, (err)=>{
            if (err) {
              return next(err);
            }
            return next();
          });
        },
        // Get badge import data
        (next)=>{
          try {
            return AchievementService.import(params.key, (err, data)=>{
              if (err || !data || data.length === 0) {
                return next(Errors.AchievementApi.BadgesAreNotAvailable);
              }
              badgeItems = data;
              return next();
            });
          } catch (ex) {
            return setImmediate(next, ex);
          }
        },
        // Create badge entries
        (next)=>{
          return Badge.bulkCreate(badgeItems).then((records)=>{
            if (records.length !== badgeItems.length) {
              return next(Errors.QuestionApi.FatalError);
            }
            return next();
          }).catch((err)=>{
            return CrudHelper.callbackError(err, next);
          });
        },
        // Delete badge import file from S3
        (next)=>{
          try {
            return AchievementService.delete(params.key, next);
          } catch (ex) {
            return setImmediate(next, ex);
          }
        }
      ], (err)=>{
        if (err) {
          return CrudHelper.callbackError(err, callback);
        }
        return CrudHelper.callbackSuccess(null, callback);
      });
    } catch (ex) {
      return CrudHelper.callbackError(ex, callback);
    }
  },

  badgeExport: (params, message, clientSession, callback)=>{
    const self = this;
    const badgeItems = [];
    try {
      return async.series([
        // Get badge data
        (next)=>{
          try {
            return Badge.findAll().then((badges)=>{
              if (!badges) {
                return next(Errors.DatabaseApi.NoRecordFound);
              }
              _.forEach(badges, (badge)=>{
                const badgeItem = self._badgeMap(badge);
                badgeItems.push(badgeItem);
              });
              return next();
            }).catch((err)=>{
              return CrudHelper.callbackError(err, next);
            });
          } catch (ex) {
            return setImmediate(next, ex);
          }
        },
        // Export badge data to S3
        (next)=>{
          try {
            return AchievementService.export(params.key, badgeItems, ()=>{ }, next);
          } catch (ex) {
            return setImmediate(next, ex);
          }
        }
      ], (err)=>{
        if (err) {
          return CrudHelper.callbackError(err, callback);
        }
        return CrudHelper.callbackSuccess(null, callback);
      });
    } catch (ex) {
      return CrudHelper.callbackError(ex, callback);
    }
  },

  _badgeMap: (badge)=>{
    // Custom Badge mapper object
    const badgeItem = {};
    const badgePlain = badge.get({ plain: true });
    AutoMapper.mapDefined(badgePlain, badgeItem)
      .forMember('id')
      .forMember('tenantId')
      .forMember('name')
      .forMember('description')
      .forMember('type')
      .forMember('status')
      .forMember('time')
      .forMember('timePeriod')
      .forMember('image', 'imageId')
      .forMember('bonusPointsReward')
      .forMember('creditReward')
      .forMember('paymentMultiplier')
      .forMember('accessRules')
      .forMember('earningRules')
      .forMember('messaging');
    if (_.has(badgePlain, 'isReward')) {
      badgeItem.reward = badgePlain.isReward === 1;
    }
    if (_.has(badgePlain, 'usage')) {
      badgeItem.uniquePerUser = badgePlain.usage === Badge.constants().USAGE_SINGLE;
    }
    if (_.has(badgePlain, 'createDate')) {
      badgeItem.createdOn = DateUtils.isoPublish(badgePlain.createDate);
    }
    const badgePublish = AutoMapper.mapDefinedBySchema('badgePublishModel', INSTANCE_MAPPINGS, badgeItem);
    return badgePublish;
  },

  badgeGetForPublish: (params, message, clientSession, callback)=>{
    const self = this;
    try {
      let badgePublish;
      let sessionTenantId;
      return async.series([
        // Get tenant id from session
        (next)=>{
          return TenantService.getSessionTenantId(message, clientSession, (err, tenantId)=>{
            if (err) {
              return next(err);
            }
            sessionTenantId = tenantId;
            return next();
          });
        },
        // Get badge record by tenant
        (next)=>{
          return Badge.findOne({
            where: { id: params.id, tenantId: sessionTenantId },
            include: { model: BadgeHasBadge, required: false }
          }).then((badge)=>{
            if (!badge) {
              return next(Errors.DatabaseApi.NoRecordFound);
            }
            if (
              params.status === Badge.constants().STATUS_ACTIVE &&
                badge.status === Badge.constants().STATUS_ACTIVE
            ) {
              return next(Errors.AchievementApi.BadgeIsActivated);
            }
            if (
              params.status === Badge.constants().STATUS_INACTIVE &&
                badge.status === Badge.constants().STATUS_INACTIVE
            ) {
              return next(Errors.AchievementApi.BadgeIsDeactivated);
            }
            if (badge.status === Badge.constants().STATUS_ARCHIVED) {
              return next(Errors.AchievementApi.BadgeIsArchived);
            }
            badgePublish = self._badgeMap(badge);
            return next();
          }).catch((err)=>{
            return CrudHelper.callbackError(err, next);
          });
        }
      ], (err)=>{
        if (err) {
          return CrudHelper.callbackError(err, callback);
        }
        logger.info('BadgeApiFactory.badgeGetForPublish', JSON.stringify(badgePublish));
        return CrudHelper.callbackSuccess(badgePublish, callback);
      });
    } catch (ex) {
      return CrudHelper.callbackError(ex, callback);
    }
  },

  badgePublish: (params, message, clientSession, callback)=>{
    if (params.id === undefined) {
      return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
    }
    const self = this;
    try {
      let badge;
      return async.series([
        // Get badge data for publishing
        (next)=>{
          try {
            return self.badgeGetForPublish(Object.assign(params, {
              status: Badge.constants().STATUS_ACTIVE
            }), message, clientSession, (err, badgePublish)=>{
              if (err) {
                return next(err);
              }
              badge = badgePublish;
              badge.id = params.id;
              return next();
            });
          } catch (ex) {
            return setImmediate(next, ex);
          }
        },
        // Publish badge
        (next)=>{
          try {
            return AerospikeBadge.publish(badge, next);
          } catch (ex) {
            return setImmediate(next, ex);
          }
        },
        // Publish badge list
        (next)=>{
          try {
            return AerospikeBadgeList.publish(badge, next);
          } catch (ex) {
            return setImmediate(next, ex);
          }
        },
        // Set badge deployment status
        (next)=>{
          try {
            const badgeParams = {
              id: params.id,
              status: Badge.constants().STATUS_ACTIVE
            };
            return self.badgeSetStatus(badgeParams, next);
          } catch (ex) {
            return setImmediate(next, ex);
          }
        },
        // publish refresh event
        (next)=>{
          try {
            return clientSession.getConnectionService().getEventService().publish('analytics.achievements.refresh', {}, false, next);
          } catch (e) {
            return setImmediate(next, e);
          }
        }

      ], (err)=>{
        if (err) {
          return CrudHelper.callbackError(err, callback);
        }
        return CrudHelper.callbackSuccess(null, callback);
      });
    } catch (ex) {
      return CrudHelper.callbackError(ex, callback);
    }
  },

  badgeUnpublish: (params, message, clientSession, callback)=>{
    if (params.id === undefined) {
      return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
    }
    const self = this;
    try {
      let badge;
      return async.series([
        // Get badge data for publishing
        (next)=>{
          try {
            return self.badgeGetForPublish(Object.assign(params, {
              status: Badge.constants().STATUS_INACTIVE
            }), message, clientSession, (err, badgePublish)=>{
              if (err) {
                return next(err);
              }
              badge = badgePublish;
              badge.id = params.id;
              return next();
            });
          } catch (ex) {
            return setImmediate(next, ex);
          }
        },
        // Unpublish badge
        (next)=>{
          try {
            return AerospikeBadge.unpublish(badge, next);
          } catch (ex) {
            return setImmediate(next, ex);
          }
        },
        // Unpublish badge list
        (next)=>{
          try {
            return AerospikeBadgeList.unpublish(badge, next);
          } catch (ex) {
            return setImmediate(next, ex);
          }
        },
        // Reset badge deployment status
        (next)=>{
          try {
            const badgeParams = {};
            badgeParams.id = params.id;
            badgeParams.status = Badge.constants().STATUS_INACTIVE;
            return self.badgeSetStatus(badgeParams, next);
          } catch (ex) {
            return setImmediate(next, ex);
          }
        },
        // publish refresh event
        (next)=>{
          try {
            return clientSession.getConnectionService().getEventService().publish('analytics.achievements.refresh', {}, false, next);
          } catch (e) {
            return setImmediate(next, e);
          }
        }

      ], (err)=>{
        if (err) {
          return CrudHelper.callbackError(err, callback);
        }
        return CrudHelper.callbackSuccess(null, callback);
      });
    } catch (ex) {
      return CrudHelper.callbackError(ex, callback);
    }
  }
};
