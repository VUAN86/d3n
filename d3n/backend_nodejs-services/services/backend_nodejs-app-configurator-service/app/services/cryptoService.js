var _ = require('lodash');
var crypto = require('crypto');
var Config = require('./../config/config.js');

// Initialize S3 storage with the configuration settings
var S3Client = require('nodejs-s3client');
var storage = S3Client.getInstance(_.merge(S3Client.Config, {amazon_s3: {bucket: Config.blob.bucket}})).Storage;

module.exports = {
    cdnStorage: storage,
    encryptData: function( object, objectKey, callback ) {
        var self = this;
        self.encrypt(object, callback);
    },
    
    encryptObject: function( objectKey, callback ) {
        var self = this;
        try {
            self.getObject(objectKey, function (err, object) {
                if (err) {
                    return setImmediate(callback, err);
                }
                return self.encrypt(object, callback);
            });
        } catch (ex) {
            return setImmediate(callback, ex);
        }
    },

    getObject: function( objectKey, callback ) {
        var self = this;
        self.cdnStorage.getObjectAsBuffer({ objectId: objectKey }, function (err, object) {
            if (err) {
                return callback(err);
            }
            return callback(null, object);
        });
    },

    encrypt: function( object, callback ) {
        try {
            var encryptionKey = crypto.randomBytes(128);
            var cipher = crypto.createCipher(Config.blob.cipher, encryptionKey);

            var encryptedBlob = new Buffer('');

            cipher.on('readable', function () {
                var data = cipher.read();
                if (data) {
                    encryptedBlob = Buffer.concat([encryptedBlob, data]);
                }
            });

            cipher.on('end', function () {
                callback(null, encryptedBlob, encryptionKey);
            });

            cipher.write(object);
            cipher.end();
        } catch (ex) {
            return setImmediate(callback, ex);
        }
    }

};
