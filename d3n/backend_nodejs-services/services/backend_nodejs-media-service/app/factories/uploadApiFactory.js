var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var AutoMapper = AutoMapperInstance.AutoMapper;
var CrudHelper = AutoMapperInstance.CrudHelper;
var gm = require('gm').subClass({imageMagick: true});
var fs = require('fs');
var nodepath = require('path');
var Database = require('nodejs-database').getInstance(Config);
var logger = require('nodejs-logger')();
var StandardErrors = require('nodejs-errors');

// Initialize S3 storage with the configuration settings
var S3Client = require('nodejs-s3client').getInstance();

module.exports = {

    cdnStorage: S3Client.Storage,
    secureStorage: S3Client.SecureStorage,

    /*
     * All image processing is done inside queue, to preserve system
     * resources by limiting number of concurent opened child processes
     * (each image processing starts external lib in a new child process)
     */
    imageProcessingQueue: async.queue(gmWorker, Config.mediaConfiguration.imageProcessingQueueSize),

    getMediaPrefix: function (media) {
      if (_.hasIn(media, 'id') && _.hasIn(media, 'namespace')) {
        var fileinfo = nodepath.parse(media.id);
        return media.namespace + "/" + fileinfo.name;
      } else {
        throw new Error('missing media id or namespace');
      }
    },

    getMediaKey: function (media) {
      if (_.hasIn(media, 'id') && _.hasIn(media, 'namespace')) {
        return media.namespace + "/" + media.id;
      } else {
        throw new Error('missing media id or namespace');
      }
    },

    addMedia: function (params, callback) {
      var self = this;

      try {
          self.cdnStorage.uploadObject(self.getMediaKey(params),
              fs.createReadStream(params._fileInfo.path),
                 function (evt) { // progress callback
              },
              function(err, data) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess(null, callback);
              });
      } catch (e) {
          return CrudHelper.callbackError(e, callback);
      }
    },

    /**
     * @param media
     */
    createMetadataCDNObjectForMedia: function(media) {
        //Create object
        var metaDataCDNObjectForMedia = {
            title: media.title,
            description: media.description,
            source: media.source,
            copyright: media.copyright,
            licence: media.licence,
            usageInformation: media.usageInformation,
            metaData: media.metaData
        };
        return metaDataCDNObjectForMedia;
    },

    uploadMetadataForMedia: function (media, callback) {
      var self = this;

      // Upload metadata file for object
      try {
        self.cdnStorage.uploadObject(self.getMediaPrefix(media) + ".metadata.json",
            JSON.stringify(self.createMetadataCDNObjectForMedia(media)),
            function (evt) { /* progress callback */ },
            function(err, data) {
              if (err) {
                  return CrudHelper.callbackError(err, callback);
              }
              return CrudHelper.callbackSuccess(null, callback);
            });
      } catch (e) {
        return CrudHelper.callbackError(e, callback);
      }
    },
    
    thumbnailInfoCompile: function (params, mediaConfiguration) {
      var tempFolder = Config.httpUpload.tempFolder;
      var fileType = params._generatedFileInfo.extension.substring(1);
      console.log('fileType', fileType);

      params._thumbnails = {};

      console.log('mediaConfiguration.imageTypes.indexOf(fileType)', mediaConfiguration.imageTypes.indexOf(fileType));
      if (mediaConfiguration.imageTypes.indexOf(fileType) >=0 ) {
          console.log('IN_IF');
        _.forEach(mediaConfiguration.namespace[params.namespace], function (resolutionConfig) {
          var metrics = _.split(resolutionConfig.resolution, 'x'),
              width = '', height = '';
          console.log('metrics', metrics);

          var filename = mediaConfiguration.fileFormatTemplate.replace(/%BASENAME%/, params._generatedFileInfo.basename)
                              .replace(/%LABEL%/, resolutionConfig.label)
                              .replace(/%EXTENSION%/, params._generatedFileInfo.extension);
          console.log('filename', filename);

          width = _.toInteger(metrics[0]);
          height = _.toInteger(metrics[1]);
          console.log('width', width);
          console.log('height', height);

          params._thumbnails[resolutionConfig.label] = {
            'path': tempFolder + '/' + filename,
            'filename': params.namespace + '/' + filename,
            'width': width,
            'height': height
          }
          console.log('resolutionConfig',resolutionConfig);
          console.log('resolutionConfig.label',resolutionConfig.label);
          console.log('params',params);
          console.log('params',params);
          console.log('params._thumbnails',params._thumbnails);
          console.log('params._thumbnails[resolutionConfig.label]',params._thumbnails[resolutionConfig.label]);
        });
      }
    },

    convertOriginalFileToJpg: function (params, callback) {
      var rePNG = new RegExp(".+\\.png$");
      var self = this;
      var tempFolder = Config.httpUpload.tempFolder;

      try {
        params._originalTempPath = tempFolder + '/' + params.id;
        var image;
        
        // Convert to jpg image before pushing to storage
        var uploadedImagePathOrBuffer = params._fileInfo.path;
        if (_.isUndefined(params._fileInfo.path)) {
          logger.warn("httpMediaApi:updateProfilePicture Upload file path missing", JSON.stringify(params._fileInfo));
          if (_.has(params._fileInfo, 'destination') && _.has(params._fileInfo, 'filename')) {
            uploadedImagePathOrBuffer = params._fileInfo.destination + "/" + params._fileInfo.filename;
          } else {
            return CrudHelper.callbackError(Errors.MediaApi.FatalError, callback);
          }
        }
        if (rePNG.test(params._originalTempPath) && params.mayBePng) {    //dont convert png files "app" and "gameType" namespaces
            try {
                var fs = require('fs');
                fs.createReadStream(uploadedImagePathOrBuffer).pipe(fs.createWriteStream(params._originalTempPath));
                return CrudHelper.callbackSuccess(null, callback);
            } catch(e) {
                logger.debug("httpMediaApi:updateProfilePicture error:", e);
                return CrudHelper.callbackError(e, callback);
            }
        } else {
            self.imageProcessingQueue.push({
                sourcePath: uploadedImagePathOrBuffer,
                destPath: params._originalTempPath
            }, function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess(null, callback);
            });
        }
      } catch(e) {
        return CrudHelper.callbackError(e, callback);
      }
    },

    thumbnailsGenerate: function (params, callback) {
      var self = this;
      try {
        async.map(params._thumbnails, function(thumbnail, next){
          if (thumbnail.width > 0 || thumbnail.height > 0) {
            // generate thumb
            self.imageProcessingQueue.push({
              sourcePath: params._fileInfo.path,
              destPath: thumbnail.path,
              width: thumbnail.width,
              height: thumbnail.height
            }, next);
          } else {
            next();
          }
        }, function(err, result) {
          if (err) {
            logger.error("Could not generate thumbnail " + err);
            return CrudHelper.callbackError(err, callback);
          }

          return CrudHelper.callbackSuccess(null, callback);
        });
      } catch (e) {
          return CrudHelper.callbackError(e, callback);
      }
    },

    uploadToStorage: function (params, callback) {
      var self = this,
          thumbnailData = _.clone(params._thumbnails);

      thumbnailData['_original'] = {
        "path": params._originalTempPath,
        "filename": self.getMediaKey(params)
      };

      try {
        async.map(thumbnailData, function(thumbnail, next){
          try {
            self.cdnStorage.uploadObject(
                thumbnail.filename,
                fs.createReadStream(thumbnail.path),
                function () {}, // progress callback
                function (err, data) {
                  if (err) {
                      return next(err);
                  }

                  return next(false);
                }
            );
          } catch (e) {
              return next(e);
          }
        }, function(err, result) {
          if (err) {
            logger.error("Could not upload file to storage " + err);
            return CrudHelper.callbackError(err, callback);
          }

          return CrudHelper.callbackSuccess(null, callback);
        });
      } catch (e) {
          return CrudHelper.callbackError(e, callback);
      }
    },

    convertProfilePictureToJpgAndUpload: function(params, callback) {
        console.log('>>>convertProfilePictureToJpgAndUpload>>>');
      var self = this;
      var tempFolder = Config.httpUpload.tempFolder;

      try {
          console.log('self',self);
          console.log('tempFolder',tempFolder);
        params._originalTempPath = tempFolder + '/' + params.id;
        var image;
        
        // Convert to jpg image before pushing to storage
        var uploadedImagePathOrBuffer = params._fileInfo.path;
          console.log('uploadedImagePathOrBuffer',uploadedImagePathOrBuffer)
          console.log('params._fileInfo.path',params._fileInfo)
        if (_.isUndefined(params._fileInfo.path)) {
          logger.warn("httpMediaApi:updateProfilePicture Upload file path missing", JSON.stringify(params._fileInfo));
          if (_.has(params._fileInfo, 'destination') && _.has(params._fileInfo, 'filename')) {
            uploadedImagePathOrBuffer = params._fileInfo.destination + "/" + params._fileInfo.filename;
          } else {
              console.log('convertProfilePictureToJpgAndUpload ERROR');
            return CrudHelper.callbackError(Errors.MediaApi.FatalError, callback);
          }
        }

        async.series([
            //validate image size
            function (next) {
                console.log(1,'validate image size');
              self.imageProcessingQueue.push({
                sourcePath: uploadedImagePathOrBuffer
              }, next);
            },

            // write file to disk
            function (next) {
                console.log(2,'write file to disk');
              self.imageProcessingQueue.push({
                sourcePath: uploadedImagePathOrBuffer,
                destPath: params._originalTempPath
              }, next);
            },

            // Create the thumbnail files on local disk
            function (next) {
                console.log(3,'Create the thumbnail files on local disk');
                try {
                    self.thumbnailsGenerate(params, next);
                } catch (e) {
                    logger.error("httpMediaApi.convertProfilePictureToJpgAndUpload thumbnail generation failed",
                      JSON.stringify(params), e);
                    return next(Errors.MediaApi.FatalError);
                  }
            },

            // Upload to storage
            function (next) {
                console.log(4,'Upload to storage');
                try {
                    self.uploadToStorage(params, next);
                } catch (e) {
                    logger.error("httpMediaApi.addMedia upload to storage failed",
                        JSON.stringify(params), e);
                    return next(Errors.MediaApi.FatalError);
                }
            },
            function (next) {
                console.log(5,'fs.unlink');
              fs.unlink(params._originalTempPath, function (err) {
                  if(err) {
                    logger.warn('Profile image from tmp path ' + params._originalTempPath + ' could not be deleted.');
                    return next(err);
                  }
                  return next(false);
              });
            },
            // cleanup
            function (next) {
                console.log(6,'cleanup');
              try {
                self.cleanupTemporaryFiles(params, next);
              } catch (e) {
                return next(Errors.MediaApi.FatalError);
              }
            },
          // read uploaded image
        ], function (err, data) {
            console.log(7,'read uploaded image');
            console.log('err', err);
          if (err) {
            return CrudHelper.callbackError(err, callback);
          }
          return CrudHelper.callbackSuccess(null, callback);
        });
      } catch(e) {
          console.log(8,'catch');
        return CrudHelper.callbackError(e, callback);
      }
    },

    // Delete pictures from storage and all their associated data
    deleteFromStorage: function (media, callback) {
      console.log('deleteFromStorage BEGIN');
      var self = this;

      self.cdnStorage.listObjects({"Prefix": self.getMediaPrefix(media)}, function (err, data){
          console.log('deleteFromStorage before IF', data, data.Contents);
        if (data.Contents.length > 0 && data.Contents.length < 20) {
          var s3filelist = _.map(data.Contents, 'Key');
          // also cleanup
          return self.cdnStorage.deleteObjects({objectKeys: s3filelist}, callback);
        } else {
          logger.error("uploadApiFactory.deleteFromStorage No media to delete or media cleanup limit reached " + data.Contents.length);
          return CrudHelper.callbackSuccess(null, callback);
        }
     });
    },

    cleanupTemporaryFiles: function (params, callback) {
      var self = this,
          tempFolder = Config.httpUpload.tempFolder;

      try {
        async.each(params._thumbnails, function(thumbnail, next) {
          fs.unlink(thumbnail.path, function (err) {
              if(err) {
                  logger.warn('uploadApiFactory.cleanupTemporaryFiles Thumbnail from path ' + thumbnail.path +' could not be deleted.');
                  return next(err);
              }
              return next(false);
          });
        }, function(err, result) {
          if (err) {
            logger.warn("uploadApiFactory.cleanupTemporaryFiles error deleting files from tmp storage " + err);
          }
          return CrudHelper.callbackSuccess(null, callback);
        });
      } catch (e) {
        if (e) {
          logger.warn("cleanupTemporaryFiles - Could not cleanup files from tmp storage " + err);
        }
        return CrudHelper.callbackSuccess(null, callback);
      }
    },

    uploadSecureStorage: function (params, callback) {
      var self = this;

      try {
          self.secureStorage.uploadObject(params.key,
              fs.createReadStream(params.path),
                 function (evt) { // progress callback
              },
              function(err, data) {
                if (err) {
                    logger.error("uploadApiFactory:uploadSecureStorage failed", 
                        JSON.stringify(params), err);
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess(null, callback);
              });
      } catch (e) {
          return CrudHelper.callbackError(e, callback);
      }
    }
};

function gmWorker(task, callback) {
  try {
      console.log('gmWorker>>>')
      console.log('_.isObject(task)',_.isObject(task))
      console.log('_.has(task, \'sourcePath\')',_.has(task, 'sourcePath'))
      console.log('task',task)
    if (_.isObject(task) && _.has(task, 'sourcePath')) {
      var processImageWithTimeout = async.timeout(processImage, Config.mediaConfiguration.imageProcessingQueueTimeout);
      console.log('processImageWithTimeout', processImageWithTimeout);
      processImageWithTimeout(task, callback);
    } else {
        console.log('gmWorker error new Error()')
      throw new Error();
    }
  } catch (err) {
      console.log('gmWorker error', err)
    return callback(err);
  }
}

function processImage(task, callback) {
    console.log('processImage>>>>');
  var image = gm(task.sourcePath);
  console.log('image',image);
  console.log('task',task);
  if (_.has(task, 'destPath')) {
    image = image.strip() //strip any EXIF from original
    .setFormat('JPEG')
    .quality(Config.mediaConfiguration.imageQuality) //quality reduction from config
    .interlace('Plane') //this generates progressive image 
    /*
      NOTE: main reason for choosing imagemagick against graphicsmagick is
      that imagemagick produce progressive image using both interlace('Line') and interlace('Plane')
      while graphicsmagic produce progressive image using only interlace('Line')
      - interlace('Plane') is superior - image appears blurred first but entirely
      while interlace('Line') makes image load line by line
      - however, plane interlacing shall not be supported by JPEG,
      so is possible that both libraries produces same progressive effects
    */
    ;
    console.log('_.has(task, \'destPath\')',_.has(task, 'destPath'));
    console.log('image',image)
    console.log('task',task)
    if (_.has(task, 'width') && _.has(task, 'height')) {
        console.log('_.has(task, \'width\') && _.has(task, \'height\')');
        image = image.resize(
            task.width > 0 ? task.width : null,
            task.height > 0 ? task.height : null
          );
        console.log('image',image);
        console.log('task',task);
    }
    image.write(task.destPath, callback);
    console.log('image.write(task.destPath, callback);');
      console.log('image',image);
  } else {
      console.log('before image.size(function(err, size)>>');
    image.size(function(err, size) {
        console.log('In image.size callback');
        console.log('err',err);
      if (!err && (size.width > 1024 || size.height > 768)) {
        err = 'ERR_VALIDATION_FAILED';
      }
        console.log('err',err);
      setImmediate(callback, err);
    });
  }
};