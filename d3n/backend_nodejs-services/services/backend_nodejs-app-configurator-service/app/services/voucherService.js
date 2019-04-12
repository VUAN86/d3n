var _ = require('lodash');
var csvParser = require('csv-parse');

// Initialize S3 storage with the configuration settings
var S3Client = require('nodejs-s3client').getInstance();

module.exports = {

    secureStorage: S3Client.SecureStorage,

    loadCodes: function (key, callback) {
        var self = this;
        try {
            self.secureStorage.getObjectAsBuffer({
                objectId: key
            }, function (err, data) {
                if (err) {
                    return setImmediate(callback, err);
                }
                return setImmediate(callback, null, data);
            });
        } catch (ex) {
            return setImmediate(callback, ex);
        }
    },

    getCodes: function (key, callback) {
        var self = this;
        try {
            self.loadCodes(key, function (err, data) {
                if (err) {
                    return setImmediate(callback, err);
                }
                return csvParser(data, { comment: '#' }, function (err, output) {
                    if (err) {
                        return setImmediate(callback, err);
                    }
                    try {
                        var codes = [];
                        _.forEach(output, function (line) {
                            codes.push({ code: line[0], expirationDate: line[1] });
                        });
                        return setImmediate(callback, null, codes);
                    } catch (ex) {
                        return setImmediate(callback, ex);
                    }
                });
            });
        } catch (ex) {
            return setImmediate(callback, ex);
        }
    },

    deleteCodes: function (key, callback) {
        var self = this;
        return self.secureStorage.deleteObjects({
            objectKeys: [key]
        }, callback);
    }
};
