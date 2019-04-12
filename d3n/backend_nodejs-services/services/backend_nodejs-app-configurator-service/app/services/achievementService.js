var _ = require('lodash');
var csvParser = require('csv-parse');

// Initialize S3 storage with the configuration settings
var S3Client = require('nodejs-s3client').getInstance();

module.exports = {

    secureStorage: S3Client.SecureStorage,

    load: function (key, callback) {
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

    import: function (key, callback) {
        var self = this;
        try {
            self.load(key, function (err, data) {
                if (err) {
                    return setImmediate(callback, err);
                }
                return csvParser(data, { comment: '#' }, function (err, output) {
                    if (err) {
                        return setImmediate(callback, err);
                    }
                    try {
                        var data = [];
                        _.forEach(output, function (line) {
                            var item = {
                                tenantId: parseInt(line[0]),
                                name: line[1],
                                description: line[2],
                                status: 'inactive',
                                type: line[3],
                                time: line[4],
                                timePeriod: parseInt(line[5]),
                                imageId: line[6],
                                usage: line[7],
                                isReward: parseInt(line[8]),
                                bonusPointsReward: parseInt(line[9]),
                                creditReward: parseInt(line[10]),
                                paymentMultiplier: parseFloat(line[11]),
                                accessRules: JSON.parse(line[12]),
                                earningRules: JSON.parse(line[13]),
                                messaging: JSON.parse(line[14]),
                                createDate: _.now()
                            };
                            data.push(item);
                        });
                        return setImmediate(callback, null, data);
                    } catch (ex) {
                        return setImmediate(callback, ex);
                    }
                });
            });
        } catch (ex) {
            return setImmediate(callback, ex);
        }
    },

    export: function (key, object, progress, callback) {
        var self = this;
        try {
            return self.cdnStorage.uploadObject(key, JSON.stringify(object),
                function (evt) {
                    return progress(evt);
                },
                function (err, data) {
                    return callback(err, data);
                }
            );
        } catch (ex) {
            return setImmediate(callback, ex);
        }
    },

    delete: function (key, callback) {
        var self = this;
        return self.secureStorage.deleteObjects({
            objectKeys: [key]
        }, callback);
    }
};
