var _ = require('lodash');
var Config = require('./../config/config.js');
var async = require('async');

// Initialize S3 storage with the configuration settings
var S3Client = require('nodejs-s3client');
var storage = S3Client.getInstance(_.merge(_.merge({}, S3Client.Config), {amazon_s3: {bucket: Config.advertisementPublish.bucket}})).Storage;

module.exports = {
    cdnStorage: storage,
    publish: function(advertisement, progress, callback) {
        var self = this;
        try {
            var fileName = self.buildPublishKey(advertisement);
            self.cdnStorage.uploadObject(fileName, JSON.stringify(advertisement),
            function (evt) {
                progress(evt);
            },
            function(err, data) {
                return callback(err, data);
            });
        } catch (ex) {
            return setImmediate(callback, ex);
        }
    },
    
    buildPublishKey: function (advertisement) {
        return 'provider_' + advertisement.advertisementProviderId + '_advertisement_' + advertisement.publishIdx + '.json';
    },
    
    unpublish: function (providerId, unpublishIdx, listLength, callback) {
        var self = this;
        try {
            if (unpublishIdx === listLength) { // only delete last file in list
                var fileNameToDelete = 'provider_' + providerId + '_advertisement_' + unpublishIdx + '.json';
                self.cdnStorage.deleteObjects({
                    objectKeys: [fileNameToDelete]
                }, callback);
                
            } else {
                var lastFile = 'provider_' + providerId + '_advertisement_' + listLength + '.json';
                var fileNameToDelete = 'provider_' + providerId + '_advertisement_' + unpublishIdx + '.json';
                
                var lastFileContent = '';
                async.series([
                    function (next) {
                        self.cdnStorage.getObjectAsBuffer({
                            objectId: lastFile
                        }, function (err, data) {
                            if (err) {
                                return next(err);
                            }
                            
                            lastFileContent = data;
                            return next();
                        });
                    },
                    
                    function (next) {
                        self.cdnStorage.deleteObjects({
                            objectKeys: [fileNameToDelete, lastFile]
                        }, next);
                    },
                    
                    function (next) {
                        self.cdnStorage.uploadObject(fileNameToDelete, lastFileContent,
                        function (evt) {
                            progress(evt);
                        },
                        function(err, data) {
                            return next(err, data);
                        });
                    }
                ], callback);
                
            }
        } catch (ex) {
            return setImmediate(callback, ex);
        }
    },
    
    getS3Client: function () {
        return this.cdnStorage;
    }
    
};
