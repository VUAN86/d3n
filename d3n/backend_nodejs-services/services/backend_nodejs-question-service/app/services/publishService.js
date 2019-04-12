var _ = require('lodash');
var Config = require('./../config/config.js');
var streamBuffers = require('stream-buffers');
var logger = require('nodejs-logger')();

// Initialize S3 storage with the configuration settings
var S3Client = require('nodejs-s3client');
var S3ClientInstance = S3Client.getInstance(_.merge(S3Client.Config, { amazon_s3: { bucket: Config.blob.inputBucket }, secure_s3: { bucket: Config.blob.outputBucket } }));

module.exports = {
    cdnEncryptedStorage: S3ClientInstance.SecureStorage,

    publishMedia: function (media, progress, callback) {
        var self = this;
        try {
            return self.uploadObject(media.encryptedMedia, media.encryptedMediaName,
            function (evt) {
                progress(evt);
            },
            function(err, data) {
                return callback(err, data)
            });
        } catch (ex) {
            logger.error('publishMedia Config.blob.inputBucket:"',Config.blob.inputBucket,'" Config.blob.outputBucket:"',Config.blob.outputBucket,'" error: ', ex);
            return setImmediate(callback, ex);
        }
    },

    publishQuestionStep: function(step, progress, callback) {
        var self = this;
        try {
            return self.uploadObject(step.encryptedStep, step.encryptedStepName,
            function (evt) {
                progress(evt);
            },
            function(err, data) {
                return callback(err, data)
            });
        } catch (ex) {
            logger.error('publishQuestionStep Config.blob.inputBucket:"',Config.blob.inputBucket,'" Config.blob.outputBucket:"',Config.blob.outputBucket,'" error: ', ex);
            return setImmediate(callback, ex);
        }
    },

    unpublishObjects: function(keys, callback) {
        var self = this;
        try {
            return self.deleteObjects({objectKeys: keys}, function(err, list) {
                return callback(err, list)
            });
        } catch (ex) {
            logger.error('unpublishObjects Config.blob.inputBucket:"',Config.blob.inputBucket,'" Config.blob.outputBucket:"',Config.blob.outputBucket,'" error: ', ex);
            return setImmediate(callback, ex);
        }
    },

    deleteObjects: function(keys, callback) {
        var self = this;
        try {
            return self.cdnEncryptedStorage.deleteObjects(keys, function(err, list) {
                return callback(err, list)
            });
        } catch (ex) {
            logger.error('deleteObjects Config.blob.inputBucket:"',Config.blob.inputBucket,'" Config.blob.outputBucket:"',Config.blob.outputBucket,'" error: ', ex);
            return setImmediate(callback, ex);
        }
    },

    uploadObject: function(object, objectName, progress, callback) {
        var self = this;
        try {
            var objectStream = new streamBuffers.WritableStreamBuffer({
                initialSize: (100 * 1024),   // start at 100 kilobytes. 
                incrementAmount: (10 * 1024) // grow by 10 kilobytes each time buffer overflows.
            });
            objectStream.write(object);
            return self.cdnEncryptedStorage.uploadObject(objectName,
                objectStream.getContents(),
                function (evt) { // progress callback
                },
                function(err, data) {
                    return callback(err, data);
                });
        } catch (ex) {
            logger.error('uploadObject Config.blob.inputBucket:"',Config.blob.inputBucket,'" Config.blob.outputBucket:"',Config.blob.outputBucket,'" error: ', ex);
            return setImmediate(callback, ex);
        }
    },

    getResolutions: function (namespace) {
        var resolutions = S3ClientInstance.Config.mediaConfiguration.namespace[namespace];
        if (_.isArray(resolutions)) {
            return resolutions;
        }
        return [];
    },

    getMediaFileFormatTemplate: function () {
        return S3ClientInstance.Config.mediaConfiguration.fileFormatTemplate;
    },

};
