var _ = require('lodash');
var async = require('async');
var GlobalErrors = require('nodejs-errors');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var UploadApiFactory = require('./../factories/uploadApiFactory.js');
var ProtocolMessage = require('nodejs-protocol');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var AutoMapper = AutoMapperInstance.AutoMapper;
var Database = require('nodejs-database').getInstance(Config);
var Media = Database.RdbmsService.Models.Media.Media;
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikeTaskLock = KeyvalueService.Models.AerospikeTaskLock;
var jwt = require('jsonwebtoken');
var uuid = require('uuid');
var nodepath = require('path');
var logger = require('nodejs-logger')();
var StandardErrors = require('nodejs-errors');

var INSTANCE_MAPPINGS = {
    'mediaModel': []
}


var updateProfilePictureQueue = {};
function updateProfilePictureGetOrCreateQueue(userId) {
    if(_.isUndefined(updateProfilePictureQueue[userId])) {
        updateProfilePictureQueue[userId] = async.queue(doUpdateProfilePicture, 1);
    }
    
    return updateProfilePictureQueue[userId];
};

function updateProfilePictureRemoveQueue(userId) {
    var queue = updateProfilePictureQueue[userId];
    if(!_.isUndefined(queue) && queue.length() === 0) {
        //logger.debug('>>>>>removing queue', userId);
        queue.kill();
        delete updateProfilePictureQueue[userId];
    }
};


function generateMediaId(fileName, mayBePng) {
    var rePNG = new RegExp(".+\\.png$");
    try {
        var id = uuid.v4(),
            extension = nodepath.extname(fileName);
        if (extension.length < 3 || Config.mediaConfiguration.imageTypes.indexOf(extension.toLowerCase().replace('.', '')) >= 0) {
            if (rePNG.test(fileName) && mayBePng){      //dont convert png files "app" and "gameType" namespaces
                extension = '.png';
            } else {
                extension = '.jpg';
            }
        }

        return {
            'basename': id,
            'extension': extension,
            'fullname': id + extension
        }
    } catch (e) {
        throw e;
    }
}

function generateMediaIdForProfile(userId) {
    try {
        return {
            'basename': userId,
            'extension': '.jpg',
            'fullname': (userId + '.jpg')
        };
    } catch (e) {
        throw e;
    }
}

module.exports = {
    // POST /addMedia
    addMedia: function (req, res, cb) {
        var params = {};
        var media = null;
        params.mayBePng =  req.body.namespace === Media.constants().NAMESPACE_GAME_TYPE
                        || req.body.namespace === Media.constants().NAMESPACE_APP
                            ? true
                            : false; //dont convert png files "app" and "gameType" namespaces
        try {
            async.series([
                // Check jwt token
                function (next) {
                    if (_.has(req.body, 'token')) {
                        jwt.verify(
                            req.body.token,
                            Config.auth.publicKey,
                            { algorithms: [Config.auth.algorithm] },
                            function (err, decoded) {
                                if (err)
                                  return next('ERR_TOKEN_NOT_VALID');

                                params.creatorResourceId = decoded.userId;
                                params.createDate = _.now();

                                return next(false);
                            }
                        );
                    } else {
                        return next('ERR_TOKEN_NOT_VALID');
                    }
                },
                // Check if there is content_type defined
                function (next) {
                    if (_.has(req.body, 'content_type') && ['image', 'audio', 'video'].indexOf(req.body.content_type) !== -1) {
                        params.contentType = req.body.content_type;
                        return next(false);
                    } else {
                      return next('ERR_VALIDATION_FAILED');
                    }
                },

                // Check if there is namespace defined
                function (next) {
                    var namespaceList = [
                        Media.constants().NAMESPACE_APP,
                        Media.constants().NAMESPACE_LANGUAGE,
                        Media.constants().NAMESPACE_VOUCHER,
                        Media.constants().NAMESPACE_VOUCHER_BIG,
                        Media.constants().NAMESPACE_PROFILE,
                        Media.constants().NAMESPACE_GROUP,
                        Media.constants().NAMESPACE_QUESTION,
                        Media.constants().NAMESPACE_ANSWER,
                        Media.constants().NAMESPACE_GAME,
                        Media.constants().NAMESPACE_GAME_TYPE,
                        Media.constants().NAMESPACE_AD,
                        Media.constants().NAMESPACE_TOMBOLA,
                        Media.constants().NAMESPACE_TOMBOLA_BUNDLES,
                        Media.constants().NAMESPACE_PROMO,
                        Media.constants().NAMESPACE_POOLS,
                        Media.constants().NAMESPACE_TENANT_LOGO,
                        Media.constants().NAMESPACE_ACHIEVEMENT,
                        Media.constants().NAMESPACE_BADGE,
                        Media.constants().NAMESPACE_ICON,
                        Media.constants().NAMESPACE_SHOP_ARTICLE,
                        Media.constants().NAMESPACE_INTRO
                    ];

                    if (_.has(req.body, 'namespace') && namespaceList.indexOf(req.body.namespace) !== -1) {
                        params.namespace = req.body.namespace;
                        return next(false);
                    } else {
                      return next('ERR_VALIDATION_FAILED');
                    }
                },
                // Check if there is metadata object
                function (next) {

                    if (!_.has(req.body, 'meta_data')) {
                        req.body.meta_data='{}';
                    }
                        try {
                          params.metaData = JSON.parse(req.body.meta_data);
                        } catch (e) {
                          return next('ERR_VALIDATION_FAILED');
                        }
                        return next(false)
                },
                // Check if there is workorder id setup
                function (next) {
                    if (_.has(req.body, 'workorder_id')) {
                        params.workorderId = req.body.workorder_id;
                    }
                    if (_.has(req.body, 'parent_id')) {
                        params.parentId = req.body.parent_id;
                    }
                    if (_.has(req.body, 'type')) {
                        params.type = req.body.type;
                    }
                    if (_.has(req.body, 'title')) {
                        params.title = req.body.title;
                    }
                    if (_.has(req.body, 'description')) {
                        params.description = req.body.description;
                    }
                    if (_.has(req.body, 'source')) {
                        params.source = req.body.source;
                    }
                    if (_.has(req.body, 'copyright')) {
                        params.copyright = req.body.copyright;
                    }
                    if (_.has(req.body, 'licence')) {
                        params.licence = req.body.licence;
                    }
                    if (_.has(req.body, 'usage_information')) {
                        params.usageInformation = req.body.usage_information;
                    }

                    return next(false)
                },
                // Check if there is a media_file
                function (next) {
                    if (_.has(req, 'file')) {
                        params._fileInfo = req.file;
                        logger.debug('httpMediaApi/addMedia - file info being uploaded',
                          JSON.stringify(req.file));
                        return next(false);
                    }
                    return next('ERR_VALIDATION_FAILED');
                },
                // generate media id value
                function (next) {
                  try {
                    params._generatedFileInfo = generateMediaId(params._fileInfo.originalname, params.mayBePng);
                    params.id = params._generatedFileInfo.fullname;
                    params.originalFilename = params._fileInfo.originalname;

                    return next(false);
                  } catch (e) {
                    return next(Errors.MediaApi.FatalError);
                  }
                },
                function (next) {
                  try {
                    UploadApiFactory.convertOriginalFileToJpg(params, next);
                  } catch (e) {
                    logger.error("httpMediaApi.addMedia convert file to jpg",
                      JSON.stringify(params), JSON.stringify(Config.mediaConfiguration), e);
                    return next(Errors.MediaApi.FatalError);
                  }
                },
                // Create thumbnail information of said files
                function (next) {
                  try {
                    UploadApiFactory.thumbnailInfoCompile(params, Config.mediaConfiguration);
                    return next(false);
                  } catch (e) {
                    logger.error("httpMediaApi.addMedia thumbnail info compile",
                      JSON.stringify(params), JSON.stringify(Config.mediaConfiguration), e);
                    return next(Errors.MediaApi.FatalError);
                  }
                },
                // Create the thumbnail files on local disk
                function (next) {
                  try {
                    UploadApiFactory.thumbnailsGenerate(params, next);
                  } catch (e) {
                    logger.error("httpMediaApi.addMedia thumbnail generation failed",
                      JSON.stringify(params), e);
                    return next(Errors.MediaApi.FatalError);
                  }
                },
                // Upload to storage
                function (next) {
                  try {
                    UploadApiFactory.uploadToStorage(params, next);
                  } catch (e) {
                    logger.error("httpMediaApi.addMedia upload to storage failed",
                      JSON.stringify(params), e);
                    return next(Errors.MediaApi.FatalError);
                  }
                },
                // save to sql database
                function (next) {
                    var mediaMap = AutoMapper.mapDefinedBySchema('mediaModel', INSTANCE_MAPPINGS, params, true);
                    Media.build(mediaMap)
                        .save()
                        .then(function (newMedia) {
                            media = newMedia;
                            return next(false);
                        }).catch(function (e) {
                        logger.error("httpMediaApi.addMedia could not save media info into mysql database", e);
                        return next(Errors.MediaApi.FatalError);
                    });
                },
                // Upload metadata as well
                function (next) {
                  try {
                    UploadApiFactory.uploadMetadataForMedia(
                      media,
                      next);
                  } catch (e) {
                    logger.error("httpMediaApi.addMedia could not upload metadata for media ", e);
                    return next(Errors.MediaApi.FatalError);
                  }
                },
                // cleanup
                function (next) {
                  try {
                    UploadApiFactory.cleanupTemporaryFiles(params, next);
                  } catch (e) {
                    return next(Errors.MediaApi.FatalError);
                  }
                },
            ], function (err) {
                if (err) {
                    logger.error('httpMediaApi.addMedia global failure', err);
                    return _apiError('media/addMedia', err, res, cb);
                }
                var message = _message('media/addMediaResponse');
                message.setContent({ media: media });
                return _processData(err, message, res, cb);
            });
        } catch (e) {
            _apiError('media/addMedia', e, res, next);
        }
    },
    
    updateProfilePicture: function (req, res, cb) {
        try {
            var params = {
                'namespace': Media.constants().NAMESPACE_PROFILE,
                'id': ''
            };

            async.series([
                // Check jwt token
                function (next) {
                    if (_.has(req.body, 'token')) {
                        jwt.verify(
                            req.body.token,
                            Config.auth.publicKey,
                            { algorithms: [Config.auth.algorithm] },
                            function (err, decoded) {
                                if (err) {
                                    return next('ERR_TOKEN_NOT_VALID');
                                }

                                // Profile picture is hardcoded to ID.jpg
                                params.userId = decoded.userId;
                                return next(false);
                            }
                        );
                    } else {
                        return next('ERR_TOKEN_NOT_VALID');
                    }
                },
              
                function (next) {
                    try {
                        updateProfilePictureGetOrCreateQueue(params.userId).push({
                            params: params,
                            req: req
                        }, next);
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                },
                
                function (next) {
                    try {
                        updateProfilePictureRemoveQueue(params.userId);
                        return setImmediate(next);
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                }


            ], function (err) {
                if (err) {
                    logger.error('httpMediaApi.updateProfilePicture error:', err);
                    return _apiError('media/updateProfilePicture', err, res, cb);
                }
                var message = _message('media/updateProfilePictureResponse');
                message.setContent( { profile: { id: params.id, namespace: params.namespace } } );
                return _processData(err, message, res, cb);

            });
        } catch (e) {
            _apiError('media/updateProfilePicture', e, res, cb);
        }
    },
    

    // POST /updateGroupPicture
    updateGroupPicture: function (req, res, cb) {
        try {
            var params = {
                'namespace': Media.constants().NAMESPACE_GROUP,
                'id': ''
            };

            var group = null;
            var cleanupImageId = false;

            async.series([
            // Check jwt token
            function (next) {
                console.log(
                    'httpMediaApi.updateGroupPicture>>>',
                    'req>>', req,
                    'req.body>>', req.body,
                    'req.file>>', req.file
                );
                if (_.has(req.body, 'token')) {
                    jwt.verify(
                        req.body.token,
                        Config.auth.publicKey,
                        { algorithms: [Config.auth.algorithm] },
                        function (err, decoded) {
                            try {
                                if (err) {
                                    logger.error('httpMediaApi.updateGroupPicture invalid token', err, req.body.token, Config.auth.algorithm, Config.auth.publicKey);
                                    return setImmediate(next, GlobalErrors['ERR_TOKEN_NOT_VALID']);
                                }

                                // Profile picture is hardcoded to ID.jpg
                                params.userId = decoded.userId;
                                if (_.has(req.body, 'groupId') && req.body.groupId.length > 1) {
                                    params.groupId = req.body.groupId;
                                } else {
                                    logger.error('httpMediaApi.updateGroupPicture groupId not defined', req.body);
                                    return setImmediate(next, GlobalErrors['ERR_VALIDATION_FAILED']);
                                }

                                if (_.has(req.body, 'tenantId') && req.body.tenantId.length >= 1) {
                                    params.tenantId = req.body.tenantId;
                                } else {
                                    logger.error('httpMediaApi.updateGroupPicture tenantId not defined', req.body);
                                    return setImmediate(next, GlobalErrors['ERR_VALIDATION_FAILED']);
                                }

                                if (_.has(req.body, 'action') && req.body.action == 'delete') {
                                    return setImmediate(next);
                                }

                                console.log(_.has(req.body, 'action'), req.body.action == 'update', _.has(req, 'file'));
                                console.log(_.has(req.body, 'action') && req.body.action == 'update' && _.has(req, 'file'));

                                if (_.has(req.body, 'action') && req.body.action == 'update' && _.has(req, 'file')) {
                                    params._fileInfo = req.file;
                                    console.log('params._fileInfo', params._fileInfo);
                                    params._generatedFileInfo = generateMediaId(params._fileInfo.originalname, false);
                                    console.log('params._generatedFileInfo', params._generatedFileInfo);
                                    params.id = params._generatedFileInfo.fullname;
                                    console.log('params.id', params.id);
                                    console.log('params', params);
                                    UploadApiFactory.thumbnailInfoCompile(params, Config.mediaConfiguration);
                                    console.log('<<<<finish');

                                    return setImmediate(next, null);
                                }

                                console.log('error', 'httpMediaApi.updateGroupPicture invalid action');
                                logger.error('httpMediaApi.updateGroupPicture invalid action', req.body, params);
                                return setImmediate(next, GlobalErrors['ERR_VALIDATION_FAILED']);
                            } catch (ex) {
                                console.log(1, 'catch');
                                logger.error('httpMediaApi.updateGroupPicture exception validating input params', ex, req.body, params);
                                return setImmediate(next, GlobalErrors['ERR_FATAL_ERROR']);
                            }
                        }
                    );
                } else {
                    logger.error('httpMediaApi.updateGroupPicture token not set', req.body);
                    return setImmediate(next, GlobalErrors['ERR_TOKEN_NOT_VALID']);
                }
            },
            // Retrieve group information from the friend service
            function (next) {
                console.log(2, 'Retrieve group information from the friend service>>>>>>>');
                try {
                    return req.app.locals.clientInstances.friendServiceClient.groupGet(params.groupId, params.tenantId, params.userId, function (err, message) {
                        console.log('groupGet', message);
                        if (err) {
                            logger.error("httpMediaApi.updateGroupPicture friendService.groupGet failed", err, "parameters", params);
                            return setImmediate(next, GlobalErrors['ERR_FATAL_ERROR']);
                        }
                        var content = message.getContent();

                        console.log('_.has(content, "group")', _.has(content, 'group'));
                        if (_.has(content, 'group')) {
                            group = content.group;

                            console.log('group',group);
                            if (!_.has(group, 'ownerUserId') || group.ownerUserId !== params.userId) {
                                logger.error("httpMediaApi.updateGroupPicture user is not the owner of the group");
                                return setImmediate(next, GlobalErrors['ERR_INSUFFICIENT_RIGHTS']);
                            }
                            console.log(2, 'finish');
                            return setImmediate(next);
                        } else {
                            logger.error("httpMediaApi.updateGroupPicture friendService.groupGet content has no group returned");
                            return setImmediate(next, GlobalErrors['ERR_FATAL_ERROR']);
                        }
                    });
                } catch (ex) {
                    console.log(2, 'catch');
                    logger.error("httpMediaApi.updateGroupPicture exception retrieving group from friend service", ex);
                    return setImmediate(next, GlobalErrors['ERR_FATAL_ERROR']);      
                }
            },
            function (next) {
                try {
                    // For both UPDATE and DELETE calls we need to remove the old api call
                    console.log(3, 'group.image',group.image);
                    if (group.image && group.image.length > 5) {
                        cleanupImageId = group.image;
                    }

                    console.log('params',params);
                    if (req.body.action == 'update') {
                        group.image = params.namespace + '/' + params.id;
                        return UploadApiFactory.convertProfilePictureToJpgAndUpload(params, next);
                    } else if (req.body.action == 'delete') {
                        group.image = '';
                        return setImmediate(next, false);
                    }
                } catch (e) {
                    console.log(3, 'catch');
                    return next(GlobalErrors['ERR_FATAL_ERROR']);
                }
            },
            // Update Group information
            function (next) {
                console.log(4, 'Update Group information');
                try {
                    return req.app.locals.clientInstances.friendServiceClient.groupUpdate(params.groupId, params.tenantId, params.userId, group.name, group.type, group.image, function (err, message) {
                        console.log(4, 'before error', err)
                        if (err) {
                            logger.error("httpMediaApi.updateGroupPicture friendService.groupUpdate failed", err, "parameters", params);
                            return setImmediate(next, GlobalErrors['ERR_FATAL_ERROR']);
                        }
                        console.log(4, 'after error');
                        return setImmediate(next);
                    });
                } catch (ex) {
                    console.log(4, 'catch');
                    logger.error("httpMediaApi.updateGroupPicture exception calling friendService.groupUpdate", ex, params, group);
                    return setImmediate(next, GlobalErrors['ERR_FATAL_ERROR']);
                }
            },
            function (next) {
                try {
                    console.log(5, 'last function');
                    // If there is nothing to delete skip this step
                    console.log('cleanupImageId', cleanupImageId);
                    if (false === cleanupImageId) {
                        return setImmediate(next, null);
                    }

                    var cleanupImage = {
                        id: cleanupImageId,
                        namespace: params.namespace
                    };
                    console.log('cleanupImage',cleanupImage);
                    UploadApiFactory.deleteFromStorage(cleanupImage, function (err, success) {
                        if (!err) {
                            params.id = '';
                        }
                        console.log(5, 'after deleteFromStorage');
                        return setImmediate(next, err);
                    });
                } catch (ex) {
                    logger.error('httpMediaApi.updateGroupPicture delete from storage exception', ex);
                    return setImmediate(next, ex);
                }
            }], function (err) {
                console.log(6, err)
                if (err) {
                    return _apiError('media/updateGroupPicture', err, res, cb);
                }
                var message = _message('media/updateGroupPictureResponse');
                message.setContent( { group: { image: group.image } } );
                console.log(6, 'message', message);
                return _processData(err, message, res, cb);
            });
        } catch (e) {
            console.log(6, 'catch');
            _apiError('media/updateGroupPicture', e, res, next);
        }
    }
}

function doUpdateProfilePicture(task, cb) {
    async.timeout(_doUpdateProfilePicture, Config.timeoutUpdateProfilePicture)(task, function (err) {
        AerospikeTaskLock.remove(_updateProfilePictureLockTaskId(task.params.userId), function (errUnlock) {
            if(errUnlock) {
                logger.error('httpMediaApi._doUpdateProfilePicture() remove lock task error:', task.params.userId, errUnlock);
            }

            return cb(err);
        });
    });
};

function _updateProfilePictureLockTaskId(userId) {
    return 'updateProfilePictureTask-' + userId;
}

function _doUpdateProfilePicture(task, cb) {
    try {
        var req = task.req;
        var params = task.params;
        
        async.series([
            // try to lock the task. if task is already locked(by other instance for example), try multiple times, till task is unlocked by the other instance
            function (next) {
                try {
                    var retryCnt = 0;
                    async.retry({
                        times: 50,
                        interval: 1000
                    }, function (cbTry) {
                        AerospikeTaskLock.lock(_updateProfilePictureLockTaskId(params.userId), function (err, lock) {
                            try {
                                retryCnt++;
                                if(retryCnt > 1) {
                                    logger.debug('httpMediaApi._doUpdateProfilePicture() task loked by other instance');
                                }
                                if (err) {
                                    return cbTry(err);
                                }

                                // locked by someone else
                                if (lock !== true) {
                                    return cbTry(new Error('UPDATE_PROFILE_PICTURE_ALREADY_LOCKED'));
                                }
                                
                                return cbTry();
                                
                            } catch (e) {
                                return cbTry(e);
                            }
                        });
                    }, function (err) {
                        if(err) {
                            logger.error('httpMediaApi._doUpdateProfilePicture() failed to lock the task');
                            return next(err);
                        }
                        
                        return next();
                    });
                } catch (e) {
                    return setImmediate(next, e);
                }
            },

            function (next) {
                try {
                    params.id = params.userId + '.jpg';
                    params._generatedFileInfo = generateMediaIdForProfile(params.userId);
                    UploadApiFactory.thumbnailInfoCompile(params, Config.mediaConfiguration);
                    return setImmediate(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },

            function (next) {
                try {
                    if (_.has(req.body, 'action')) {
                        if (req.body.action === 'update' && _.has(req, 'file')) {
                            params._fileInfo = req.file;
                            UploadApiFactory.convertProfilePictureToJpgAndUpload(params, next);
                        } else if (req.body.action === 'delete') {
                            UploadApiFactory.deleteFromStorage(params, function (err, success) {
                                if (!err) {
                                    params.id = '';
                                }
                                return next(err);
                            });
                        } else {
                            return next('ERR_VALIDATION_FAILED');
                        }
                    } else {
                        return next('ERR_VALIDATION_FAILED');
                    }
                } catch (e) {
                    return next('ERR_VALIDATION_FAILED');
                }
            }
        ], function (err) {
            return cb(err);
        });
    } catch (e) {
        return setImmediate(cb, e);
    }
}



function _message(api) {
    var pm = new ProtocolMessage();
    pm.setMessage(api);
    return pm;
}

function _processData(err, data, res, next) {
    console.log('_processData');
    console.log('err', err);
    console.log('data', data);
    console.log('res', res);
    console.log('next', next);
    if (err) {
        var message = err.getMessageContainer();
        res.status(401).send(message);
        res.end();
        if (next) {
            next(true);
        }
    } else {
        var message = data.getMessageContainer();
        res.send(message);
        res.end();
        if (next) {
            next(false);
        }
    }
}

function _apiError(api, err, res, next) {
    var error = new ProtocolMessage();
    error.setMessage(api + 'Response');
    if (err && _.isObject(err) && _.has(err, 'stack')) {
        error.setError(Errors.MediaApi.FatalError);
    } else {
        error.setError(err);
    }
    var message = error.getMessageContainer();
    res.status(401).send(message);
    res.end();
}
