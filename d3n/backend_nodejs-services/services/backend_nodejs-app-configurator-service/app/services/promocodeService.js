var _ = require('lodash');
var csvParser = require('csv-parse');
var passwordGenerator = require('generate-password');

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

    getCodes: function (id, key, callback) {
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
                            codes.push({
                                number: line[0] === 'null' ? 1 : parseInt(line[0]),
                                isUnique: line[1] === 'true',
                                code: line[2] === 'null' ? self.generateRandom(id) : line[2],
                                isQRCodeRequired: line[3] === 'true',
                                numberOfUses: line[4] === 'null' ? 1 : parseInt(line[4])
                            });
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
    },

    generateRandom: function (id) {
        var promocode = passwordGenerator.generate({
            length: 10,
            numbers: true,
            uppercase: true,
            symbols: false
        }) + id.toString();
        return promocode.toUpperCase();
    }
};
