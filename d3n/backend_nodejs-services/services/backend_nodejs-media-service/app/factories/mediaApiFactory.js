var _ = require('lodash');
var async = require('async');
var uuid = require('uuid');
var fs = require('fs');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var AutoMapper = AutoMapperInstance.AutoMapper;
var CrudHelper = AutoMapperInstance.CrudHelper;
var UploadApiFactory = require('./uploadApiFactory.js');
var TenantService = require('../services/tenantService.js');
var Database = require('nodejs-database').getInstance(Config);
var Media = Database.RdbmsService.Models.Media.Media;
var MediaHasTag = Database.RdbmsService.Models.Media.MediaHasTag;
var Tag = Database.RdbmsService.Models.Question.Tag;
var Pool = Database.RdbmsService.Models.Question.Pool;
var PoolHasMedia = Database.RdbmsService.Models.Question.PoolHasMedia;
var PoolHasTenant = Database.RdbmsService.Models.Question.PoolHasTenant;
var logger = require('nodejs-logger')();

var INSTANCE_MAPPINGS = {
    'mediaModel': [
        {
            destination: '$root',
            model: Media,
            fullTextQuery: 'LOWER(`mediaHasTags`.`tagTag`) LIKE "%fulltext_word%" OR LOWER(`media`.`title`) LIKE "%fulltext_word%" OR LOWER(`media`.`description`) LIKE "%fulltext_word%"',
            fullTextInclude: [MediaHasTag],
            allInfoAttributes: ['title', 'description', 'source', 'copyright', 'licence', 'usageInformation']
        },
        {
            destination: 'tags',
            model: MediaHasTag,
            attributes: [
                { 'tag': MediaHasTag.tableAttributes.tagTag }
            ],
            limit: 0
        },
        {
            destination: 'poolsIds',
            model: PoolHasMedia,
            attribute: PoolHasMedia.tableAttributes.poolId,
            limit: 0
        },
        {
            destination: 'submediasIds',
            model: Media,
            alias: 'subMedias',
            attribute: Media.tableAttributes.id,
            limit: 0
        },
    ],

    'mediaUpdateModel': [
        {
            destination: '$root',
            model: Media
        },
        {
            destination: 'tags',
            model: MediaHasTag,
            attributes: [
                { 'tag': MediaHasTag.tableAttributes.tagTag }
            ]
        }
    ]
}

module.exports = {

    /**
     * Create/update media
     * @param {{}} params
     * @param {ClientSession} clientSession
     * @param {function} callback
     * @returns {*}
     */
    mediaUpdate: function (params, message, clientSession, callback) {
        var self = this;
        var updatedMedia;

        try {
            var sessionTenantId;
            var mediaItem = AutoMapper.mapDefinedBySchema('mediaUpdateModel', INSTANCE_MAPPINGS, params, true);
            var mediaItemAssociates = undefined;

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
                // Update main entity
                function (next) {
                    Media.update(mediaItem, { where: { id: mediaItem.id } }).then(function (count) {
                        if (count[0] === 0) {
                            return next('ERR_DATABASE_NO_RECORD_ID');
                        }
                        return next();
                    }).catch(function (err) {
                        logger.error('mediaApiFactory.mediaUpdate update error', err);
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Update main media entry
                function (next) {
                    try {
                        mediaItemAssociates = AutoMapper.mapAssociatesDefinedBySchema(
                            'mediaUpdateModel', INSTANCE_MAPPINGS, params, {
                                field: 'mediaId',
                                value: mediaItem.id,
                            }
                        );
                        return next();
                    } catch (ex) {
                        return next(ex);
                    }
                },
                // Delete child entities
                function (next) {
                    async.mapSeries(mediaItemAssociates, function (itemAssociate, remove) {
                        itemAssociate.model.destroy({ where: { mediaId: mediaItem.id } }).then(function (count) {
                            return remove();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, remove);
                        });
                    }, function (err) {
                        if (err) {
                            return next(err);
                        }
                        return next();
                    });
                },
                // Optional bulk upsert tags if passed: create tags which are not exists
                function (next) {
                    if (!_.has(params, 'tags')) {
                        return next();
                    }
                    var tagRecords = [];
                    _.forEach(params.tags, function (tag) {
                        tagRecords.push({ tag: tag, tenantId: sessionTenantId });
                    });
                    Tag.bulkCreate(tagRecords, { updateOnDuplicate: [Tag.tableAttributes.tag.field, Tag.tableAttributes.tenantId.field] }).then(function (records) {
                        if (records.length !== tagRecords.length) {
                            return next('ERR_FATAL_ERROR');
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Bulk insert populated values for every updateable associated entity
                function (next) {
                    if (_.isUndefined(mediaItemAssociates)) {
                        return next();
                    }
                    async.mapSeries(mediaItemAssociates, function (itemAssociate, create) {
                        if (itemAssociate.values.length === 0) {
                            return create();
                        }
                        itemAssociate.model.bulkCreate(itemAssociate.values).then(function (records) {
                            if (records.length !== itemAssociate.values.length) {
                                return create(Errors.MediaApi.FatalError);
                            }
                            return create();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, create);
                        });
                    }, function (err) {
                        if (err) {
                            return next(err);
                        }
                        return next();
                    });
                },
                function (next) {
                    try {
                        self.mediaGet(params, message, clientSession, function(err, data) {
                      if (err) {
                        return CrudHelper.callbackError(err, next);
                      }

                      updatedMedia = data;
                      next(false);
                    });
                  } catch (e) {
                    return CrudHelper.callbackError(e, next);
                  }
                },
                function (next) {
                  if (_.has(params, 'metaData')) {
                      try {
                        UploadApiFactory.uploadMetadataForMedia(
                          updatedMedia.media,
                          next);
                      } catch (e) {
                        return setImmediate(next, 'ERR_VALIDATION_FAILED');
                      }
                  } else {
                    return next(false);
                  }
                },
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess(updatedMedia, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * Get media
     * @param {{}} params
     * @param {Object} message
     * @param {ClientSession} clientSession
     * @param {function} callback
     * @returns {*}
     */
    mediaGet: function (params, message, clientSession, callback) {
        try {
            var sessionTenantId;
            var mediaItem;
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
                // Get media, filter tags by tenant
                function (next) {
                    try {
                        var include = CrudHelper.include(Media, [
                            Media,
                            MediaHasTag,
                            PoolHasMedia
                        ]);
                        include = CrudHelper.includeAlias(include, Media, 'subMedias');
                        include = CrudHelper.includeNestedWhere(include, PoolHasMedia, [Pool, PoolHasTenant], { tenantId: sessionTenantId }, true);
                        include = CrudHelper.includeNestedWhere(include, MediaHasTag, Tag, { tenantId: sessionTenantId }, false);
                        return Media.findOne({
                            where: { id: params.id },
                            include: include
                        }).then(function (media) {
                            if (!media) {
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            TenantService.cleanTags(media.mediaHasTags);
                            mediaItem = AutoMapper.mapDefinedBySchema('mediaModel', INSTANCE_MAPPINGS, media);
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        return CrudHelper.callbackError(ex, next);
                    }
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess({ media: mediaItem }, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(Errors.MediaApi.FatalError, callback);
        }
    },

    /**
     * List media
     * @param {{}} params
     * @param {Object} message
     * @param {ClientSession} clientSession
     * @param {function} callback
     * @returns {*}
     */
    mediaList: function (params, message, clientSession, callback) {
        try {
            var mapBySchema = 'mediaModel';
            var attributes = _.keys(Media.attributes);
            var orderBy = CrudHelper.orderBy(params, Media);
            var searchBy = CrudHelper.searchBy(params, Media, mapBySchema, INSTANCE_MAPPINGS);
            if (_.has(params, 'searchBy') && _.has(params.searchBy, 'fulltext')) {
                searchBy = CrudHelper.searchByFullText(params, Media, mapBySchema, INSTANCE_MAPPINGS);
            }
            var include = CrudHelper.includeDefined(Media, [
                MediaHasTag,
                PoolHasMedia,
                Media
            ], params, mapBySchema, INSTANCE_MAPPINGS);
            include = CrudHelper.includeAlias(include, Media, 'subMedias');
            var attributes = CrudHelper.distinctAttributes(Media.tableAttributes.id);
            var mediaItems = [];
            var total = 0;
            var sessionTenantId;
            async.series([
                // Get tenant id from session
                function (next) {
                    return TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if (err) {
                            return next(err);
                        }
                        sessionTenantId = tenantId;
                        include = CrudHelper.includeNestedWhere(include, PoolHasMedia, [Pool, PoolHasTenant], { tenantId: sessionTenantId }, true);
                        include = CrudHelper.includeNestedWhere(include, MediaHasTag, Tag, { tenantId: sessionTenantId }, false);
                        return next();
                    });
                },
                // Populate total count
                function (next) {
                    return Media.count({
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
                // Populate total count and media ids (involve base model and nested models only with defined search criterias)
                function (next) {
                    return Media.findAll({
                        limit: params.limit === 0 ? null : params.limit,
                        offset: params.limit === 0 ? null : params.offset,
                        attributes: attributes,
                        where: searchBy,
                        include: include,
                        order: orderBy,
                        subQuery: false,
                        raw: true
                    }).then(function (medias) {
                        if (!_.has(searchBy, 'id')) {
                            var mediasIds = [];
                            _.forEach(medias, function (media) {
                                mediasIds.push(media.id);
                            });
                            searchBy = {
                                id: { '$in': mediasIds }
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
                    return Media.findAll({
                        where: searchBy,
                        order: orderBy,
                        subQuery: false
                    }).then(function (medias) {
                        mediaItems = medias;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Populate nested items for each media item separately
                function (next) {
                    if (total === 0) {
                        return setImmediate(next, null);
                    }
                    return async.mapSeries(mediaItems, function (mediaItem, nextMedia) {
                        return async.series([
                            // Populate paged tags by tenant
                            function (nextItem) {
                                var include = CrudHelper.include(MediaHasTag, [Tag]);
                                include = CrudHelper.includeWhere(include, Tag, { tenantId: sessionTenantId }, true);

                                var where = { mediaId: mediaItem.id };
                                if (_.has(params, 'searchBy') && _.has(params.searchBy, 'tags')) {
                                    where.tagTag = { '$in': params.searchBy.tags };
                                }

                                return MediaHasTag.findAndCountAll({
                                    where: where,
                                    include: include,
                                    limit: Config.rdbms.limit,
                                    offset: 0
                                }).then(function (mediaTags) {
                                    mediaItem.mediaHasTags = mediaTags;
                                    return nextItem();
                                }).catch(function (err) {
                                    return CrudHelper.callbackError(err, nextItem);
                                });
                            },
                            // Populate paged pool ids
                            function (nextItem) {
                                try {
                                    include = CrudHelper.includeNestedWhere([], Pool, [PoolHasTenant], { tenantId: sessionTenantId }, true);
                                    var where = { mediaId: mediaItem.id };

                                    if (_.has(params, 'searchBy') && _.has(params.searchBy, 'poolsIds')) {
                                        where.poolId = { '$in': params.searchBy.poolsIds }
                                    }
                                    return PoolHasMedia.findAndCountAll({
                                        where: where,
                                        include: include,
                                        limit: Config.rdbms.limit,
                                        offset: 0
                                    }).then(function (poolsIds) {
                                        mediaItem.poolHasMedia = poolsIds;
                                        return nextItem();
                                    }).catch(function (err) {
                                        logger.error("mediaApiFactory.mediaList - error populating poolIds");
                                        return CrudHelper.callbackError(err, nextItem);
                                    });
                                } catch (ex) {
                                    logger.error("mediaApiFactory.mediaList - exception populating poolIds", ex);
                                    return setImmediate(next, ex);
                                }
                            },
                            // Populate submedias with entries
                            function (nextItem) {
                                var where = { parentId: mediaItem.id };
                                if (_.has(params, 'searchBy') && _.has(params.searchBy, 'submediasIds')) {
                                    where.id = { '$in': params.searchBy.submediasIds }
                                }
                                return Media.findAndCountAll({
                                    where: where,
                                    limit: Config.rdbms.limit,
                                    offset: 0
                                }).then(function (subMedias) {
                                    mediaItem.subMedias = subMedias;
                                    return nextItem();
                                }).catch(function (err) {
                                    return CrudHelper.callbackError(err, nextItem);
                                });
                            },
                        ], nextMedia);
                    }, next);
                },

            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                var instanceMappings = AutoMapper.limitedMappings(INSTANCE_MAPPINGS);
                var mediaItemsMapped = AutoMapper.mapListDefinedBySchema(mapBySchema, instanceMappings, mediaItems);
                return CrudHelper.callbackSuccess({
                    items: mediaItemsMapped,
                    limit: params.limit,
                    offset: params.offset,
                    total: total,
                }, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    mediaDelete: function (params, callback) {
        var self = this;
        var oldMedia = {};
        var where = {
            'id': params.id
        };
        try {
            // List of images to be deleted
            async.series([
                // Delete nested entries
                function (next) {
                    async.mapSeries([
                        MediaHasTag,
                        PoolHasMedia
                    ], function (model, serie) {
                        try {
                            model.destroy({ where: { mediaId: params.id } }).then(function (count) {
                                return serie();
                            }).catch(function (err) {
                                return CrudHelper.callbackError(err, serie);
                            });
                        } catch (ex) {
                            return serie(Errors.MediaApi.FatalError);
                        }
                    }, function (err) {
                        return next(err);
                    });
                },
                // Read media entry, will need it for namespace
                function (next) {
                    Media.findOne({
                        where: where
                    }).then(function (media) {
                        if (!media) {
                            return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, next);
                        }
                        oldMedia = AutoMapper.mapDefinedBySchema('mediaModel', INSTANCE_MAPPINGS, media);
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Delete main media entry
                function (next) {
                    Media.destroy({ where: where }).then(function (count) {
                        if (count === 0) {
                            return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, next);
                        }
                        return next(false);
                    }).catch(function (err) {
                        if (_.has(err, 'name') && err.name === 'SequelizeForeignKeyConstraintError') {
                            return CrudHelper.callbackError(Errors.DatabaseApi.ExistingDependencies, next);
                        }
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Delete media from S3
                function (next) {
                    try {
                        return UploadApiFactory.deleteFromStorage(oldMedia, next);
                    } catch (e) {
                        logger.warn("Could not cleanup data from s3 storage for " + oldMedia.id);
                        return CrudHelper.callbackError(Errors.MediaApi.FatalError, next);
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

    /**
     * Add or remove media from the pool
     * @param {{}} params
     * @param {ClientSession} clientSession
     * @param {*} callback
     * @returns {*}
     */
    mediaAddDeletePool: function (params, clientSession, callback) {
        if (!params.hasOwnProperty('id')) {
            return CrudHelper.callbackError('ERR_VALIDATION_FAILED', callback);
        }
        try {
            async.series([
                // Create/update main entity
                function (next) {
                    if (params.hasOwnProperty('deletePools')) {
                        async.mapSeries(params.deletePools, function (poolId, remove) {
                            try {
                                var poolMediaQuery = { mediaId: params.id, poolId: poolId };
                                PoolHasMedia.destroy({ where: poolMediaQuery }).then(function (count) {
                                    return remove();
                                }).catch(function (err) {
                                    return CrudHelper.callbackError(err, remove);
                                });
                            } catch (ex) {
                                return remove(ex);
                            }
                        }, function (err) {
                            if (err != null) {
                                next(err);
                            }
                            next();
                        });
                    } else {
                        next();
                    }
                },
                function (next) {
                    if (params.hasOwnProperty('addPools')) {
                        async.mapSeries(params.addPools, function (poolId, addCb) {
                            var poolMedia = { mediaId: params.id, poolId: poolId };
                            PoolHasMedia.create(poolMedia).then(function (created) {
                                return addCb();
                            }).catch(function (err) {
                                return CrudHelper.callbackError(err, addCb);
                            });
                        }, function (err) {
                            if (err != null) {
                                return next(err);
                            }
                            next();
                        });
                    } else {
                        next();
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

    /**
     * Add or remove media from the pool
     * @param {{}} params
     * @param {ClientSession} clientSession
     * @param {*} callback
     * @returns {*}
     */
    mediaUpdateProfilePicture: function (params, clientSession, callback) {
        if (!params.hasOwnProperty('userId') || !params.hasOwnProperty('action') || (params.action === 'update' && !params.hasOwnProperty('data'))) {
            return CrudHelper.callbackError('ERR_VALIDATION_FAILED', callback);
        }
        try {
            return async.series([
                function (next) {
                    // Profile picture is hardcoded to ID.jpg
                    params.id = params.userId + '.jpg';
                    params.namespace = Media.constants().NAMESPACE_PROFILE;
                    params._generatedFileInfo = {
                        'basename': params.userId,
                        'extension': '.jpg',
                        'fullname': params.id
                    };
                    UploadApiFactory.thumbnailInfoCompile(params, Config.mediaConfiguration);
                    return next();
                },
                function (next) {
                    if (params.action == 'update') {
                        params._fileInfo = {
                            path: params.data
                        };
                        return UploadApiFactory.convertProfilePictureToJpgAndUpload(params, next);
                    } else if (params.action == 'delete') {
                        return UploadApiFactory.deleteFromStorage(params, next);
                    }
                    return next();
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
    }
};
