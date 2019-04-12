var _ = require('lodash');
var utf8 = require('utf8');
var crypto = require('crypto');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var logger = require('nodejs-logger')();

// Initialize S3 storage with the configuration settings
var S3Client = require('nodejs-s3client');
var S3ClientInstance = S3Client.getInstance(_.merge(S3Client.Config, { amazon_s3: { bucket: Config.blob.inputBucket }, secure_s3: { bucket: Config.blob.outputBucket } }));

module.exports = {
    cdnMediaStorage: S3ClientInstance.Storage,

    encryptData: function (object, objectKey, encryptionKey, callback) {
        var self = this;
        var stringifiedObject = JSON.stringify(object).toString('utf8');
        stringifiedObject = utf8.encode(stringifiedObject);
        return self.encrypt(stringifiedObject, encryptionKey, callback);
    },
    
    encryptObject: function( objectKey, encryptionKey, callback ) {
        var self = this;
        try {
            logger.debug('encryptObject getObject objectKey:',objectKey,' encryptionKey:',encryptionKey,' Config.blob.inputBucket:"',Config.blob.inputBucket,'" Config.blob.outputBucket:"',Config.blob.outputBucket);
            return self.getObject(objectKey, function (err, object) {
                if (err) {
                    logger.error('encryptObject getObject error: ', err);
                    return setImmediate(callback, err);
                }
                return self.encrypt(object, encryptionKey, callback);
            });
        } catch (ex) {
            logger.error('encryptObject objectKey:',objectKey,' encryptionKey:',encryptionKey,'  Config.blob.inputBucket:"',Config.blob.inputBucket,'" Config.blob.outputBucket:"',Config.blob.outputBucket,'" error: ', ex);
            return setImmediate(callback, ex);
        }
    },

    getObject: function( objectKey, callback ) {
        var self = this;
        try {
            return self.cdnMediaStorage.getObjectAsBuffer({ objectId: objectKey }, function (err, object) {
                if (err) {
                    return callback(err);
                }
                return callback(null, object);
            });
        } catch (ex) {
            logger.error('getObject Config.blob.inputBucket:"',Config.blob.inputBucket,'" Config.blob.outputBucket:"',Config.blob.outputBucket,'" error: ', ex);
            return setImmediate(callback, ex);
        }
    },

    encrypt: function (object, encryptionKey, callback) {
        var self = this;
        try {
            const cipher = crypto.createCipheriv(Config.blob.cipher, encryptionKey, Config.blob.iv);
            var encryptedBlob = new Buffer('', 'utf8');
            cipher.on('readable', function () {
                var data = cipher.read();
                if (data) {
                    encryptedBlob = Buffer.concat([encryptedBlob, data]);
                }
            });
            cipher.on('end', function () {
                callback(null, encryptedBlob);
            });
            cipher.write(object);
            cipher.end();
        } catch (ex) {
            logger.error('encrypt Config.blob.inputBucket:"',Config.blob.inputBucket,'" Config.blob.outputBucket:"',Config.blob.outputBucket,'" error: ', ex);
            return setImmediate(callback, ex);
        }
    },

    decrypt: function (buffer, encryptionKey) {
        var decipher = crypto.createDecipheriv(Config.blob.cipher, encryptionKey, Config.blob.iv);
        decipher.write(buffer);
        decipher.end();
        var dec = decipher.read().toString();
        return JSON.parse(dec);
    },

    generateEncryptionKey: function () {
        return crypto.randomBytes(16);
    },

};
